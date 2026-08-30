package com.dietagent.repository;

import com.dietagent.entity.DietRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 聚合查询的真实执行测试（连本地 MySQL）：
 * 修复"Spring Data 对 Object[] 返回值解包与假设不符导致聚合恒为 0"的缺陷后，
 * 该测试保证返回形态为 List（每行一个 Object[]），数值可直接按 Number 换算。
 * CI 中由 GitHub Actions 的 MySQL service 容器提供数据库。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class DietRecordAggregateRepositoryTest {

    @Autowired
    private DietRecordRepository repository;

    private DietRecord record(long userId, String food, int calories, String protein, String fat, String carb, LocalDateTime time) {
        return DietRecord.builder()
                .userId(userId)
                .foodName(food)
                .mealType("午餐")
                .calories(calories)
                .protein(new BigDecimal(protein))
                .fat(new BigDecimal(fat))
                .carbohydrate(new BigDecimal(carb))
                .mealTime(time)
                .build();
    }

    @Test
    @DisplayName("聚合查询返回首行为四列数值，跨类型可按 Number 换算")
    void aggregateReturnsNumericRow() {
        LocalDateTime t = LocalDate.of(2026, 1, 10).atTime(12, 0);
        repository.save(record(990001L, "米饭", 300, "5", "1", "66", t));
        repository.save(record(990001L, "鸡胸肉", 200, "40", "5", "2", t));
        repository.flush();

        List<Object[]> rows = repository.aggregateNutrition(990001L,
                LocalDate.of(2026, 1, 10).atStartOfDay(),
                LocalDate.of(2026, 1, 10).atTime(23, 59, 59));

        assertFalse(rows.isEmpty(), "纯聚合查询无 GROUP BY 时必返回一行");
        Object[] row = rows.get(0);
        assertEquals(4, row.length);
        // SUM(Integer) 在 JPQL 里是 Long，SUM(BigDecimal) 是 BigDecimal——都必须能按 Number 取值
        assertTrue(row[0] instanceof Number, "calories 汇总应为数值类型，实际: " + (row[0] == null ? "null" : row[0].getClass()));
        assertEquals(500L, ((Number) row[0]).longValue());
        assertTrue(row[1] instanceof Number, "protein 汇总应为数值类型，实际: " + (row[1] == null ? "null" : row[1].getClass()));
        assertEquals(0, ((Number) row[1]).doubleValue() - 45.0, 0.001);
        assertEquals(0, ((Number) row[3]).doubleValue() - 68.0, 0.001);
    }

    @Test
    @DisplayName("区间外记录不参与聚合")
    void aggregateFiltersByRange() {
        repository.save(record(990002L, "昨天的饭", 999, "10", "10", "10",
                LocalDate.of(2026, 1, 9).atTime(12, 0)));
        repository.flush();

        List<Object[]> rows = repository.aggregateNutrition(990002L,
                LocalDate.of(2026, 1, 10).atStartOfDay(),
                LocalDate.of(2026, 1, 10).atTime(23, 59, 59));

        assertFalse(rows.isEmpty());
        Object[] row = rows.get(0);
        assertEquals(4, row.length);
        assertTrue(row[0] == null || ((Number) row[0]).longValue() == 0,
                "区间外记录不应被统计，实际: " + row[0]);
    }
}
