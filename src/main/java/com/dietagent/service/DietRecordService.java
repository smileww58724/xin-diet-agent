package com.dietagent.service;

import com.dietagent.config.CacheConfig;
import com.dietagent.dto.request.DietRecordRequest;
import com.dietagent.dto.response.DietRecordResponse;
import com.dietagent.entity.DietRecord;
import com.dietagent.exception.BusinessException;
import com.dietagent.repository.DietRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DietRecordService {

    private final DietRecordRepository dietRecordRepository;

    @CacheEvict(cacheNames = CacheConfig.NUTRITION_SUMMARY, allEntries = true)
    public DietRecordResponse addRecord(Long userId, DietRecordRequest request) {
        DietRecord record = DietRecord.builder()
                .userId(userId)
                .foodName(request.getFoodName())
                .mealType(request.getMealType())
                .portionSize(request.getPortionSize())
                .calories(request.getCalories())
                .protein(request.getProtein())
                .fat(request.getFat())
                .carbohydrate(request.getCarbohydrate())
                .mealTime(request.getMealTime())
                .build();

        record = dietRecordRepository.save(record);
        return toResponse(record);
    }

    @CacheEvict(cacheNames = CacheConfig.NUTRITION_SUMMARY, allEntries = true)
    public DietRecordResponse updateRecord(Long userId, Long recordId, DietRecordRequest request) {
        DietRecord record = dietRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("记录不存在"));

        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此记录");
        }

        record.setFoodName(request.getFoodName());
        record.setMealType(request.getMealType());
        record.setPortionSize(request.getPortionSize());
        record.setCalories(request.getCalories());
        record.setProtein(request.getProtein());
        record.setFat(request.getFat());
        record.setCarbohydrate(request.getCarbohydrate());
        record.setMealTime(request.getMealTime());

        record = dietRecordRepository.save(record);
        return toResponse(record);
    }

    @CacheEvict(cacheNames = CacheConfig.NUTRITION_SUMMARY, allEntries = true)
    public void deleteRecord(Long userId, Long recordId) {
        DietRecord record = dietRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("记录不存在"));

        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此记录");
        }

        dietRecordRepository.delete(record);
    }

    public List<DietRecordResponse> getRecords(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return dietRecordRepository.findByUserIdAndMealTimeBetweenOrderByMealTimeAsc(userId, start, end)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<DietRecordResponse> getRecords(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return dietRecordRepository.findByUserIdAndMealTimeBetween(userId, start, end)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private DietRecordResponse toResponse(DietRecord record) {
        return DietRecordResponse.builder()
                .id(record.getId())
                .foodName(record.getFoodName())
                .mealType(record.getMealType())
                .portionSize(record.getPortionSize())
                .calories(record.getCalories())
                .protein(record.getProtein())
                .fat(record.getFat())
                .carbohydrate(record.getCarbohydrate())
                .mealTime(record.getMealTime())
                .build();
    }
}
