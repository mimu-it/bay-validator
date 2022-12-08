package com.baymax.validator.engine.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public class ParamUtil {

    /**
     * 从yml文件中获取参数配置值，转换成BigInteger
     * @param rulesMap
     * @param key
     * @return
     */
    public static BigInteger getBigInteger(Map<String, Object> rulesMap, String key) {
        BigInteger numeric = null;
        Object numericObj = rulesMap.get(key);
        if(numericObj != null) {
            if(numericObj instanceof BigInteger) {
                numeric = (BigInteger) numericObj;
            } else {
                numeric = new BigInteger(String.valueOf(numericObj));
            }
        }

        return numeric;
    }

    /**
     * 从yml文件中获取参数配置值，转换成BigDecimal
     * @param rulesMap
     * @param key
     * @return
     */
    public static BigDecimal getBigDecimal(Map<String, Object> rulesMap, String key) {
        BigDecimal decimal = null;
        Object decimalObj = rulesMap.get(key);

        if(decimalObj != null) {
            /*if(!(decimalObj instanceof String)) {
                throw new IllegalArgumentException("The type of yaml decimal option must be string");
            }*/
            decimal = new BigDecimal(String.valueOf(decimalObj));
        }

        return decimal;
    }
}
