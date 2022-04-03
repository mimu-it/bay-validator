/**
 * 
 */
package com.baymax.validator.engine.generator.kit;

import com.baymax.validator.engine.generator.meta.ColumnMeta;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;



/**
 * @author huxiao
 *
 */
public class TableMetaKit {

	/**
	 * 获取指定数据库的所有表名
	 * @return
	 * @throws SQLException
	 */
	public static List<String> getTables(DataSource dataSource, List<String> exceptTables) {
		try {
			Connection conn = dataSource.getConnection();
			DatabaseMetaData dbMetData = conn.getMetaData();
			// mysql convertDatabaseCharsetType null
			ResultSet rs = dbMetData.getTables(null, null, null, new String[] { "TABLE", "VIEW" });
			
			List<String> tables = new ArrayList<String>();
			while (rs.next()) {
				if (rs.getString(4) != null
						&& (rs.getString(4).equalsIgnoreCase("TABLE") || rs
								.getString(4).equalsIgnoreCase("VIEW"))) {
					String tableName = rs.getString(3).toLowerCase();
					if(!exceptTables.contains(tableName)) {
						System.out.print(tableName + "\t");
						tables.add(tableName);
					}
				}
			}
			return tables;
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * 获得字段的相关信息
	 * 
	 * getColumnType(int); 返回此列的 SQL 数据类型。这些数据类型包括 

		BIGINT 
		BINARY 
		BIT 
		CHAR 
		DATE 
		DECIMAL 
		DOUBLE 
		FLOAT 
		INTEGER 
		LONGVARBINARY 
		LONGVARCHAR 
		NULL 
		NUMERIC 
		OTHER 
		REAL 
		SMALLINT 
		TIME 
		TIMESTAMP 
		TINYINT 
		VARBINARY 
		VARCHAR
	 * @param tableName
	 * @return
	 */
	public static List<ColumnMeta> getColumnsMeta(Connection con, String tableName) {
		List<ColumnMeta> list = new ArrayList<>();
		try {
			Statement statement = con.createStatement();
			ResultSet resultSet = statement.executeQuery("select * from " + tableName);
			
			//获取元数据
            ResultSetMetaData metaData = resultSet.getMetaData();
            
            //获取共有多少字段
            int columnCount = metaData.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                //获取字段名
                String columnName = metaData.getColumnLabel(i + 1);
                String originClass = metaData.getColumnClassName(i + 1);
                String columnDbType = metaData.getColumnTypeName(i + 1);
                
                //通过字段名获取数据
                System.out.println(String.format("getColumnsMeta() => Table's name is: %s", tableName));
                System.out.println("COLUMN: " + columnName + ", CLASS: " 
                + originClass + ", DBTYPE:" + columnDbType);
                
                String abbreviationClass = "";

                if("java.lang.Integer".equals(originClass)) {
                	abbreviationClass = "Integer";
                }
                else if("java.lang.BigInteger".equals(originClass)) {
                	abbreviationClass = "BigInteger";
                }
                else if("java.lang.Long".equals(originClass)) {
                	abbreviationClass = "Long";
                }
                else if("java.sql.Timestamp".equals(originClass)) {
                	abbreviationClass = "Date";
                	originClass = "java.util.Date";
                }
                else if("java.lang.String".equals(originClass)) {
                	abbreviationClass = "String";
                }
                else if("java.math.BigDecimal".equals(originClass)) {
                	abbreviationClass = "BigDecimal";
                }
                else if("java.lang.Boolean".equals(originClass)) {
                	abbreviationClass = "Boolean";
                }
                else if("java.sql.Date".equals(originClass)) {
                	abbreviationClass = "Date";
                	originClass = "java.util.Date";
                }
                
                ColumnMeta oColumnMeta = new ColumnMeta();
                oColumnMeta.setName(columnName);
                oColumnMeta.setAbbreviationClass(abbreviationClass);
                oColumnMeta.setOriginClass(originClass);
                
                list.add(oColumnMeta);
            }
		} catch (SQLException e) {
			e.printStackTrace();
			list = null;
		}
		
		return list;
	}
}
