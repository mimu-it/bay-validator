package com.baymax;

import static org.junit.Assert.assertTrue;

import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.model.sub.StringRegexFieldRule;
import com.baymax.validator.engine.utils.BeanUtil;
import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void testBeanInfoUtil() {
        FieldRule rule = new StringRegexFieldRule();
        rule.setFieldKey("common.id");
        rule.setStringLengthMin(1);
        rule.setStringLengthMax(128);
        rule.setStringCharset("utf8");
        rule.setType("String");

        Map<String, Object> map = BeanUtil.getPropertyByIntrospector(rule);
        System.out.println(map);
        Assert.assertEquals("{stringCharset=utf8, fieldKey=common.id, dbType=null, stringRegexKey=null, numericMin=null, type=String, decimalMax=null, stringLengthMax=128, stringLengthMin=1, enumDict=null, class=class com.baymax.validator.engine.model.sub.StringRegexFieldRule, decimalMin=null, numericMax=null, enumValues=null}",
                map.toString());


        map = BeanUtil.objectToMap(rule);
        System.out.println(map);
        Assert.assertEquals("{}", map.toString());

        map = BeanUtil.beanToMap(rule);
        System.out.println(map);
        Assert.assertEquals("{dbType=null, fieldKey=common.id, type=String, " +
                "numericMin=null, numericMax=null, decimalMin=null, decimalMax=null, " +
                "stringCharset=utf8, stringRegexKey=null, stringLengthMin=1, " +
                "stringLengthMax=128, enumValues=null, enumDict=null}", map.toString());
    }

    /**
     * 测试使用yaml引擎是否成功
     */
    @Test
    public void testYAMLEngine() {
        Yaml yaml = new Yaml();
        String document = " a: 1\n b:\n c: 3\n d: 4\n";
        Assert.assertEquals("{a: 1, b: null, c: 3, d: 4}\n", yaml.dump(yaml.load(document)));

        //yaml Map取值
        String document1 = "hello: 25";
        Map map = (Map<String, Object>) yaml.load(document1);
        Assert.assertEquals("{hello=25}", map.toString());
        Assert.assertEquals(25, map.get("hello"));
    }

    /**
     * 测试加载 src/main/resources/regex_dict.yml是否成功
     */
    @Test
    public void testLoadRegexYml() {
        Yaml yaml = new Yaml();
        //文件路径是相对类目录(src/main/java)的相对路径
        InputStream is = null;
        Map<String, Object> map;
        try {
            is = App.class.getClassLoader().getResourceAsStream(Const.COMMON_DICT_FILENAME);
            map = yaml.loadAs(is, Map.class);
        } finally {
            try {
                if(is != null) { is.close(); } //再一次关闭inputStream，这样会引起 Stream closed 异常
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String positiveIntegerRegex = map.getOrDefault("positive_integer2", "123").toString();
        Assert.assertEquals("^[1-9]\\d*$", positiveIntegerRegex);
    }

    /**
     * 读取配置文件内容为java对象之后，再反向生成新的yaml配置，测试两者是否相同
     */
    @Test
    public void testBasicValueRules() {
        Yaml yaml = new Yaml();
        InputStream is = null;
        Map<String, Object> map;
        String originValueRulesStr;
        try {
            is = App.class.getClassLoader().getResourceAsStream("value_rules.yml");

            /**
             *  读取所有输入,包括回车换行符
             *  \\A为正则表达式,表示从字符头开始
             */
            Scanner s = new Scanner(is).useDelimiter("\\A");
            originValueRulesStr = s.hasNext() ? s.next() : "";

            map = yaml.loadAs(originValueRulesStr, Map.class);
        } finally {
            try {
                if(is != null) { is.close(); } //再一次关闭inputStream，这样会引起 Stream closed 异常
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //dumpStr 最后一行之后始终会多一个换行
        String dumpStr = yaml.dumpAs(map, Tag.MAP, DumperOptions.FlowStyle.BLOCK);
        Assert.assertEquals(originValueRulesStr.trim(), dumpStr.trim());

        String dumpCleanStr = yaml.dumpAs(ValidatorEngine.cleanCopyOfMap(map), Tag.MAP, DumperOptions.FlowStyle.BLOCK);
        System.out.println("dumpCleanStr:" + dumpCleanStr);


        // 当yaml文件中配置的值是 null时，其在java中就是null，而不是字符串"null"
        Object numericMin = ((Map)((Map)map.get("student")).get("gender")).get("numeric_min");
        Assert.assertEquals(null, numericMin);

        Object studentIdType = ((Map)((Map)map.get("student")).get("id")).get("type");
        Assert.assertEquals("numeric", studentIdType);

        Object studentIdNumericMin = ((Map)((Map)map.get("student")).get("id")).get("numeric_min");
        Assert.assertEquals(1, studentIdNumericMin);

        Object studentIdNumericMax = ((Map)((Map)map.get("student")).get("id")).get("numeric_max");
        Assert.assertEquals(128, studentIdNumericMax);

        Object studentIdStringCharset = ((Map)((Map)map.get("student")).get("id")).get("string_charset");
        Assert.assertEquals("utf8", studentIdStringCharset);

        Object studentIdStringRegexKey = ((Map)((Map)map.get("student")).get("id")).get("string_regex_key");
        Assert.assertEquals("positive_integer", studentIdStringRegexKey);

        ArrayList studentGenderEnum = (ArrayList)((Map)((Map)map.get("student")).get("gender")).get("enum_values");
        Assert.assertEquals(2, studentGenderEnum.size());
        Assert.assertTrue(studentGenderEnum.contains("male"));
        Assert.assertTrue(studentGenderEnum.contains("female"));
    }


    /**
     * 读取配置文件内容为java的Map之后，清理Map再反向生成新的yaml配置，测试是否生成干净的目标配置
     */
    @Test
    public void testLoadAndDumpYaml() {
        Yaml yaml = new Yaml();
        InputStream is = null;
        Map<String, Object> map;
        String originValueRulesStr;
        try {
            is = App.class.getClassLoader().getResourceAsStream("value_rules_clean.yml");

            /**
             *  读取所有输入,包括回车换行符
             *  \\A为正则表达式,表示从字符头开始
             */
            Scanner s = new Scanner(is).useDelimiter("\\A");
            originValueRulesStr = s.hasNext() ? s.next() : "";

            map = yaml.loadAs(originValueRulesStr, Map.class);
        } finally {
            try {
                if(is != null) { is.close(); } //再一次关闭inputStream，这样会引起 Stream closed 异常
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //dumpStr 最后一行之后始终会多一个换行
        String dumpCleanStr = yaml.dumpAs(ValidatorEngine.cleanCopyOfMap(map), Tag.MAP, DumperOptions.FlowStyle.BLOCK);
        System.out.println("dumpCleanStr:\n" + dumpCleanStr);
        Assert.assertEquals("student:\n" +
                "  id:\n" +
                "    type: integer\n" +
                "    numeric_min: 1\n" +
                "    numeric_max: 128", dumpCleanStr.trim());
    }

}
