package com.baymax.rule;

import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import com.baymax.validator.engine.preset.DbType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

public class EnumStringFieldRuleTest {

    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DbType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/enum_string_field_rule.yml")
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
        HxValidator.builder().validate("student.gender", "male");
    }

    @Test
    public void secondValue() {
        HxValidator.builder().validate("student.gender", "female");
    }

    // ================== 非法值 ==================

    @Test(expected = Exception.class)
    public void notInEnum() {
        HxValidator.builder().validate("student.gender", "other");
    }

    @Test(expected = Exception.class)
    public void caseSensitive() {
        HxValidator.builder().validate("student.gender", "MALE");
    }

    @Test(expected = Exception.class)
    public void emptyString() {
        HxValidator.builder().validate("student.gender", "");
    }

    @Test(expected = Exception.class)
    public void nullValue() {
        HxValidator.builder().validate("student.gender", (Object) null);
    }

    @Test(expected = Exception.class)
    public void numericString() {
        HxValidator.builder().validate("student.gender", "123");
    }

    @Test(expected = Exception.class)
    public void partialMatch() {
        HxValidator.builder().validate("student.gender", "male_extra");
    }
}
