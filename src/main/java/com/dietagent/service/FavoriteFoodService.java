package com.dietagent.service;

import com.dietagent.dto.request.FavoriteFoodRequest;
import com.dietagent.entity.FavoriteFood;
import com.dietagent.exception.BusinessException;
import com.dietagent.repository.FavoriteFoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户偏好食物库：AI 个性化推荐的 RAG 数据源。
 * 所有操作都做 userId 归属校验，防止越权读写他人偏好。
 */
@Service
@RequiredArgsConstructor
public class FavoriteFoodService {

    /** 清单软上限：条目都会注入 prompt，防止上下文无限膨胀 */
    private static final int MAX_ITEMS = 100;

    private final FavoriteFoodRepository repository;

    public List<FavoriteFood> list(Long userId) {
        return repository.findByUserIdOrderByIdDesc(userId);
    }

    public FavoriteFood add(Long userId, FavoriteFoodRequest request) {
        if (repository.countByUserId(userId) >= MAX_ITEMS) {
            throw new BusinessException("偏好清单已达上限（" + MAX_ITEMS + " 条），请先清理不常吃的");
        }
        if (repository.existsByUserIdAndFoodNameIgnoreCase(userId, request.getFoodName().trim())) {
            throw new BusinessException("该食物已在偏好清单中");
        }
        FavoriteFood food = FavoriteFood.builder()
                .userId(userId)
                .foodName(request.getFoodName().trim())
                .category(trimToNull(request.getCategory()))
                .note(trimToNull(request.getNote()))
                .caloriesPer100g(request.getCaloriesPer100g())
                .proteinPer100g(request.getProteinPer100g())
                .fatPer100g(request.getFatPer100g())
                .carbPer100g(request.getCarbPer100g())
                .build();
        return repository.save(food);
    }

    public FavoriteFood update(Long userId, Long id, FavoriteFoodRequest request) {
        FavoriteFood food = getOwned(userId, id);
        String newName = request.getFoodName().trim();
        // 改名时查重：同名已存在且不是自己 → 拒绝
        repository.findByUserIdAndFoodNameIgnoreCase(userId, newName)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessException("该食物已在偏好清单中");
                });
        food.setFoodName(newName);
        food.setCategory(trimToNull(request.getCategory()));
        food.setNote(trimToNull(request.getNote()));
        food.setCaloriesPer100g(request.getCaloriesPer100g());
        food.setProteinPer100g(request.getProteinPer100g());
        food.setFatPer100g(request.getFatPer100g());
        food.setCarbPer100g(request.getCarbPer100g());
        return repository.save(food);
    }

    public void delete(Long userId, Long id) {
        FavoriteFood food = getOwned(userId, id);
        repository.delete(food);
    }

    private FavoriteFood getOwned(Long userId, Long id) {
        FavoriteFood food = repository.findById(id)
                .orElseThrow(() -> new BusinessException("偏好记录不存在"));
        if (!food.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此偏好记录");
        }
        return food;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
