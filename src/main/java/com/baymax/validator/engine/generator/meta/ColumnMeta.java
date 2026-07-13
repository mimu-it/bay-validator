package com.baymax.validator.engine.generator.meta;

/**
 * @author huxiao
 *
 */
public class ColumnMeta {

	private String name;
	private int displaySize;
	private String abbreviationClass;
	private String originClass;

	// 新增字段
	private int dataType;           // JDBC 类型
	private String typeName;        // 数据库类型名
	private int decimalDigits;      // 小数位数
	private boolean nullable;       // 是否可空
	private String columnDef;       // 默认值
	private int ordinalPosition;    // 列顺序
	private String remarks;         // 备注
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAbbreviationClass() {
		return abbreviationClass;
	}
	public void setAbbreviationClass(String abbreviationClass) {
		this.abbreviationClass = abbreviationClass;
	}
	public String getOriginClass() {
		return originClass;
	}
	public void setOriginClass(String originClass) {
		this.originClass = originClass;
	}

	public int getDisplaySize() {
		return displaySize;
	}

	public void setDisplaySize(int displaySize) {
		this.displaySize = displaySize;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public void setDecimalDigits(int decimalDigits) {
		this.decimalDigits = decimalDigits;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public String getColumnDef() {
		return columnDef;
	}

	public void setColumnDef(String columnDef) {
		this.columnDef = columnDef;
	}

	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public void setOrdinalPosition(int ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
}
