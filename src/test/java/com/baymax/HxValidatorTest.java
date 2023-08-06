package com.baymax;

import com.baymax.validator.engine.DataBaseType;
import com.baymax.validator.engine.HxValidator;
import com.baymax.validator.engine.KeyMode;
import com.baymax.validator.engine.exception.IllegalValueException;
import com.baymax.vo.Student;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * @author xiao.hu
 * @date 2023-07-18
 * @apiNote
 */
public class HxValidatorTest {

    /**
     * 初始化
     */
    @Before
    public void init() {
        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)
                .commonRules("value_rules_common.yml")
                .rules("value_rules.yml")
                .regexDict("common_dict.yml")
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
    public void test() {
        Student student = new Student();
        student.setPhoneNumber("15973166256");
        student.setLargeNumber(123456);
        student.setId(22);
        student.setVersion(1);
        student.setCreatedAt(LocalDateTime.now());
        student.setCreator("xiao.hu");
        student.setModifier("xiao.hu");
        student.setUpdatedAt(LocalDateTime.now());
        student.setMoney(new BigDecimal("300"));
        student.setGender("male");
        student.setGameLongCard(4000000000L);
        student.setFloatCard(3.11f);
        List<String> errors = HxValidator.builder().with(student).bind("student").nullableKeys(new String[]{"id"}).validate();
        Assert.assertTrue(errors.isEmpty());
    }

    /**
     * 不合规 money 参数
     */
    @Test
    public void testIllegalMoney() {
        Student student = new Student();
        student.setPhoneNumber("15973166256");
        student.setLargeNumber(123456);
        student.setId(22);
        student.setVersion(1);
        student.setCreatedAt(LocalDateTime.now());
        student.setCreator("xiao.hu");
        student.setModifier("xiao.hu");
        student.setUpdatedAt(LocalDateTime.now());
        student.setMoney(new BigDecimal("500"));
        student.setGender("male");
        student.setGameLongCard(3000000000L);
        student.setFloatCard(4000000000.123456f);
        List<String> errors = HxValidator.builder().with(student).bind("student").nullableKeys(new String[]{"id"}).validate();
        Assert.assertTrue(errors.contains("money"));
    }

    /**
     * 不合规 phone number
     */
    @Test
    public void testIllegalPhoneNumber() {
        Student student = new Student();
        student.setPhoneNumber("159731662568");
        student.setLargeNumber(123456);
        student.setId(22);
        student.setVersion(1);
        student.setCreatedAt(LocalDateTime.now());
        student.setCreator("xiao.hu");
        student.setModifier("xiao.hu");
        student.setUpdatedAt(LocalDateTime.now());
        student.setMoney(new BigDecimal("200"));
        student.setGameLongCard(4000000000L);
        student.setFloatCard(4000000000.123456f);
        List<String> errors = HxValidator.builder().with(student).bind("student").nullableKeys(new String[]{"id"}).validate();
        Assert.assertTrue(errors.contains("phone_number"));
        Assert.assertTrue(errors.contains("gender"));
    }


    /**
     * 正常流程逻辑
     */
    @Test
    public void testIllegalFloat() {
        Student student = new Student();
        student.setPhoneNumber("15973166256");
        student.setLargeNumber(123456);
        student.setId(22);
        student.setVersion(1);
        student.setCreatedAt(LocalDateTime.now());
        student.setCreator("xiao.hu");
        student.setModifier("xiao.hu");
        student.setUpdatedAt(LocalDateTime.now());
        student.setMoney(new BigDecimal("300"));
        student.setGender("male");
        student.setGameLongCard(4000000000L);
        student.setFloatCard(5.88f);
        List<String> errors = HxValidator.builder().with(student).bind("student").nullableKeys(new String[]{"id"}).validate();
        Assert.assertTrue(errors.contains("float_card"));
    }


    @Test(expected = IllegalValueException.class)
    public void testIllegalFloat2() {
        HxValidator.builder().validate("student.float_card", 5.88f)
                .validateIfNonnull("student.gender", "male");
    }

    @Test
    public void testlegalFloat() {
        HxValidator.builder().validate("student.float_card", 3.11f)
                .validateIfNonnull("student.gender", "male");
    }
}
