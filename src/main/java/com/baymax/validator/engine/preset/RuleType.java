package com.baymax.validator.engine.preset;

/**
 * @author xiao.hu
 * @date 2024-02-19
 * @apiNote
 */
public enum RuleType {

    /**
     * yml校验规则的类型
     */
    numeric,
    decimal,
    string,
    enum_string,
    enum_numeric,
    enum_decimal,
    date,
    datetime;

    public static boolean isEnum(String type) {
        return enum_string.name().equals(type)
                || enum_numeric.name().equals(type)
                || enum_decimal.name().equals(type);
    }
}
