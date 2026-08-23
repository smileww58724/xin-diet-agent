package com.dietagent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallEvent {
    private String type;  // "tool_call_start", "tool_call_end"
    private String toolName;
    private String result;
}
