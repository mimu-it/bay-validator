package com.baymax.validator.engine.constant;

/**
 * @author xiao.hu
 * @date 2023-03-04
 * @apiNote
 */
public class Const {

    public static final String COMMON_DICT_FILENAME = "common_dict.yml";
    public static final String VALUE_RULES_FILENAME = "value_rules.yml";
    public static final String ENUM_FIELD_RULE_FILENAME = "EnumFieldRule.enjoy";
    public static final String ENUM_VALUES_TEMPLATE_FILENAME = "EnumValues.enjoy";
    public static final String TABLE_FILENAME = "Table.enjoy";
    public static final String VALUE_ENUM_RANGE_FILENAME = "ValueEnumRange.enjoy";
    public static final String HX_VALIDATOR = "hxValidator";

    public enum TemplateKey {
        fieldName,
        enumValues,
        javaType,
        prefix,
        value,
        valueFormat,
        tableName,
        fieldRuleList,
        tableList,
        generateTime
    }

    public enum JavaType {
        NUMBER,
        STRING
    }

    public enum FileType {
        java,
        js
    }
}
