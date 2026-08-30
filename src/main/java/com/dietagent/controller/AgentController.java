package com.dietagent.controller;

import com.dietagent.agent.memory.ChatMemory;
import com.dietagent.dto.request.ChatRequest;
import com.dietagent.dto.response.AgentResponse;
import com.dietagent.dto.response.UsageStatsResponse;
import com.dietagent.exception.BusinessException;
import com.dietagent.ratelimit.ChatRateLimiter;
import com.dietagent.service.AgentUsageService;
import com.dietagent.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ChatService chatService;
    private final ChatRateLimiter chatRateLimiter;
    private final AgentUsageService agentUsageService;

    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(
            @AuthenticationPrincipal Long userId,
            @RequestBody ChatRequest request) {
        requireQuota(userId);
        String response = chatService.chat(userId, request.getMessage());
        return ResponseEntity.ok(AgentResponse.builder()
                .message(response)
                .type("success")
                .build());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chatStream(
            @AuthenticationPrincipal Long userId,
            @RequestBody ChatRequest request) {
        requireQuota(userId);
        Flux<String> flux = chatService.chatStream(userId, request.getMessage());
        return ResponseEntity.ok(flux);
    }

    @PostMapping("/chat/stop")
    public ResponseEntity<AgentResponse> stopGeneration(@AuthenticationPrincipal Long userId) {
        chatService.stopGeneration(userId);
        return ResponseEntity.ok(AgentResponse.builder()
                .message("已停止生成")
                .type("success")
                .build());
    }

    @DeleteMapping("/memory")
    public ResponseEntity<AgentResponse> clearMemory(
            @AuthenticationPrincipal Long userId) {
        chatService.clearMemory(userId);
        return ResponseEntity.ok(AgentResponse.builder()
                .message("对话历史已清除")
                .type("success")
                .build());
    }

    /** 当前用户的对话历史：前端切页返回时从持久化记忆恢复会话 */
    @GetMapping("/memory")
    public ResponseEntity<List<ChatMemory.ChatMessage>> history(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(chatService.getHistory(userId));
    }

    /** 当前用户的 AI 调用用量统计（成本看板数据源） */
    @GetMapping("/usage")
    public ResponseEntity<UsageStatsResponse> usage(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(agentUsageService.stats(userId));
    }

    /** AI 调用按 token 计费，进入业务逻辑前先做用户级限流 */
    private void requireQuota(Long userId) {
        if (!chatRateLimiter.tryAcquire(userId)) {
            throw new BusinessException("发送太频繁啦，喝口水休息一下，稍后再试");
        }
    }
}
