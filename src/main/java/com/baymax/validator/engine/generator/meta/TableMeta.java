/**
 * 
 */
package com.baymax.validator.engine.generator.meta;

import java.util.List;

/**
 * @author huxiao
 *
 */
public class TableMeta {

	private String tableName;
	private List<ColumnMeta> columnMetaList;
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public List<ColumnMeta> getColumnMetaList() {
		return columnMetaList;
	}
	public void setColumnMetaList(List<ColumnMeta> columnMetaList) {
		this.columnMetaList = columnMetaList;
	}
}
