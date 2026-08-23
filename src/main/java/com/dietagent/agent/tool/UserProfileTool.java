package com.dietagent.agent.tool;

import com.dietagent.entity.User;
import com.dietagent.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class UserProfileTool implements Function<UserProfileTool.Request, UserProfileTool.Response> {

    private final UserService userService;

    @Override
    public Response apply(Request request) {
        User user = userService.getUserById(request.userId);

        String info = String.format(
                "用户信息：\n" +
                "- 用户ID: %d\n" +
                "- 昵称: %s\n" +
                "- 身高: %s cm\n" +
                "- 体重: %s kg\n" +
                "- 年龄: %d岁\n" +
                "- 性别: %s\n" +
                "- 运动水平: %s\n" +
                "- 目标类型: %s\n" +
                "- 每日目标: 卡路里 %dkcal, 蛋白质 %dg, 脂肪 %dg, 碳水 %dg",
                user.getId(),
                user.getNickname() != null ? user.getNickname() : "未设置",
                user.getHeight() != null ? user.getHeight() : "未设置",
                user.getWeight() != null ? user.getWeight() : "未设置",
                user.getAge() != null ? user.getAge() : 0,
                user.getGender() != null ? user.getGender() : "未设置",
                user.getActivityLevel() != null ? user.getActivityLevel() : "未设置",
                user.getGoalType() != null ? user.getGoalType() : "未设置",
                user.getDailyCalorieGoal() != null ? user.getDailyCalorieGoal() : 0,
                user.getProteinGoal() != null ? user.getProteinGoal() : 0,
                user.getFatGoal() != null ? user.getFatGoal() : 0,
                user.getCarbGoal() != null ? user.getCarbGoal() : 0
        );

        return new Response(info);
    }

    public record Request(Long userId) {}
    public record Response(String userProfile) {}
}
