package com.baymax.validator.engine.generator;

import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.YamlConfigLoader;
import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.generator.kit.TableMetaKit;
import com.baymax.validator.engine.generator.meta.ColumnMeta;
import com.baymax.validator.engine.generator.meta.TableMeta;
import com.baymax.validator.engine.model.FieldRule;
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

/**
 * @author xiao.hu
 * @date 2022-03-30
 * @apiNote
 */
public class ValidatorCodeGenerator {

    /**
     * 生成字段校验相关的java类，此类用于数据校验，根据此类可以得到对应的属性，可以以json的形式反馈到前端
     * 用于前端js校验
     */
    public static void generateValidatorConfig(String dbType, DataSource dataSource, String databaseName, List<String> exceptTables,
                                               String valueRuleModuleTargetPath,
                                               String valueEnumRangeModuleTargetPath,
                                               String packageName,
                                               Set<String> userIgnoreKeys, boolean customUseSnake,
                                               String valueRulesDirectory) throws SQLException {
        String valueRulesYmlDirectory = getValueRulesYmlDirectory(valueRulesDirectory);

        String oldValueRulesYmlFilePath = valueRulesYmlDirectory + Const.VALUE_RULES_FILENAME;
        String regexDictYmlFilePath = valueRulesYmlDirectory + Const.COMMON_DICT_FILENAME;
        YamlConfigLoader configLoader = new YamlConfigLoader();
        Map<String, Map<String, Object>> oldConfig = configLoader.loadValueRulesYml(oldValueRulesYmlFilePath);

        List<String> tables = TableMetaKit.getTables(dataSource, databaseName, exceptTables);
        if(tables == null || tables.isEmpty()) {
            return;
        }

        Map<String, TableMeta> tablesWithColumnMetaMapping = buildTableMetaMap(dataSource, tables);

        List<FieldRule> list = new ArrayList<>();
        for (Map.Entry<String, TableMeta> entry : tablesWithColumnMetaMapping.entrySet()) {
            String tableName = entry.getKey();
            TableMeta tableMeta = entry.getValue();

            List<ColumnMeta> columnMetaList = tableMeta.getColumnMetaList();
            for (ColumnMeta meta : columnMetaList) {
                String columnName = meta.getName();
                String clazzName = meta.getOriginClass();

                if(ValidatorEngine.containIgnoreKeys(columnName)) {
                    continue;
                }

                Integer displaySize = meta.getDisplaySize();

                if(String.class.getName().equals(clazzName)
                        || Date.class.getName().equals(clazzName)
                        || LocalDateTime.class.getName().equals(clazzName)
                        || LocalDate.class.getName().equals(clazzName)) {
                    ValidatorEngine.makeAnyStringRule(list, tableName, columnName, displaySize);
                }
                else {
                    ValidatorEngine.makeNumericRule(list, tableName, columnName, clazzName);
                }
            }
        }

        Path srcResourcesPath = Paths.get(valueRuleModuleTargetPath, "..", "..", "src", "main", "resources", valueRulesYmlDirectory);

        // 构建合并的 tableMap（保留旧配置，合并新配置，删除已废弃字段）
        Map<String, Object> mergedTableMap = ValidatorEngine.INSTANCE.buildMergedTableMap(oldConfig, list);

        // 按表名分文件写入 rules 目录
        String rulesDirPath = srcResourcesPath + File.separator + Const.VALUE_RULES_DIR;
        ValidatorEngine.INSTANCE.generatePerTableYmlFiles(mergedTableMap, rulesDirPath);

        // 初始化引擎，生成枚举代码
        ValidatorEngine.INSTANCE.initFromDir(
                rulesDirPath, regexDictYmlFilePath, null, regexDictYmlFilePath,
                userIgnoreKeys, customUseSnake);

        String sourceFormat = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);

        Path srcJavaPath = Paths.get(valueEnumRangeModuleTargetPath, "..", "..", "src", "main", "java");
        String packagePath = packageName.replaceAll("\\.", File.separator);
        Path valueEnumRangePath = Paths.get(srcJavaPath.toString(), packagePath);

        FileWriter.write(valueEnumRangePath.toString(), "ValueEnumRange", "java", sourceFormat);
    }

    private static Map<String, TableMeta> buildTableMetaMap(DataSource dataSource, List<String> tables) throws SQLException {
        return ValidatorEngine.makeStringTableMetaMap(dataSource, tables);
    }

    /**
     * 保护一下输入值
     * @param valueRulesDirectory
     * @return
     */
    private static String getValueRulesYmlDirectory(String valueRulesDirectory) {
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
