package com.baymax.validator.engine;

import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.generator.EnumCodeGenerator;
import com.baymax.validator.engine.generator.formatter.IFormatter;
import com.baymax.validator.engine.generator.kit.TableMetaKit;
import com.baymax.validator.engine.generator.meta.ColumnMeta;
import com.baymax.validator.engine.generator.meta.TableMeta;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.model.sub.*;
import com.baymax.validator.engine.preset.DbType;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.preset.RuleType;
import com.baymax.validator.engine.utils.BeanUtil;
import com.baymax.validator.engine.utils.FileWriter;
import com.baymax.validator.engine.utils.NameUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 校验引擎核心（单例枚举）。
 *
 * <p><b>核心职责：</b>
 * <ol>
 *   <li>加载并持有校验规则配置（{@code valueRulesMap / commonValueRulesMap}）</li>
 *   <li>提供单字段校验、Java Bean 校验入口</li>
 *   <li>提供 FieldRule 查询与 JSON 序列化输出</li>
 *   <li>提供 YML 配置合并/生成和 Java 枚举代码生成辅助接口</li>
 * </ol>
 *
 * <p><b>YAML 配置结构（value_rules.yml）：</b>
 * <pre>{@code
 * student:                          # ← 表名（tableName）
 *   id:                             # ← 字段名（fieldName）
 *     type: numeric                 # 校验类型
 *     numeric_min: 1                # 数值最小值
 *     numeric_max: 128              # 数值最大值
 *   gender:
 *     type: enum_string             # 字符串枚举
 *     enum_values:
 *       - male
 *       - female
 *   phone_number:
 *     type: string                  # 字符串 + 正则
 *     string_regex_key: phone_number
 *     string_length_min: 11
 *     string_length_max: 11
 *     string_charset: utf8
 *   money:
 *     type: decimal                 # 浮点小数
 *     decimal_min: 0.00
 *     decimal_max: 300.00
 *   birthday:
 *     type: date                    # 日期
 *     begin_at: 2020-01-01
 *     end_at: 2030-12-31
 *   created_at:
 *     type: datetime                # 日期时间
 *     begin_at: 2020-01-01 00:00:00
 *     end_at: 2030-12-31 23:59:59
 *   game_card:
 *     type: enum_numeric            # 数字枚举
 *     enum_values:
 *       - 1
 *       - 2
 * }</pre>
 *
 * <p><b>字段 Key 格式：</b> {@code tableName.fieldName}，例如 {@code "student.id"}。
 * 如果不包含 {@code .}，会自动补全为 {@code _common.xxx}。
 *
 * <p>YAML 加载 → {@link YamlConfigLoader} <br>
 * 枚举代码生成 → {@link EnumCodeGenerator}
 *
 * @author xiao.hu
 */
public enum ValidatorEngine {
    /**
     * 单例实例
     */
    INSTANCE;

    private static final Logger logger = Logger.getLogger(ValidatorEngine.class.getName());

    private final YamlConfigLoader configLoader = new YamlConfigLoader();
    private EnumCodeGenerator enumCodeGenerator;

    private static ObjectMapper mapper = new ObjectMapper();



    /**
     * 校验规则配置缓存。<br>
     * 结构：{@code Map<tableName, Map<fieldName, Map<ruleKey, ruleValue>>>}
     * <pre>
     * {
     *   "student": {
     *     "id":            { "type": "numeric",      "numeric_min": 1, "numeric_max": 128 },
     *     "gender":        { "type": "enum_string",  "enum_values": ["male", "female"] },
     *     "phone_number":  { "type": "string",       "string_regex_key": "phone_number", "string_length_min": 11, "string_length_max": 11 },
     *     "money":         { "type": "decimal",      "decimal_min": 0.00, "decimal_max": 300.00 },
     *     "birthday":      { "type": "date",         "begin_at": 2020-01-01, "end_at": 2030-12-31 },
     *     "game_card":     { "type": "enum_numeric", "enum_values": [1, 2] }
     *   },
     *   "order": { ... }
     * }
     * </pre>
     */
    Map<String, Map<String, Object>> valueRulesMap;

    /**
     * 通用校验规则配置缓存（跨模块共享的公共规则）。
     * 当 {@link #valueRulesMap} 中查不到指定表名时，会回退到此 Map 查找。
     */
    Map<String, Map<String, Object>> commonValueRulesMap;

    /**
     * FieldRule 工厂映射。<br>
     * 根据 {@link RuleType} 创建对应的 {@link FieldRule} 子类实例。
     * <pre>
     * "numeric"       → NumericFieldRule
     * "decimal"       → DecimalFieldRule
     * "string"        → StringRegexFieldRule
     * "enum_string"   → EnumStringFieldRule
     * "enum_numeric"  → EnumNumericFieldRule&lt;BigInteger&gt;
     * "enum_decimal"  → EnumNumericFieldRule&lt;BigDecimal&gt;
     * "date"          → DateFieldRule
     * "datetime"      → DatetimeFieldRule
     * </pre>
     */
    private static Map<String, Supplier<FieldRule>> fieldRuleMap = new HashMap<>();

    static {
        fieldRuleMap.put(RuleType.numeric.name(), () -> new NumericFieldRule());
        fieldRuleMap.put(RuleType.decimal.name(), () -> new DecimalFieldRule());
        fieldRuleMap.put(RuleType.string.name(), () -> new StringRegexFieldRule());
        fieldRuleMap.put(RuleType.enum_string.name(), () -> new EnumStringFieldRule());
        fieldRuleMap.put(RuleType.enum_numeric.name(), () -> new EnumNumericFieldRule<>(BigInteger.class));
        fieldRuleMap.put(RuleType.enum_decimal.name(), () -> new EnumNumericFieldRule<>(BigDecimal.class));
        fieldRuleMap.put(RuleType.date.name(), () -> new DateFieldRule());
        fieldRuleMap.put(RuleType.datetime.name(), () -> new DatetimeFieldRule());
    }

    /**
     * 键模式：{@code true}=下划线模式（snake_case），{@code false}=驼峰模式（camelCase）。<br>
     * 默认 {@code true}。
     * <pre>
     * true  → "phoneNumber" 转为 "phone_number" 再与规则匹配
     * false → "phone_number" 直接与规则匹配
     * </pre>
     */
    private boolean isSnakeKeyMode = true;

    /**
     * 当前数据库类型，影响字段长度换算规则等。
     */
    private static DbType dbType = null;

    /**
     * 全局忽略字段集合。校验 Java Bean 时自动跳过这些字段。<br>
     * <b>默认值：</b>
     * <pre>{@code
     * {"id", "gmt_created", "creator", "gmt_modified",
     *  "modifier", "is_deleted", "version"}
     * }</pre>
     * 可自定义。
     */
    private static Set<String> ignoreKeys = new HashSet<>();

    static {
        ignoreKeys.add("id");
        ignoreKeys.add("gmt_created");
        ignoreKeys.add("creator");
        ignoreKeys.add("gmt_modified");
        ignoreKeys.add("modifier");
        ignoreKeys.add("is_deleted");
        ignoreKeys.add("version");
    }

    /**
     * 代码格式化插件，可选。用于生成 Java 枚举代码时格式化输出。
     */
    private IFormatter formatter = null;

    // ===================== Getters / Setters =====================

    /**
     * 获取当前数据库类型。
     *
     * @return 当前 {@link DbType}，可能为 {@code null}（未初始化时）
     * <pre>{@code
     * DbType type = ValidatorEngine.getDbType(); // mysql 或 oracle
     * }</pre>
     */
    public static DbType getDbType() {
        return dbType;
    }

