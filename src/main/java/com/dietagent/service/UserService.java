package com.dietagent.service;

import com.dietagent.config.jwt.JwtTokenProvider;
import com.dietagent.dto.request.LoginRequest;
import com.dietagent.dto.request.RegisterRequest;
import com.dietagent.dto.response.LoginResponse;
import com.dietagent.entity.User;
import com.dietagent.exception.BusinessException;
import com.dietagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .token(token)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .token(token)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public User updateUserProfile(Long userId, User updateData) {
        User user = getUserById(userId);
        if (updateData.getNickname() != null) {
            user.setNickname(updateData.getNickname());
        }
        if (updateData.getHeight() != null) {
            user.setHeight(updateData.getHeight());
        }
        if (updateData.getWeight() != null) {
            user.setWeight(updateData.getWeight());
        }
        if (updateData.getAge() != null) {
            user.setAge(updateData.getAge());
        }
        if (updateData.getGender() != null) {
            user.setGender(updateData.getGender());
        }
        if (updateData.getActivityLevel() != null) {
            user.setActivityLevel(updateData.getActivityLevel());
        }
        if (updateData.getGoalType() != null) {
            user.setGoalType(updateData.getGoalType());
        }
        if (updateData.getDailyCalorieGoal() != null) {
            user.setDailyCalorieGoal(updateData.getDailyCalorieGoal());
        }
        if (updateData.getProteinGoal() != null) {
            user.setProteinGoal(updateData.getProteinGoal());
        }
        if (updateData.getFatGoal() != null) {
            user.setFatGoal(updateData.getFatGoal());
        }
        if (updateData.getCarbGoal() != null) {
            user.setCarbGoal(updateData.getCarbGoal());
        }
        return userRepository.save(user);
    }
}
