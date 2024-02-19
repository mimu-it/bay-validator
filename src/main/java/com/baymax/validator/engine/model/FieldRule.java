/**
 * 
 */
package com.baymax.validator.engine.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

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
	private Date beginAt = null;
	private Date endAt = null;

	/** Object 可能是 String,  此时需要引用 common_dict 中的值
	 * 也可能是 List<Object>
	 */
	private Object enumValues = null;

	/** Object 可能是 String,  此时需要引用 common_dict 中的值
	 * 也可能是 Map<Object, String>
	 */
	private Object enumDict = null;

	/**
	 *
	 * @param fieldKey
	 * @param type
	 * @param rulesMap
	 */
	public abstract void build(String fieldKey, String type, Map<String, Object> rulesMap);

	/**
	 *
	 * @param value
	 * @return
	 */
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

	public Object getEnumValues() {
		return enumValues;
	}

	public void setEnumValues(Object enumValues) {
		this.enumValues = enumValues;
	}

	public Object getEnumDict() {
		return enumDict;
	}

	public void setEnumDict(Object enumDict) {
		this.enumDict = enumDict;
	}

	public Date getBeginAt() {
		return beginAt;
	}

	public void setBeginAt(Date beginAt) {
		this.beginAt = beginAt;
	}

	public Date getEndAt() {
		return endAt;
	}

	public void setEndAt(Date endAt) {
		this.endAt = endAt;
	}
}
