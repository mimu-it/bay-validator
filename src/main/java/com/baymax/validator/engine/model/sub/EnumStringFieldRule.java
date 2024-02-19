package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.CommonDict;
import com.baymax.validator.engine.common.Common;
import com.baymax.validator.engine.model.FieldRule;

import java.util.List;
import java.util.Map;

/**
 * @author xiao.hu
 *
 * @date 2022-03-30
 * @apiNote
 */
public class EnumStringFieldRule extends FieldRule {

    @Override
    public void build(String fieldKey, String type, Map<String, Object> rulesMap) {
        /**
         * enum的相关配置
         */
        List<Object> enumValuesList = Common.getEnumValues(rulesMap);
        this.setFieldKey(fieldKey);
        this.setType(type);
        this.setEnumValues(enumValuesList);
    }

    @Override
    public boolean validate(Object realVal) {
        String valueStr = String.valueOf(realVal);
        Object enumValuesObj = super.getEnumValues();
        if(enumValuesObj == null) {
            throw new IllegalStateException("enum values is null");
        }

        List<String> enumValues = null;
        if(enumValuesObj instanceof String) {
            enumValues = (List<String>) CommonDict.INSTANCE.getRule((String) enumValuesObj);
        }
        else if(enumValuesObj instanceof List) {
            enumValues = (List<String>) enumValuesObj;
        }

        if(enumValues == null) {
            throw new IllegalStateException("enum values is null");
        }

        for(Object obj : enumValues) {
            String objVal = (String) obj;
            if(objVal.equals(valueStr)) {
                return true;
            }
        }

        return false;
    }
}