    /**
     * 获取当前加载的校验规则配置（只读场景使用）。
     *
     * @return valueRulesMap，结构见 {@link #valueRulesMap} 字段说明
     * <pre>{@code
     * Map<String, Map<String, Object>> rules = ValidatorEngine.INSTANCE.getValueRulesMap();
     * Map<String, Object> studentFields = rules.get("student");
     * }</pre>
     */
    public Map<String, Map<String, Object>> getValueRulesMap() {
        return valueRulesMap;
    }

    /**
     * 判断指定 key 是否在全局忽略字段集合中。
     *
     * @param key 字段名（已转换为对应命名模式后的值）
     * @return 如果该字段应被忽略则返回 {@code true}
     * <pre>{@code
     * ValidatorEngine.containIgnoreKeys("id");           // true
     * ValidatorEngine.containIgnoreKeys("is_deleted");   // true
     * ValidatorEngine.containIgnoreKeys("name");         // false
     * }</pre>
     */
    public static boolean containIgnoreKeys(String key) {
        return ignoreKeys.contains(key);
    }

    /**
     * 设置代码格式化器。仅在生成 Java 枚举代码时使用。
     *
     * @param formatter 实现了 {@link IFormatter} 的格式化器实例
     *                  <pre>{@code
     *                  ValidatorEngine.INSTANCE.setFormatter(new IFormatter() {
     *                      public String formatJava(String s) {
     *                          return new Formatter().formatSource(s);
     *                      }
     *                  });
     *                  }</pre>
     */
    public void setFormatter(IFormatter formatter) {
        this.formatter = formatter;
    }

    // ===================== DbType =====================

    /**
     * 根据名称初始化数据库类型。
     *
     * @param dbTypeName 数据库类型名称（{@code "mysql"} 或 {@code "oracle"}），不区分大小写。
     *                   传入 {@code "oracle"} 以外的值均视为 {@code "mysql"}。
     *                   <pre>{@code
     *                   ValidatorEngine.initDbType("mysql");   // dbType = DbType.mysql
     *                   ValidatorEngine.initDbType("oracle");  // dbType = DbType.oracle
     *                   ValidatorEngine.initDbType("mariadb"); // dbType = DbType.mysql（兼容）
     *                   }</pre>
     */
    public static void initDbType(String dbTypeName) {
        if (DbType.oracle.name().equals(dbTypeName)) {
            dbType = DbType.oracle;
        } else {
            dbType = DbType.mysql;
        }
    }

    // ===================== 初始化 =====================

    /**
     * 基础初始化（仅供内部 init 重载调用）。
     *
     * @param dbType                数据库类型名称
     * @param valueRulesYmlFilePath value_rules.yml 的 classpath 路径
     * @param regexDictYmlFilePath  common_dict.yml 的 classpath 路径（为空则使用默认值 {@code common_dict.yml}）
     *                              <pre>{@code
     *                              ValidatorEngine.INSTANCE.init0("mysql", "value_rules.yml", "common_dict.yml");
     *                              }</pre>
     */
    public void init0(String dbType, String valueRulesYmlFilePath, String regexDictYmlFilePath) {
        initDbType(dbType);
        CommonDict.INSTANCE.init(regexDictYmlFilePath);
        this.valueRulesMap = configLoader.loadValueRulesYml(valueRulesYmlFilePath);
    }

    /**
     * 最简初始化。数据库默认为 MySQL，不加载自定义 common_dict。
     *
     * @param valueRulesYmlFilePath value_rules.yml 的 classpath 路径
     *                              <pre>{@code
     *                              // 从 classpath 根目录加载 value_rules.yml
     *                              ValidatorEngine.INSTANCE.init("value_rules.yml");
     *
     *                              // 校验示例（读取上面配置后）：
     *                              boolean ok = ValidatorEngine.INSTANCE.validate("student.id", 1); // true
     *                              }</pre>
     */
    public void init(String valueRulesYmlFilePath) {
        initDbType(DbType.mysql.name());
        CommonDict.INSTANCE.init("");
        this.valueRulesMap = configLoader.loadValueRulesYml(valueRulesYmlFilePath);
    }

    /**
     * 初始化，自定义 common_dict 路径。数据库默认为 MySQL。
     *
     * @param valueRulesYmlFilePath value_rules.yml 的 classpath 路径
     * @param regexDictYmlFilePath  common_dict.yml 的 classpath 路径
     *                              <pre>{@code
     *                              ValidatorEngine.INSTANCE.init("value_rules.yml", "my_custom_dict.yml");
     *                              }</pre>
     */
    public void init(String valueRulesYmlFilePath, String regexDictYmlFilePath) {
        initDbType(DbType.mysql.name());
        CommonDict.INSTANCE.init(regexDictYmlFilePath);
        this.valueRulesMap = configLoader.loadValueRulesYml(valueRulesYmlFilePath);
    }

    /**
     * 初始化，支持通用规则文件。数据库类型自定义。
     *
     * @param dbType                      数据库类型名称
     * @param valueRulesYmlFilePath       主规则文件路径
     * @param commonValueRulesYmlFilePath 通用规则文件路径（可为 null）
     * @param regexDictYmlFilePath        common_dict 路径
     *                                    <pre>{@code
     *                                    ValidatorEngine.INSTANCE.init("mysql", "value_rules.yml",
     *                                        "common_value_rules.yml", "common_dict.yml");
     *                                    }</pre>
     */
    public void init(String dbType, String valueRulesYmlFilePath,
                     String commonValueRulesYmlFilePath, String regexDictYmlFilePath) {
        init0(dbType, valueRulesYmlFilePath, regexDictYmlFilePath);
        if (commonValueRulesYmlFilePath != null) {
            this.commonValueRulesMap = configLoader.loadValueRulesYml(commonValueRulesYmlFilePath);
        }
    }

    /**
     * 全参数初始化，支持通用规则文件和自定义忽略字段。
     *
     * @param dbType                      数据库类型名称
     * @param valueRulesYmlFilePath       主规则文件路径
     * @param commonValueRulesYmlFilePath 通用规则文件路径（可为 null）
     * @param regexDictYmlFilePath        common_dict 路径
     * @param userIgnoreKeys              自定义忽略字段集合
     * @param customUseSnake              {@code true}=下划线命名模式，{@code false}=驼峰命名模式
     *                                    <pre>{@code
     *                                    Set<String> ignoreKeys = new HashSet<>(Arrays.asList("id", "version", "is_deleted"));
     *                                    ValidatorEngine.INSTANCE.init("mysql", "value_rules.yml",
     *                                        null, "common_dict.yml", ignoreKeys, true);
     *                                    }</pre>
     */
    public void init(String dbType, String valueRulesYmlFilePath, String commonValueRulesYmlFilePath,
                     String regexDictYmlFilePath,
                     Set<String> userIgnoreKeys, boolean customUseSnake) {
        this.isSnakeKeyMode = customUseSnake;
        ignoreKeys = userIgnoreKeys;
        checkIgnoreKeysForLegality();
        init(dbType, valueRulesYmlFilePath, commonValueRulesYmlFilePath, regexDictYmlFilePath);
    }

    /**
     * 初始化（不带通用规则文件），支持自定义忽略字段。
     *
     * @param dbType                数据库类型名称
     * @param valueRulesYmlFilePath 主规则文件路径
     * @param regexDictYmlFilePath  common_dict 路径
     * @param userIgnoreKeys        自定义忽略字段集合
     * @param customUseSnake        {@code true}=下划线命名模式，{@code false}=驼峰命名模式
     *                              <pre>{@code
     *                              Set<String> ignoreKeys = new HashSet<>(Arrays.asList("id", "version"));
     *                              ValidatorEngine.INSTANCE.init("mysql", "value_rules.yml",
     *                                  "common_dict.yml", ignoreKeys, false);
     *                              }</pre>
     */
    public void init(String dbType, String valueRulesYmlFilePath, String regexDictYmlFilePath,
                     Set<String> userIgnoreKeys, boolean customUseSnake, boolean lruCacheOpen, int lruCacheSize) {
        this.isSnakeKeyMode = customUseSnake;
        ignoreKeys = userIgnoreKeys;
        checkIgnoreKeysForLegality();
        init0(dbType, valueRulesYmlFilePath, regexDictYmlFilePath);
    }

