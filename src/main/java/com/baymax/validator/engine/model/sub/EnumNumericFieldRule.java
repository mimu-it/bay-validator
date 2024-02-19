package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.CommonDict;
import com.baymax.validator.engine.common.Common;
import com.baymax.validator.engine.model.FieldRule;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * 数字型枚举
 * @param <T>
 * @author xiao.hu
 */
public class EnumNumericFieldRule<T extends Comparable> extends FieldRule {

    private Class<T> entityClass;

    public EnumNumericFieldRule(Class<T> clazz) {
        this.entityClass = clazz;
    }


    @Override
    public void build(String fieldKey, String type, Map<String, Object> rulesMap) {
        /**
         * enum的相关配置
         */
        List<Object> enumValuesList = Common.getEnumValues(rulesMap);
        Map<Object, String> enumDict = Common.getEnumDict(rulesMap);
        this.setFieldKey(fieldKey);
        this.setType(type);
        this.setEnumValues(enumValuesList);
        this.setEnumDict(enumDict);
    }

    @Override
    public boolean validate(Object value) {
        String valueStr = String.valueOf(value);
        Comparable realVal = transform(valueStr);
        if(realVal == null) {
            return false;
        }

        Object enumValuesObj = super.getEnumValues();
        if(enumValuesObj == null) {
            throw new IllegalStateException("enum values is null");
        }

        List<Object> enumValues = null;
        if(enumValuesObj instanceof String) {
            enumValues = (List<Object>) CommonDict.INSTANCE.getRule((String) enumValuesObj);
        }
        else if(enumValuesObj instanceof List) {
            enumValues = (List<Object>) enumValuesObj;
        }

        if(enumValues == null) {
            throw new IllegalStateException("enum values is null");
        }

        for(Object obj : enumValues) {
            Comparable objVal = transform(String.valueOf(obj));
            if(objVal == null) {
                return false;
            }

            if(objVal.compareTo(realVal) == 0) {
                return true;
            }
        }

        return false;
    }



    private Comparable transform(String value) {
        if(entityClass.isAssignableFrom(BigInteger.class)) {
            return new BigInteger(value);
        }
        else if(entityClass.isAssignableFrom(BigDecimal.class)) {
            return new BigDecimal(value);
        }

        return null;
    }
}
