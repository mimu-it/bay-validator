package com.baymax.validator.engine;

import com.baymax.validator.engine.exception.ErrorCode;
import com.baymax.validator.engine.exception.IllegalValueException;
import com.baymax.validator.engine.generator.ValidatorCodeGenerator;
import com.baymax.validator.engine.generator.formatter.IFormatter;
import com.baymax.validator.engine.utils.StrUtil;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * @author xiao.hu
 * @date 2023-07-12
 * @apiNote
 */
public class HxValidator {
    private Object bean = null;
    private String tableName = null;
    private String[] ignoreKeys = null;
    private String[] nullableKeys = null;

    /**
     * 用于引擎初始化
     */
    public static class Engine {
        private DataBaseType dbType;
        private String valueRulesYmlFilePath;
        private String commonValueRulesYmlFilePath;
        private String ruleDictYmlFilePath;
        private Set<String> userIgnoreKeys;
        private KeyMode keyMode = KeyMode.camel;
        private IFormatter formatter = null;

        public static Engine create() {
            return new Engine();
        }

        public Engine dbType(DataBaseType dbType) {
            this.dbType = dbType;
            return this;
        }

        public Engine rules(String valueRulesYmlFilePath) {
            this.valueRulesYmlFilePath = valueRulesYmlFilePath;
            return this;
        }

        public Engine commonRules(String commonValueRulesYmlFilePath) {
            this.commonValueRulesYmlFilePath = commonValueRulesYmlFilePath;
            return this;
        }

        public Engine ruleDict(String ruleDictYmlFilePath) {
            this.ruleDictYmlFilePath = ruleDictYmlFilePath;
            return this;
        }

        public Engine ignoreKeys(Set<String> userIgnoreKeys) {
            this.userIgnoreKeys = userIgnoreKeys;
            return this;
        }

        public Engine keyMode(KeyMode keyMode) {
            this.keyMode = keyMode;
            return this;
        }

        public Engine javaFormatter(IFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        public void init() {
            if(StrUtil.isBlank(this.valueRulesYmlFilePath)) {
                throw new IllegalArgumentException("Value rules file path is blank");
            }

            ValidatorEngine.INSTANCE.init(this.dbType.name(),
                    this.valueRulesYmlFilePath,
                    this.commonValueRulesYmlFilePath,
                    this.ruleDictYmlFilePath,
                    this.userIgnoreKeys,
                    this.keyMode == KeyMode.snake);

            if(this.formatter != null){
                ValidatorEngine.INSTANCE.setFormatter(this.formatter);
            }
        }
    }


    /**
     * 用于代码中进行字段验证
     */
    public static class Builder implements IBuilder<HxValidator> {
        private HxValidator hxValidator = new HxValidator();

        @Override
        public HxValidator build() {
            return this.hxValidator;
        }

        public Builder with(Object bean) {
            hxValidator.setBean(bean);
            return this;
        }

        public Builder bind(String tableName) {
            hxValidator.setTableName(tableName);
            return this;
        }

        public Builder ignoreKeys(String[] ignoreKeys) {
            if(ignoreKeys == null) {
                return this;
            }

            hxValidator.setIgnoreKeys(ignoreKeys);
            return this;
        }

        public Builder nullableKeys(String[] nullableKeys) {
            if(nullableKeys == null) {
                return this;
            }

            hxValidator.setNullableKeys(nullableKeys);
            return this;
        }

        /**
         * 验证 Bean 的属性
         * @return
         */
        public List<String> validate() {
            return hxValidator.validate();
        }

        /**
         * 链式验证单个参数
         * @param validatorKey
         * @param paramValue
         * @return
         */
        public Builder validate(String validatorKey, Object paramValue) {
            if(StringUtils.isBlank(validatorKey)) {
                throw IllegalValueException.builder().errorCode(ErrorCode.illegal_argument.name())
                        .params(validatorKey, paramValue).build();
            }

            boolean result = hxValidator.validate(validatorKey, paramValue);
            if(!result) {
                throw IllegalValueException.builder().errorCode(ErrorCode.validate_failure.name())
                        .params(validatorKey, paramValue).build();
            }

            return this;
        }

