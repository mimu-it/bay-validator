package com.baymax.validator.engine.common;

import com.baymax.validator.engine.CommonDict;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.preset.RuleType;
import com.baymax.validator.engine.utils.NameUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author xiao.hu
 * @date 2024-02-19
 * @apiNote
 */
public class Common {


    public static List<Object> getEnumValues(Map<String, Object> rulesMap) {
        Object enumValues = rulesMap.get(RuleKey.enum_values.name());
        List<Object> enumValuesList = null;
        if(enumValues instanceof List) {
            enumValuesList = (List<Object>) enumValues;
        }
        else if(enumValues instanceof String) {
            enumValuesList = CommonDict.INSTANCE.getList((String) enumValues);
        }
        return enumValuesList;
    }


    /**
     * 对于 number 类型的枚举值，可以在 enum_dict 中配置转义字典
     * @param rulesMap
     * @return
     */
    public static Map<Object, String> getEnumDict(Map<String, Object> rulesMap) {
        Object enumDict = rulesMap.get(RuleKey.enum_dict.name());
        if(enumDict instanceof Map) {
            return (Map<Object, String>) enumDict;
        }
        return null;
    }
}