    /**
     * 从 rules 目录初始化（每表一个 yml 文件），数据库默认为 MySQL。
     * <pre>{@code
     * // resources/rules/ 目录结构：
     * //   rules/
     * //     student.yml
     * //     order.yml
     * //     product.yml
     * ValidatorEngine.INSTANCE.initFromDir("rules");
     * }</pre>
     *
     * @param valueRulesDir rules 目录名称（相对于 classpath）
     */
    public void initFromDir(String valueRulesDir) {
        initDbType(DbType.mysql.name());
        CommonDict.INSTANCE.init("");
        this.valueRulesMap = configLoader.loadValueRulesYmlFromDir(valueRulesDir);
    }

    /**
     * 从 rules 目录初始化，自定义 common_dict 路径。
     *
     * @param valueRulesDir        rules 目录名称（相对于 classpath）
     * @param regexDictYmlFilePath common_dict.yml 路径
     *                             <pre>{@code
     *                             ValidatorEngine.INSTANCE.initFromDir("rules", "my_dict.yml");
     *                             }</pre>
     */
    public void initFromDir(String valueRulesDir, String regexDictYmlFilePath) {
        initDbType(DbType.mysql.name());
        CommonDict.INSTANCE.init(regexDictYmlFilePath);
        this.valueRulesMap = configLoader.loadValueRulesYmlFromDir(valueRulesDir);
    }

    /**
     * 从 rules 目录全参数初始化，支持通用规则目录、自定义忽略字段。
     *
     * @param dbType               数据库类型名称
     * @param valueRulesDir        主规则目录（相对于 classpath）
     * @param commonValueRulesDir  通用规则目录（可为 null）
     * @param regexDictYmlFilePath common_dict 路径
     * @param userIgnoreKeys       自定义忽略字段集合
     * @param customUseSnake       {@code true}=下划线命名模式
     *                             <pre>{@code
     *                             Set<String> ignoreKeys = new HashSet<>(Arrays.asList("id", "version"));
     *                             ValidatorEngine.INSTANCE.initFromDir("mysql", "rules", null,
     *                                 "common_dict.yml", ignoreKeys, true);
     *                             }</pre>
     */
    public void initFromDir(String dbType, String valueRulesDir, String commonValueRulesDir,
                            String regexDictYmlFilePath,
                            Set<String> userIgnoreKeys, boolean customUseSnake) {
        this.isSnakeKeyMode = customUseSnake;
        ignoreKeys = userIgnoreKeys;
        checkIgnoreKeysForLegality();

        initDbType(dbType);
        CommonDict.INSTANCE.init(regexDictYmlFilePath);
        this.valueRulesMap = configLoader.loadValueRulesYmlFromDir(valueRulesDir);

        if (commonValueRulesDir != null) {
            this.commonValueRulesMap = configLoader.loadValueRulesYmlFromDir(commonValueRulesDir);
        }
    }

    /**
     * 校验自定义忽略字段的合法性。<br>
     * 当下划线模式时，字段名不能包含大写字母；当驼峰模式时，字段名不能包含下划线。
     *
     * @throws IllegalArgumentException 如果字段名不匹配当前命名模式
     */
    private void checkIgnoreKeysForLegality() {
        for (String key : ignoreKeys) {
            if (this.isSnakeKeyMode) {
                char[] chars = key.toCharArray();
                for (char s : chars) {
                    if (Character.isUpperCase(s)) {
                        throw new IllegalArgumentException("ignore keys must be snake naming mode");
                    }
                }
            } else {
                char[] chars = key.toCharArray();
                for (char s : chars) {
                    if (s == '_') {
                        throw new IllegalArgumentException("ignore keys must be camel naming mode");
                    }
                }
            }
        }
    }

    // ===================== 规则查询 =====================

    /**
     * 判断指定表名和字段名是否有校验规则。
     *
     * @param tableName 表名，如 {@code "student"}
     * @param fieldName 字段名，如 {@code "id"}
     * @return 存在非空规则则返回 {@code true}
     * <pre>{@code
     * boolean existed = ValidatorEngine.INSTANCE.isRuleExisted("student", "id"); // true
     * boolean existed2 = ValidatorEngine.INSTANCE.isRuleExisted("student", "no_such_field"); // false
     * }</pre>
     */
    public boolean isRuleExisted(String tableName, String fieldName) {
        Map<String, Object> fieldsMap = this.valueRulesMap.get(tableName);
        if (fieldsMap == null) {
            return false;
        }
        Map<String, Object> rulesMap = (Map<String, Object>) fieldsMap.get(fieldName);
        if (rulesMap == null) {
            return false;
        }
        return rulesMap.size() > 0;
    }

    /**
     * 根据 fieldKey 获取对应的 {@link FieldRule} 实例。
     *
     * @param fieldKey 格式为 {@code tableName.fieldName}，如 {@code "student.id"}
     * @return {@link FieldRule} 实例，如果未找到则返回 {@code null}
     * <pre>{@code
     * FieldRule rule = ValidatorEngine.INSTANCE.getFieldRules("student.id");
     * // 返回 NumericFieldRule {type="numeric", numericMin=1, numericMax=128}
     *
     * FieldRule rule2 = ValidatorEngine.INSTANCE.getFieldRules("student.gender");
     * // 返回 EnumStringFieldRule {type="enum_string", enumValues=["male","female"]}
     * }</pre>
     */
    public FieldRule getFieldRules(String fieldKey) {
        Map<String, Object> rulesMap = getRulesMap(fieldKey);
        return (rulesMap == null) ? null : this.buildFieldRule(fieldKey, rulesMap);
    }

    /**
     * 从 fieldKey 中拆分 tableName 和 fieldName，再查询规则 Map。
     *
     * @param fieldKey 格式 {@code "table.field"} 或纯字段名（自动补 {@code _common} 前缀）
     * @return 规则 Map，未找到则返回 null
     */
    private Map<String, Object> getRulesMap(String fieldKey) {
        String[] keys = secureFieldKey(fieldKey);
        return getRuleMap(keys[0], keys[1]);
    }

    /**
     * 安全性解析 fieldKey。
     * <ul>
     *   <li>{@code "student.id"} → {@code ["student", "id"]}</li>
     *   <li>{@code "status"}     → {@code ["_common", "status"]}（自动补 {@code _common}）</li>
     * </ul>
     *
     * @param fieldKey 字段键值
     * @return 长度为 2 的数组，[tableName, fieldName]
     * @throws IllegalArgumentException 如果 fieldKey 为 null
     */
    private String[] secureFieldKey(String fieldKey) {
        if (fieldKey == null) {
            throw new IllegalArgumentException("fieldKey is illegal");
        }
        String[] keys = fieldKey.split("\\.");
        if (keys.length != 2) {
            keys = new String[]{"_common", keys[0]};
        }
        return keys;
    }

