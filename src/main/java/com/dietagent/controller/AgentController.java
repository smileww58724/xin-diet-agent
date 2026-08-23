package com.dietagent.controller;

import com.dietagent.dto.request.ChatRequest;
import com.dietagent.dto.response.AgentResponse;
import com.dietagent.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(@RequestBody ChatRequest request) {
        String response = chatService.chat(request.getUserId(), request.getMessage());
        return ResponseEntity.ok(AgentResponse.builder()
                .message(response)
                .type("success")
                .build());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chatStream(@RequestBody ChatRequest request) {
        Flux<String> flux = chatService.chatStream(request.getUserId(), request.getMessage());
        return ResponseEntity.ok(flux);
    }

    @PostMapping("/chat/stop")
    public ResponseEntity<AgentResponse> stopGeneration(@RequestBody ChatRequest request) {
        chatService.stopGeneration(request.getUserId());
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
}
