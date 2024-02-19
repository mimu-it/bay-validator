package com.baymax.validator.engine.preset;

/**
 * @author xiao.hu
 * @date 2024-02-19
 * @apiNote
 */
public enum RuleKey {

    /**
     * yml文件的规则键值
     */
    type,
    numeric_min,
    numeric_max,
    decimal_min,
    decimal_max,
    string_charset,
    string_regex_key,
    string_length_min,
    string_length_max,
    enum_values,
    enum_dict,
    begin_at,
    end_at
}
