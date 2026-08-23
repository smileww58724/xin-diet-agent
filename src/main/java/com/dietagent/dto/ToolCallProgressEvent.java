package com.dietagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallProgressEvent {
    private String toolName;
    private String status; // "start", "end"
    private String result;
}
