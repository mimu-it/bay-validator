package com.baymax.rule;

import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import com.baymax.validator.engine.preset.DbType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

public class EnumNumericFieldRuleTest {

    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DbType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/enum_numeric_field_rule.yml")
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
        HxValidator.builder().validate("student.game_card", 1);
    }

    @Test
    public void secondValue() {
        HxValidator.builder().validate("student.game_card", 2);
    }

    @Test
    public void thirdValue() {
        HxValidator.builder().validate("student.game_card", 100);
    }

    @Test
    public void stringNumber() {
        HxValidator.builder().validate("student.game_card", "1");
    }

    // ================== 非法值 ==================

    @Test(expected = Exception.class)
    public void notInEnum() {
        HxValidator.builder().validate("student.game_card", 3);
    }

    @Test(expected = Exception.class)
    public void negativeValue() {
        HxValidator.builder().validate("student.game_card", -1);
    }

    @Test(expected = Exception.class)
    public void zeroNotInEnum() {
        HxValidator.builder().validate("student.game_card", 0);
    }

    @Test(expected = Exception.class)
    public void decimalValue() {
        HxValidator.builder().validate("student.game_card", 1.5);
    }

    @Test(expected = Exception.class)
    public void emptyString() {
        HxValidator.builder().validate("student.game_card", "");
    }

    @Test(expected = Exception.class)
    public void nullValue() {
        HxValidator.builder().validate("student.game_card", (Object) null);
    }

    @Test(expected = Exception.class)
    public void nonNumericString() {
        HxValidator.builder().validate("student.game_card", "abc");
    }
}
