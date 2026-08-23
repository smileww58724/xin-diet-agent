package com.dietagent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietRecordResponse {

    private Long id;
    private String foodName;
    private String mealType;
    private String portionSize;
    private Integer calories;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbohydrate;
    private LocalDateTime mealTime;
}
