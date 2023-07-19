package com.baymax;

import com.baymax.validator.engine.CommonDict;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;


public class CommonDictTest {

    @Test
    public void testGetRegex() {
        CommonDict.INSTANCE.init();
        String regexStr = (String) CommonDict.INSTANCE.getRule("positive_integer2");

        /**
         * main/resources 和 test/resources 中如果有同名文件，这里是优先加载test/resources中的文件的
         */
        String regexStr2 = (String) CommonDict.INSTANCE.getRule("positive_integer");
        System.out.println("regexStr2 => " + regexStr2); //regexStr2 => null
        Assert.assertEquals("^[1-9]\\d*$", regexStr);
    }

    @Test
    public void testGetRuleOfNumericEnum() {
        CommonDict.INSTANCE.init();
        List<Object> rule = (List<Object>) CommonDict.INSTANCE.getRule("order_status");
        System.out.println("rule => " + rule);
        Assert.assertEquals(rule.toString(), "[1, 2, 3, 4, 5, 6]");
    }


    @Test
    public void testGetRuleOfNumericDict() {
        CommonDict.INSTANCE.init();
        Map<String, Object> rule = (Map<String, Object>) CommonDict.INSTANCE.getRule("order_status_dict");
        System.out.println("rule => " + rule);
        Assert.assertEquals(rule.toString(), "{0=unpaid, 1=cancel, 2=paid, 3=refund, 4=received, 5=refund_return, 6=reject}");
    }
}
