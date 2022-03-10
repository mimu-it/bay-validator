package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;

import java.util.List;

public class EnumStringFieldRule extends FieldRule {

    @Override
    public boolean validate(Object value) {
        String realVal = (String) value;

        List<Object> enumValues = super.getEnumValues();
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
