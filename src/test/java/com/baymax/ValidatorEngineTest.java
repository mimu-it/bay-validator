package com.baymax;

import com.baymax.validator.engine.CommonDict;
import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.generator.formatter.IFormatter;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.model.sub.StringRegexFieldRule;
import com.baymax.vo.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.jfinal.template.stat.ast.For;
import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ValidatorEngineTest {
    private static ObjectMapper mapper = new ObjectMapper();

    public enum PAGE_BIG_INTEGER {
        NUMBER_0(new BigInteger("0")),
        NUMBER_30000000(new BigInteger("30000000"));

        private final BigInteger value;

        PAGE_BIG_INTEGER(final BigInteger newValue) {
            value = newValue;
        }

        public BigInteger val() {
            return value;
        }
    }

    /**
     * 官方文档中建议使用字符串形式，这样不会丢失精度
     */
    public enum PAGE_BIG_DECIMAL {
        NUMBER_0$0(new BigDecimal("0.0")),
        NUMBER_30000000$1(new BigDecimal("30000000.1"));

        private final BigDecimal value;

        PAGE_BIG_DECIMAL(final BigDecimal newValue) {
            value = newValue;
        }

        public BigDecimal val() {
            return value;
        }
    }

    @Test
    public void testPAGE() {
        Assert.assertEquals(new BigInteger("0"), PAGE_BIG_INTEGER.NUMBER_0.val());
        Assert.assertEquals(new BigInteger("30000000"), PAGE_BIG_INTEGER.NUMBER_30000000.val());
    }

    @Test
    public void testPAGE_DOUBLE() {
        Assert.assertEquals(new BigDecimal("0.0"), PAGE_BIG_DECIMAL.NUMBER_0$0.val());
        Assert.assertEquals(new BigDecimal("30000000.1"), PAGE_BIG_DECIMAL.NUMBER_30000000$1.val());
    }

    @Test
    public void testReplaceAll() {
        Assert.assertEquals("1$1", "1.1".replaceAll("\\.", "\\$"));
        Assert.assertEquals("4000000000$123456", "4000000000.123456".replaceAll("\\.", "\\$"));

        double x = 4000000000.123456;
        Double xx = new Double(x);
        BigDecimal xxBigDecimal = new BigDecimal(xx);
        System.out.println(xxBigDecimal.toString()); //4000000000.12345600128173828125 精度异常
        Assert.assertEquals("4.000000000123456E9", String.valueOf(xx));
        Assert.assertEquals("4.000000000123456E9", xx.toString());

        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        System.out.println(decimalFormat.format(xx)); // 4000000000.1235

    }

    @Test
    public void testValidateInteger() {
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        boolean isOK = ValidatorEngine.INSTANCE.validate("student.id", 1);
        Assert.assertTrue(isOK);

        boolean isOK2 = ValidatorEngine.INSTANCE.validate("student.id", "2");
        Assert.assertTrue(isOK2);

        boolean isOK3 = ValidatorEngine.INSTANCE.validate("student.id", 0);
        Assert.assertFalse(isOK3);

        boolean isOK4 = ValidatorEngine.INSTANCE.validate("student.id", 129);
        Assert.assertFalse(isOK4);
    }

    @Test
    public void testValidateBigInteger() {
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        /**
         * js number 最大值(16位) 9007199254740992
         */
        boolean isOK = ValidatorEngine.INSTANCE.validate("student.big_id", 9007199254740992l);
        Assert.assertTrue(isOK);

        boolean isOK_false = ValidatorEngine.INSTANCE.validate("student.big_id", 9007199254740994l);
        Assert.assertFalse(isOK_false);


        boolean isOK_true = ValidatorEngine.INSTANCE.validate("student.large_number", "18446744073709551615");
        Assert.assertTrue(isOK_true);

    }

    @Test
    public void testValidateDecimalMoney() {
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        boolean isOK = ValidatorEngine.INSTANCE.validate("student.money", "200.00");
        Assert.assertTrue(isOK);

        boolean isOK2 = ValidatorEngine.INSTANCE.validate("student.money", "200.01");
        Assert.assertTrue(isOK2);

        boolean isOK3 = ValidatorEngine.INSTANCE.validate("student.money", "200.0123456789");
        Assert.assertTrue(isOK3);

        boolean isOK_false = ValidatorEngine.INSTANCE.validate("student.money", "300.01");
        Assert.assertFalse(isOK_false);
    }

    @Test
    public void testValidateString() {
        CommonDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        boolean isOK_true = ValidatorEngine.INSTANCE.validate("student.phone_number", "15973166256");
        Assert.assertTrue(isOK_true);

        boolean isOK_false = ValidatorEngine.INSTANCE.validate("student.phone_number", "159731662561");
        Assert.assertFalse(isOK_false);

        boolean isOK_false2 = ValidatorEngine.INSTANCE.validate("student.phone_number", "19973166256");
        Assert.assertFalse(isOK_false2);
    }

    @Test
    public void testValidateEnumString() {
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        boolean isOK1 = ValidatorEngine.INSTANCE.validate("student.gender", "male");
        Assert.assertTrue(isOK1);

        boolean isOK2 = ValidatorEngine.INSTANCE.validate("student.gender", "female");
        Assert.assertTrue(isOK2);
    }

    @Test
    public void testValidateEnumInt() {
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        boolean isOK1 = ValidatorEngine.INSTANCE.validate("student.game_card", 1);
        Assert.assertTrue(isOK1);

        boolean isOK2 = ValidatorEngine.INSTANCE.validate("student.game_card", 2);
        Assert.assertTrue(isOK2);

        boolean isOK_false1 = ValidatorEngine.INSTANCE.validate("student.game_card", 3);
        Assert.assertFalse(isOK_false1);
    }

    @Test
    public void testValidateEnumLong() {
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        boolean isOK1 = ValidatorEngine.INSTANCE.validate("student.game_long_card", 3000000000l);
        Assert.assertTrue(isOK1);

        boolean isOK2 = ValidatorEngine.INSTANCE.validate("student.game_long_card", 4000000000l);
        Assert.assertTrue(isOK2);

        boolean isOK_false1 = ValidatorEngine.INSTANCE.validate(
                "student.game_long_card", 2000000000l);
        Assert.assertFalse(isOK_false1);
    }


    @Test
    public void testGetFieldValidatorRulesJsonStr() {
        CommonDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        String jsonStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesJsonStr("student.game_long_card");
        System.out.println("testGetFieldValidatorRulesJsonStr => \n" + jsonStr);
        Assert.assertEquals("{\"fieldKey\":\"\",\"type\":\"enum_numeric\",\"enumValues\":[3000000000,4000000000]}", jsonStr);

        String jsonStr2 = ValidatorEngine.INSTANCE.getFieldValidatorRulesJsonStr("student.phone_number");
        System.out.println("testGetFieldValidatorRulesJsonStr => \n" + jsonStr2);
        Assert.assertEquals(
                "{\"fieldKey\":\"\"," +
                        "\"type\":\"string\"," +
                        "\"stringCharset\":\"utf8\"," +
                        "\"stringRegexKey\":\"phone_number\"," +
                        "\"stringLengthMin\":11," +
                        "\"stringLengthMax\":11," +
                        "\"regexStr\":\"^1[3|4|5|7|8][0-9]{9}$\"}", jsonStr2);
    }


    @Test
    public void testGenerateJavaEnumCode() {
        ValidatorEngine.INSTANCE.init("value_rules_enum_output_test.yml");

        String packageName = "com.baymax.pvg2.values";
        String sourceFormat = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);
        ValidatorEngine.INSTANCE.writeToFile("ValueEnumRange",
                packageName, sourceFormat, true);

        try {
            /**
             * 也许文件还没编译到Target中，就开始了下面代码，所以这里休息10秒
             */
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            String fullClassName = packageName + ".ValueEnumRange";
            Object obj = Class.forName(fullClassName).newInstance();
            Assert.assertTrue(obj.toString().startsWith(fullClassName));

            // 1.得到枚举类对象
            Class<Enum> clz = (Class<Enum>) Class.forName(fullClassName);
            // 2.得到所有枚举常量
            Class[] declaredClasses = clz.getDeclaredClasses();
            for(Class clz2 : declaredClasses) {
                Class[] declaredClasses2 = clz2.getDeclaredClasses();
                for(Class clz3 : declaredClasses2) {
                    Object[] objects = clz3.getEnumConstants();

                    Assert.assertEquals(2, objects.length);
                    Object enumConstant1 = objects[0];
                    Object enumConstant2 = objects[1];

                    Assert.assertEquals("NUMBER_3$11", enumConstant1.toString());
                    Assert.assertEquals("NUMBER_4000000000$123456", enumConstant2.toString());
                }
            }
        } catch (Exception e) {
            Assert.fail();
        }
    }


    /**
     * 因为生成的代码生成.class的时间不固定，所以在打包的时候注释掉这个测试
     */
    @Test
    public void testGenerateJavaEnumCodeWithRuleDict() {
        ValidatorEngine.INSTANCE.init("value_rules_enum_output_test_dict.yml");
        ValidatorEngine.INSTANCE.setFormatter(new IFormatter() {
            @Override
            public String formatJava(String originalContent) {
                try {
                    return new Formatter().formatSource(originalContent);
                } catch (FormatterException e) {
                    return originalContent;
                }
            }
        });

        String packageName = "com.baymax.generator.output.values";
        String sourceFormat = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);
        ValidatorEngine.INSTANCE.writeToFile("ValueEnumRange",
                packageName, sourceFormat, true);

        try {
            /**
             * 也许文件还没编译到Target中，就开始了下面代码，所以这里休息10秒
             */
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            String fullClassName = packageName + ".ValueEnumRange";
            Object obj = Class.forName(fullClassName).newInstance();
            Assert.assertTrue(obj.toString().startsWith(fullClassName));

            // 1.得到枚举类对象
            Class<Enum> clz = (Class<Enum>) Class.forName(fullClassName);
            // 2.得到所有枚举常量
            Class[] declaredClasses = clz.getDeclaredClasses();
            for(Class clz2 : declaredClasses) {
                Class[] declaredClasses2 = clz2.getDeclaredClasses();
                for(Class clz3 : declaredClasses2) {
                    Object[] objects = clz3.getEnumConstants();

                    for(Object k : objects) {
                        System.out.println(String.format("object %s", String.valueOf(k)));
                    }

                    Assert.assertEquals(3, objects.length);
                    Object enumConstant1 = objects[0];
                    Object enumConstant2 = objects[1];
                    Object enumConstant3 = objects[2];

                    Assert.assertEquals("STRING_bearing", enumConstant1.toString());
                    Assert.assertEquals("STRING_pad", enumConstant2.toString());
                    Assert.assertEquals("STRING_rotor", enumConstant3.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /**
     * 测试使用转义字典，生成代码
     *
     * 因为生成的代码生成.class的时间不固定，所以在打包的时候注释掉这个测试
     */
    //@Test
    public void testGenerateJavaEnumCodeWithEnumDict() {
        ValidatorEngine.INSTANCE.init("value_rules_enum_output_test_numeric_enum.yml");

        String packageName = "com.baymax.generator.output.values";
        String sourceFormat = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);
        ValidatorEngine.INSTANCE.writeToFile("ValueEnumRange",
                packageName, sourceFormat, true);

        try {
            /**
             * 也许文件还没编译到Target中，就开始了下面代码，所以这里休息10秒
             */
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            String fullClassName = packageName + ".ValueEnumRange";
            Object obj = Class.forName(fullClassName).newInstance();
            Assert.assertTrue(obj.toString().startsWith(fullClassName));

            // 1.得到枚举类对象
            Class<Enum> clz = (Class<Enum>) Class.forName(fullClassName);
            // 2.得到所有枚举常量
            Class[] declaredClasses = clz.getDeclaredClasses();
            for(Class clz2 : declaredClasses) {
                Class[] declaredClasses2 = clz2.getDeclaredClasses();
                for(Class clz3 : declaredClasses2) {
                    Object[] objects = clz3.getEnumConstants();

                    for(Object k : objects) {
                        System.out.println(String.format("object %s", String.valueOf(k)));
                    }

                    Assert.assertEquals(7, objects.length);
                    Object enumConstant0 = objects[0];
                    Object enumConstant1 = objects[1];
                    Object enumConstant2 = objects[2];
                    Object enumConstant3 = objects[3];
                    Object enumConstant4 = objects[4];
                    Object enumConstant5 = objects[5];
                    Object enumConstant6 = objects[6];


                    Assert.assertEquals("unpaid_0", enumConstant0.toString());
                    Assert.assertEquals("cancel_1", enumConstant1.toString());
                    Assert.assertEquals("paid_2", enumConstant2.toString());
                    Assert.assertEquals("refund_3", enumConstant3.toString());
                    Assert.assertEquals("received_4", enumConstant4.toString());
                    Assert.assertEquals("refund_return_5", enumConstant5.toString());
                    Assert.assertEquals("reject_6", enumConstant6.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }


    @Test
    public void testEnumStringWithCommonDict() {
        CommonDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr("student.domain");
        System.out.println(jsonMapStr);

        boolean isOK1 = ValidatorEngine.INSTANCE.validate("student.domain", "bearing");
        Assert.assertTrue(isOK1);

        boolean isOK2 = ValidatorEngine.INSTANCE.validate("student.domain", "rotor");
        Assert.assertTrue(isOK2);

        boolean isOK3 = ValidatorEngine.INSTANCE.validate("student.domain", "pad");
        Assert.assertTrue(isOK3);
    }

    @Test
    public void testEnumNumericWithCommonDict() {
        CommonDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules_enum_ref_numeric_enum.yml");
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr("order.status");
        System.out.println(jsonMapStr);

        for(int i = 1, len = 6; i <= len; i++) {
            boolean isOK = ValidatorEngine.INSTANCE.validate("order.status", i);
            Assert.assertTrue(isOK);
        }

        boolean isOK4 = ValidatorEngine.INSTANCE.validate("order.status", 7);
        Assert.assertTrue(isOK4 == false);
    }


    @Test
    public void testGenerateYml() {
        FieldRule rule1 = new StringRegexFieldRule();
        rule1.setFieldKey("common.id");
        rule1.setStringLengthMin(1);
        rule1.setStringLengthMax(128);
        rule1.setStringCharset("utf8");
        rule1.setType("String");


        FieldRule rule2 = new StringRegexFieldRule();
        rule2.setFieldKey("court.id");
        rule2.setStringLengthMin(2);
        rule2.setStringLengthMax(3);
        rule2.setStringCharset("utf8");
        rule2.setType("String");


        List<FieldRule> list = new ArrayList<>();
        list.add(rule1);
        list.add(rule2);

        String dumpStr = ValidatorEngine.INSTANCE.generateDefaultYml("value_rules_clean.yml", list);

        Yaml yaml = new Yaml();
        HashMap map = yaml.loadAs(dumpStr, HashMap.class);
        HashMap commonIdMap = (HashMap) ((HashMap) map.get("common")).get("id");
        HashMap courtIdMap = (HashMap) ((HashMap) map.get("court")).get("id");

        Assert.assertEquals(1, commonIdMap.get("string_length_min"));
        Assert.assertEquals(128, commonIdMap.get("string_length_max"));
        Assert.assertEquals("utf8", commonIdMap.get("string_charset"));
        Assert.assertEquals("String", commonIdMap.get("type"));

        Assert.assertEquals(2, courtIdMap.get("string_length_min"));
        Assert.assertEquals(3, courtIdMap.get("string_length_max"));
        Assert.assertEquals("utf8", courtIdMap.get("string_charset"));
        Assert.assertEquals("String", courtIdMap.get("type"));
    }



    @Test
    public void testValidateBean() {
        ValidatorEngine.INSTANCE.init("mysql", "value_rules.yml",
                null, new HashSet<String>() {{
                    add("id");
                    add("version");
                    add("is_deleted");
                    add("modifier");
                    add("creator");
                    add("created_at");
                    add("updated_at");
                }}, true);

        Student student = new Student();
        student.setId(1);
        student.setLargeNumber(9999);
        student.setPhoneNumber("15973166256A");
        List<String> errorKeys = ValidatorEngine.INSTANCE.validate(student, "student", null, null);
        Assert.assertTrue(!errorKeys.isEmpty());
    }
}
