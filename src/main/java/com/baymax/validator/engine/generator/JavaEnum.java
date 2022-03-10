package com.baymax.validator.engine.generator;

public class JavaEnum {

    private String fieldName;
    private String javaType;
    private String canonicalJavaType;
    private String enumValues;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getCanonicalJavaType() {
        return canonicalJavaType;
    }

    public void setCanonicalJavaType(String canonicalJavaType) {
        this.canonicalJavaType = canonicalJavaType;
    }

    public String getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(String enumValues) {
        this.enumValues = enumValues;
    }
}
