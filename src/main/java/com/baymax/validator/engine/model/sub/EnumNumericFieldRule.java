package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;

import java.util.List;

public class EnumNumericFieldRule<T extends Comparable> extends FieldRule {

    @Override
    public boolean validate(Object value) {
        T realVal = (T) value;

        List<Object> enumValues = super.getEnumValues();
        if(enumValues == null) {
            throw new IllegalStateException("enum values is null");
        }

        for(Object obj : enumValues) {
            T objVal = (T) obj;
            if(objVal.compareTo(realVal) == 0) {
                return true;
            }
        }

        return false;
    }
}
