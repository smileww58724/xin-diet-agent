package com.dietagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "diet_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String foodName;

    @Column(nullable = false, length = 20)
    private String mealType;

    @Column(length = 50)
    private String portionSize;

    private Integer calories;

    @Column(precision = 5, scale = 2)
    private BigDecimal protein;

    @Column(precision = 5, scale = 2)
    private BigDecimal fat;

    @Column(precision = 5, scale = 2)
    private BigDecimal carbohydrate;

    @Column(nullable = false)
    private LocalDateTime mealTime;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
