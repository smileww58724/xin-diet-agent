package com.dietagent.repository;

import com.dietagent.entity.FavoriteFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteFoodRepository extends JpaRepository<FavoriteFood, Long> {

    List<FavoriteFood> findByUserIdOrderByIdDesc(Long userId);

    /** 查重用：同一用户下忽略大小写比对食物名 */
    boolean existsByUserIdAndFoodNameIgnoreCase(Long userId, String foodName);

    Optional<FavoriteFood> findByUserIdAndFoodNameIgnoreCase(Long userId, String foodName);

    long countByUserId(Long userId);
}
