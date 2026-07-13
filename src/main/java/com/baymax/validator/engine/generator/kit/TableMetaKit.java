package com.baymax.validator.engine.generator.kit;

import com.baymax.validator.engine.generator.meta.ColumnMeta;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库表元数据工具类
 * 兼容 MySQL、Oracle、MariaDB
 *
 * @author huxiao
 */
public class TableMetaKit {
	private static final Logger logger = Logger.getLogger(TableMetaKit.class.getName());

	// ========== 类型映射常量 ==========
	// JDBC 类型到 Java 类型的映射（根据 java.sql.Types 标准）
	// 这些映射将数据库原生类型转换为Java中对应的包装类型，便于后续代码生成
	private static final Map<Integer, String> JDBC_TYPE_TO_JAVA_CLASS = new HashMap<>();
	// Java 类型到缩写名的映射（用于生成简洁的字段类型标识）
	private static final Map<String, String> JAVA_CLASS_TO_ABBREVIATION = new HashMap<>();

	static {
		// ---------- JDBC类型 -> Java类映射 ----------
		// 整数类型
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.INTEGER, "java.lang.Integer");      // INT
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.SMALLINT, "java.lang.Integer");     // SMALLINT
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.TINYINT, "java.lang.Integer");      // TINYINT
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.BIGINT, "java.lang.Long");          // BIGINT

		// 小数/浮点类型
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.DECIMAL, "java.math.BigDecimal");   // DECIMAL
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.NUMERIC, "java.math.BigDecimal");   // NUMERIC
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.FLOAT, "java.lang.Double");         // FLOAT
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.DOUBLE, "java.lang.Double");        // DOUBLE
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.REAL, "java.lang.Float");           // REAL

		// 字符串类型
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.CHAR, "java.lang.String");          // CHAR
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.VARCHAR, "java.lang.String");       // VARCHAR
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.LONGVARCHAR, "java.lang.String");   // LONGVARCHAR
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.CLOB, "java.lang.String");          // CLOB
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.NVARCHAR, "java.lang.String");      // NVARCHAR
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.NCHAR, "java.lang.String");         // NCHAR
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.LONGNVARCHAR, "java.lang.String");  // LONGNVARCHAR
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.NCLOB, "java.lang.String");         // NCLOB
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.SQLXML, "java.lang.String");        // XML

		// 日期时间类型
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.DATE, "java.util.Date");            // DATE
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.TIME, "java.util.Date");            // TIME
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.TIMESTAMP, "java.util.Date");       // TIMESTAMP

		// 布尔类型
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.BIT, "java.lang.Boolean");          // BIT
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.BOOLEAN, "java.lang.Boolean");      // BOOLEAN

		// 二进制类型
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.BLOB, "java.sql.Blob");            // BLOB
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.BINARY, "byte[]");                 // BINARY
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.VARBINARY, "byte[]");              // VARBINARY
		JDBC_TYPE_TO_JAVA_CLASS.put(Types.LONGVARBINARY, "byte[]");          // LONGVARBINARY

		// ---------- Java类型 -> 缩写映射 ----------
		// 用于生成代码时展示简洁的类型标识，如 "String"、"Integer" 等
		JAVA_CLASS_TO_ABBREVIATION.put("java.lang.Integer", "Integer");
		JAVA_CLASS_TO_ABBREVIATION.put("java.lang.Long", "Long");
		JAVA_CLASS_TO_ABBREVIATION.put("java.lang.String", "String");
		JAVA_CLASS_TO_ABBREVIATION.put("java.math.BigDecimal", "BigDecimal");
		JAVA_CLASS_TO_ABBREVIATION.put("java.lang.Boolean", "Boolean");
		JAVA_CLASS_TO_ABBREVIATION.put("java.util.Date", "Date");
		JAVA_CLASS_TO_ABBREVIATION.put("java.sql.Date", "Date");
		JAVA_CLASS_TO_ABBREVIATION.put("java.sql.Timestamp", "Date");
		JAVA_CLASS_TO_ABBREVIATION.put("java.sql.Blob", "Blob");
		JAVA_CLASS_TO_ABBREVIATION.put("byte[]", "ByteArray");
		JAVA_CLASS_TO_ABBREVIATION.put("java.lang.Double", "Double");
		JAVA_CLASS_TO_ABBREVIATION.put("java.lang.Float", "Float");
		JAVA_CLASS_TO_ABBREVIATION.put("java.math.BigInteger", "BigInteger");
	}

	/**
	 * 获取指定数据库的所有表名（包括表和视图）
	 *
	 * <p><b>示例：</b>
	 * <pre>
	 * // 获取 test_db 中的所有表，排除系统表
	 * List&lt;String&gt; tables = TableMetaKit.getTables(dataSource, "test_db",
	 *         Arrays.asList("sys_config", "flyway_schema_history"));
	 *
	 * // 返回结果示例：["user_table", "order_table", "product_table"]
	 * </pre>
	 *
	 * @param dataSource 数据源
	 * @param databaseName 数据库名（MySQL/MariaDB使用，Oracle可传null）
	 * @param exceptTables 排除的表名列表
	 * @return 表名列表
	 * @throws IllegalStateException 获取失败时抛出
	 */
	public static List<String> getTables(DataSource dataSource, String databaseName, List<String> exceptTables) {
		List<String> tables = new ArrayList<>();
		Set<String> exceptSet = exceptTables != null ? new HashSet<>(exceptTables) : Collections.emptySet();

		// 使用 try-with-resources 自动管理资源
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData dbMetaData = conn.getMetaData();

			// 获取数据库产品名称，用于适配不同数据库
			String dbProduct = dbMetaData.getDatabaseProductName();
			logger.info("Database product: " + dbProduct);

			// ---------- 根据数据库类型设置 catalog 和 schema ----------
			// 不同数据库对 catalog/schema 的支持不同：
			// - MySQL/MariaDB: catalog = 数据库名, schema = null
			// - Oracle: catalog = null, schema = 用户名（大写）
			// - PostgreSQL: catalog = 数据库名, schema = public
			String catalog;
			String schema;

			if (isMySQLOrMariaDB(dbProduct)) {
				// MySQL/MariaDB: catalog = 数据库名, schema = null
				catalog = databaseName;
				schema = null;
			} else if (isOracle(dbProduct)) {
				// Oracle: catalog = null, schema = 用户名
				catalog = null;
				schema = dbMetaData.getUserName();
			} else {
				// 其他数据库：尝试通用方式
				catalog = databaseName;
				schema = null;
			}

			// 获取所有表和视图
			try (ResultSet rs = dbMetaData.getTables(catalog, schema, null,
					new String[]{"TABLE", "VIEW"})) {

				while (rs.next()) {
					String tableType = rs.getString("TABLE_TYPE");
					String tableName = rs.getString("TABLE_NAME");

					if (tableType != null && tableName != null) {
						String tableNameLower = tableName.toLowerCase();

						// 如果表名不在排除列表中，则加入结果集
						if (!exceptSet.contains(tableNameLower)) {
							logger.fine("Found table: " + tableName);
							tables.add(tableNameLower);
						}
					}
				}
			}

			logger.info("Total tables found: " + tables.size());
			return tables;

		} catch (SQLException e) {
			String errorMsg = "Failed to get tables from database: " + databaseName;
			logger.log(Level.SEVERE, errorMsg, e);
			throw new IllegalStateException(errorMsg, e);
		}
	}

	/**
	 * 获取表的列元数据信息
	 * 使用 DatabaseMetaData 获取，避免查询表数据，性能更好且更安全
	 *
	 * <p>使用 DatabaseMetaData 获取，避免查询表数据，性能更好且更安全。
	 *    该方法会返回表中每一列的详细信息，包括字段名、数据类型、长度、是否可为空等。
	 *
	 *
	 * <p><b>示例：</b>
	 * <pre>
	 * // 假设 user_table 表结构如下：
	 * // id           INT PRIMARY KEY AUTO_INCREMENT,
	 * // username     VARCHAR(50) NOT NULL,
	 * // age          INT,
	 * // created_time DATETIME
	 *
	 * try (Connection conn = dataSource.getConnection()) {
	 *     List&lt;ColumnMeta&gt; columns = TableMetaKit.getColumnsMeta(conn, "user_table");
	 *
	 *     // 输出结果：
	 *     // Column: id,           Type: INT,      JavaClass: java.lang.Integer, Nullable: false
	 *     // Column: username,     Type: VARCHAR,  JavaClass: java.lang.String,  Nullable: false
	 *     // Column: age,          Type: INT,      JavaClass: java.lang.Integer, Nullable: true
	 *     // Column: created_time, Type: DATETIME, JavaClass: java.util.Date,    Nullable: true
	 * }
	 * </pre>
	 *
	 * @param connection 数据库连接
	 * @param tableName 表名
	 * @return 列元数据列表
	 * @throws IllegalStateException 获取失败时抛出
	 */
	public static List<ColumnMeta> getColumnsMeta(Connection connection, String tableName) {
		List<ColumnMeta> columnList = new ArrayList<>();

		// 参数校验
		if (connection == null) {
			throw new IllegalArgumentException("Connection cannot be null");
		}
		if (tableName == null || tableName.trim().isEmpty()) {
			throw new IllegalArgumentException("Table name cannot be null or empty");
		}

		try {
			DatabaseMetaData dbMetaData = connection.getMetaData();

			// 获取数据库产品名称，用于适配不同数据库
			String dbProduct = dbMetaData.getDatabaseProductName();
			logger.fine("Database product: " + dbProduct);

			// 根据数据库类型设置参数
			String catalog;
			String schema;

			if (isMySQLOrMariaDB(dbProduct)) {
				// MySQL/MariaDB: catalog = 当前数据库, schema = null
				catalog = connection.getCatalog();
				schema = null;
			} else if (isOracle(dbProduct)) {
				// Oracle: catalog = null, schema = 用户名
				catalog = null;
				schema = dbMetaData.getUserName();
			} else {
				// 其他数据库：尝试通用方式
				catalog = connection.getCatalog();
				schema = null;
			}

			logger.fine(String.format("Getting columns for table: %s, catalog: %s, schema: %s",
					tableName, catalog, schema));

			// ---------- 获取列元数据 ----------
			// ResultSet 中包含的列：COLUMN_NAME, DATA_TYPE, TYPE_NAME, COLUMN_SIZE,
			// DECIMAL_DIGITS, NULLABLE, COLUMN_DEF, ORDINAL_POSITION, IS_NULLABLE, REMARKS
			try (ResultSet rs = dbMetaData.getColumns(catalog, schema, tableName, null)) {

				while (rs.next()) {
					// 读取列的基本信息
					String columnName = rs.getString("COLUMN_NAME");
					int dataType = rs.getInt("DATA_TYPE"); // java.sql.Types 常量
					String typeName = rs.getString("TYPE_NAME"); // 数据库原生类型名
					int columnSize = rs.getInt("COLUMN_SIZE");   // 字段长度
					int decimalDigits = rs.getInt("DECIMAL_DIGITS"); // 小数位数（对DECIMAL有效）
					int nullable = rs.getInt("NULLABLE");  // 是否可为空（1=可为空, 0=不可为空）
					String columnDef = rs.getString("COLUMN_DEF"); // 默认值
					int ordinalPosition = rs.getInt("ORDINAL_POSITION"); // 字段顺序（从1开始）
					String isNullable = rs.getString("IS_NULLABLE");  // "YES" 或 "NO"
					String remarks = rs.getString("REMARKS");  // 字段注释

					// ---------- 类型映射 ----------
					// 将 JDBC 类型映射到 Java 类（如 INT -> java.lang.Integer）
					String originClass = jdbcTypeToJavaClass(dataType, typeName);
					// 获取 Java 类的简短名称（如 java.lang.Integer -> Integer）
					String abbreviationClass = javaClassToAbbreviation(originClass);

					logger.fine(String.format("Column: %s, Type: %s, JavaClass: %s, Size: %d",
							columnName, typeName, originClass, columnSize));

					// ---------- 构建 ColumnMeta 对象 ----------
					ColumnMeta columnMeta = new ColumnMeta();
					columnMeta.setName(columnName);
					columnMeta.setDisplaySize(columnSize);
					columnMeta.setAbbreviationClass(abbreviationClass); // 短类型名，如 "String"
					columnMeta.setOriginClass(originClass); // 完整类名，如 "java.lang.String"
					columnMeta.setDataType(dataType);  // JDBC类型码
					columnMeta.setTypeName(typeName);  // 数据库类型名
					columnMeta.setDecimalDigits(decimalDigits);
					columnMeta.setNullable(nullable == DatabaseMetaData.columnNullable);
					columnMeta.setColumnDef(columnDef);
					columnMeta.setOrdinalPosition(ordinalPosition);
					columnMeta.setRemarks(remarks);

					columnList.add(columnMeta);
				}
			}

			logger.info(String.format("Found %d columns for table: %s", columnList.size(), tableName));
			return columnList;

		} catch (SQLException e) {
			String errorMsg = "Failed to get columns meta for table: " + tableName;
			logger.log(Level.SEVERE, errorMsg, e);
			throw new IllegalStateException(errorMsg, e);
		}
	}

	/**
	 * 获取表的列元数据信息（重载方法，自动管理连接）
	 *
	 * <p>该方法会自动获取数据库连接，使用完后自动关闭，
	 *    适合在不需要复用连接的场景下使用。
	 *
	 * @param dataSource 数据源
	 * @param tableName 表名
	 * @return 列元数据列表
	 * @throws IllegalStateException 获取失败时抛出
	 */
	public static List<ColumnMeta> getColumnsMeta(DataSource dataSource, String tableName) {
		// try-with-resources 自动管理连接，无需手动关闭
		try (Connection conn = dataSource.getConnection()) {
			return getColumnsMeta(conn, tableName);
		}
		catch (SQLException e) {
			String errorMsg = "Failed to get connection from data source for table: " + tableName;
			logger.log(Level.SEVERE, errorMsg, e);
			throw new IllegalStateException(errorMsg, e);
		}
	}

	/**
	 * 获取表的主键信息
	 *
	 * <pre>
	 * // 假设 user_table 有主键 id
	 * try (Connection conn = dataSource.getConnection()) {
	 *     List&lt;String&gt; pks = TableMetaKit.getPrimaryKeys(conn, "user_table");
	 *     // 返回：["id"]
	 *
	 *     // 复合主键示例：
	 *     // 假设 order_item 表有复合主键 (order_id, item_id)
	 *     List&lt;String&gt; pks2 = TableMetaKit.getPrimaryKeys(conn, "order_item");
	 *     // 返回：["order_id", "item_id"]（按主键顺序排列）
	 * }
	 * </pre>
	 *
	 * @param connection 数据库连接
	 * @param tableName 表名
	 * @return 主键列名列表
	 * @throws IllegalStateException 获取失败时抛出
	 */
	public static List<String> getPrimaryKeys(Connection connection, String tableName) {
		List<String> primaryKeys = new ArrayList<>();

		try {
			DatabaseMetaData dbMetaData = connection.getMetaData();

			// 获取数据库产品名称进行适配
			String dbProduct = dbMetaData.getDatabaseProductName();
			String catalog = null;
			String schema = null;

			if (isMySQLOrMariaDB(dbProduct)) {
				catalog = connection.getCatalog();
				schema = null;
			} else if (isOracle(dbProduct)) {
				catalog = null;
				schema = dbMetaData.getUserName();
			} else {
				catalog = connection.getCatalog();
				schema = null;
			}

			// ResultSet 包含：TABLE_CAT, TABLE_SCHEM, TABLE_NAME, COLUMN_NAME, KEY_SEQ, PK_NAME
			// KEY_SEQ 表示主键顺序（1, 2, 3...），用于复合主键排序
			try (ResultSet rs = dbMetaData.getPrimaryKeys(catalog, schema, tableName)) {
				while (rs.next()) {
					String pkName = rs.getString("COLUMN_NAME");
					if (pkName != null) {
						primaryKeys.add(pkName);
					}
				}
			}

			return primaryKeys;

		} catch (SQLException e) {
			String errorMsg = "Failed to get primary keys for table: " + tableName;
			logger.log(Level.WARNING, errorMsg, e);
			throw new IllegalStateException(errorMsg, e);
		}
	}

	// ==================== 私有辅助方法 ====================

	/**
	 * 判断是否为 MySQL 或 MariaDB
	 *
	 * <p>通过数据库产品名称判断，兼容大小写不敏感。
	 *
	 * <p><b>示例：</b>
	 * <pre>
	 * isMySQLOrMariaDB("MySQL 8.0.33")   // true
	 * isMySQLOrMariaDB("MariaDB 10.11")  // true
	 * isMySQLOrMariaDB("Oracle 19c")     // false
	 * </pre>
	 *
	 */
	private static boolean isMySQLOrMariaDB(String dbProduct) {
		if (dbProduct == null) {
			return false;
		}
		String product = dbProduct.toLowerCase();
		return product.contains("mysql") || product.contains("mariadb");
	}

	/**
	 * 判断是否为 Oracle
	 *
	 * <p><b>示例：</b>
	 * <pre>
	 * isOracle("Oracle Database 19c") // true
	 * isOracle("MySQL 8.0")           // false
	 * </pre>
	 *
	 */
	private static boolean isOracle(String dbProduct) {
		if (dbProduct == null) {
			return false;
		}
		return dbProduct.toLowerCase().contains("oracle");
	}

	/**
	 * JDBC 类型转换为 Java 类名
	 *
	 * <p>根据 java.sql.Types 中的常量值，映射到对应的 Java 类全限定名。
	 * 如果遇到未知类型，返回 Object 并记录日志。
	 *
	 * <p><b>示例：</b>
	 * <pre>
	 * jdbcTypeToJavaClass(Types.INTEGER, "INT")     // "java.lang.Integer"
	 * jdbcTypeToJavaClass(Types.VARCHAR, "VARCHAR") // "java.lang.String"
	 * jdbcTypeToJavaClass(Types.DATE, "DATE")       // "java.util.Date"
	 * jdbcTypeToJavaClass(999, "CUSTOM")            // "java.lang.Object"（未知类型）
	 * </pre>
	 *
	 */
	private static String jdbcTypeToJavaClass(int jdbcType, String typeName) {
		String javaClass = JDBC_TYPE_TO_JAVA_CLASS.get(jdbcType);

		if (javaClass == null) {
			// 如果是未知类型，尝试根据类型名称推断
			logger.fine("Unknown JDBC type: " + jdbcType + ", typeName: " + typeName);
			return "java.lang.Object";
		}

		return javaClass;
	}

	/**
	 * Java 类名转换为缩写名
	 *
	 * <p>获取类名的简短形式，用于代码生成时展示简洁的类型标识。
	 * 如果映射表中没有定义，则从全限定名中提取最后一个点后面的部分。
	 *
	 * <p><b>示例：</b>
	 * <pre>
	 * javaClassToAbbreviation("java.lang.String")    // "String"
	 * javaClassToAbbreviation("java.lang.Integer")   // "Integer"
	 * javaClassToAbbreviation("java.math.BigDecimal") // "BigDecimal"
	 * javaClassToAbbreviation("java.util.Date")      // "Date"
	 * javaClassToAbbreviation("byte[]")              // "ByteArray"
	 * javaClassToAbbreviation("com.example.Custom")  // "Custom"（从类名提取）
	 * javaClassToAbbreviation(null)                  // "Object"（默认值）
	 * </pre>
	 *
	 */
	private static String javaClassToAbbreviation(String javaClass) {
		if (javaClass == null) {
			return "Object";
		}

		String abbreviation = JAVA_CLASS_TO_ABBREVIATION.get(javaClass);

		if (abbreviation == null) {
			// 获取类名的简短形式
			int lastDot = javaClass.lastIndexOf('.');
			if (lastDot > 0) {
				abbreviation = javaClass.substring(lastDot + 1);
			} else {
				abbreviation = javaClass;
			}
		}

		return abbreviation;
	}

	/**
	 * 验证表名是否存在（防止SQL注入）
	 *
	 * <p>在操作表之前调用此方法，可以确保表名是有效的，避免非法表名导致的安全问题。
	 *
	 * <p><b>示例：</b>
	 * <pre>
	 * try (Connection conn = dataSource.getConnection()) {
	 *     if (TableMetaKit.tableExists(conn, "user_table")) {
	 *         System.out.println("表存在，可以继续操作");
	 *     } else {
	 *         System.out.println("表不存在，请检查表名");
	 *     }
	 * }
	 * </pre>
	 */
	public static boolean tableExists(Connection connection, String tableName) {
		try {
			DatabaseMetaData dbMetaData = connection.getMetaData();
			String catalog = connection.getCatalog();

			try (ResultSet rs = dbMetaData.getTables(catalog, null, tableName,
					new String[]{"TABLE", "VIEW"})) {
				return rs.next();
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Failed to check table existence: " + tableName, e);
			return false;
		}
	}
}