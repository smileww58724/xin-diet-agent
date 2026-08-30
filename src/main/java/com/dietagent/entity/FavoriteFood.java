package com.dietagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_food", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "foodName"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String foodName;

    @Column(length = 30)
    private String category;

    @Column(length = 200)
    private String note;

    @Column(name = "calories_per_100g")
    private Integer caloriesPer100g;

    @Column(name = "protein_per_100g", precision = 5, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "fat_per_100g", precision = 5, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "carb_per_100g", precision = 5, scale = 2)
    private BigDecimal carbPer100g;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
