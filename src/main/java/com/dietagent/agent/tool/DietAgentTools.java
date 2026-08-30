package com.dietagent.agent.tool;

import com.dietagent.dto.request.DietRecordRequest;
import com.dietagent.dto.response.DietRecordResponse;
import com.dietagent.dto.response.NutritionSummaryResponse;
import com.dietagent.entity.User;
import com.dietagent.service.DietRecordService;
import com.dietagent.service.NutritionAnalysisService;
import com.dietagent.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * AI 数据查询工具集（原生 function calling，由 Spring AI 按 @Tool 注解暴露给模型）。
 * 每次对话请求创建一个新实例，userId 在构造时绑定当前登录用户：
 * 模型只能查询当前用户的数据，无法指定他人 ID，天然防止越权。
 */
@Slf4j
public class DietAgentTools {

    private final Long userId;
    private final UserService userService;
    private final DietRecordService dietRecordService;
    private final NutritionAnalysisService nutritionAnalysisService;

    public DietAgentTools(Long userId, UserService userService,
                          DietRecordService dietRecordService,
                          NutritionAnalysisService nutritionAnalysisService) {
        this.userId = userId;
        this.userService = userService;
        this.dietRecordService = dietRecordService;
        this.nutritionAnalysisService = nutritionAnalysisService;
    }

    @Tool(description = "查询当前用户指定日期的饮食记录列表。用于回答'我某天吃了什么'等问题")
    public String getDietRecords(
            @ToolParam(description = "查询日期，格式 yyyy-MM-dd；不传表示今天", required = false) String date) {
        LocalDate day = parseDateOrToday(date);
        List<DietRecordResponse> records = dietRecordService.getRecords(userId, day);
        // 工具层做兜底：任何异常路径都不能向模型抛裸异常，返回可转述的文本
        if (records == null || records.isEmpty()) {
            return day.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 暂无饮食记录";
        }
        StringBuilder sb = new StringBuilder();
        for (DietRecordResponse r : records) {
            sb.append(String.format("[%s] %s - %s kcal (蛋白质: %sg, 脂肪: %sg, 碳水: %sg)\n",
                    r.getMealType(), r.getFoodName(), r.getCalories(),
                    r.getProtein(), r.getFat(), r.getCarbohydrate()));
        }
        return sb.toString();
    }

    @Tool(description = "查询当前用户指定日期的单日营养摄入汇总（总热量/蛋白质/脂肪/碳水及相对目标的完成进度）")
    public String getNutritionSummary(
            @ToolParam(description = "查询日期，格式 yyyy-MM-dd；不传表示今天", required = false) String date) {
        LocalDate day = parseDateOrToday(date);
        return formatSummary(day.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 营养摄入汇总",
                nutritionAnalysisService.getDailySummary(userId, day));
    }

    @Tool(description = "查询当前用户从指定日期起连续 7 天的每周营养汇总")
    public String getWeeklySummary(
            @ToolParam(description = "起始日期，格式 yyyy-MM-dd；不传表示从今天开始", required = false) String startDate) {
        LocalDate start = parseDateOrToday(startDate);
        return formatSummary(start.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 起 7 天营养汇总",
                nutritionAnalysisService.getWeeklySummary(userId, start));
    }