    /**
     * 通过 tableName 和 fieldName 查询配置规则。
     * <p>优先从 {@link #valueRulesMap} 查找，找不到时回退到 {@link #commonValueRulesMap}。</p>
     *
     * @param tableName 表名
     * @param fieldName 字段名
     * @return 规则配置 Map，未找到则返回 null
     * <pre>{@code
     * // 假设 valueRulesMap = { "student": { "id": {"type":"numeric", ...} } }
     * // 假设 commonValueRulesMap = { "_common": { "status": {"type":"enum_string", ...} } }
     *
     * Map<String, Object> idRule = getRuleMap("student", "id");
     * // → {"type":"numeric", "numeric_min":1, "numeric_max":128}
     *
     * Map<String, Object> statusRule = getRuleMap("_common", "status");
     * // → {"type":"enum_string", "enum_values":["active","inactive"]}
     *
     * Map<String, Object> none = getRuleMap("student", "no_such_field");
     * // → null
     * }</pre>
     */
    private Map<String, Object> getRuleMap(String tableName, String fieldName) {
        Map<String, Object> fieldsMap = this.valueRulesMap.get(tableName);
        if (fieldsMap == null) {
            if (this.commonValueRulesMap == null) {
                return null;
            }
            fieldsMap = this.commonValueRulesMap.get(tableName);
            if (fieldsMap == null) {
                return null;
            }
        }
        Map<String, Object> rulesMap = (Map<String, Object>) fieldsMap.get(fieldName);
        if (rulesMap == null) {
            return null;
        }
        return rulesMap;
    }

    /**
     * 根据规则 Map 构建 {@link FieldRule} 实例。
     *
     * @param fieldKey 字段键
     * @param rulesMap 规则配置 Map
     * @return FieldRule 具体子类实例
     * @throws NullPointerException 如果 {@code type} 对应的工厂方法不存在
     *                              <pre>{@code
     *                              // 输入：
     *                              //   fieldKey = "student.id"
     *                              //   rulesMap = {"type":"numeric", "numeric_min":1, "numeric_max":128}
     *                              // 输出：NumericFieldRule{fieldKey="student.id", type="numeric", numericMin=1, numericMax=128}
     *                              }</pre>
     */
    private FieldRule buildFieldRule(String fieldKey, Map<String, Object> rulesMap) {
        String type = (String) rulesMap.get(RuleKey.type.name());
        FieldRule fr = fieldRuleMap.get(type).get();
        if (fr == null) {
            return null;
        }
        fr.build(fieldKey, type, rulesMap);
        return fr;
    }

    // ===================== 规则序列化为 JSON =====================

    /**
     * 获取指定字段的完整校验规则（Map 形式，外层包裹 fieldKey）。
     *
     * @param fieldKey 格式 {@code "table.field"}
     * @return Map，key = fieldKey，value = 规则 JSON Map
     * <pre>{@code
     * Map<String, Object> result = ValidatorEngine.INSTANCE.getFieldValidatorRules("student.phone_number");
     * // 输出：
     * // {
     * //   "student.phone_number": {
     * //     "type": "string",
     * //     "stringCharset": "utf8",
     * //     "stringRegexKey": "phone_number",
     * //     "stringLengthMin": 11,
     * //     "stringLengthMax": 11,
     * //     "regexStr": "^1[3|4|5|7|8][0-9]{9}$"
     * //   }
     * // }
     * }</pre>
     */
    public Map<String, Object> getFieldValidatorRules(String fieldKey) {
        Map<String, Object> map = new HashMap<>(1);
        map.put(fieldKey, this.getFieldValidatorRulesJson(fieldKey));
        return map;
    }

    /**
     * 获取指定字段的校验规则 JSON 字符串。
     *
     * @param fieldKey 格式 {@code "table.field"}
     * @return JSON 字符串
     * <pre>{@code
     * String json = ValidatorEngine.INSTANCE.getFieldValidatorRulesStr("student.phone_number");
     * // json = "{\"student.phone_number\":{\"type\":\"string\", ...}}"
     * }</pre>
     */
    public String getFieldValidatorRulesStr(String fieldKey) {
        try {
            return mapper.writeValueAsString(this.getFieldValidatorRules(fieldKey));
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Failed to serialize field rules for " + fieldKey, e);
        }
        return null;
    }

    /**
     * 获取指定字段的校验规则（纯规则 Map，不含 fieldKey 包裹）。
     * <p>对于 {@code type="string"} 类型的字段，会自动从 {@link CommonDict} 中取出正则表达式并追加到 {@code regexStr} 字段。</p>
     *
     * @param fieldKey 格式 {@code "table.field"}
     * @return 规则 Map
     * <pre>{@code
     * // 数值类型字段（不追加 regexStr）：
     * Map<String, Object> rule = ValidatorEngine.INSTANCE.getFieldValidatorRulesJson("student.id");
     * // → {"fieldKey":"", "type":"numeric", "numericMin":1, "numericMax":128}
     *
     * // 字符串类型字段（自动追加 regexStr）：
     * Map<String, Object> rule2 = ValidatorEngine.INSTANCE.getFieldValidatorRulesJson("student.phone_number");
     * // → {"fieldKey":"", "type":"string", "stringCharset":"utf8", "stringLengthMin":11,
     * //    "stringLengthMax":11, "stringRegexKey":"phone_number", "regexStr":"^1[3|4|5|7|8][0-9]{9}$"}
     *
     * // 枚举类型字段：
     * Map<String, Object> rule3 = ValidatorEngine.INSTANCE.getFieldValidatorRulesJson("student.gender");
     * // → {"fieldKey":"", "type":"enum_string", "enumValues":["male","female"]}
     *
     * // 不存在的字段：
     * Map<String, Object> rule4 = ValidatorEngine.INSTANCE.getFieldValidatorRulesJson("nonexist.field");
     * // → null
     * }</pre>
     */
    public Map<String, Object> getFieldValidatorRulesJson(String fieldKey) {
        // 关键代码， getEnumValues也是使用这个方法，在此增加缓存
        FieldRule fieldRule = this.getFieldRules(fieldKey);
        if (fieldRule == null) {
            return null;
        }
        fieldRule.setFieldKey("");

        try {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String jsonStr = mapper.writeValueAsString(fieldRule);

            if (!RuleType.string.name().equals(fieldRule.getType())) {
                Map<String, Object> m = mapper.readValue(jsonStr, Map.class);
                return m;
            }

            String regexKey = fieldRule.getStringRegexKey();
            String regexStr = (String) CommonDict.INSTANCE.getRule(regexKey);
            if (StringUtils.isBlank(regexStr)) {
                throw new IllegalStateException(String.format("regex is blank, regexKey is %s", regexKey));
            }

            Map m = mapper.readValue(jsonStr, Map.class);
            if(m == null) {
                // String jsonStr = "null"; 时m会是null；
                return null;
            }

            m.put("regexStr", regexStr);
            // 指示 JS 端按字符数还是字节数做长度校验
            String charset = fieldRule.getStringCharset();
            m.put("lengthMode", org.apache.commons.lang3.StringUtils.isBlank(charset) ? "char" : "byte");
            return m;
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Failed to build field rule JSON for " + fieldKey, e);
        }
        return null;
    }

    /**
     * 获取指定字段的校验规则 JSON 字符串（不含 fieldKey 外层包裹）。
     *
     * @param fieldKey 格式 {@code "table.field"}
     * @return JSON 字符串
     * <pre>{@code
     * String json = ValidatorEngine.INSTANCE.getFieldValidatorRulesJsonStr("student.game_long_card");
     * // json = "{\"fieldKey\":\"\",\"type\":\"enum_numeric\",\"enumValues\":[3000000000,4000000000]}"
     * }</pre>
     */
    public String getFieldValidatorRulesJsonStr(String fieldKey) {
        try {
            return mapper.writeValueAsString(this.getFieldValidatorRulesJson(fieldKey));
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Failed to serialize field rule JSON for " + fieldKey, e);
        }
        return null;
    }

    /**
     * 关键代码，获取下拉列表框所需的数据
     *
     *
     * @param fieldKey
     * @return
     */
    public List<Object> getEnumValues(String fieldKey) {
        Map<String, Object> ruleJson = this.getFieldValidatorRulesJson(fieldKey);
        if (ruleJson == null) {
            return null;
        }

        String type = (String) ruleJson.get(RuleKey.type.name());
        if (RuleType.isEnum(type)) {
            return (List<Object>) ruleJson.get(NameUtil.lineToHump(RuleKey.enum_values.name()));
        }

        return null;
    }

