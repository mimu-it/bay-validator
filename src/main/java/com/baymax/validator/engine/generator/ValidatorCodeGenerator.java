package com.baymax.validator.engine.generator;

import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.generator.kit.TableMetaKit;
import com.baymax.validator.engine.generator.meta.ColumnMeta;
import com.baymax.validator.engine.generator.meta.TableMeta;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.utils.FileWriter;

import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;
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
                                               String targetClassesPath,
                                               String packageName, String classFileName) throws SQLException {
        List<String> tables = TableMetaKit.getTables(dataSource, databaseName, exceptTables);
        if(tables == null || tables.isEmpty()) {
            return;
        }

        Map<String, TableMeta> tablesWithColumnMetaMapping = ValidatorEngine.makeStringTableMetaMap(dataSource, tables);

        List<FieldRule> list = new ArrayList<>();
        Iterator<Map.Entry<String, TableMeta>> it = tablesWithColumnMetaMapping.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, TableMeta> entry = it.next();
            String tableName = entry.getKey();
            TableMeta tableMeta = entry.getValue();

            /**
             * columnName: is_deleted, clazzName:String
             * columnName: parent_id, clazzName:Long
             * columnName: name, clazzName:String
             * columnName: password, clazzName:String
             * columnName: name_cn, clazzName:String
             * columnName: user_number, clazzName:String
             * columnName: is_admin, clazzName:String
             * columnName: ticket, clazzName:String
             * columnName: version, clazzName:Integer
             */
            List<ColumnMeta> columnMetaList =  tableMeta.getColumnMetaList();
            for(ColumnMeta meta : columnMetaList) {
                String columnName = meta.getName();
                String clazzName = meta.getAbbreviationClass();

                if(ValidatorEngine.containIgnoreKeys(columnName)) {
                    continue;
                }

                Integer displaySize = meta.getDisplaySize();

                if(String.class.getSimpleName().equals(clazzName)
                        || Date.class.getSimpleName().equals(clazzName)) {
                    ValidatorEngine.makeAnyStringRule(list, tableName, columnName, displaySize);
                }
                else {
                    ValidatorEngine.makeNumericRule(list, tableName, columnName, clazzName, displaySize);
                }
            }
        }

        String newValueRulesYmlStr = ValidatorEngine.INSTANCE.generateDefaultYml("value_rules.yml", list);
        System.out.println(newValueRulesYmlStr);

        String srcResourcesPath = targetClassesPath + "../../src/main/resources";

        FileWriter.backupAndWrite(srcResourcesPath, "value_rules", "yml", newValueRulesYmlStr);

        ValidatorEngine.INSTANCE.init0(dbType, "value_rules.yml");
        String sourceFormat = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);

        String srcJavaPath = targetClassesPath + "../../src/main/java";
        String packagePath = File.separator + packageName.replaceAll("\\.", File.separator);
        FileWriter.write(srcJavaPath + packagePath, classFileName, "java", sourceFormat);
    }
}
