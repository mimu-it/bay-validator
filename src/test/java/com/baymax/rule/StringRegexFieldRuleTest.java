package com.baymax.rule;

import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import com.baymax.validator.engine.preset.DbType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

public class StringRegexFieldRuleTest {

    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DbType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/string_regex_field_rule.yml")
                .ruleDict("common_dict.yml")
                .ignoreKeys(new HashSet<String>() {{
                    add("id"); add("version"); add("is_deleted");
                    add("modifier"); add("creator");
                    add("created_at"); add("updated_at");
                }})
                .keyMode(KeyMode.snake)
                .init();
    }

    // ====================================================================
    // 字符模式（无 charset）：按字符数校验，lengthMode="char"
    //   student.description: only_chinese_and_english_and_underline, length 5-20
    // ====================================================================

    @Test
    public void charMode_minBoundary() {
        HxValidator.builder().validate("student.description", "aaaaa");
    }

    @Test
    public void charMode_maxBoundary() {
        HxValidator.builder().validate("student.description", "aaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    public void charMode_inside() {
        HxValidator.builder().validate("student.description", "hello123456");
    }

    @Test(expected = Exception.class)
    public void charMode_belowMin() {
        HxValidator.builder().validate("student.description", "aa");
    }

    @Test(expected = Exception.class)
    public void charMode_aboveMax() {
        HxValidator.builder().validate("student.description", "aaaaaaaaaaaaaaaaaaaaa");
    }

    // ====================================================================
    // 字节模式（utf8 charset）：按字节数校验，lengthMode="byte"
    //   student.user_name: utf8, length min=3, max=12
    //   "胡晓" = 3+3 = 6 字节，通过（6 >= 3）
    //   "胡晓_huxia" = 3+3+1+1+1+1+1+1 = 12 字节，通过
    //   "胡晓_huxiao" = 3+3+1+1+1+1+1+1+1 = 13 字节，超限
    // ====================================================================

    @Test
    public void byteMode_utf8_chineseMin() {
        HxValidator.builder().validate("student.user_name", "胡晓");
    }

    @Test
    public void byteMode_utf8_mixedMax() {
        HxValidator.builder().validate("student.user_name", "胡晓_huxia");
    }

    @Test
    public void byteMode_utf8_singleChar() {
        HxValidator.builder().validate("student.user_name", "胡");
    }

    @Test
    public void byteMode_utf8_asciiMin() {
        HxValidator.builder().validate("student.user_name", "hhh");
    }

    @Test
    public void byteMode_utf8_asciiMax() {
        HxValidator.builder().validate("student.user_name", "hhhhhhhhhhhh");
    }

    @Test
    public void byteMode_utf8_digits() {
        HxValidator.builder().validate("student.user_name", "abc123");
    }

    @Test
    public void byteMode_utf8_underscore() {
        HxValidator.builder().validate("student.user_name", "a_b_c");
    }

    @Test(expected = Exception.class)
    public void byteMode_utf8_exceedMax() {
        HxValidator.builder().validate("student.user_name", "胡晓_huxiao");
    }

    @Test(expected = Exception.class)
    public void byteMode_utf8_belowMin() {
        HxValidator.builder().validate("student.user_name", "hh");
    }

    @Test(expected = Exception.class)
    public void byteMode_utf8_specialChars() {
        HxValidator.builder().validate("student.user_name", "hh*");
    }

    @Test(expected = Exception.class)
    public void byteMode_utf8_space() {
        HxValidator.builder().validate("student.user_name", "h h");
    }

    @Test(expected = Exception.class)
    public void byteMode_utf8_empty() {
        HxValidator.builder().validate("student.user_name", "");
    }

    // ====================================================================
    // 字节模式（GBK charset）
    //   student.user_name_gbk: GBK, length min=3, max=12
    //   中文在 GBK 中占 2 字节，英文占 1 字节
    //   "胡晓" = 2+2 = 4 字节，通过
    //   "胡晓_huxia" = 2+2+1+1+1+1+1+1 = 10 字节，通过
    //   那么"胡晓_huxiao" = 2+2+1+1+1+1+1+1+1 = 11 字节，通过
    //   "aaaaaaaaaaaa" = 12 字节，通过
    //   "aaaaaaaaaaaaa" = 13 字节，超限
    // ====================================================================

    @Test
    public void byteMode_gbk_chineseMin() {
        HxValidator.builder().validate("student.user_name_gbk", "胡晓");
    }

    @Test
    public void byteMode_gbk_mixedMax() {
        HxValidator.builder().validate("student.user_name_gbk", "aaaa");
    }

    @Test
    public void byteMode_gbk_asciiMax() {
        HxValidator.builder().validate("student.user_name_gbk", "aaaaaaaaaaaa");
    }

    @Test(expected = Exception.class)
    public void byteMode_gbk_asciiExceedMax() {
        HxValidator.builder().validate("student.user_name_gbk", "aaaaaaaaaaaaa");
    }

    @Test
    public void nullAsStringNull() {
        HxValidator.builder().validate("student.user_name", (Object) null);
    }
}