    // ===================== 校验入口 =====================

    /**
     * <b>单字段校验。</b>
     * <p>根据配置的规则校验单个参数值。</p>
     *
     * <p><b>校验规则类型对照表：</b>
     * <pre>{@code
     * type           | 校验方式
     * ---------------|------------------------------
     * numeric        | 转 BigInteger，检查最小值≤值≤最大值
     * decimal        | 转 BigDecimal，检查最小值≤值≤最大值
     * string         | 检查字符长度（支持 charset），正则匹配
     * enum_string    | 检查值是否在枚举列表中
     * enum_numeric   | 转 BigInteger/BigDecimal，检查是否在枚举列表中
     * enum_decimal   | 同上（使用 BigDecimal）
     * date           | 解析 yyyy-MM-dd，检查是否在 [beginAt, endAt) 范围内
     * datetime       | 解析 yyyy-MM-dd HH:mm:ss，检查是否在 [beginAt, endAt) 范围内
     * }</pre>
     *
     * @param validatorKey 字段 key，格式 {@code "tableName.fieldName"}。
     *                     如果仅传入字段名（不含 {@code .}），则自动补 {@code "_common."} 前缀
     * @param paramValue   待校验的参数值，支持 String、Number、Boolean、Date 等
     * @return 校验通过返回 {@code true}，否则返回 {@code false}
     * @throws IllegalStateException 如果对应的规则未找到
     *
     *                               <pre>{@code
     *                               // 假设 YAML 配置如下：
     *                               // student:
     *                               //   id:          { type: numeric, numeric_min: 1, numeric_max: 128 }
     *                               //   gender:      { type: enum_string, enum_values: [male, female] }
     *                               //   phone_number:{ type: string, string_regex_key: phone_number, string_length_min: 11, string_length_max: 11 }
     *                               //   money:       { type: decimal, decimal_min: 0.00, decimal_max: 300.00 }
     *                               //   birthday:    { type: date, begin_at: 2020-01-01, end_at: 2030-12-31 }
     *                               //   game_card:   { type: enum_numeric, enum_values: [1, 2] }
     *                               //   game_long_card: { type: enum_numeric, enum_values: [3000000000, 4000000000] }
     *
     *                               ValidatorEngine.INSTANCE.init("value_rules.yml");
     *
     *                               // --- numeric ---
     *                               ValidatorEngine.INSTANCE.validate("student.id", 1);               // true,  1 ∈ [1, 128]
     *                               ValidatorEngine.INSTANCE.validate("student.id", 0);               // false, 0 ∉ [1, 128]
     *                               ValidatorEngine.INSTANCE.validate("student.id", "2");             // true,  字符串自动转数字
     *
     *                               // --- decimal ---
     *                               ValidatorEngine.INSTANCE.validate("student.money", "200.00");     // true,  200.00 ∈ [0, 300]
     *                               ValidatorEngine.INSTANCE.validate("student.money", "300.01");     // false, 300.01 ∉ [0, 300]
     *
     *                               // --- string ---
     *                               ValidatorEngine.INSTANCE.validate("student.phone_number", "15973166256");  // true, 符合长度和正则
     *                               ValidatorEngine.INSTANCE.validate("student.phone_number", "159731662561"); // false, 超长
     *                               ValidatorEngine.INSTANCE.validate("student.phone_number", "19973166256");  // false, 正则不匹配
     *
     *                               // --- enum_string ---
     *                               ValidatorEngine.INSTANCE.validate("student.gender", "male");       // true
     *                               ValidatorEngine.INSTANCE.validate("student.gender", "other");      // false
     *
     *                               // --- enum_numeric ---
     *                               ValidatorEngine.INSTANCE.validate("student.game_card", 1);         // true
     *                               ValidatorEngine.INSTANCE.validate("student.game_card", 3);         // false
     *                               ValidatorEngine.INSTANCE.validate("student.game_long_card", 3000000000l);  // true
     *                               ValidatorEngine.INSTANCE.validate("student.game_long_card", 2000000000l);  // false
     *
     *                               // --- 使用 _common 前缀（不写表名）---
     *                               // 当 validate("status", value) 时，会自动查找 _common.status 的规则
     *                               ValidatorEngine.INSTANCE.validate("status", "active");  // 查找 "_common.status" 的规则
     *                               }</pre>
     */
    public boolean validate(String validatorKey, Object paramValue) {
        FieldRule fr = this.getFieldRules(validatorKey);
        if (fr == null) {
            fr = this.getFieldRules("_common." + validatorKey);
            if (fr == null) {
                throw new IllegalStateException(String.format("No field(%s) rule found", validatorKey));
            }
        }
        return fr.validate(paramValue);
    }

    /**
     * <b>Java Bean 对象校验。</b>
     * <p>遍历 Bean 的所有属性，使用 {@code prefix.propertyName} 作为 fieldKey 逐个校验。</p>
     *
     * <p><b>处理流程：</b>
     * <ol>
     *   <li>通过 Introspector 获取所有属性描述符</li>
     *   <li>过滤掉没有 setter 方法的属性（不校验）</li>
     *   <li>根据 {@link #isSnakeKeyMode} 决定是否将属性名从驼峰转为下划线</li>
     *   <li>忽略 {@link #ignoreKeys} 和 {@code customIgnoreKeys} 中指定的字段</li>
     *   <li>跳过不支持的类型（日期类型直接通过，复杂对象抛异常）</li>
     *   <li>空值判断：在 {@code nullableKeys} 中的跳过，否则记为错误</li>
     *   <li>非空值：调用 {@link #validate(String, Object)} 进行校验</li>
     * </ol>
     *
     * @param bean             待校验的 Java Bean 对象
     * @param prefix           表名/前缀，用于拼接 fieldKey（如 {@code "student"}）
     * @param customIgnoreKeys 额外忽略的字段名数组（可为 null）
     * @param nullableKeys     允许为空的字段名数组（可为 null）
     * @return 校验失败的字段名列表（全部通过时返回空列表）
     *
     * <pre>{@code
     * // 假设 Student 类：
     * //   private int id;
     * //   private String phoneNumber;
     * //   private String gender;
     * //   private BigDecimal money;
     * //   private long gameLongCard;
     * //   private int version;
     *
     * Student student = new Student();
     * student.setPhoneNumber("15973166256");
     * student.setGender("male");
     * student.setMoney(new BigDecimal("200.00"));
     * student.setGameLongCard(4000000000L);
     *
     * List<String> errors = ValidatorEngine.INSTANCE.validate(student, "student",
     *     new String[]{"version"},        // 额外忽略 version 字段
     *     new String[]{"id"});             // id 字段允许为空
     *
     * // 如果 phoneNumber 格式不对，errors 包含 "phone_number"
     * // 如果 gender 不是 male/female，errors 包含 "gender"
     * // 如果所有字段都合规，errors 为空列表 []
     * }</pre>
     */
    public List<String> validate(Object bean, String prefix,
                                 String[] customIgnoreKeys, String[] nullableKeys) {
        List<String> errorKeys = new ArrayList<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] proDescriptors = beanInfo.getPropertyDescriptors();

            List<String> setMethodCache = new ArrayList<>();
            Method[] names = bean.getClass().getDeclaredMethods();
            for (Method method : names) {
                if (method.getName().startsWith("set")) {
                    setMethodCache.add(method.getName());
                }
            }

