/**
 * 
 */
package com.baymax.validator.engine.generator.meta;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author huxiao
 *
 */
public class MetaData {

	private Map<String, TableMeta> tablesWithColumnMetaMapping;

	public Map<String, TableMeta> getTablesWithColumnMetaMapping() {
		return tablesWithColumnMetaMapping;
	}

	public void setTablesWithColumnMetaMapping(Map<String, TableMeta> tablesWithColumnMetaMapping) {
		this.tablesWithColumnMetaMapping = tablesWithColumnMetaMapping;
	}
	
	public Set<String> getTablesNames() {
		return tablesWithColumnMetaMapping.keySet();
	}
	
	public List<ColumnMeta> getColumnMetaList(String tableName) {
		TableMeta oTableMeta = tablesWithColumnMetaMapping.get(tableName);
		return oTableMeta.getColumnMetaList();
	}
}
