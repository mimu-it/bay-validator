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
public class StringRegexFieldRuleTest {

    /**
     * 初始化
     */
    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("rule/string_regex_field_rule.yml")
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
    public void testLegal_1() {
        try {
            HxValidator.builder().validate("student.user_name", "胡晓");
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
    public void testLegal_2() {
        try {
            /** 3*2 + 6 = 12 */
            HxValidator.builder().validate("student.user_name", "胡晓_huxia");
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
    public void testLegal_3() {
        try {
            HxValidator.builder().validate("student.user_name", "胡");
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
    public void testLegal_4() {
        try {
            HxValidator.builder().validate("student.user_name", "hhh");
        } catch (Exception e) {
            Assert.assertTrue(false);
            return;
        }

        Assert.assertTrue(true);
    }

    /**
     * 异常逻辑
     * 超过长度
     */
    @Test
    public void testIllegal_1() {
        try {
            HxValidator.builder().validate("student.user_name", "胡晓_huxiao");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }

    /**
     * 异常逻辑
     * 长度短了
     */
    @Test
    public void testIllegal_2() {
        try {
            HxValidator.builder().validate("student.user_name", "hh");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }


    /**
     * 异常逻辑
     * 正则表达式，必须是中文开头
     */
    @Test
    public void testIllegal_3() {
        try {
            HxValidator.builder().validate("student.user_name", "hh*");
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }

        Assert.assertTrue(false);
    }
}
