package com.baymax.rule;

import com.baymax.validator.engine.DataBaseType;
import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashSet;

public class DecimalFieldRuleTest {

    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/decimal_field_rule.yml")
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
    public void minBoundary() {
        HxValidator.builder().validate("student.money", "0.00");
    }

    @Test
    public void maxBoundary() {
        HxValidator.builder().validate("student.money", "300.00");
    }

    @Test
    public void insideRange() {
        HxValidator.builder().validate("student.money", "200.00");
    }

    @Test
    public void zeroOnly() {
        HxValidator.builder().validate("student.money", "0");
    }

    @Test
    public void bigDecimalObject() {
        HxValidator.builder().validate("student.money", new BigDecimal("150.50"));
    }

    @Test
    public void integerString() {
        HxValidator.builder().validate("student.money", "100");
    }

    // ================== 非法值 ==================

    @Test(expected = Exception.class)
    public void belowMin() {
        HxValidator.builder().validate("student.money", "-0.01");
    }

    @Test(expected = Exception.class)
    public void aboveMax() {
        HxValidator.builder().validate("student.money", "300.01");
    }

    @Test(expected = Exception.class)
    public void negativeValue() {
        HxValidator.builder().validate("student.money", "-100.00");
    }

    @Test(expected = Exception.class)
    public void nonNumericString() {
        HxValidator.builder().validate("student.money", "abc");
    }

    @Test(expected = Exception.class)
    public void emptyString() {
        HxValidator.builder().validate("student.money", "");
    }

    @Test(expected = Exception.class)
    public void nullValue() {
        HxValidator.builder().validate("student.money", (Object) null);
    }
}
