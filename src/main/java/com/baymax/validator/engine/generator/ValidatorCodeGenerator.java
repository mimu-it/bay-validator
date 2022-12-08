package com.baymax.validator.engine.generator;

import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.generator.kit.TableMetaKit;
import com.baymax.validator.engine.generator.meta.ColumnMeta;
import com.baymax.validator.engine.generator.meta.TableMeta;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.model.sub.DecimalFieldRule;
import com.baymax.validator.engine.model.sub.NumericFieldRule;
import com.baymax.validator.engine.model.sub.StringRegexFieldRule;
import com.baymax.validator.engine.utils.FileWriter;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
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
    public static void generateValidatorConfig(DataSource dataSource, List<String> exceptTables,
                                               String targetClassesPath,
                                               String packageName, String classFileName) throws SQLException {
        List<String> tables = TableMetaKit.getTables(dataSource, exceptTables);
        if(tables == null || tables.isEmpty()) {
            return;
        }

        Map<String, TableMeta> tablesWithColumnMetaMapping = new HashMap<>();
        Connection con = dataSource.getConnection(); //连接不能在循环里面，不然有可能占用很多的链接
        for(String tableName : tables) {
            List<ColumnMeta> columnsMetaList = TableMetaKit.getColumnsMeta(con, tableName);
            if(columnsMetaList == null || columnsMetaList.isEmpty()){
                continue;
            }

            TableMeta oTableMeta = new TableMeta();
            oTableMeta.setTableName(tableName);
            oTableMeta.setColumnMetaList(columnsMetaList);

            tablesWithColumnMetaMapping.put(tableName, oTableMeta);
        }


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

                if("id".equals(columnName) || "gmt_created".equals(columnName)
                        || "creator".equals(columnName) || "gmt_modified".equals(columnName)
                        || "modifier".equals(columnName) || "is_deleted".equals(columnName)
                        || "version".equals(columnName)) {
                    continue;
                }

                if(String.class.getSimpleName().equals(clazzName)
                        || Date.class.getSimpleName().equals(clazzName)) {
                    FieldRule rule = new StringRegexFieldRule();
                    rule.setFieldKey(tableName + "." + columnName);
                    rule.setStringRegexKey("any_string");
                    rule.setStringLengthMin(1);
                    rule.setStringLengthMax(128);
                    rule.setStringCharset("utf8");
                    rule.setType(ValidatorEngine.RuleType.string.name());
                    list.add(rule);
                }
                else if(Integer.class.getSimpleName().equals(clazzName)
                        || Long.class.getSimpleName().equals(clazzName)) {
                    FieldRule fr = new NumericFieldRule();
                    fr.setFieldKey(tableName + "." + columnName);
                    fr.setType(ValidatorEngine.RuleType.numeric.name());
                    fr.setNumericMin(new BigInteger("0"));
                    fr.setNumericMax(new BigInteger("999"));
                    list.add(fr);
                }
                else if(BigDecimal.class.getSimpleName().equals(clazzName)) {
                    FieldRule fr = new DecimalFieldRule();
                    fr.setFieldKey(tableName + "." + columnName);
                    fr.setType(ValidatorEngine.RuleType.decimal.name());
                    fr.setDecimalMin(new BigDecimal("0.00"));
                    fr.setDecimalMax(new BigDecimal("999.00"));
                    list.add(fr);
                }
            }
        }

        String newValueRulesYmlStr = ValidatorEngine.INSTANCE.generateDefaultYml("value_rules.yml", list);
        System.out.println(newValueRulesYmlStr);

        String srcResourcesPath = targetClassesPath + "../../src/main/resources";

        FileWriter.backupAndWrite(srcResourcesPath, "value_rules", "yml", newValueRulesYmlStr);

        ValidatorEngine.INSTANCE.init("value_rules.yml");
        String sourceFormated = ValidatorEngine.INSTANCE.generateJavaEnumCode(packageName);

        String srcJavaPath = targetClassesPath + "../../src/main/java";
        String packagePath = File.separator + packageName.replaceAll("\\.", File.separator);
        FileWriter.write(srcJavaPath + packagePath, classFileName, "java", sourceFormated);
    }
}
