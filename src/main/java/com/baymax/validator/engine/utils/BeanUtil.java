package com.baymax.validator.engine.utils;

import tools.jackson.databind.json.JsonMapper;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BeanUtil {

    public static Map<String, Object> getPropertyByIntrospector(Object bean) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] proDescritptors = beanInfo.getPropertyDescriptors();

            Map<String, Object> map = new HashMap<>(proDescritptors.length);
            if (proDescritptors != null && proDescritptors.length > 0) {
                for (PropertyDescriptor propDesc : proDescritptors) {
                    String property = propDesc.getName();

                    Method methodGetUserName = propDesc.getReadMethod();
                    Object value = methodGetUserName.invoke(bean);
                    map.put(property, value);
                }
            }
            return map;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        try {
            Field[] declaredFields = obj.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(obj));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return map;
    }

    public static Map<String, Object> beanToMap(JsonMapper mapper, Object obj) {
        try {
            return mapper.readValue(mapper.writeValueAsString(obj), Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
