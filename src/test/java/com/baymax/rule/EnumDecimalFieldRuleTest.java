package com.baymax.rule;

import com.baymax.validator.engine.DataBaseType;
import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

/**
 * @author xiao.hu
 * @date 2024-02-20
 * @apiNote
 */
public class EnumDecimalFieldRuleTest {

    /**
     * 初始化
     */
    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/enum_decimal_field_rule.yml")
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
            HxValidator.builder().validate("student.float_card", "3.11");
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
            HxValidator.builder().validate("student.float_card", "4000000000.123456");
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
            HxValidator.builder().validate("student.float_card", "123.0");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }
}