            if (proDescriptors != null && proDescriptors.length > 0) {
                for (PropertyDescriptor propDesc : proDescriptors) {

                    if (propDesc.getWriteMethod() == null) {
                        String poSetMethod = "s" + propDesc.getReadMethod().getName().substring(1);
                        if (!setMethodCache.contains(poSetMethod)) {
                            continue;
                        }
                    }

                    String name = propDesc.getName();
                    if (isSnakeKeyMode) {
                        name = NameUtil.humpToLine(name);
                    }

                    Method method = propDesc.getReadMethod();
                    Object value = method.invoke(bean);

                    if (ignoreKeys.contains(name)) {
                        continue;
                    }

                    if (customIgnoreKeys != null) {
                        if (Arrays.asList(customIgnoreKeys).contains(name)) {
                            continue;
                        }
                    }

                    if ((value != null) && !(value instanceof String) && !(value instanceof Number)
                            && (!(value instanceof Boolean))) {
                        if ((value instanceof LocalDateTime) || (value instanceof LocalDate) || (value instanceof Date)) {
                            continue;
                        }
                        throw new UnsupportedOperationException(
                                String.format("The type of parameter is not supported, name: %s, value: %s"
                                        , name, mapper.writeValueAsString(value)));
                    }

                    String valueStr = value == null ? "" : String.valueOf(value);
                    if (StringUtils.isBlank(valueStr)) {
                        if (nullableKeys != null) {
                            if (Arrays.asList(nullableKeys).contains(name)) {
                                continue;
                            }
                        }
                        errorKeys.add(name);
                        continue;
                    }

                    String prefixName = prefix + "." + name;
                    boolean isOK = INSTANCE.validate(prefixName, valueStr);
                    if (!isOK) {
                        errorKeys.add(name);
                    }
                }
            }
            return errorKeys;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ===================== 代码生成（委托给 EnumCodeGenerator） =====================

    /**
     * 根据当前加载的 valueRulesMap 中的枚举类型配置，生成 Java 枚举代码。
     * <p>仅对 {@code enum_string}、{@code enum_numeric}、{@code enum_decimal} 类型的字段生成。</p>
     *
     * @param packageName 目标 Java 包名，如 {@code "com.baymax.pvg2.values"}
     * @return 格式化后的 Java 源码字符串
     *
     * <pre>{@code
     * // 假设配置中有枚举字段：
     * // student.game_long_card: enum_numeric, values: [3000000000, 4000000000]
     * // student.float_card:     enum_decimal, values: [3.11, 4000000000.123456]
     *
     * ValidatorEngine.INSTANCE.init("value_rules_enum_output_test.yml");
     * String source = ValidatorEngine.INSTANCE.generateJavaEnumCode("com.baymax.pvg2.values");
     * // 生成的代码片段：
     * // public final class ValueEnumRange implements Serializable {
     * //   public static final class student {
     * //     public enum float_card {
     * //       NUMBER_3$11(new BigDecimal("3.11")),
     * //       NUMBER_4000000000$123456(new BigDecimal("4000000000.123456"));
     * //     }
     * //   }
     * // }
     * }</pre>
     */
    public String generateJavaEnumCode(String packageName) {
        if (enumCodeGenerator == null) {
            enumCodeGenerator = new EnumCodeGenerator(this);
        }
        if (formatter != null) {
            enumCodeGenerator.setFormatter(formatter);
        }
        return enumCodeGenerator.generateJavaEnumCode(packageName);
    }

    /**
     * 将生成的内容写入文件。
     *
     * @param fileName    文件名（不含后缀）
     * @param packageName Java 包名
     * @param content     文件内容
     * @param toSrcTest   {@code true}=写入 src/test/java，{@code false}=写入 src/main/java
     *
     *                    <pre>{@code
     *                    ValidatorEngine.INSTANCE.writeToFile("ValueEnumRange",
     *                        "com.baymax.pvg2.values", sourceCode, true);
     *                    // 写入到: target/classes/../../src/test/java/com/baymax/pvg2/values/ValueEnumRange.java
     *                    }</pre>
     */
    public void writeToFile(String fileName, String packageName, String content, boolean toSrcTest) {
        if (enumCodeGenerator == null) {
            enumCodeGenerator = new EnumCodeGenerator(this);
        }
        enumCodeGenerator.writeToFile(fileName, packageName, content, toSrcTest);
    }

