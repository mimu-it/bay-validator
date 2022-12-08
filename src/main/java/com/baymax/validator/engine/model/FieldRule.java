/**
 * 
 */
package com.baymax.validator.engine.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author xiao.hu
 *
 */
public abstract class FieldRule {
	private String dbType = null;
	private String fieldKey = null;
	private String type = null;
	private BigInteger numericMin = null;
	private BigInteger numericMax = null;
	private BigDecimal decimalMin = null;
	private BigDecimal decimalMax = null;
	private String stringCharset = null;
	private String stringRegexKey = null;
	private Integer stringLengthMin = null;
	private Integer stringLengthMax = null;
	private List<Object> enumValues = null;

	public abstract boolean validate(Object value);

	public String getDbType() {
		return dbType;
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public String getFieldKey() {
		return fieldKey;
	}

	public void setFieldKey(String fieldKey) {
		this.fieldKey = fieldKey;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public BigInteger getNumericMin() {
		return numericMin;
	}

	public void setNumericMin(BigInteger numericMin) {
		this.numericMin = numericMin;
	}

	public BigInteger getNumericMax() {
		return numericMax;
	}

	public void setNumericMax(BigInteger numericMax) {
		this.numericMax = numericMax;
	}

	public BigDecimal getDecimalMin() {
		return decimalMin;
	}

	public void setDecimalMin(BigDecimal decimalMin) {
		this.decimalMin = decimalMin;
	}

	public BigDecimal getDecimalMax() {
		return decimalMax;
	}

	public void setDecimalMax(BigDecimal decimalMax) {
		this.decimalMax = decimalMax;
	}

	public String getStringCharset() {
		return stringCharset;
	}

	public void setStringCharset(String stringCharset) {
		this.stringCharset = stringCharset;
	}

	public String getStringRegexKey() {
		return stringRegexKey;
	}

	public void setStringRegexKey(String stringRegexKey) {
		this.stringRegexKey = stringRegexKey;
	}

	public Integer getStringLengthMin() {
		return stringLengthMin;
	}

	public void setStringLengthMin(Integer stringLengthMin) {
		this.stringLengthMin = stringLengthMin;
	}

	public Integer getStringLengthMax() {
		return stringLengthMax;
	}

	public void setStringLengthMax(Integer stringLengthMax) {
		this.stringLengthMax = stringLengthMax;
	}

	public List<Object> getEnumValues() {
		return enumValues;
	}

	public void setEnumValues(List<Object> enumValues) {
		this.enumValues = enumValues;
	}
}
