package com.baymax.validator.engine.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * @author xiao.hu
 * @date 2023-07-12
 * @apiNote  针对所有可能的数值，参考java的分类，对整型和浮点型选择通用容器进行保存
 */
public class ParamUtil {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
            decimal = new BigDecimal(String.valueOf(decimalObj));
        }

        return decimal;
    }

    /**
     *
     * @param rulesMap
     * @param key
     * @return
     */
    public static Date getDate(Map<String, Object> rulesMap, String key) {
        return getDate(rulesMap, key, dateFormat);
    }

    /**
     *
     * @param rulesMap
     * @param key
     * @return
     */
    public static Date getDatetime(Map<String, Object> rulesMap, String key) {
        return getDate(rulesMap, key, datetimeFormat);
    }

    /**
     *
     * @param rulesMap
     * @param key
     * @param datetimeFormat
     * @return
     */
    private static Date getDate(Map<String, Object> rulesMap, String key, SimpleDateFormat datetimeFormat) {
        Object dateObj = rulesMap.get(key);
        if(dateObj != null) {
            if(dateObj instanceof String) {
                String dateStr = String.valueOf(dateObj);
                try {
                    return datetimeFormat.parse(dateStr);
                } catch (Exception e) {
                    return null;
                }
            }
            else if(dateObj instanceof Date) {
                return (Date)dateObj;
            }
        }

        return null;
    }
}
