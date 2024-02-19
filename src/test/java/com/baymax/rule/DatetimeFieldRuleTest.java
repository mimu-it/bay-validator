package com.baymax.rule;

import com.baymax.validator.engine.DataBaseType;
import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;

/**
 * @author xiao.hu
 * @date 2024-02-19
 * @apiNote
 */
public class DatetimeFieldRuleTest {

    /**
     * 初始化
     */
    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/datetime_field_rule.yml")
                .ruleDict("common_dict.yml")
                .ignoreKeys(new HashSet<String>() {{
                    add("id");
                    add("version");
                    add("is_deleted");
                    add("modifier");
                    add("creator");
                    add("created_at");
                    add("updated_at");
                }})
                .keyMode(KeyMode.snake)
                .init();
    }

    /**
     * 正常流程逻辑
     */
    @Test
    public void testLegal_0() {
        try {
            HxValidator.builder().validate("student.birthday", "2023-11-11 10:00:00");
        } catch (Exception e) {
            Assert.assertTrue(false);
            return;
        }

        Assert.assertTrue(true);
    }

    /**
     *
     */
    @Test
    public void testLegal_1() {
        LocalDateTime localDateTime = LocalDateTime.of(2023, 11, 11, 10, 0, 0)
                .atZone(ZoneId.of("UTC")).toLocalDateTime();

        try {
            HxValidator.builder().validate("student.birthday", localDateTime);
        } catch (Exception e) {
            Assert.assertTrue(false);
            return;
        }

        Assert.assertTrue(true);
    }

    /**
     * 异常流程逻辑
     */
    @Test
    public void testIllegal_1() {
        try {
            HxValidator.builder().validate("student.birthday", "2021-10-01 01:20:11");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }

    /**
     * 异常流程逻辑
     */
    @Test
    public void testIllegal_2() {
        try {
            HxValidator.builder().validate("student.birthday", "2024-12-26 20:11:00");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }

    /**
     * 异常流程逻辑
     */
    @Test
    public void testIllegal_3() {
        try {
            HxValidator.builder().validate("student.birthday", "2024-12-26 20:10:59");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }
}
