package com.baymax.validator.engine.generator;

import com.baymax.validator.engine.ValidatorEngine;
import com.jfinal.kit.Kv;
import com.jfinal.template.Engine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class JavaEnumTemplateRender {
    static Engine engine = Engine.use();

    /**
     * 根据type类型构造je，方便代码生成时的对象使用
     * @param fieldName
     * @param type
     * @param enumValues
     * @return
     */
    public static JavaEnum build(String fieldName, String type, List<Object> enumValues) {
        JavaEnum je = new JavaEnum();
        je.setFieldName(fieldName);

        if(ValidatorEngine.RuleType.enum_numeric.name().equals(type)) {
            je.setJavaType(BigInteger.class.getSimpleName());
            je.setCanonicalJavaType(BigInteger.class.getCanonicalName());
            je.setEnumValues(buildEnumValuesStr(enumValues, "NUMBER", je.getJavaType()));
        }
        else if(ValidatorEngine.RuleType.enum_decimal.name().equals(type)) {
            /**
             * 由于double的精度是有误差的，所以yml配置enum_decimal时，务必使用string
             * 举例：'4000000000.123456'
             */
            je.setJavaType(BigDecimal.class.getSimpleName());
            je.setCanonicalJavaType(BigDecimal.class.getCanonicalName());
            je.setEnumValues(buildEnumValuesStr(enumValues, "NUMBER", je.getJavaType()));
        }
        else {
            je.setJavaType(String.class.getSimpleName());
            je.setCanonicalJavaType(String.class.getCanonicalName());
            je.setEnumValues(buildEnumValuesStr(enumValues, "STRING", je.getJavaType()));
        }

        return je;
    }

    /**
     * 构建enum中的值，以string的形式返回给代码生成引擎用于代码输出
     * @param enumValues
     * @param prefix
     * @param javaType
     * @return
     */
    private static String buildEnumValuesStr(List<Object> enumValues, String prefix, String javaType) {
        StringBuffer sb = new StringBuffer();
        for (Object val : enumValues) {
            String valStr = String.valueOf(val);

            if(valStr == null) {
                throw new IllegalStateException(String.format("enumValues's item is null"));
            }

            Kv cond = Kv.by("prefix", prefix).set("javaType", javaType)
                    .set("value", valStr).set("valueFormated", formatValueEscapeDot(valStr));
            String enumValueStr = engine.getTemplate("EnumValues.enjoy").renderToString(cond);
            sb.append(",").append(enumValueStr);
        }

        if (sb.length() > 0) {
            return sb.substring(1);
        }
        return null;
    }

    /**
     * 将小数点转义成$符号
     * @param value
     * @return
     */
    private static String formatValueEscapeDot(String value) {
        return value.replaceAll("\\.", "\\$");
    }
}