    /**
     * 将 hxValidator.js 发布到指定路径下，供前端远程加载使用。
     *
     * @param filePath 目标目录路径
     *
     *                 <pre>{@code
     *                 ValidatorEngine.INSTANCE.publishHxValidatorJS("/path/to/webapp/js/");
     *                 // 生成文件：/path/to/webapp/js/hxValidator.js
     *                 }</pre>
     */
    public void publishHxValidatorJS(String filePath) {
        String path = Thread.currentThread().getContextClassLoader()
                .getResource(Const.HX_VALIDATOR + "." + Const.FileType.js.name()).getPath();
        try {
            String content = new String(Files.readAllBytes(Paths.get(path)));
            FileWriter.write(filePath, Const.HX_VALIDATOR, Const.FileType.js.name(), content);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ===================== 静态规则创建辅助方法 =====================

    /**
     * 创建数值类型字段的规则（numeric 或 decimal）。
     * <p>根据 Java 类型自动选择 {@link NumericFieldRule} 或 {@link DecimalFieldRule}。</p>
     *
     * @param list       规则列表（追加到该列表）
     * @param tableName  表名
     * @param columnName 字段名
     * @param clazzName  Java 类型的全限定名，如 {@code "java.lang.Integer"}
     *
     *                   <pre>{@code
     *                   List<FieldRule> list = new ArrayList<>();
     *                   ValidatorEngine.makeNumericRule(list, "student", "age", "java.lang.Integer");
     *                   // → NumericFieldRule{fieldKey="student.age", type="numeric", numericMin=0, numericMax=Long.MAX_VALUE}
     *
     *                   ValidatorEngine.makeNumericRule(list, "product", "price", "java.math.BigDecimal");
     *                   // → DecimalFieldRule{fieldKey="product.price", type="decimal", decimalMin=0.00, decimalMax=Long.MAX_VALUE}
     *
     *                   ValidatorEngine.makeNumericRule(list, "product", "count", "java.lang.String");
     *                   // → 不匹配任何类型，不添加（String 会被当作字符串字段处理）
     *                   }</pre>
     */
    public static void makeNumericRule(List<FieldRule> list, String tableName,
                                       String columnName, String clazzName) {
        Long displaySize = Long.MAX_VALUE;

        if (Integer.class.getName().equals(clazzName)
                || Long.class.getName().equals(clazzName)
                || BigInteger.class.getName().equals(clazzName)
                || Boolean.class.getName().equals(clazzName)) {
            FieldRule fr = new NumericFieldRule();
            fr.setFieldKey(tableName + "." + columnName);
            fr.setType(RuleType.numeric.name());
            fr.setNumericMin(new BigInteger("0"));
            fr.setNumericMax(new BigInteger(String.valueOf(displaySize)));
            list.add(fr);
        } else if (BigDecimal.class.getName().equals(clazzName)) {
            FieldRule fr = new DecimalFieldRule();
            fr.setFieldKey(tableName + "." + columnName);
            fr.setType(RuleType.decimal.name());
            fr.setDecimalMin(new BigDecimal("0.00"));
            fr.setDecimalMax(new BigDecimal(String.valueOf(displaySize)));
            list.add(fr);
        }
    }

    /**
     * 创建字符串类型字段的规则。
     * <p>默认使用 {@code "any_string"} 正则（匹配任意内容），长度范围从 1 到 {@code displaySize}。</p>
     *
     * @param list        规则列表（追加到该列表）
     * @param tableName   表名
     * @param columnName  字段名
     * @param displaySize 数据库 varchar 长度；如果为 null 则默认为 128
     *
     *                    <pre>{@code
     *                    List<FieldRule> list = new ArrayList<>();
     *                    ValidatorEngine.makeAnyStringRule(list, "student", "name", 50);
     *                    // → StringRegexFieldRule{
     *                    //     fieldKey="student.name", type="string",
     *                    //     stringRegexKey="any_string", stringLengthMin=1, stringLengthMax=50
     *                    //   }
     *
     *                    ValidatorEngine.makeAnyStringRule(list, "student", "bio", null);
     *                    // → stringLengthMax=128（默认值）
     *
     *                    // 当 dbType = oracle 时：
     *                    ValidatorEngine.initDbType("oracle");
     *                    ValidatorEngine.makeAnyStringRule(list, "student", "name", 30);
     *                    // → 额外设置 stringCharset="utf8"
     *                    }</pre>
     */
    public static void makeAnyStringRule(List<FieldRule> list, String tableName, String columnName, Integer displaySize) {
        if (displaySize == null) {
            displaySize = 128;
        }

        FieldRule rule = new StringRegexFieldRule();
        rule.setFieldKey(tableName + "." + columnName);
        rule.setStringRegexKey("any_string");
        rule.setStringLengthMin(1);
        rule.setStringLengthMax(displaySize);

        if (getDbType() == DbType.oracle) {
            rule.setStringCharset("utf8");
        }

        rule.setType(RuleType.string.name());
        list.add(rule);
    }

    /**
     * 从数据源中读取多张表的列元数据，构建 {@code tableName → TableMeta} 映射。
     * <p>复用同一个数据库连接（避免循环中频繁获取和释放连接）。</p>
     *
     * @param dataSource 数据源
     * @param tables     表名列表
     * @return 表名到 TableMeta 的映射
     * @throws SQLException 数据库操作异常
     *
     *                      <pre>{@code
     *                      List<String> tableNames = Arrays.asList("student", "order");
     *                      Map<String, TableMeta> tableMeta = ValidatorEngine.makeStringTableMetaMap(dataSource, tableNames);
     *                      // 返回：
     *                      // {
     *                      //   "student": TableMeta{
     *                      //     tableName="student",
     *                      //     columnMetaList=[
     *                      //       ColumnMeta{name="id", displaySize=11, originClass="java.lang.Integer"},
     *                      //       ColumnMeta{name="name", displaySize=50, originClass="java.lang.String"},
     *                      //       ColumnMeta{name="money", displaySize=12, originClass="java.math.BigDecimal"},
     *                      //     ]
     *                      //   },
     *                      //   "order": TableMeta{...}
     *                      // }
     *                      }</pre>
     */
    public static Map<String, TableMeta> makeStringTableMetaMap(DataSource dataSource, List<String> tables) throws SQLException {
        Map<String, TableMeta> tablesWithColumnMetaMapping = new ConcurrentHashMap<>();
        Connection con = dataSource.getConnection();
        try {
            for (String tableName : tables) {
                List<ColumnMeta> columnsMetaList = TableMetaKit.getColumnsMeta(con, tableName);
                if (columnsMetaList == null || columnsMetaList.isEmpty()) {
                    continue;
                }
                TableMeta oTableMeta = new TableMeta();
                oTableMeta.setTableName(tableName);
                oTableMeta.setColumnMetaList(columnsMetaList);
                tablesWithColumnMetaMapping.put(tableName, oTableMeta);
            }
        } finally {
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }
        return tablesWithColumnMetaMapping;
    }

    // ===================== YML 生成（从数据库表生成默认配置） =====================

    /**
     * 从数据库读取表结构，生成默认的 value_rules.yml 配置字符串。
     * <p>字符串类型的字段 → {@link #makeAnyStringRule} <br>
     * 数值/布尔/大数类型的字段 → {@link #makeNumericRule}</p>
     *
     * @param oldYmlPath   旧的 value_rules.yml 路径（用于读取已有的手工配置，避免覆盖）
     * @param dataSource   数据源
     * @param databaseName 数据库名称
     * @param exceptTables 排除的表名列表（可为 null）
     * @return YAML 格式的配置字符串
     * @throws SQLException 数据库操作异常
     *
     *                      <pre>{@code
     *                      // 从数据库 student 表自动生成配置：
     *                      String yml = ValidatorEngine.INSTANCE.generateDefaultYml(
     *                          "old_value_rules.yml", dataSource, "my_database", null);
     *                      // 输出示例：
     *                      // student:
     *                      //   id:
     *                      //     type: numeric
     *                      //     numeric_min: 0
     *                      //     numeric_max: 9223372036854775807
     *                      //   name:
     *                      //     type: string
     *                      //     string_regex_key: any_string
     *                      //     string_length_min: 1
     *                      //     string_length_max: 50
     *                      //   money:
     *                      //     type: decimal
     *                      //     decimal_min: 0.00
     *                      //     decimal_max: 9223372036854775807
     *                      }</pre>
     */
    public String generateDefaultYml(String oldYmlPath, DataSource dataSource, String databaseName, List<String> exceptTables)
            throws SQLException {
        List<String> tables = TableMetaKit.getTables(dataSource, databaseName, exceptTables);
        if (tables == null || tables.isEmpty()) {
            return "";
        }

        Map<String, TableMeta> tablesWithColumnMetaMapping = makeStringTableMetaMap(dataSource, tables);

        List<FieldRule> list = new ArrayList<>();
        for (Map.Entry<String, TableMeta> entry : tablesWithColumnMetaMapping.entrySet()) {
            String tableName = entry.getKey();
            TableMeta tableMeta = entry.getValue();

            for (ColumnMeta meta : tableMeta.getColumnMetaList()) {
                String columnName = meta.getName();
                String clazzName = meta.getOriginClass();
                Integer displaySize = meta.getDisplaySize();

                if (ignoreKeys.contains(columnName)) {
                    continue;
                }

                if (String.class.getName().equals(clazzName)
                        || Date.class.getName().equals(clazzName)) {
                    makeAnyStringRule(list, tableName, columnName, displaySize);
                } else {
                    makeNumericRule(list, tableName, columnName, clazzName);
                }
            }
        }

        return generateDefaultYml(oldYmlPath, list);
    }

    // ===================== YML 合并生成 =====================

    /**
     * 合并新旧 YAML 配置生成新的 YAML 配置字符串。
     * <p>已有旧配置的字段保留不变，新字段追加，表中已删除的字段自动清理。</p>
     *
     * @param oldYmlPath 旧的 YAML 配置文件路径
     * @param list       从数据库生成的新字段规则列表
     * @return 合并后的 YAML 配置字符串
     *
     * <pre>{@code
     * // 已有旧配置（value_rules_clean.yml）：
     * //   court: { id: { string_length_min: 2, string_length_max: 3, type: String } }
     * //
     * // 新生成规则列表：
     * //   [common.id (String, 1-128), court.id (String, 2-3)]
     * //
     * // 结果：
     * //   common:
     * //     id: { string_length_min: 1, string_length_max: 128, type: String }
     * //   court:
     * //     id: { string_length_min: 2, string_length_max: 3, type: String }
     * //     （保留旧配置 court.id）
     * }</pre>
     */
    public String generateDefaultYml(String oldYmlPath, List<FieldRule> list) {
        Map<String, Map<String, Object>> oldConfig = configLoader.loadValueRulesYml(oldYmlPath);
        Map<String, Object> tableMap = buildMergedTableMap(oldConfig, list);
        Yaml yaml = new Yaml();
        return yaml.dumpAs(cleanCopyOfMap(tableMap), Tag.MAP, DumperOptions.FlowStyle.BLOCK);
    }

    /**
     * 将合并后的 tableMap 按表名拆分为独立 YAML 文件，写入指定目录。
     * <p>每个文件只包含一个表的数据，文件名为表名，如 {@code student.yml}、{@code order.yml}。</p>
     *
     * @param tableMap 合并后的完整规则 Map
     * @param rulesDir 输出目录（绝对路径）
     *
     *                 <pre>{@code
     *                 Map<String, Object> tableMap = new HashMap<>();
     *                 tableMap.put("student", fieldMap1);
     *                 tableMap.put("order", fieldMap2);
     *
     *                 ValidatorEngine.INSTANCE.generatePerTableYmlFiles(tableMap, "/path/to/resources/rules");
     *                 // 生成文件：
     *                 //   /path/to/resources/rules/student.yml
     *                 //   /path/to/resources/rules/order.yml
     *
     *                 // student.yml 内容示例：
     *                 // student:
     *                 //   id:
     *                 //     type: numeric
     *                 //     numeric_min: 1
     *                 //     numeric_max: 128
     *                 }</pre>
     */
    public void generatePerTableYmlFiles(Map<String, Object> tableMap, String rulesDir) {
        Yaml yaml = new Yaml();

        // 生成前备份整个 rules 目录
        File rulesDirFile = new File(rulesDir);
        if (rulesDirFile.exists()) {
            File backupDir = new File(rulesDir + "_" + System.currentTimeMillis());
            rulesDirFile.renameTo(backupDir);
        }

        for (Map.Entry<String, Object> entry : tableMap.entrySet()) {
            String tableName = entry.getKey();
            Object fieldsVal = entry.getValue();

            Map<String, Object> singleTableMap = new HashMap<>(1);
            singleTableMap.put(tableName, fieldsVal);

            String dumpStr = yaml.dumpAs(cleanCopyOfMap(singleTableMap), Tag.MAP, DumperOptions.FlowStyle.BLOCK);
            FileWriter.write(rulesDir, tableName, "yml", dumpStr);
        }
    }

    /**
     * 合并新旧配置，生成完整的 tableMap。
     * <p>旧配置中已存在的字段保持不动，新添加的字段追加进去，
     * 数据库中已不存在的字段从结果中移除。</p>
     *
     * @param oldConfig 旧的配置 Map（从 YAML 文件读取）
     * @param newRuleList      从数据库生成的新字段规则列表
     * @return 合并后的 tableMap，结构：{@code Map<tableName, Map<fieldName, ruleMap>>}
     *
     * <pre>{@code
     * // 旧配置：
     * //   student: { id: { type: numeric, numeric_min: 1, numeric_max: 128 } }
     * //
     * // 新规则列表：
     * //   [student.id (numeric, 1-128)], [student.name (string, 1-50)]
     * //
     * // 返回：
     * //   { student: { id: { type: numeric, ... }, name: { type: string, ... } } }
     * //   （student.id 保留旧配置，student.name 为新追加）
     * }</pre>
     */
    public Map<String, Object> buildMergedTableMap(Map<String, Map<String, Object>> oldConfig, List<FieldRule> newRuleList) {
        Map<String, Object> tableMap = new HashMap<>();
        if (oldConfig != null) {
            tableMap.putAll(oldConfig);
        }

        for (FieldRule newRule : newRuleList) {
            String[] keys = secureFieldKey(newRule.getFieldKey());
            String tableName = keys[0];
            String fieldName = keys[1];

            Map<String, Object> fieldMap;
            // 不改变旧值，因为新值是采用默认配置，并不一样的适合应用系统
            if (oldConfig != null) {
                fieldMap = oldConfig.get(tableName);
                if (fieldMap != null) {
                    Map<String, Object> rulesMap = (Map<String, Object>) fieldMap.get(fieldName);
                    if (rulesMap != null && rulesMap.size() > 0) {
                        logger.info("fieldName exist: " + fieldName);
                        continue;
                    }
                }
            }

            fieldMap = (Map<String, Object>) tableMap.get(tableName);
            if (fieldMap == null) {
                fieldMap = new HashMap<>();
                tableMap.put(tableName, fieldMap);
            }

            Map<String, Object> newRuleBeanMap = BeanUtil.beanToMap(newRule);
            logger.info("newRuleBeanMap:" + newRuleBeanMap);

            Map<String, Object> ruleMap = new HashMap<>();
            for (Map.Entry<String, Object> e : newRuleBeanMap.entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                if ("fieldKey".equals(key)) {
                    // ?
                    continue;
                }
                ruleMap.put(NameUtil.humpToLine(key), val);
            }
            fieldMap.put(fieldName, ruleMap);
        }

        //移除 tableMap 中已在数据库中被删除的字段。
        removeDeletedRule(tableMap, newRuleList);
        return tableMap;
    }

    /**
     * 移除 tableMap 中已在数据库中被删除的字段。
     * <p>对比 {@code listFromTable} 中的 fieldKey，如果 tableMap 中的字段不在其中，则移除。</p>
     *
     * @param tableMap             当前完整的 tableMap
     * @param newRuleListFromTable 从数据库读取到的当前字段列表
     *
     *                             <pre>{@code
     *                             // tableMap 包含：{ student: { id: {...}, name: {...}, old_field: {...} } }
     *                             // listFromTable 包含：student.id, student.name（old_field 已被删除）
     *                             // 执行后 tableMap = { student: { id: {...}, name: {...} } }
     *                             }</pre>
     */
    private void removeDeletedRule(Map<String, Object> tableMap, List<FieldRule> newRuleListFromTable) {
        Set<String> fieldIncludeUnused = new HashSet<>();
        // 当前旧配置和新配置合并的 tableMap，用 tableName.field 的方式确定字段形成集合
        tableMap.forEach((tableName, v) -> {
            Map<String, Object> fieldMap = (Map<String, Object>) v;
            fieldMap.forEach((fieldName, rule) -> {
                fieldIncludeUnused.add(String.format("%s.%s", tableName, fieldName));
            });
        });

        // 从新表中读取到的新配置
        Set<String> fieldForOutput = new HashSet<>();
        newRuleListFromTable.forEach((rule) -> fieldForOutput.add(rule.getFieldKey()));

        // 新表中没有的就是属于需要删除的
        Set<String> fieldToBeFilter = new HashSet<>();
        fieldIncludeUnused.forEach((fieldName) -> {
            if (!fieldForOutput.contains(fieldName)) {
                fieldToBeFilter.add(fieldName);
            }
        });

        // 进行删除
        fieldToBeFilter.forEach((fieldKey) -> {
            String[] keys = secureFieldKey(fieldKey);
            String tableName = keys[0];
            String fieldName = keys[1];

            Map<String, Object> fieldMap = (Map<String, Object>) tableMap.get(tableName);
            if (fieldMap.containsKey(fieldName)) {
                fieldMap.remove(fieldName);
            }
            if (fieldMap.isEmpty()) {
                tableMap.remove(tableName);
            }
        });
    }

    /**
     * 深度清理 Map 中所有值为 null 的条目（递归）。
     *
     * @param map 待清理的 Map
     * @return 清理后的 Map（与原 Map 为同一对象，不是副本）
     *
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", null);
     * map.put("c", new HashMap() {{ put("d", null); put("e", 2); }});
     *
     * Map<String, Object> cleaned = ValidatorEngine.cleanCopyOfMap(map);
     *
     * // 结果：
     * // { "a": 1, "c": { "e": 2 } }
     * // b 和 c.d 因为值为 null 被移除
     * }</pre>
     */
    public static Map<String, Object> cleanCopyOfMap(Map<String, Object> map) {
        Map<String, Object> copyMap = new HashMap<>(map.size());
        copyMap.putAll(map);
        return deepCleanMap(copyMap);
    }

    /**
     * 递归清理 Map 中的 null 值条目。
     *
     * @param copyMap 待清理的 Map（会直接修改此 Map）
     * @return 清理后的 Map
     */
    private static Map<String, Object> deepCleanMap(Map<String, Object> copyMap) {
        copyMap.entrySet().removeIf((entry) -> {
            if (entry.getValue() instanceof Map) {
                deepCleanMap((Map<String, Object>) entry.getValue());
            }
            return entry.getValue() == null;
        });
        return copyMap;
    }
}
