package com.dietagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal_setting", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "goalDate"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate goalDate;

    private Integer targetCalories;

    private Integer targetProtein;

    private Integer targetFat;

    private Integer targetCarb;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean achieved;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (achieved == null) {
            achieved = false;
        }
    }
}
