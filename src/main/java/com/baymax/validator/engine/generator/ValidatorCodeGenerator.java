package com.baymax.validator.engine.generator;

import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.YamlConfigLoader;
import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.generator.kit.TableMetaKit;
import com.baymax.validator.engine.generator.meta.ColumnMeta;
import com.baymax.validator.engine.generator.meta.TableMeta;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.preset.DbType;
import com.baymax.validator.engine.utils.FileWriter;
import com.baymax.validator.engine.utils.StrUtil;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author xiao.hu
 * @date 2022-03-30
 * @apiNote
 */
public class ValidatorCodeGenerator {
    private static final Logger logger = Logger.getLogger(ValidatorCodeGenerator.class.getName());

    /**
     * 生成字段校验相关的java类，此类用于数据校验，根据此类可以得到对应的属性，可以以json的形式反馈到前端
     * 用于前端js校验
     *
     * valueRuleModulePath 和 valueEnumRangeModulePath 都可以是
     * String moduleTargetPath = System.getProperty("user.dir") + "/spry-validator";
     */
    public static void generateValidatorConfig(DbType dbType, DataSource dataSource, String databaseName, List<String> exceptTables,
                                               String valueRuleModulePath,
                                               String valueEnumRangeModulePath,
                                               String packageName,
                                               Set<String> userIgnoreKeys, boolean customUseSnake,
                                               String valueRulesDirectory, boolean fromDir) throws SQLException {
        ValidatorEngine.INSTANCE.setUserIgnoreKeys(userIgnoreKeys);
        // 1. 获取校验规则 YAML 文件的存放目录
        // 示例：valueRulesDirectory = "validator/rules"
        // 返回：valueRulesYmlDirectory = "validator/rules/"
        String valueRulesYmlDirectory = normalizeValueRulesYmlDirectory(valueRulesDirectory);

        // 2. 获取旧的配置文件路径
        // 示例：regexDictYmlFilePath = "validator/rules/common_dict.yml"
        String regexDictYmlFilePath = valueRulesYmlDirectory + Const.COMMON_DICT_FILENAME;

        // 3. 加载旧的配置（用于合并）
        // 注意这里是单文件的旧配置
        YamlConfigLoader configLoader = new YamlConfigLoader();

        Map<String, Map<String, Object>> oldConfig;
        if(fromDir) {
            oldConfig = configLoader.loadValueRulesYmlFromDir(valueRulesYmlDirectory + "rules");
        }
        else {
            // 示例：oldValueRulesYmlFilePath = "validator/rules/value_rules.yml"
            oldConfig = configLoader.loadValueRulesYml(valueRulesYmlDirectory + Const.VALUE_RULES_FILENAME);
        }

        logger.info("load old yml rules: " + oldConfig.keySet());

        // 4. 从数据库获取所有表名（排除指定表）
        // 示例：tables = ["user", "order", "product", "category"]
        List<String> tables = TableMetaKit.getTables(dataSource, databaseName, exceptTables);
        if(tables == null || tables.isEmpty()) {
            return;
        }

        logger.info("have tables: " + tables);

        // 5. 构建表元数据映射（表名 -> 表结构信息）
        // 示例返回结果：
        // {
        //   "user": TableMeta{tableName="user", columnMetaList=[...]},
        //   "order": TableMeta{tableName="order", columnMetaList=[...]}
        // }
        Map<String, TableMeta> tablesWithColumnMetaMapping = buildTableMetaMap(dataSource, tables);

        // 6. 为每个表的每个字段生成校验规则
        // 示例：遍历 user 表的字段：
        //   - id (BIGINT) -> 生成数字类型规则
        //   - username (VARCHAR(50)) -> 生成字符串类型规则，maxLength=50
        //   - age (INT) -> 生成数字类型规则
        //   - email (VARCHAR(100)) -> 生成字符串类型规则，maxLength=100
        //   - created_at (DATETIME) -> 生成字符串类型规则
        List<FieldRule> list = new ArrayList<>();
        for (Map.Entry<String, TableMeta> entry : tablesWithColumnMetaMapping.entrySet()) {
            String tableName = entry.getKey();
            TableMeta tableMeta = entry.getValue();

            List<ColumnMeta> columnMetaList = tableMeta.getColumnMetaList();
            for (ColumnMeta meta : columnMetaList) {
                String columnName = meta.getName();          // 示例：username
                String clazzName = meta.getOriginClass();    // 示例：java.lang.String

                // 忽略用户指定或系统默认的字段（如：id, version, deleted 等）
                if(ValidatorEngine.containIgnoreKeys(columnName)) {
                    logger.info("ignore column: " + columnName);
                    continue;
                }

                Integer displaySize = meta.getDisplaySize(); // 示例：username 的 displaySize = 50

                // 根据字段类型创建不同的校验规则
                if(String.class.getName().equals(clazzName)
                        || Date.class.getName().equals(clazzName)
                        || LocalDateTime.class.getName().equals(clazzName)
                        || LocalDate.class.getName().equals(clazzName)) {
                    // 字符串类型：设置 maxLength 校验
                    ValidatorEngine.makeAnyStringRule(list, tableName, columnName, displaySize);
                }
                else {
                    // 数字类型：设置 numeric 校验
                    ValidatorEngine.makeNumericRule(list, tableName, columnName, clazzName);
                }
            }
        }

        // 7. 构建目标资源路径
        // 示例：valueRuleModuleTargetPath = "/project/validator-module"
        // 结果：srcResourcesPath = "/project/validator-module/src/main/resources/validator/rules/"
        Path srcResourcesPath = Paths.get(valueRuleModulePath, "src", "main", "resources", valueRulesYmlDirectory);

        // 构建合并的 tableMap（保留旧配置，合并新配置，删除已废弃字段）
        Map<String, Object> mergedTableMap = ValidatorEngine.INSTANCE.buildMergedTableMap(oldConfig, list);

        // 按表名分文件写入 rules 目录
        String rulesDirPath = srcResourcesPath + File.separator + Const.VALUE_RULES_DIR;
        ValidatorEngine.INSTANCE.generatePerTableYmlFiles(mergedTableMap, rulesDirPath);

        // 初始化引擎，生成枚举代码
        ValidatorEngine.INSTANCE.initFromDir(dbType,
                rulesDirPath, null, regexDictYmlFilePath,
                null, customUseSnake);

        String sourceFormat = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);
        if(StrUtil.isNotBlank(sourceFormat)) {
            // 有可能没有枚举内容
            Path srcJavaPath = Paths.get(valueEnumRangeModulePath, "src", "main", "java");
            String packagePath = packageName.replaceAll("\\.", File.separator);
            Path valueEnumRangePath = Paths.get(srcJavaPath.toString(), packagePath);

            FileWriter.write(valueEnumRangePath.toString(), "ValueEnumRange", "java", sourceFormat);
        }
    }


    private static Map<String, TableMeta> buildTableMetaMap(DataSource dataSource, List<String> tables) throws SQLException {
        return ValidatorEngine.makeStringTableMetaMap(dataSource, tables);
    }

    /**
     * 保护一下输入值
     * @param valueRulesDirectory
     * @return
     */
    private static String normalizeValueRulesYmlDirectory(String valueRulesDirectory) {
        String valueRulesYmlDirectory = "";
        if(StrUtil.isNotBlank(valueRulesDirectory)) {
            if(valueRulesDirectory.endsWith(File.separator)) {
                valueRulesYmlDirectory = valueRulesDirectory;
            }
            else {
                valueRulesYmlDirectory = valueRulesDirectory + File.separator;
            }
        }
        return valueRulesYmlDirectory;
    }
}
