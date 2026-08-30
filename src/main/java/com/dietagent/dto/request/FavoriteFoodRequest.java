package com.dietagent.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FavoriteFoodRequest {

    @NotBlank(message = "食物名称不能为空")
    @Size(max = 100, message = "食物名称不能超过100字符")
    private String foodName;

    @Size(max = 30, message = "分类不能超过30字符")
    private String category;

    @Size(max = 200, message = "备注不能超过200字符")
    private String note;

    @Min(value = 0, message = "热量不能为负数")
    private Integer caloriesPer100g;

    @DecimalMin(value = "0", message = "蛋白质不能为负数")
    private BigDecimal proteinPer100g;

    @DecimalMin(value = "0", message = "脂肪不能为负数")
    private BigDecimal fatPer100g;

    @DecimalMin(value = "0", message = "碳水不能为负数")
    private BigDecimal carbPer100g;
}
