package com.baymax.rule;

import com.baymax.validator.engine.DataBaseType;
import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Calendar;

public class DateFieldRuleTest {

    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/date_field_rule.yml")
                .ruleDict("common_dict.yml")
                .ignoreKeys(new HashSet<String>() {{
                    add("id"); add("version"); add("is_deleted");
                    add("modifier"); add("creator");
                    add("created_at"); add("updated_at");
                }})
                .keyMode(KeyMode.snake)
                .init();
    }

    // ================== 合法值 ==================

    @Test
    public void beginAtBoundary() {
        HxValidator.builder().validate("student.birthday", "2021-10-01");
    }

    @Test
    public void endAtBoundaryExclusive() {
        HxValidator.builder().validate("student.birthday", "2024-12-25");
    }

    @Test
    public void insideRange() {
        HxValidator.builder().validate("student.birthday", "2023-11-11");
        HxValidator.builder().validate("student.birthday", "2023-10-01");
    }

    @Test
    public void localDateObject() {
        LocalDate localDate = LocalDate.of(2023, 10, 1);
        HxValidator.builder().validate("student.birthday", localDate);
    }

    @Test
    public void onlyBeginAt_start() {
        HxValidator.builder().validate("student.birthday2", "2021-10-02");
    }

    @Test
    public void onlyBeginAt_after() {
        HxValidator.builder().validate("student.birthday2", "2024-12-01");
    }

    @Test
    public void onlyEndAt_before() {
        HxValidator.builder().validate("student.birthday3", "2024-12-19");
    }

    @Test
    public void onlyEndAt_min() {
        HxValidator.builder().validate("student.birthday3", "1000-01-01");
    }

    @Test
    public void noBound() {
        HxValidator.builder().validate("student.birthday4", "2024-12-19");
        HxValidator.builder().validate("student.birthday4", "3000-01-01");
    }

    // ================== 非法值 ==================

    @Test(expected = Exception.class)
    public void beforeBeginAt() {
        HxValidator.builder().validate("student.birthday", "2021-09-30");
    }

    @Test(expected = Exception.class)
    public void atEndAtIsExclusive() {
        HxValidator.builder().validate("student.birthday", "2024-12-26");
    }

    @Test(expected = Exception.class)
    public void malformedString() {
        HxValidator.builder().validate("student.birthday", "2024-12-");
    }

    @Test(expected = Exception.class)
    public void randomString() {
        HxValidator.builder().validate("student.birthday", "not-a-date");
    }

    @Test(expected = Exception.class)
    public void onlyBeginAt_before() {
        HxValidator.builder().validate("student.birthday2", "2021-10-01");
    }

    @Test(expected = Exception.class)
    public void onlyEndAt_after() {
        HxValidator.builder().validate("student.birthday3", "2024-12-21");
    }
}
