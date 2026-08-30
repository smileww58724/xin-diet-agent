package com.dietagent.service;

import com.dietagent.dto.request.FavoriteFoodRequest;
import com.dietagent.entity.FavoriteFood;
import com.dietagent.exception.BusinessException;
import com.dietagent.repository.FavoriteFoodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteFoodServiceTest {

    @Mock
    private FavoriteFoodRepository repository;

    @InjectMocks
    private FavoriteFoodService service;

    private FavoriteFoodRequest request(String name) {
        FavoriteFoodRequest req = new FavoriteFoodRequest();
        req.setFoodName(name);
        req.setCategory("高蛋白");
        req.setNote("减脂主力");
        req.setCaloriesPer100g(133);
        return req;
    }

    @Test
    @DisplayName("添加成功：归属当前用户，名称去除首尾空白")
    void addSuccess() {
        when(repository.countByUserId(1L)).thenReturn(0L);
        when(repository.existsByUserIdAndFoodNameIgnoreCase(1L, "鸡胸肉")).thenReturn(false);

        service.add(1L, request("  鸡胸肉 "));

        ArgumentCaptor<FavoriteFood> captor = ArgumentCaptor.forClass(FavoriteFood.class);
        verify(repository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("鸡胸肉", captor.getValue().getFoodName());
        assertEquals(133, captor.getValue().getCaloriesPer100g());
    }

    @Test
    @DisplayName("重复添加同名食物被拒绝")
    void addDuplicateRejected() {
        when(repository.countByUserId(1L)).thenReturn(0L);
        when(repository.existsByUserIdAndFoodNameIgnoreCase(1L, "鸡胸肉")).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.add(1L, request("鸡胸肉")));
        assertTrue(e.getMessage().contains("已在偏好清单"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("清单达到软上限时拒绝继续添加")
    void addOverLimitRejected() {
        when(repository.countByUserId(1L)).thenReturn(100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.add(1L, request("鸡胸肉")));
        assertTrue(e.getMessage().contains("上限"));
        verify(repository, never()).existsByUserIdAndFoodNameIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("更新他人记录被拒绝")
    void updateOthersRecordRejected() {
        FavoriteFood others = FavoriteFood.builder().id(7L).userId(2L).foodName("寿司").build();
        when(repository.findById(7L)).thenReturn(Optional.of(others));

        assertThrows(BusinessException.class, () -> service.update(1L, 7L, request("寿司")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("改名撞上自己的另一条同名记录被拒绝")
    void updateNameConflictRejected() {
        FavoriteFood mine = FavoriteFood.builder().id(7L).userId(1L).foodName("鸡胸肉").build();
        FavoriteFood other = FavoriteFood.builder().id(8L).userId(1L).foodName("牛肉").build();
        when(repository.findById(7L)).thenReturn(Optional.of(mine));
        when(repository.findByUserIdAndFoodNameIgnoreCase(1L, "牛肉")).thenReturn(Optional.of(other));

        assertThrows(BusinessException.class, () -> service.update(1L, 7L, request("牛肉")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("删除他人记录被拒绝；删除自己的成功")
    void deleteOwnership() {
        FavoriteFood others = FavoriteFood.builder().id(7L).userId(2L).foodName("寿司").build();
        when(repository.findById(7L)).thenReturn(Optional.of(others));

        assertThrows(BusinessException.class, () -> service.delete(1L, 7L));

        FavoriteFood mine = FavoriteFood.builder().id(8L).userId(1L).foodName("牛肉").build();
        when(repository.findById(8L)).thenReturn(Optional.of(mine));
        service.delete(1L, 8L);
        verify(repository).delete(mine);
    }

    @Test
    @DisplayName("list 透传仓库结果")
    void listPassThrough() {
        when(repository.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of(
                FavoriteFood.builder().id(1L).userId(1L).foodName("鸡胸肉").build()));

        List<FavoriteFood> result = service.list(1L);
        assertEquals(1, result.size());
        assertEquals("鸡胸肉", result.get(0).getFoodName());
    }

    @Test
    @DisplayName("eq 匹配说明：删除走精确 id 查询")
    void deleteUsesFindById() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.delete(1L, 9L));
        verify(repository).findById(9L);
    }
}
