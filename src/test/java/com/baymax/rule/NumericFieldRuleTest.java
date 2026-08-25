package com.baymax.rule;

import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import com.baymax.validator.engine.preset.DbType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

public class NumericFieldRuleTest {

    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DbType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/numeric_field_rule.yml")
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
        HxValidator.builder().validate("student.age", 0);
    }

    @Test
    public void maxBoundary() {
        HxValidator.builder().validate("student.age", 120);
    }

    @Test
    public void stringNumber() {
        HxValidator.builder().validate("student.age", "60");
    }

    @Test
    public void insideRange() {
        HxValidator.builder().validate("student.age", 1);
        HxValidator.builder().validate("student.age", 119);
        HxValidator.builder().validate("student.age", "50");
    }

    // ================== 非法值 ==================

    @Test(expected = Exception.class)
    public void belowMin() {
        HxValidator.builder().validate("student.age", -1);
    }

    @Test(expected = Exception.class)
    public void aboveMax() {
        HxValidator.builder().validate("student.age", 121);
    }

    @Test(expected = Exception.class)
    public void negativeBig() {
        HxValidator.builder().validate("student.age", -999999);
    }

    @Test(expected = Exception.class)
    public void nonNumericString() {
        HxValidator.builder().validate("student.age", "abc");
    }

    @Test(expected = Exception.class)
    public void emptyString() {
        HxValidator.builder().validate("student.age", "");
    }

    @Test(expected = Exception.class)
    public void nullValue() {
        HxValidator.builder().validate("student.age", (Object) null);
    }

    @Test(expected = Exception.class)
    public void decimalValue() {
        HxValidator.builder().validate("student.age", 1.5);
    }
}