        /**
         * 链式验证单个参数， 但是允许为null
         * @param validatorKey
         * @param paramValue
         * @return
         */
        public Builder validateIfNonnull(String validatorKey, Object paramValue) {
            if(StringUtils.isBlank(validatorKey)) {
                throw IllegalValueException.builder().errorCode(ErrorCode.illegal_argument.name())
                        .params(validatorKey, paramValue).build();
            }

            if(paramValue == null) {
                return this;
            }

            boolean result = hxValidator.validate(validatorKey, paramValue);
            if(!result) {
                throw IllegalValueException.builder().errorCode(ErrorCode.validate_failure.name())
                        .params(validatorKey, paramValue).build();
            }

            return this;
        }
    }

    /**
     * 用于生成代码
     */
    public static class Generator {
        private String dbType;
        private DataSource dataSource;
        private String databaseName;
        private List<String> exceptTables;
        private String valueRuleModuleTargetPath;
        private String valueEnumRangeModuleTargetPath;
        private String packageName;
        private Set<String> userIgnoreKeys;
        private Boolean customUseSnake;
        private String valueRulesDirectory;

        public static Generator create() {
            return new Generator();
        }

        public Generator bindToDatabase(String dbType, DataSource dataSource, String databaseName) {
            this.bindToDatabase(dbType, dataSource, databaseName, null);
            return this;
        }

        public Generator bindToDatabase(String dbType, DataSource dataSource, String databaseName, List<String> exceptTables) {
            this.dbType = dbType;
            this.dataSource = dataSource;
            this.databaseName = databaseName;
            this.exceptTables = exceptTables;
            return this;
        }

        public Generator valueRuleModuleTargetPath(String valueRuleModuleTargetPath, String valueRulesDirectory) {
            this.valueRuleModuleTargetPath = valueRuleModuleTargetPath;
            this.valueRulesDirectory = valueRulesDirectory;
            return this;
        }

        public Generator valueEnumRangeModuleTargetPath(String valueEnumRangeModuleTargetPath, String packageName) {
            this.valueEnumRangeModuleTargetPath = valueEnumRangeModuleTargetPath;
            this.packageName = packageName;
            return this;
        }

        /**
         * Set<String> userIgnoreKeys, boolean customUseSnake  两个是相关的
         * customUseSnake 决定了 userIgnoreKeys 中的值 是不是下划线模式， 或者是驼峰模式
         * @param userIgnoreKeys
         * @param customUseSnake
         * @return
         */
        public Generator userIgnoreKeys(Set<String> userIgnoreKeys, Boolean customUseSnake) {
            this.userIgnoreKeys = userIgnoreKeys;
            this.customUseSnake = customUseSnake;
            return this;
        }

        public void generate() throws SQLException {
            ValidatorCodeGenerator.generateValidatorConfig(dbType,
                    dataSource, databaseName, exceptTables,
                    valueRuleModuleTargetPath,
                    valueEnumRangeModuleTargetPath,
                    packageName, userIgnoreKeys,
                    customUseSnake, valueRulesDirectory);
        }
    }



    public static HxValidator.Builder builder() {
        return new HxValidator.Builder();
    }

    public List<String> validate() {
        return ValidatorEngine.INSTANCE.validate(bean, tableName, ignoreKeys, nullableKeys);
    }

    public boolean validate(String validatorKey, Object paramValue) {
        return ValidatorEngine.INSTANCE.validate(validatorKey, paramValue);
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setIgnoreKeys(String[] ignoreKeys) {
        this.ignoreKeys = ignoreKeys;
    }

    public void setNullableKeys(String[] nullableKeys) {
        this.nullableKeys = nullableKeys;
    }
}
