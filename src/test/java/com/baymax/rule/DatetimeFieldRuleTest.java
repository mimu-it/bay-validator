package com.baymax.rule;

import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import com.baymax.validator.engine.preset.DbType;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.HashSet;

public class DatetimeFieldRuleTest {

    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DbType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/datetime_field_rule.yml")
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
        HxValidator.builder().validate("student.birthday", "2021-10-01 01:20:12");
    }

    @Test
    public void insideRange() {
        HxValidator.builder().validate("student.birthday", "2023-11-11 10:00:00");
    }

    @Test
    public void localDateTimeObject() {
        LocalDateTime localDateTime = LocalDateTime.of(2023, 11, 11, 10, 0, 0);
        HxValidator.builder().validate("student.birthday", localDateTime);
    }

    @Test
    public void onlyBeginAt_boundary() {
        HxValidator.builder().validate("student.birthday2", "2021-10-02 01:20:12");
    }

    @Test
    public void onlyBeginAt_after() {
        HxValidator.builder().validate("student.birthday2", "2024-01-01 00:00:00");
    }

    @Test
    public void onlyEndAt_before() {
        HxValidator.builder().validate("student.birthday3", "2024-12-20 20:10:58");
    }

    @Test
    public void onlyEndAt_justBeforeEnd() {
        HxValidator.builder().validate("student.birthday3", "2024-12-20 20:10:58");
    }

    @Test
    public void noBound() {
        HxValidator.builder().validate("student.birthday4", "2024-12-20 20:10:58");
    }

    // ================== 非法值 ==================

    @Test(expected = Exception.class)
    public void beforeBeginAt() {
        HxValidator.builder().validate("student.birthday", "2021-10-01 01:20:11");
    }

    @Test(expected = Exception.class)
    public void afterEndAt() {
        HxValidator.builder().validate("student.birthday", "2024-12-26 20:11:00");
    }

    @Test(expected = Exception.class)
    public void atEndAtIsExclusive() {
        HxValidator.builder().validate("student.birthday", "2024-12-26 20:10:59");
    }

    @Test(expected = Exception.class)
    public void onlyBeginAt_before() {
        HxValidator.builder().validate("student.birthday2", "2021-10-02 01:20:11");
    }

    @Test(expected = Exception.class)
    public void onlyEndAt_after() {
        HxValidator.builder().validate("student.birthday3", "2024-12-20 20:10:59");
    }

    @Test(expected = Exception.class)
    public void malformedString() {
        HxValidator.builder().validate("student.birthday", "2024-12-");
    }

    @Test(expected = Exception.class)
    public void randomString() {
        HxValidator.builder().validate("student.birthday", "not-a-datetime");
    }

    @Test(expected = Exception.class)
    public void dateWithoutTime() {
        HxValidator.builder().validate("student.birthday", "2024-12-20");
    }
}
