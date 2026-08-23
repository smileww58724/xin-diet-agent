package com.dietagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DietRecordRequest {

    @NotBlank(message = "食物名称不能为空")
    private String foodName;

    @NotBlank(message = "餐次类型不能为空")
    private String mealType;

    private String portionSize;

    private Integer calories;

    private BigDecimal protein;

    private BigDecimal fat;

    private BigDecimal carbohydrate;

    @NotNull(message = "用餐时间不能为空")
    private LocalDateTime mealTime;
}
