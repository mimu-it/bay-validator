package com.baymax.rule;

import com.baymax.validator.engine.DataBaseType;
import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;

/**
 * @author xiao.hu
 * @date 2024-02-19
 * @apiNote
 */
public class DateFieldRuleTest {

    /**
     * 初始化
     */
    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/date_field_rule.yml")
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
    public void testLegal() {
        try {
            HxValidator.builder().validate("student.birthday", "2023-11-11");
        } catch (Exception e) {
            Assert.assertTrue(false);
            return;
        }

        Assert.assertTrue(true);
    }

    /**
     * 正常流程逻辑
     */
    @Test
    public void testLegal_1() {
        try {
            HxValidator.builder().validate("student.birthday", "2023-10-01");
        } catch (Exception e) {
            Assert.assertTrue(false);
            return;
        }

        Assert.assertTrue(true);
    }

    @Test
    public void testLegal_2() {
        LocalDate localDate = LocalDate.of(2023, 10, 01).atStartOfDay(ZoneId.of("UTC")).toLocalDate();

        try {
            HxValidator.builder().validate("student.birthday", localDate);
        } catch (Exception e) {
            Assert.assertTrue(false);
            return;
        }

        Assert.assertTrue(true);
    }

    @Test
    public void testLegal_3() {
        try {
            HxValidator.builder().validate("student.birthday2", "2021-10-03");
        } catch (Exception e) {
            Assert.assertTrue(false);
            return;
        }

        Assert.assertTrue(true);
    }

    @Test
    public void testLegal_4() {
        try {
            HxValidator.builder().validate("student.birthday3", "2024-12-19");
        } catch (Exception e) {
            Assert.assertTrue(false);
            return;
        }

        Assert.assertTrue(true);
    }

    @Test
    public void testLegal_5() {
        try {
            HxValidator.builder().validate("student.birthday4", "2024-12-19");
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
            HxValidator.builder().validate("student.birthday", "2024-12-26");
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
            HxValidator.builder().validate("student.birthday", "2021-09-30");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }


    @Test
    public void testIllegal_3() {
        try {
            HxValidator.builder().validate("student.birthday2", "2021-10-01");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }

    @Test
    public void testIllegal_4() {
        try {
            HxValidator.builder().validate("student.birthday3", "2024-12-21");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }

    @Test
    public void testIllegal_5() {
        try {
            HxValidator.builder().validate("student.birthday4", "2024-12-");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }
}
