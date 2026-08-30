package com.dietagent.service;

import com.dietagent.agent.memory.ChatMemory;
import com.dietagent.agent.memory.ChatMemoryStore;
import com.dietagent.agent.prompt.DietAgentPrompt;
import com.dietagent.agent.tool.DietAgentTools;
import com.dietagent.agent.tool.PexelsImageSearchTool;
import com.dietagent.dto.response.DietRecordResponse;
import com.dietagent.dto.response.NutritionSummaryResponse;
import com.dietagent.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.SignalType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChatService {

    /** 图片搜索标记：关键词内不允许出现 ']'，限制长度防止标记异常膨胀 */
    private static final Pattern IMAGE_MARK = Pattern.compile("\\[搜索图片:\\s*([^\\]]{1,100})\\]");

    private final ChatClient deepSeekChatClient;
    private final UserService userService;
    private final DietRecordService dietRecordService;
    private final NutritionAnalysisService nutritionAnalysisService;
    private final PexelsImageSearchTool pexelsImageSearchTool;
    private final ChatMemoryStore chatMemoryStore;
    private final AgentUsageService agentUsageService;

    /** 每用户当前一次流式生成的停止句柄；新请求直接覆盖旧句柄 */
    private final Map<Long, StopHandle> stopHandles = new ConcurrentHashMap<>();

    public ChatService(
            ChatClient deepSeekChatClient,
            UserService userService,
            DietRecordService dietRecordService,
            NutritionAnalysisService nutritionAnalysisService,
            PexelsImageSearchTool pexelsImageSearchTool,
            ChatMemoryStore chatMemoryStore,
            AgentUsageService agentUsageService) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.userService = userService;
        this.dietRecordService = dietRecordService;
        this.nutritionAnalysisService = nutritionAnalysisService;
        this.pexelsImageSearchTool = pexelsImageSearchTool;
        this.chatMemoryStore = chatMemoryStore;
        this.agentUsageService = agentUsageService;
    }

    /** 一次流式生成的停止状态：信号用于取消上游 AI 调用，标记用于收尾时跳过图片搜索 */
    private static final class StopHandle {
        final Sinks.Many<Boolean> stopSignal = Sinks.many().unicast().onBackpressureBuffer();
        final AtomicBoolean stopped = new AtomicBoolean(false);
    }

    public Flux<String> chatStream(Long userId, String userMessage) {
        User user = userService.getUserById(userId);
        // 先取历史快照再写入本轮消息：历史作为独立的按角色 message 传入，
        // 用户消息只经 .user() 发送一次（此前历史文本里已包含它，重复占用上下文 token）
        List<Message> history = toSpringAiMessages(chatMemoryStore.loadRecent(userId));
        chatMemoryStore.append(userId, "user", userMessage);

        String systemPrompt = DietAgentPrompt.getSystemPromptWithTools(user) + "\n\n" + buildTodayInfo(userId);

        // StringBuffer：客户端断开的取消信号来自其他线程，需与流线程的 append 并发安全
        StringBuffer fullResponse = new StringBuffer();
        StopHandle handle = new StopHandle();
        stopHandles.put(userId, handle);
        long startNanos = System.nanoTime();
        // DeepSeek 每个分片都携带累计 usage：只保留末次响应，流结束时统一记录一次，
        // 否则一次对话会写 N 条重复用量记录
        AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();

        // 直接返回 AI 流上的组合 Flux：客户端断开时取消信号沿链路传播，
        // 上游 DeepSeek 调用立即终止（手动 subscribe + sink 桥接的写法做不到这一点）
        return deepSeekChatClient.prompt()
                .system(systemPrompt)
                .messages(history)
                .user(userMessage)
                .tools(new DietAgentTools(userId, userService, dietRecordService, nutritionAnalysisService))
                .stream()
                .chatResponse()
                .doOnNext(lastResponse::set)
                .map(ChatService::extractText)
                .filter(text -> !text.isEmpty())
                // 收到停止信号立即取消上游 AI 调用并正常收尾，不再等模型把剩余内容生成完
                .takeUntilOther(handle.stopSignal.asFlux())
                .doOnNext(fullResponse::append)
                .doOnError(e -> log.error("AI 流处理错误", e))
                .concatWith(Mono.defer(() -> finishStream(userId, handle, fullResponse)))
                .doFinally(signal -> {
                    if (signal == SignalType.CANCEL && !fullResponse.isEmpty()) {
                        // 客户端直接断开（如关闭页面）：已生成的部分仍写入记忆，保持上下文连续
                        chatMemoryStore.append(userId, "assistant", fullResponse.toString());
                    }
                    recordUsage(userId, lastResponse.get(), elapsedMs(startNanos));
                    // 两参 remove：仅当仍是本次请求的句柄时才清理，避免误删新一轮会话的句柄
                    stopHandles.remove(userId, handle);
                });
    }

    /** 流正常结束（含被用户停止）后的收尾：写记忆，必要时拦截图片标记做全量替换 */
    private Mono<String> finishStream(Long userId, StopHandle handle, StringBuffer fullResponse) {
        String result = fullResponse.toString();
        // 记忆中保留原始标记而非替换后的图片 HTML，避免大段 HTML 占用上下文
        chatMemoryStore.append(userId, "assistant", result);
        if (handle.stopped.get() || !IMAGE_MARK.matcher(result).find()) {
            return Mono.empty();
        }
        return Mono.just("\n[FULL_RESULT]" + processImageSearch(result));
    }

    public void stopGeneration(Long userId) {
        StopHandle handle = stopHandles.get(userId);
        if (handle == null) {
            return;
        }
        handle.stopped.set(true);
        // 触发 takeUntilOther 取消上游 AI 调用；流已结束时此处无人订阅，结果可忽略
        handle.stopSignal.tryEmitNext(Boolean.TRUE);
        log.info("用户 {} 停止了生成", userId);
    }

    public String chat(Long userId, String userMessage) {
        User user = userService.getUserById(userId);
        List<Message> history = toSpringAiMessages(chatMemoryStore.loadRecent(userId));
        chatMemoryStore.append(userId, "user", userMessage);

        String systemPrompt = DietAgentPrompt.getSystemPromptWithTools(user) + "\n\n" + buildTodayInfo(userId);

        long startNanos = System.nanoTime();
        ChatResponse response = deepSeekChatClient.prompt()
                .system(systemPrompt)
                .messages(history)
                .user(userMessage)
                .tools(new DietAgentTools(userId, userService, dietRecordService, nutritionAnalysisService))
                .call()
                .chatResponse();
        recordUsage(userId, response, elapsedMs(startNanos));

        String content = extractText(response);
        if (IMAGE_MARK.matcher(content).find()) {
            content = processImageSearch(content);
        }
        chatMemoryStore.append(userId, "assistant", content);
        return content;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** 历史按角色转换为 Spring AI 消息序列（user/assistant），由框架按对话格式传给模型 */
    private List<Message> toSpringAiMessages(List<ChatMemory.ChatMessage> history) {
        return history.stream()
                .<Message>map(m -> "assistant".equals(m.getRole())
                        ? new AssistantMessage(m.getContent())
                        : new UserMessage(m.getContent()))
                .toList();
    }

    /** 记录每次调用的 token 用量（流式下仅当供应商在末尾分片携带 usage 时可得） */
    private void recordUsage(Long userId, ChatResponse response, long elapsedMs) {
        if (response == null || response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return;
        }
        Usage usage = response.getMetadata().getUsage();
        Integer total = usage.getTotalTokens();
        if (total == null || total == 0) {
            return; // 中间分片一般不携带 usage
        }
        Integer prompt = usage.getPromptTokens();
        Integer completion = usage.getCompletionTokens();
        log.info("AI 用量 userId={} model={} promptTokens={} completionTokens={} totalTokens={} 耗时={}ms",
                userId, response.getMetadata().getModel(),
                prompt != null ? prompt : 0, completion != null ? completion : 0, total, elapsedMs);
        agentUsageService.record(userId, response.getMetadata().getModel(), prompt, completion, total, elapsedMs);
    }

    private static String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text != null ? text : "";
    }

    /** 处理回答中的全部图片标记（此前只处理第一个，且关键词含 ']' 会截断） */
    private String processImageSearch(String text) {
        Matcher matcher = IMAGE_MARK.matcher(text);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        boolean replaced = false;
        while (matcher.find()) {
            sb.append(text, last, matcher.start())
              .append("\n")
              .append(searchImages(matcher.group(1).trim()));
            last = matcher.end();
            replaced = true;
        }
        if (!replaced) {
            return text;
        }
        return sb.append(text.substring(last)).toString();
    }

    private String searchImages(String query) {
        log.info("执行图片搜索: {}", query);
        try {
            PexelsImageSearchTool.Response result = pexelsImageSearchTool.apply(
                    new PexelsImageSearchTool.Request(query, 3));
            log.info("图片搜索完成，结果长度: {}", result.imageResults().length());
            return result.imageResults();
        } catch (Exception e) {
            log.error("图片搜索失败", e);
            return "[图片搜索失败]";
        }
    }

    private String buildTodayInfo(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            List<DietRecordResponse> todayRecords = dietRecordService.getRecords(userId, today);
            NutritionSummaryResponse summary = nutritionAnalysisService.getDailySummary(userId, today);

            StringBuilder sb = new StringBuilder();
            sb.append("=== 今日饮食信息 (").append(today.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(") ===\n\n");
            if (todayRecords.isEmpty()) {
                sb.append("今日还没有饮食记录。\n");
            } else {
                sb.append("今日饮食记录：\n");
                for (DietRecordResponse record : todayRecords) {
                    sb.append("- [").append(record.getMealType()).append("] ").append(record.getFoodName());
                    if (record.getPortionSize() != null) sb.append(" (").append(record.getPortionSize()).append(")");
                    sb.append(" | ").append(record.getCalories()).append(" kcal");
                    if (record.getProtein() != null) sb.append(" | 蛋白质: ").append(record.getProtein()).append("g");
                    sb.append("\n");
                }
            }
            sb.append("\n今日营养汇总：\n总热量: ").append(summary.getTotalCalories()).append(" / ").append(summary.getCalorieGoal()).append(" kcal\n");
            sb.append("蛋白质: ").append(summary.getTotalProtein()).append(" / ").append(summary.getProteinGoal()).append(" g\n");
            sb.append("脂肪: ").append(summary.getTotalFat()).append(" / ").append(summary.getFatGoal()).append(" g\n");
            sb.append("碳水: ").append(summary.getTotalCarbohydrate()).append(" / ").append(summary.getCarbGoal()).append(" g\n");
            sb.append("\n完成进度：\n");
            sb.append("- 热量: ").append(String.format("%.1f", summary.getCalorieProgress())).append("%\n");
            sb.append("- 蛋白质: ").append(String.format("%.1f", summary.getProteinProgress())).append("%\n");
            sb.append("- 脂肪: ").append(String.format("%.1f", summary.getFatProgress())).append("%\n");
            sb.append("- 碳水: ").append(String.format("%.1f", summary.getCarbProgress())).append("%");
            return sb.toString();
        } catch (Exception e) {
            log.error("获取今日饮食信息失败", e);
            return "今日饮食信息: 获取失败";
        }
    }

    public void clearMemory(Long userId) {
        chatMemoryStore.clear(userId);
    }
}
