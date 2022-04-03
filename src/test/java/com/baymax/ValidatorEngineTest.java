package com.baymax;

import com.baymax.validator.engine.RegexDict;
import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.model.sub.StringRegexFieldRule;
import com.baymax.vo.Student;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.junit.Assert;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
        RegexDict.INSTANCE.init();
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
        RegexDict.INSTANCE.init();
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
        String sourceFormated = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);
        ValidatorEngine.INSTANCE.writeToFile("ValueEnumRange",
                packageName, sourceFormated, true);

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


    //@Test
    public void testGenerateJavaEnumCodeWithDict() {
        ValidatorEngine.INSTANCE.init("value_rules_enum_output_test_dict.yml");

        String packageName = "com.baymax.pvg2.values";
        String sourceFormated = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);
        ValidatorEngine.INSTANCE.writeToFile("ValueEnumRange",
                packageName, sourceFormated, true);

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

    //@Test
    public void testJsValidatorEnumInteger() {
        RegexDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr("student.game_long_card");

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

        String jsPath = App.class.getClassLoader().getResource("hxValidator.js").getPath();

        try {
            String scriptStr = new String(Files.readAllBytes(Paths.get(jsPath)));
            engine.eval(scriptStr);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        engine.put("fieldConfig", jsonMapStr);
        engine.put("key", "student.game_long_card");
        engine.put("value", 30000000001l);
        try {
            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("参数不符合规范", result);
        } catch (ScriptException e) {
            throw new IllegalStateException(e);
        }


        engine.put("value", 3000000000l);
        try {
            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("", result);
        } catch (ScriptException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testJsValidatorEnumDecimal() {
        RegexDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");

        String fieldKey = "student.float_card";
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr(fieldKey);

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

        String jsPath = App.class.getClassLoader().getResource("hxValidator.js").getPath();

        try {
            String scriptStr = new String(Files.readAllBytes(Paths.get(jsPath)));
            engine.eval(scriptStr);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        engine.put("fieldConfig", jsonMapStr);
        engine.put("key", fieldKey);
        engine.put("value", 1000000000.123456);
        try {
            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("浮点类型请使用字符串参数", result);
        } catch (ScriptException e) {
            throw new IllegalStateException(e);
        }

        try {
            String jsonStr = mapper.writeValueAsString("4000000000.123456");
            System.out.println("jsonStr: " + jsonStr);
            engine.put("value", jsonStr);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        try {
            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("", result);
        } catch (ScriptException e) {
            throw new IllegalStateException(e);
        }
    }


    @Test
    public void testEnumStringWithDict() {
        RegexDict.INSTANCE.init();
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
    public void testJsValidatorEnumDecimalMultiSelection() {
        RegexDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");

        String fieldKey = "student.float_card";
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr(fieldKey);

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

        String jsPath = App.class.getClassLoader().getResource("hxValidator.js").getPath();

        try {
            String scriptStr = new String(Files.readAllBytes(Paths.get(jsPath)));
            engine.eval(scriptStr);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        List<String> mItems = new ArrayList<>();
        mItems.add("4000000000.123456");
        mItems.add("3.12");

        engine.put("fieldConfig", jsonMapStr);
        engine.put("key", fieldKey);

        try {
            String jsonStr = mapper.writeValueAsString(mItems);
            System.out.println("jsonStr: " + jsonStr);
            engine.put("value", jsonStr);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        try {
            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("参数不符合规范", result);
        } catch (ScriptException e) {
            throw new IllegalStateException(e);
        }

        List<String> mItems2 = new ArrayList<>();
        mItems2.add("4000000000.123456");
        mItems2.add("3.11");
        try {
            String jsonStr = mapper.writeValueAsString(mItems2);
            System.out.println("jsonStr: " + jsonStr);
            engine.put("value", jsonStr);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        try {
            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("", result);
        } catch (ScriptException e) {
            throw new IllegalStateException(e);
        }
    }


    @Test
    public void testJsValidatorString() {
        RegexDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");

        String fieldKey = "student.phone_number";
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr(fieldKey);

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

        String jsPath = App.class.getClassLoader().getResource("hxValidator.js").getPath();

        try {
            String scriptStr = new String(Files.readAllBytes(Paths.get(jsPath)));
            engine.eval(scriptStr);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        engine.put("fieldConfig", jsonMapStr);
        engine.put("key", fieldKey);

        try {
            String jsonStr = mapper.writeValueAsString("139731662566");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("参数字符长度不符合规则", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }


        try {
            String jsonStr = mapper.writeValueAsString("13973166256");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        try {
            String jsonStr = mapper.writeValueAsString("1397316625A");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("参数字符不符合规则", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @Test
    public void testJsValidatorString2() {
        RegexDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");

        String fieldKey = "student.user_name";
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr(fieldKey);

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

        String jsPath = App.class.getClassLoader().getResource("hxValidator.js").getPath();

        try {
            String scriptStr = new String(Files.readAllBytes(Paths.get(jsPath)));
            engine.eval(scriptStr);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        engine.put("fieldConfig", jsonMapStr);
        engine.put("key", fieldKey);

        try {
            String jsonStr = mapper.writeValueAsString("你好A");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        try {
            String jsonStr = mapper.writeValueAsString("你好A+");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("参数字符不符合规则", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @Test
    public void testJsValidatorNumber() {
        RegexDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");

        String fieldKey = "student.age";
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr(fieldKey);

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

        String jsPath = App.class.getClassLoader().getResource("hxValidator.js").getPath();

        try {
            String scriptStr = new String(Files.readAllBytes(Paths.get(jsPath)));
            engine.eval(scriptStr);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        engine.put("fieldConfig", jsonMapStr);
        engine.put("key", fieldKey);

        try {
            engine.put("value", 0);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        try {
            engine.put("value", 32);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }


        try {
            engine.put("value", 121);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("数值型大小校验规则不符合规则", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        try {
            engine.put("value", -1);

            String result = (String) engine.eval("testHxValidator(fieldConfig, key, value)");
            Assert.assertEquals("数值型大小校验规则不符合规则", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @Test
    public void testJsValidatorStringLengthUTF8() {
        RegexDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");

        String fieldKey = "student.user_name";
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr(fieldKey);

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

        String jsPath = App.class.getClassLoader().getResource("hxValidator.js").getPath();

        try {
            String scriptStr = new String(Files.readAllBytes(Paths.get(jsPath)));
            engine.eval(scriptStr);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        engine.put("fieldConfig", jsonMapStr);
        engine.put("key", fieldKey);

        try {
            String jsonStr = mapper.writeValueAsString("你");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testStringLength(fieldConfig, key, value)");
            Assert.assertEquals("", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }


        try {
            String jsonStr = mapper.writeValueAsString("A");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testStringLength(fieldConfig, key, value)");
            Assert.assertEquals("参数字符长度不符合规则", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }



    @Test
    public void testJsValidatorStringLengthGBK() {
        RegexDict.INSTANCE.init();
        ValidatorEngine.INSTANCE.init("value_rules.yml");

        String fieldKey = "student.user_name_gbk";
        String jsonMapStr = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr(fieldKey);

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

        String jsPath = App.class.getClassLoader().getResource("hxValidator.js").getPath();

        try {
            String scriptStr = new String(Files.readAllBytes(Paths.get(jsPath)));
            engine.eval(scriptStr);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        engine.put("fieldConfig", jsonMapStr);
        engine.put("key", fieldKey);

        try {
            String jsonStr = mapper.writeValueAsString("你");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testStringLength(fieldConfig, key, value)");
            Assert.assertEquals("参数字符长度不符合规则", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }


        try {
            String jsonStr = mapper.writeValueAsString("A");
            engine.put("value", jsonStr);

            String result = (String) engine.eval("testStringLength(fieldConfig, key, value)");
            Assert.assertEquals("参数字符长度不符合规则", result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
        Assert.assertEquals("common:\n" +
                "  id:\n" +
                "    string_charset: utf8\n" +
                "    string_length_min: 1\n" +
                "    string_length_max: 128\n" +
                "    type: String\n" +
                "court:\n" +
                "  id:\n" +
                "    string_charset: utf8\n" +
                "    string_length_min: 2\n" +
                "    string_length_max: 3\n" +
                "    type: String\n", dumpStr);
    }



    @Test
    public void testValidateBean() {
        ValidatorEngine.INSTANCE.init("value_rules.yml",
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
