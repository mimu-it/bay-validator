package com.baymax.rule;

import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import com.baymax.validator.engine.preset.DbType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

public class EnumDecimalFieldRuleTest {

    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DbType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/enum_decimal_field_rule.yml")
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
    public void firstValue() {
        HxValidator.builder().validate("student.float_card", "3.11");
    }

    @Test
    public void secondValue() {
        HxValidator.builder().validate("student.float_card", "4000000000.123456");
    }

    // ================== 非法值 ==================

    @Test(expected = Exception.class)
    public void notInEnum() {
        HxValidator.builder().validate("student.float_card", "123.0");
    }

    @Test(expected = Exception.class)
    public void nearMatch() {
        HxValidator.builder().validate("student.float_card", "3.12");
    }

    @Test(expected = Exception.class)
    public void emptyString() {
        HxValidator.builder().validate("student.float_card", "");
    }

    @Test(expected = Exception.class)
    public void nonNumericString() {
        HxValidator.builder().validate("student.float_card", "abc");
    }

    @Test(expected = Exception.class)
    public void nullValue() {
        HxValidator.builder().validate("student.float_card", (Object) null);
    }
}