    @Tool(description = "查询当前用户的个人资料与每日营养目标（身高/体重/年龄/性别/运动水平/目标类型及热量与三大营养素目标）")
    public String getUserProfile() {
        User user = userService.getUserById(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("用户ID: ").append(user.getId()).append("\n");
        sb.append("昵称: ").append(user.getNickname() != null ? user.getNickname() : "未设置").append("\n");
        sb.append("身高: ").append(user.getHeight() != null ? user.getHeight() + " cm" : "未设置").append("\n");
        sb.append("体重: ").append(user.getWeight() != null ? user.getWeight() + " kg" : "未设置").append("\n");
        sb.append("年龄: ").append(user.getAge() != null ? user.getAge() + " 岁" : "未设置").append("\n");
        sb.append("性别: ").append(user.getGender() != null ? user.getGender() : "未设置").append("\n");
        sb.append("运动水平: ").append(user.getActivityLevel() != null ? user.getActivityLevel() : "未设置").append("\n");
        sb.append("目标类型: ").append(user.getGoalType() != null ? user.getGoalType() : "未设置").append("\n");
        sb.append(String.format("每日目标: 卡路里 %d kcal, 蛋白质 %d g, 脂肪 %d g, 碳水 %d g",
                user.getDailyCalorieGoal() != null ? user.getDailyCalorieGoal() : 0,
                user.getProteinGoal() != null ? user.getProteinGoal() : 0,
                user.getFatGoal() != null ? user.getFatGoal() : 0,
                user.getCarbGoal() != null ? user.getCarbGoal() : 0));
        return sb.toString();
    }

    @Tool(description = "为当前用户记录一条饮食。当用户陈述吃了什么或喝了什么（如'我中午吃了一碗米饭和一个鸡蛋'）时调用；" +
            "热量与蛋白质/脂肪/碳水按常见食物营养估算，并在回复中注明是估算值；" +
            "食物或份量不明确时先向用户确认，不要凭空猜测")
    public String addDietRecord(
            @ToolParam(description = "食物名称，如'米饭''鸡胸肉'", required = true) String foodName,
            @ToolParam(description = "餐次：早餐/午餐/晚餐/加餐", required = true) String mealType,
            @ToolParam(description = "份量描述，如'一碗(约200g)'，可空", required = false) String portionSize,
            @ToolParam(description = "估算热量(kcal)", required = true) Integer calories,
            @ToolParam(description = "估算蛋白质(g)", required = false) Double protein,
            @ToolParam(description = "估算脂肪(g)", required = false) Double fat,
            @ToolParam(description = "估算碳水化合物(g)", required = false) Double carb,
            @ToolParam(description = "进食日期 yyyy-MM-dd，不传表示今天", required = false) String date) {
        LocalDate day = parseDateOrToday(date);
        String meal = normalizeMealType(mealType);

        DietRecordRequest request = new DietRecordRequest();
        request.setFoodName(foodName.trim());
        request.setMealType(meal);
        request.setPortionSize(portionSize);
        request.setCalories(calories);
        request.setProtein(toBigDecimal(protein));
        request.setFat(toBigDecimal(fat));
        request.setCarbohydrate(toBigDecimal(carb));
        request.setMealTime(mealTimeOf(day, meal));

        DietRecordResponse saved = dietRecordService.addRecord(userId, request);
        log.info("AI 记录饮食 userId={} recordId={} {} {} {} kcal", userId, saved.getId(), meal, foodName, calories);
        String portion = portionSize != null && !portionSize.isBlank() ? "（" + portionSize + "）" : "";
        return String.format("已记录：%s - %s%s，约 %s kcal（营养素为估算值）。记录编号 %d，可在饮食记录页面修改或删除。",
                meal, saved.getFoodName(), portion,
                saved.getCalories() != null ? saved.getCalories() : "未知", saved.getId());
    }

    /** 口语化餐次归一化：'午饭/中饭'→午餐，'下午茶'→加餐，无法识别归为加餐 */
    private String normalizeMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) {
            return "加餐";
        }
        if (mealType.contains("早")) return "早餐";
        if (mealType.contains("晚")) return "晚餐";
        // '下午茶' 含'午'但属于加餐
        if ((mealType.contains("午") && !mealType.contains("下")) || mealType.contains("中")) return "午餐";
        return "加餐";
    }

    /** 未指定具体时间时按餐次取常规进食时间，保证记录在正确的日期聚合口径内 */
    private LocalDateTime mealTimeOf(LocalDate day, String meal) {
        return switch (meal) {
            case "早餐" -> day.atTime(8, 0);
            case "午餐" -> day.atTime(12, 0);
            case "晚餐" -> day.atTime(18, 30);
            default -> day.atTime(15, 0);
        };
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private String formatSummary(String title, NutritionSummaryResponse s) {
        return String.format(
                "%s：\n" +
                "- 总热量: %d / %d kcal (进度: %.1f%%)\n" +
                "- 蛋白质: %.1fg / %dg (进度: %.1f%%)\n" +
                "- 脂肪: %.1fg / %dg (进度: %.1f%%)\n" +
                "- 碳水: %.1fg / %dg (进度: %.1f%%)",
                title,
                s.getTotalCalories(), s.getCalorieGoal(), s.getCalorieProgress(),
                s.getTotalProtein(), s.getProteinGoal(), s.getProteinProgress(),
                s.getTotalFat(), s.getFatGoal(), s.getFatProgress(),
                s.getTotalCarbohydrate(), s.getCarbGoal(), s.getCarbProgress());
    }

    private LocalDate parseDateOrToday(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            log.warn("工具收到无法解析的日期 '{}'，回退为今天", date);
            return LocalDate.now();
        }
    }
}
