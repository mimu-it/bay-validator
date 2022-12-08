/**
 * 
 */
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
}
