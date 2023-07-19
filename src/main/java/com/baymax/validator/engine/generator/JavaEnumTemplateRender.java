package com.baymax.validator.engine.generator;

import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.utils.StrUtil;
import com.jfinal.kit.Kv;
import com.jfinal.template.Engine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * @author xiao.hu
 */
public class JavaEnumTemplateRender {
    private static Engine engine = Engine.use();

    /**
     * 根据type类型构造je，方便代码生成时的对象使用
     * @param fieldName
     * @param type
     * @param enumValues
     * @return
     */
    public static JavaEnum build(String fieldName, String type, List enumValues, Map enumDict) {
        JavaEnum je = new JavaEnum();
        je.setFieldName(fieldName);

        if(ValidatorEngine.RuleType.enum_numeric.name().equals(type)) {
            je.setJavaType(BigInteger.class.getSimpleName());
            je.setCanonicalJavaType(BigInteger.class.getCanonicalName());
            je.setEnumValues(buildEnumValuesStr(enumValues, Const.JavaType.NUMBER.name(), je.getJavaType(), enumDict));
        }
        else if(ValidatorEngine.RuleType.enum_decimal.name().equals(type)) {
            /**
             * 由于double的精度是有误差的，所以yml配置enum_decimal时，务必使用string
             * 举例：'4000000000.123456'
             */
            je.setJavaType(BigDecimal.class.getSimpleName());
            je.setCanonicalJavaType(BigDecimal.class.getCanonicalName());
            je.setEnumValues(buildEnumValuesStr(enumValues, Const.JavaType.NUMBER.name(), je.getJavaType(), enumDict));
        }
        else {
            je.setJavaType(String.class.getSimpleName());
            je.setCanonicalJavaType(String.class.getCanonicalName());
            je.setEnumValues(buildEnumValuesStr(enumValues, Const.JavaType.STRING.name(), je.getJavaType(), enumDict));
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
    private static String buildEnumValuesStr(List<Object> enumValues, String prefix, String javaType,
                                             Map<Object, String> dict) {
        StringBuffer sb = new StringBuffer();
        for (Object val : enumValues) {
            String valStr = String.valueOf(val);

            if(valStr == null) {
                throw new IllegalStateException(String.format("enumValues's item is null"));
            }

            String valueMeaning = prefix;
            if(dict != null) {
                String meaning = dict.get(val);
                if(StrUtil.isNotBlank(meaning)) {
                    valueMeaning = meaning;
                }
            }

            Kv cond = Kv.by(Const.TemplateKey.prefix.name(), valueMeaning)
                    .set(Const.TemplateKey.javaType.name(), javaType)
                    .set(Const.TemplateKey.value.name(), valStr)
                    .set(Const.TemplateKey.valueFormat.name(), formatValueEscapeDot(valStr));

            String enumValueStr = engine.getTemplate(Const.ENUM_VALUES_TEMPLATE_FILENAME).renderToString(cond);
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
