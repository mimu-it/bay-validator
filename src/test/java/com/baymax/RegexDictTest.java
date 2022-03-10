package com.baymax;

import com.baymax.validator.engine.RegexDict;
import org.junit.Assert;
import org.junit.Test;


public class RegexDictTest {

    @Test
    public void testGetRegex() {
        RegexDict.INSTANCE.init();
        String regexStr = RegexDict.INSTANCE.getRegex("positive_integer2");

        /**
         * main/resources 和 test/resources 中如果有同名文件，这里是优先加载test/resources中的文件的
         */
        String regexStr2 = RegexDict.INSTANCE.getRegex("positive_integer");
        System.out.println("regexStr2 => " + regexStr2); //regexStr2 => null
        Assert.assertEquals("^[1-9]\\d*$", regexStr);
    }
}
