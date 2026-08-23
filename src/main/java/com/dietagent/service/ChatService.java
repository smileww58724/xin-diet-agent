package com.dietagent.service;

import com.dietagent.agent.memory.ChatMemory;
import com.dietagent.agent.prompt.DietAgentPrompt;
import com.dietagent.agent.tool.PexelsImageSearchTool;
import com.dietagent.dto.response.DietRecordResponse;
import com.dietagent.dto.response.NutritionSummaryResponse;
import com.dietagent.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatService {

    private final ChatClient deepSeekChatClient;
    private final UserService userService;
    private final DietRecordService dietRecordService;
    private final NutritionAnalysisService nutritionAnalysisService;
    private final PexelsImageSearchTool pexelsImageSearchTool;

    private final ConcurrentHashMap<Long, ChatMemory> userMemories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, StringBuilder> pendingResponses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Sinks.Many<String>> responseSinks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> stoppedStreams = new ConcurrentHashMap<>();

    public ChatService(
            ChatClient deepSeekChatClient,
            UserService userService,
            DietRecordService dietRecordService,
            NutritionAnalysisService nutritionAnalysisService,
            PexelsImageSearchTool pexelsImageSearchTool) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.userService = userService;
        this.dietRecordService = dietRecordService;
        this.nutritionAnalysisService = nutritionAnalysisService;
        this.pexelsImageSearchTool = pexelsImageSearchTool;
    }

    public Flux<String> chatStream(Long userId, String userMessage) {
        // 清除之前的停止标记
        stoppedStreams.remove(userId);

        User user = userService.getUserById(userId);
        ChatMemory memory = userMemories.computeIfAbsent(userId, ChatMemory::new);
        memory.addUserMessage(userMessage);

        String systemPrompt = DietAgentPrompt.getSystemPromptWithTools(user);
        String fullPrompt = buildFullPrompt(userId, systemPrompt, userMessage, memory);

        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        responseSinks.put(userId, sink);
        StringBuilder fullResponse = new StringBuilder();
        pendingResponses.put(userId, fullResponse);

        // 异步处理 AI 响应
        deepSeekChatClient.prompt()
                .system(fullPrompt)
                .user(userMessage)
                .stream()
                .content()
                .subscribe(
                        chunk -> {
                            fullResponse.append(chunk);
                            sink.emitNext(chunk, Sinks.EmitFailureHandler.FAIL_FAST);
                        },
                        error -> {
                            log.error("AI 流处理错误", error);
                            pendingResponses.remove(userId);
                            responseSinks.remove(userId);
                            stoppedStreams.remove(userId);
                            try {
                                sink.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST);
                            } catch (Exception ignored) {}
                        },
                        () -> {
                            // 检查是否被用户停止
                            if (stoppedStreams.containsKey(userId)) {
                                log.info("流已被用户停止，跳过图片搜索");
                                memory.addAssistantMessage(fullResponse.toString());
                                pendingResponses.remove(userId);
                                responseSinks.remove(userId);
                                stoppedStreams.remove(userId);
                                try {
                                    sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                                } catch (Exception ignored) {}
                                return;
                            }

                            // 流结束时处理图片搜索
                            String result = fullResponse.toString();
                            if (result.contains("[搜索图片:")) {
                                result = processImageSearch(result);
                                // 发送完整结果替换标记，前端会检测并更新
                                try {
                                    sink.emitNext("\n[FULL_RESULT]" + result, Sinks.EmitFailureHandler.FAIL_FAST);
                                } catch (Exception ignored) {}
                            }

                            memory.addAssistantMessage(result);
                            pendingResponses.remove(userId);
                            responseSinks.remove(userId);
                            stoppedStreams.remove(userId);
                            try {
                                sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                            } catch (Exception ignored) {}
                        }
                );

        return sink.asFlux();
    }

    public void stopGeneration(Long userId) {
        stoppedStreams.put(userId, true);
        log.info("用户 {} 停止了生成", userId);
    }

    public String chat(Long userId, String userMessage) {
        User user = userService.getUserById(userId);
        ChatMemory memory = userMemories.computeIfAbsent(userId, ChatMemory::new);
        memory.addUserMessage(userMessage);

        String systemPrompt = DietAgentPrompt.getSystemPromptWithTools(user);
        String fullPrompt = buildFullPrompt(userId, systemPrompt, userMessage, memory);

        String response = deepSeekChatClient.prompt()
                .system(fullPrompt)
                .user(userMessage)
                .call()
                .content();

        if (response.contains("[搜索图片:")) {
            response = processImageSearch(response);
        }
        memory.addAssistantMessage(response);
        return response;
    }

    private String processImageSearch(String text) {
        int start = text.indexOf("[搜索图片:");
        if (start == -1) return text;
        int end = text.indexOf("]", start);
        if (end == -1) return text;

        String searchQuery = text.substring(start + 6, end).trim();
        log.info("执行图片搜索: {}", searchQuery);

        try {
            PexelsImageSearchTool.Response result = pexelsImageSearchTool.apply(
                    new PexelsImageSearchTool.Request(searchQuery, 3));
            log.info("图片搜索完成，结果长度: {}", result.imageResults().length());
            return text.substring(0, start) + "\n" + result.imageResults();
        } catch (Exception e) {
            log.error("图片搜索失败", e);
            return text.replace("[搜索图片:" + searchQuery + "]", "\n[图片搜索失败]\n");
        }
    }

    private String buildFullPrompt(Long userId, String systemPrompt, String userMessage, ChatMemory memory) {
        String todayInfo = buildTodayInfo(userId);
        String userProfile = buildUserProfile(userId);
        String recentHistory = memory.getConversationHistory();
        return systemPrompt + "\n\n" + userProfile + "\n\n" + todayInfo + "\n\n对话历史：\n" + recentHistory;
    }

    private String buildUserProfile(Long userId) {
        try {
            User user = userService.getUserById(userId);
            StringBuilder sb = new StringBuilder();
            sb.append("=== 用户信息 ===\n用户ID: ").append(user.getId()).append("\n");
            if (user.getNickname() != null) sb.append("昵称: ").append(user.getNickname()).append("\n");
            if (user.getHeight() != null) sb.append("身高: ").append(user.getHeight()).append(" cm\n");
            if (user.getWeight() != null) sb.append("体重: ").append(user.getWeight()).append(" kg\n");
            if (user.getAge() != null) sb.append("年龄: ").append(user.getAge()).append(" 岁\n");
            if (user.getGender() != null) sb.append("性别: ").append(user.getGender()).append("\n");
            if (user.getActivityLevel() != null) sb.append("运动水平: ").append(user.getActivityLevel()).append("\n");
            if (user.getGoalType() != null) sb.append("目标: ").append(user.getGoalType()).append("\n");
            if (user.getDailyCalorieGoal() != null) sb.append("每日卡路里目标: ").append(user.getDailyCalorieGoal()).append(" kcal\n");
            if (user.getProteinGoal() != null) sb.append("蛋白质目标: ").append(user.getProteinGoal()).append(" g\n");
            if (user.getFatGoal() != null) sb.append("脂肪目标: ").append(user.getFatGoal()).append(" g\n");
            if (user.getCarbGoal() != null) sb.append("碳水目标: ").append(user.getCarbGoal()).append(" g\n");
            return sb.toString();
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return "用户信息: 获取失败";
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
        ChatMemory memory = userMemories.get(userId);
        if (memory != null) memory.clear();
    }
}
