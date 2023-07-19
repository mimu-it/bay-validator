package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.CommonDict;
import com.baymax.validator.engine.model.FieldRule;

import java.util.List;

/**
 * @author xiao.hu
 *
 * @date 2022-03-30
 * @apiNote
 */
public class EnumStringFieldRule extends FieldRule {

    @Override
    public boolean validate(String realVal) {
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
            if(objVal.equals(realVal)) {
                return true;
            }
        }

        return false;
    }
}
