package com.baymax.validator.engine.generator;

import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.generator.formatter.IFormatter;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.preset.RuleType;
import com.baymax.validator.engine.utils.NameUtil;
import com.baymax.validator.engine.common.Common;
import com.jfinal.kit.Kv;
import com.jfinal.template.Engine;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 根据 YAML 配置生成 Java 枚举代码
 */
public class EnumCodeGenerator {

    private static final Logger LOG = Logger.getLogger(EnumCodeGenerator.class.getName());

    private final ValidatorEngine validatorEngine;
    private IFormatter formatter;

    public EnumCodeGenerator(ValidatorEngine validatorEngine) {
        this.validatorEngine = validatorEngine;
    }

    public void setFormatter(IFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * 根据 valueRulesMap 中的枚举配置，生成 Java 代码
     */
    public String generateJavaEnumCode(String packageName) {
        Map<String, Map<String, Object>> valueRulesMap = validatorEngine.getValueRulesMap();
        if (valueRulesMap == null) {
            throw new IllegalStateException("valueRulesMap is null, please init first");
        }

        Engine.setFastMode(true);
        Engine engine = Engine.use();
        engine.setDevMode(true);
        engine.setToClassPathSourceFactory();

        Iterator<Map.Entry<String, Map<String, Object>>> itTable = valueRulesMap.entrySet().iterator();

        List<String> tableTemplateList = new ArrayList<>();
        Set<String> importList = new HashSet<>();

        while (itTable.hasNext()) {
            Map.Entry<String, Map<String, Object>> tableEntry = itTable.next();
            String tableName = tableEntry.getKey();
            Map<String, Object> tableRuleMap = tableEntry.getValue();
            if (tableRuleMap == null) {
                throw new IllegalStateException(String.format("yaml's format is illegal, table name is %s", tableName));
            }

            List<String> fieldRuleTemplateList = new ArrayList<>();
            for (Map.Entry<String, Object> fieldEntry : tableRuleMap.entrySet()) {
                String fieldName = fieldEntry.getKey();
                Object fieldRuleMapObj = fieldEntry.getValue();
                if (!(fieldRuleMapObj instanceof Map)) {
                    throw new IllegalStateException(String.format(
                            "yaml's format is illegal, field name is %s.%s", tableName, fieldName));
                }

                Map<String, Object> fieldRuleMap = (Map<String, Object>) fieldRuleMapObj;
                String type = (String) fieldRuleMap.get(RuleKey.type.name());
                if (!RuleType.isEnum(type)) {
                    continue;
                }

                List<Object> enumValues = Common.getEnumValues(fieldRuleMap);
                Map<Object, String> enumDict = Common.getEnumDict(fieldRuleMap);

                JavaEnum je = JavaEnumTemplateRender.build(fieldName, type, enumValues, enumDict);
                importList.add("import " + je.getCanonicalJavaType());

                Kv cond = Kv.by(Const.TemplateKey.fieldName.name(), fieldName)
                        .set(Const.TemplateKey.enumValues.name(), je.getEnumValues())
                        .set(Const.TemplateKey.javaType.name(), je.getJavaType());
                String template = engine.getTemplate(Const.ENUM_FIELD_RULE_FILENAME).renderToString(cond);
                fieldRuleTemplateList.add(template);
            }

            Kv cond = Kv.by(Const.TemplateKey.tableName.name(), tableName)
                    .set(Const.TemplateKey.fieldRuleList.name(), fieldRuleTemplateList);
            String tableTemplate = engine.getTemplate(Const.TABLE_FILENAME).renderToString(cond);
            tableTemplateList.add(tableTemplate);
        }

        String importStr = StringUtils.join(importList, ";") + ";";
        Kv cond = Kv.by(Const.TemplateKey.tableList.name(), tableTemplateList)
                .set("package", packageName)
                .set("import", importStr)
                .set(Const.TemplateKey.generateTime.name(),
                        DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        String valueRangeTemplate = engine.getTemplate(Const.VALUE_ENUM_RANGE_FILENAME).renderToString(cond);

        return formatJava(valueRangeTemplate);
    }

    /**
     * 格式化 Java 代码
     */
    private String formatJava(String source) {
        if (formatter == null) {
            return source;
        }
        try {
            return formatter.formatJava(source);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to format generated Java code", e);
            return source;
        }
    }

    /**
     * 将生成代码写入文件
     */
    public void writeToFile(String fileName, String packageName, String content, boolean toSrcTest) {
        String targetClassesPath = validatorEngine.getClass().getProtectionDomain()
                .getCodeSource().getLocation().getPath();
        String srcPath = toSrcTest
                ? targetClassesPath + "../../src/test/java"
                : targetClassesPath + "../../src/main/java";

        String packagePath = java.io.File.separator + packageName.replaceAll("\\.", "\\" + java.io.File.separator);

        if (toSrcTest) {
            com.baymax.validator.engine.utils.FileWriter.write(
                    srcPath + packagePath, fileName, Const.FileType.java.name(), content);
        } else {
            com.baymax.validator.engine.utils.FileWriter.backupAndWrite(
                    srcPath + packagePath, fileName, Const.FileType.java.name(), content);
        }
    }
}
