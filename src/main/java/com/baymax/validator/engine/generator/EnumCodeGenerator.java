package com.baymax.validator.engine.generator;

import com.baymax.validator.engine.ValidatorEngine;
import com.baymax.validator.engine.common.Common;
import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.generator.formatter.IFormatter;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.preset.RuleType;
import com.baymax.validator.engine.utils.FileWriter;
import com.jfinal.kit.Kv;
import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 根据 YAML 配置生成 Java 枚举代码
 *
 * <p>功能说明：
 * 从 YAML 配置文件中读取枚举类型的字段定义，自动生成对应的 Java 枚举类代码。
 *
 * <p>使用场景示例：
 * 假设有如下 YAML 配置：
 * <pre>
 * user:
 *   status:
 *     type: enum
 *     values: [ACTIVE, INACTIVE, DELETED]
 *     dict:
 *       ACTIVE: "活跃"
 *       INACTIVE: "未激活"
 *       DELETED: "已删除"
 *   role:
 *     type: enum
 *     values: [ADMIN, USER, GUEST]
 *     dict:
 *       ADMIN: "管理员"
 *       USER: "普通用户"
 *       GUEST: "访客"
 * </pre>
 *
 * 将生成如下 Java 代码：
 * <pre>
 * package com.example.enums;
 *
 * public enum UserStatus {
 *     STRING_ACTIVE("active"),
 *     STRING_INACTIVE("inactive"),
 *     STRING_DELETED("deleted");
 *
 *     private String description;
 *     // ... 构造方法、getter等
 * }
 * </pre>
 *
 * @author Baymax Team
 * @version 1.0
 */
public class EnumCodeGenerator {

    // ============ 常量定义 ============

    /**
     * 日志记录器 - 使用 java.util.logging 而非其他日志框架
     * 原因：避免引入第三方依赖，保持轻量级
     */
    private static final Logger logger = Logger.getLogger(EnumCodeGenerator.class.getName());

    /**
     * 日期格式化模板
     * 原因：统一时间格式，便于代码追溯和版本管理
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 模板引擎实例名称
     * 原因：使用独立的引擎实例名称，避免与系统中其他 Engine 实例冲突
     */
    private static final String TEMPLATE_ENGINE_NAME = "enum-generator";

    // ============ 实例变量 ============

    /**
     * 验证引擎实例 - 持有配置数据
     * 原因：EnumCodeGenerator 需要从 ValidatorEngine 获取 YAML 配置数据
     */
    private final ValidatorEngine validatorEngine;

    /**
     * 独立的模板引擎实例 - 线程安全
     * 原因：原代码使用全局单例 Engine.use() 在多线程环境下会导致配置互相覆盖
     * 例如：线程A设置 devMode=true，线程B设置 devMode=false，会造成不确定的行为
     * 使用 Engine.create() 创建独立实例，每个实例有自己的配置，互不干扰
     */
    private final Engine engine;

    /**
     * 模板缓存 - 避免重复加载
     * 原因：原代码每次循环都从文件系统加载模板，性能低下
     * 使用 ConcurrentHashMap 实现缓存，线程安全且高性能
     * 例如：如果有 100 个表，原代码需要加载 300 次模板（每个表3个模板）
     * 使用缓存后只加载 3 次，性能提升 100 倍
     */
    private final Map<String, Template> templateCache;

    /**
     * 代码格式化器 - 可选
     * 原因：生成的代码可能格式不美观，通过格式化器进行美化
     * 例如：Google Java Format、Eclipse Formatter 等
     */
    private IFormatter formatter;

    // ============ 构造方法 ============

    /**
     * 构造 EnumCodeGenerator 实例
     *
     * @param validatorEngine 验证引擎实例，包含 YAML 配置数据
     * @throws IllegalArgumentException 如果 validatorEngine 为 null
     *
     * <p>示例：
     * <pre>
     * ValidatorEngine engine = new ValidatorEngine();
     * engine.init("config/validator.yaml");
     * EnumCodeGenerator generator = new EnumCodeGenerator(engine);
     * </pre>
     */
    public EnumCodeGenerator(ValidatorEngine validatorEngine) {
        // 参数校验 - 防御性编程
        if (validatorEngine == null) {
            throw new IllegalArgumentException("validatorEngine cannot be null");
        }

        this.validatorEngine = validatorEngine;

        // 创建独立的模板引擎实例
        // 使用独立的引擎名称，避免与全局引擎冲突
        this.engine = Engine.create(TEMPLATE_ENGINE_NAME);
        // 设置为从 classpath 加载模板文件
        // 原因：模板文件通常放在 resources 目录下，需要从 classpath 加载
        this.engine.setToClassPathSourceFactory();

        // 初始化模板缓存 - 使用 ConcurrentHashMap 保证线程安全
        // 原因：多线程环境下，多个线程可能同时访问模板缓存
        this.templateCache = new ConcurrentHashMap<>();

        logger.info("EnumCodeGenerator initialized successfully");
    }

    /**
     * 设置代码格式化器
     *
     * @param formatter 格式化器实例
     *
     * <p>示例：
     * <pre>
     * // 使用 Google Java Format
     * generator.setFormatter(new GoogleJavaFormatter());
     * </pre>
     */
    public void setFormatter(IFormatter formatter) {
        this.formatter = formatter;
        logger.info("Formatter set: " + formatter.getClass().getSimpleName());
    }

    // ============ 公共方法 ============

    /**
     * 根据 valueRulesMap 中的枚举配置，生成 Java 代码
     *
     * @param packageName 生成的枚举类所在的包名
     * @return 生成的 Java 代码字符串
     * @throws IllegalArgumentException 如果 packageName 为空
     * @throws RuntimeException 如果生成过程中发生异常
     *
     * <p>完整示例：
     * <pre>
     * // 假设 YAML 配置了 user 和 order 两个表的枚举字段
     * String code = generator.generateJavaEnumCode("com.example.enums");
     *
     * // 生成的代码大致如下：
     * package com.example.enums;
     *
     * import java.util.Map;
     * import java.util.HashMap;
     *
     * public enum UserStatus {
     *     STRING_ACTIVE("active"),
     *     STRING_INACTIVE("inactive"),
     *     STRING_DELETED("deleted");
     *     // ...
     * }
     *
     * </pre>
     */
    public String generateJavaEnumCode(String packageName) {
        // ============ 1. 参数校验 ============
        // 原因：方法入参校验是防御性编程的基本要求
        // 防止空指针异常和无效数据导致后续处理失败
        if (StringUtils.isBlank(packageName)) {
            throw new IllegalArgumentException("packageName cannot be null or empty");
        }

        // 获取配置数据
        Map<String, Map<String, Object>> valueRulesMap = validatorEngine.getValueRulesMap();

        // 数据校验：如果配置为空，直接返回空字符串而不是抛异常
        // 原因：配置为空可能是正常情况（没有枚举需要生成），不应视为错误
        if (valueRulesMap == null || valueRulesMap.isEmpty()) {
            logger.warning("valueRulesMap is null or empty, no enum code to generate");
            return StringUtils.EMPTY;
        }

        logger.info("Starting enum code generation for package: " + packageName);
        logger.info("Processing " + valueRulesMap.size() + " tables");

        try {
            // ============ 2. 预加载模板 ============
            // 原因：提前加载所有需要的模板，避免在循环中重复加载
            // 性能优化：如果有 N 个表，每个表需要 3 个模板，原代码加载 3N 次
            // 优化后只加载 3 次，性能提升显著
            Template fieldRuleTemplate = getTemplate(Const.ENUM_FIELD_RULE_FILENAME);
            Template tableTemplate = getTemplate(Const.TABLE_FILENAME);
            Template valueEnumRangeTemplate = getTemplate(Const.VALUE_ENUM_RANGE_FILENAME);

            logger.fine("Templates loaded successfully");

            // ============ 3. 处理表格数据 ============
            // 使用 List 而非直接在循环中拼接字符串
            // 原因：StringBuilder 在大量拼接时性能更好，但使用 List 配合最后的 join 更清晰
            List<String> tableTemplateList = new ArrayList<>();
            // 使用 Set 存储导入语句，自动去重
            // 原因：不同的枚举可能需要导入相同的类，Set 自动去重避免重复导入
            Set<String> importList = new HashSet<>();

            // 遍历每个表
            // 使用增强 for 循环替代迭代器
            // 原因：代码更简洁，可读性更好，性能无差异
            for (Map.Entry<String, Map<String, Object>> tableEntry : valueRulesMap.entrySet()) {
                String tableName = tableEntry.getKey();
                /**
                 * tableRuleMap的值是：
                 * {
                 *    float_card: {
                 *        type: enum_decimal,
                 *        enum_values: ["3.11", "4000000..."]
                 *    }
                 * }
                 */
                Map<String, Object> tableRuleMap = tableEntry.getValue();

                // 数据校验
                if (tableRuleMap == null) {
                    // 记录错误但继续处理其他表
                    // 原因：一个表的数据异常不应影响其他表的生成
                    logger.warning(String.format("Table %s has null rules, skipping", tableName));
                    continue;
                }

                // 处理当前表的所有字段
                List<String> fieldRuleTemplateList = processFieldRules(
                        tableRuleMap, tableName, fieldRuleTemplate, importList);

                // 只有包含枚举字段的表才生成代码
                if (!fieldRuleTemplateList.isEmpty()) {
                    // 构建表级别的模板数据
                    Kv cond = Kv.by(Const.TemplateKey.tableName.name(), tableName)
                            .set(Const.TemplateKey.fieldRuleList.name(), fieldRuleTemplateList);

                    // 渲染表模板
                    String tableTemplateResult = tableTemplate.renderToString(cond);
                    tableTemplateList.add(tableTemplateResult);

                    logger.fine(String.format("Generated code for table: %s with %d enum fields",
                            tableName, fieldRuleTemplateList.size()));
                }
            }

            // 如果没有任何枚举字段，返回空
            if (tableTemplateList.isEmpty()) {
                logger.warning("No enum fields found in configuration");
                return StringUtils.EMPTY;
            }

            // ============ 4. 渲染最终代码 ============
            String result = renderFinalCode(packageName, tableTemplateList, importList, valueEnumRangeTemplate);

            // ============ 5. 格式化 ============
            // 原因：生成的代码可能格式不统一，通过格式化器美化
            String formattedResult = formatJava(result);

            logger.info("Enum code generation completed successfully");
            logger.info("Generated " + tableTemplateList.size() + " enum classes");

            return formattedResult;

        } catch (Exception e) {
            // 捕获所有异常，记录日志并转换为运行时异常
            // 原因：避免调用方处理受检异常，简化调用代码
            logger.log(Level.SEVERE, "Failed to generate enum code", e);
            throw new RuntimeException("Failed to generate enum code: " + e.getMessage(), e);
        }
    }

    /**
     * 处理字段规则 - 提取枚举类型字段并生成模板
     *
     * @param tableRuleMap 表的规则映射
     * @param tableName 表名
     * @param fieldRuleTemplate 字段模板
     * @param importList 导入列表（会被修改）
     * @return 字段模板列表
     *
     * <p>处理流程：
     * 1. 遍历表的所有字段
     * 2. 检查字段是否为枚举类型
     * 3. 提取枚举值和描述
     * 4. 构建 JavaEnum 对象
     * 5. 渲染字段模板
     *
     * <p>示例：
     * 输入：{status={type=enum, values=[ACTIVE, INACTIVE], dict={ACTIVE="活跃", INACTIVE="未激活"}}}
     * 输出：["public enum UserStatus { ACTIVE(\"活跃\"), INACTIVE(\"未激活\"); ... }"]
     */
    private List<String> processFieldRules(Map<String, Object> tableRuleMap, String tableName,
                                           Template fieldRuleTemplate, Set<String> importList) {
        List<String> fieldRuleTemplateList = new ArrayList<>();

        // 遍历表的所有字段
        for (Map.Entry<String, Object> fieldEntry : tableRuleMap.entrySet()) {
            String fieldName = fieldEntry.getKey();
            Object fieldRuleMapObj = fieldEntry.getValue();

            // ============ 类型检查 ============
            // 原因：YAML 解析后可能是各种类型，需要进行类型检查
            // 防止 ClassCastException
            if (!(fieldRuleMapObj instanceof Map)) {
                // 记录警告但不中断处理
                // 原因：可能是配置错误，但不影响其他字段
                logger.warning(String.format("Field %s.%s is not a map (actual type: %s), skipping",
                        tableName, fieldName, fieldRuleMapObj.getClass().getSimpleName()));
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> fieldRuleMap = (Map<String, Object>) fieldRuleMapObj;

            // ============ 获取字段类型 ============
            // 原因：只有枚举类型才需要处理
            String type = (String) fieldRuleMap.get(RuleKey.type.name());
            if (!RuleType.isEnum(type)) {
                // 非枚举类型跳过
                continue;
            }

            try {
                // ============ 构建枚举数据 ============
                // 提取枚举值列表
                List<Object> enumValues = Common.getEnumValues(fieldRuleMap);
                // 提取枚举描述字典
                Map<Object, String> enumDict = Common.getEnumDict(fieldRuleMap);

                // ============ 数据校验 ============
                // 原因：空的枚举列表没有意义，跳过处理
                if (enumValues == null || enumValues.isEmpty()) {
                    logger.warning(String.format("Field %s.%s has empty enum values, skipping",
                            tableName, fieldName));
                    continue;
                }

                // ============ 构建 Java 枚举对象 ============
                // 将配置数据转换为 Java 枚举表示
                JavaEnum je = JavaEnumTemplateRender.build(engine, fieldName, type, enumValues, enumDict);

                // ============ 收集导入语句 ============
                // 原因：生成的枚举可能需要导入其他类（如父类、接口等）
                String canonicalJavaType = je.getCanonicalJavaType();
                if (StringUtils.isNotBlank(canonicalJavaType)) {
                    importList.add("import " + canonicalJavaType);
                }

                // ============ 渲染字段模板 ============
                // 构建模板数据
                Kv cond = Kv.by(Const.TemplateKey.fieldName.name(), fieldName)
                        .set(Const.TemplateKey.enumValues.name(), je.getEnumValues())
                        .set(Const.TemplateKey.javaType.name(), je.getJavaType());

                // 渲染模板
                String template = fieldRuleTemplate.renderToString(cond);
                fieldRuleTemplateList.add(template);

                logger.fine(String.format("Processed enum field: %s.%s with %d values",
                        tableName, fieldName, enumValues.size()));

            } catch (Exception e) {
                // 单个字段处理失败不影响其他字段
                // 原因：错误隔离，提高健壮性
                logger.log(Level.WARNING, String.format("Failed to process field %s.%s",
                        tableName, fieldName), e);
            }
        }

        return fieldRuleTemplateList;
    }

    /**
     * 渲染最终代码 - 组合所有部分生成完整的 Java 文件
     *
     * @param packageName 包名
     * @param tableTemplateList 表模板列表
     * @param importList 导入列表
     * @param valueEnumRangeTemplate 枚举范围模板
     * @return 完整的 Java 代码
     *
     * <p>渲染过程：
     * 1. 构建导入语句
     * 2. 组装模板数据
     * 3. 渲染最终模板
     *
     * <p>示例输出：
     * <pre>
     * package com.example.enums;
     *
     * import java.util.Map;
     * import java.util.HashMap;
     *
     * /**
     *  * 自动生成的枚举类
     *  * 生成时间：2026-07-11 10:30:00
     *  *&#47;
     *
     * public enum UserStatus {
     *     STRING_ACTIVE("active"),
     *     STRING_INACTIVE("inactive"),
     *     STRING_DELETED("deleted");
     *     // ...
     * }
     *
     * </pre>
     */
    private String renderFinalCode(String packageName, List<String> tableTemplateList,
                                   Set<String> importList, Template valueEnumRangeTemplate) {
        // ============ 构建导入语句 ============
        // 原因：importList 是 Set，需要转换为排序的列表
        // 排序让导入语句更规范，便于代码审查
        String importStr = buildImportStatement(importList);

        // ============ 构建模板数据 ============
        Kv cond = Kv.by(Const.TemplateKey.tableList.name(), tableTemplateList)
                .set("package", packageName)
                .set("import", importStr)
                .set(Const.TemplateKey.generateTime.name(),
                        DateFormatUtils.format(new Date(), DATE_PATTERN));

        // ============ 渲染最终模板 ============
        // 原因：最终模板包含 package、import 和所有枚举类的组合
        String result = valueEnumRangeTemplate.renderToString(cond);

        logger.fine("Final code rendering completed, total length: " + result.length() + " characters");

        return result;
    }

    /**
     * 构建导入语句 - 将 Set 转换为分号分隔的字符串
     *
     * @param importList 导入列表（Set）
     * @return 分号分隔的导入语句
     *
     * <p>处理逻辑：
     * 1. 空列表返回空字符串
     * 2. 对导入进行排序（保证确定性）
     * 3. 使用分号连接
     *
     * <p>示例：
     * 输入：["java.util.Map", "java.util.HashMap", "java.util.List"]
     * 输出："import java.util.HashMap;import java.util.List;import java.util.Map;"
     *
     * 注意：排序后输出是确定的，便于版本控制
     */
    private String buildImportStatement(Set<String> importList) {
        if (importList == null || importList.isEmpty()) {
            return StringUtils.EMPTY;
        }

        // ============ 去重并排序 ============
        // 原因：Set 已经去重，但顺序不确定
        // 排序让生成的代码具有确定性，便于版本控制
        List<String> sortedImports = new ArrayList<>(importList);
        Collections.sort(sortedImports);

        // ============ 构建导入字符串 ============
        // 使用分号连接，最后加一个分号
        // 原因：模板中可能直接使用 ${import}，需要以分号结尾
        String result = StringUtils.join(sortedImports, ";") + ";";

        logger.fine("Built import statement with " + sortedImports.size() + " imports");

        return result;
    }

    /**
     * 获取模板 - 从缓存获取，如果不存在则加载
     *
     * @param templateName 模板名称
     * @return Template 实例
     * @throws RuntimeException 如果模板加载失败
     *
     * <p>缓存策略：
     * 1. 首次调用时从 classpath 加载模板
     * 2. 后续调用直接从缓存返回
     * 3. 使用 computeIfAbsent 保证原子性
     *
     * <p>性能对比：
     * 假设有 50 个表，每个表需要 3 个模板：
     * - 无缓存：150 次文件 IO
     * - 有缓存：3 次文件 IO
     * 性能提升：50 倍
     */
    private Template getTemplate(String templateName) {
        // ============ 使用 computeIfAbsent 实现缓存 ============
        // 原因：computeIfAbsent 是原子操作，保证只有一个线程加载模板
        // 避免重复加载
        return templateCache.computeIfAbsent(templateName, name -> {
            try {
                // 从 classpath 加载模板
                Template template = engine.getTemplate(name);
                logger.info("Loaded template: " + name);
                return template;
            } catch (Exception e) {
                // 加载失败时抛出运行时异常
                // 原因：模板加载失败是无法恢复的，应该立即终止
                logger.log(Level.SEVERE, "Failed to load template: " + name, e);
                throw new RuntimeException("Failed to load template: " + name, e);
            }
        });
    }

    /**
     * 格式化 Java 代码 - 使用配置的格式化器美化代码
     *
     * @param source 原始 Java 代码
     * @return 格式化后的 Java 代码
     *
     * <p>格式化示例：
     * 输入：
     * <pre>
     * public enum Status{
     *      STRING_ACTIVE("active"),STRING_INACTIVE("inactive");private String desc;public String getDesc(){return desc;}}
     * </pre>
     * 输出：
     * <pre>
     * public enum Status {
     *     STRING_ACTIVE("active"),
     *     STRING_INACTIVE("inactive");
     *
     *     private String desc;
     *
     *     public String getDesc() {
     *         return desc;
     *     }
     * }
     * </pre>
     */
    private String formatJava(String source) {
        // 如果没有配置格式化器，直接返回原代码
        if (formatter == null) {
            return source;
        }

        try {
            // 调用格式化器格式化代码
            String formatted = formatter.formatJava(source);
            logger.fine("Code formatted successfully");
            return formatted;
        } catch (Exception e) {
            // 格式化失败时使用原始代码
            // 原因：格式化失败不应该影响代码生成功能
            logger.log(Level.WARNING, "Failed to format generated Java code, using original", e);
            return source;
        }
    }

    /**
     * 将生成代码写入文件
     *
     * @param fileName 文件名（不含扩展名）
     * @param packageName 包名
     * @param content 文件内容
     * @param toSrcTest true 写入测试目录，false 写入主代码目录
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果写入失败
     *
     * <p>路径示例：
     * 假设项目路径为：/home/user/project
     *
     * toSrcTest = true:
     * /home/user/project/target/classes/../../src/test/java/com/example/enums/UserStatusEnum.java
     * 实际路径：/home/user/project/src/test/java/com/example/enums/UserStatusEnum.java
     *
     * toSrcTest = false:
     * /home/user/project/target/classes/../../src/main/java/com/example/enums/UserStatusEnum.java
     * 实际路径：/home/user/project/src/main/java/com/example/enums/UserStatusEnum.java
     *
     * <p>备份策略：
     * - 测试代码：直接覆盖（测试代码通常不需要备份）
     * - 主代码：先备份再写入（防止误操作丢失代码）
     */
    public void writeToFile(String fileName, String packageName, String content, boolean toSrcTest) {
        // ============ 参数校验 ============
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("fileName cannot be null or empty");
        }
        if (StringUtils.isBlank(packageName)) {
            throw new IllegalArgumentException("packageName cannot be null or empty");
        }
        if (StringUtils.isBlank(content)) {
            // 空内容时记录警告但不抛出异常
            // 原因：可能是正常情况（没有枚举需要生成）
            logger.warning("Content is empty, skipping file write");
            return;
        }

        try {
            // ============ 获取基础路径 ============
            String basePath = getBasePath(toSrcTest);

            // ============ 构建包路径 ============
            String packagePath = buildPackagePath(packageName);

            // ============ 构建完整路径 ============
            String fullPath = basePath + packagePath;

            // ============ 确保目录存在 ============
            // 原因：FileWriter 不会自动创建目录，需要手动创建
            File directory = new File(fullPath);
            if (!directory.exists()) {
                // 创建目录，包括所有父目录
                // 原因：包名可能有多层目录，需要递归创建
                boolean created = directory.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create directory: " + fullPath);
                }
                logger.info("Created directory: " + fullPath);
            }

            // ============ 写入文件 ============
            String fullFileName = fileName + "." + Const.FileType.java.name();
            if (toSrcTest) {
                // 测试代码直接覆盖
                // 原因：测试代码通常需要频繁重新生成
                FileWriter.write(
                        fullPath, fileName, Const.FileType.java.name(), content);
                logger.info(String.format("Written test code to: %s%s", fullPath, fullFileName));
            } else {
                // 主代码先备份再写入
                // 原因：防止误操作导致代码丢失
                // 备份文件会添加 .bak 后缀
                FileWriter.backupAndWrite(
                        fullPath, fileName, Const.FileType.java.name(), content);
                logger.info(String.format("Written main code to: %s%s (with backup)", fullPath, fullFileName));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to write file: " + fileName, e);
            throw new RuntimeException("Failed to write file: " + fileName, e);
        }
    }

    /**
     * 获取基础路径 - 根据运行环境和目标目录
     *
     * @param toSrcTest true 返回测试目录，false 返回主代码目录
     * @return 基础路径
     *
     * <p>路径解析过程：
     * 1. 获取当前类所在的 JAR 或 classes 目录
     * 2. 根据 toSrcTest 决定目标目录
     * 3. 使用 Path 规范化路径
     * 4. 如果失败，使用当前用户目录作为降级方案
     *
     * <p>路径示例：
     * 开发环境（IDE）：
     * - target/classes/ -> src/main/java/
     * - target/test-classes/ -> src/test/java/
     *
     * JAR 包运行：
     * - /app/myapp.jar -> ./src/main/java/（相对路径）
     *
     * 降级方案：
     * - 使用 user.dir 当前工作目录
     */
    private String getBasePath(boolean toSrcTest) {
        try {
            // ============ 获取类路径 ============
            // 获取当前类所在的 JAR 或 classes 目录
            // 例如：file:/home/user/project/target/classes/
            //
            // 获取 validatorEngine 对象所在 Jar 包或 class 文件在文件系统中的绝对路径
            // 1.获取 validatorEngine 对象的运行时类（Class<?> 对象）。
            String targetClassesPath = validatorEngine.getClass()
                    // 2.返回该类的保护域（ProtectionDomain）
                    // 保护域包含该类在 JVM 中的安全权限信息，以及代码来源（CodeSource）
                    .getProtectionDomain()
                    // 3.从保护域中提取代码来源（CodeSource）
                    // CodeSource 记录了该类的字节码是从哪里加载的（URL 位置）以及相关的签名证书
                    .getCodeSource()
                    // 4.从 CodeSource 中获取位置 URL（URL 对象）
                    //   这个 URL 指向该类所在的根路径：
                    //   如果类来自 Jar 包，返回 jar:file:/path/to/xxx.jar!/ 或 file:/path/to/xxx.jar
                    //   如果类来自 class 文件（未打包），返回 file:/path/to/classes/（包路径的根目录）
                    //   如果类来自 JDK 核心类，可能返回 null
                    .getLocation()
                    // 将 URL 转换为 URI
                    // 这一步是为了安全地处理路径中的空格和特殊字符（URL 编码转义），避免直接拿 URL.getPath() 时出现乱码问题。
                    .toURI()
                    // 从 URI 中提取路径字符串（String），去除协议（file:、jar: 等），只保留文件系统路径
                    // file:/home/app/lib/validator.jar → /home/app/lib/validator.jar
                    // file:/home/app/classes/ → /home/app/classes/
                    .getPath();

            // ============ 使用 Path 处理路径 ============
            // 原因：Path 提供跨平台的路径操作
            // normalize() 会解析 .. 和 . 等相对路径
            Path targetPath = Paths.get(targetClassesPath);

            // resolve 是 Java NIO 中 Path 接口的一个核心方法，它的含义是：解析并拼接路径。
            // 简单来说：resolve 就是把当前路径当作"父目录"，把传入的路径当作"子路径"，拼合成一个完整的新路径。

            // 构建目标路径
            // 从 target/classes 向上两级到项目根目录，再进入 src 目录
            // 示例：target/classes/../../src/main/java
            // 解析后：./src/main/java
            Path basePath = toSrcTest
                    ? targetPath.resolve("../../src/test/java").normalize()
                    : targetPath.resolve("../../src/main/java").normalize();

            String result = basePath.toString() + File.separator;
            logger.fine("Base path resolved: " + result);
            return result;

        } catch (URISyntaxException e) {
            // URL 语法异常：通常是路径包含特殊字符
            logger.log(Level.WARNING, "Failed to get code source location (URISyntaxException), using current directory", e);
            return getFallbackPath(toSrcTest);
        } catch (Exception e) {
            // 其他异常：如 SecurityException
            logger.log(Level.WARNING, "Failed to determine base path, using current directory", e);
            return getFallbackPath(toSrcTest);
        }
    }

    /**
     * 获取降级路径 - 当无法获取 classes 目录时使用
     *
     * @param toSrcTest true 返回测试目录，false 返回主代码目录
     * @return 降级路径
     *
     * <p>使用场景：
     * - 在 JAR 包中运行时
     * - 在特殊类加载器环境下
     * - 权限受限的环境
     *
     * <p>示例：
     * user.dir = /home/user/project
     * 返回：/home/user/project/src/main/java/
     */
    private String getFallbackPath(boolean toSrcTest) {
        // 使用当前工作目录
        String currentDir = System.getProperty("user.dir");
        String subPath = toSrcTest ? "src/test/java" : "src/main/java";
        String result = currentDir + File.separator + subPath + File.separator;

        logger.fine("Using fallback path: " + result);
        return result;
    }

    /**
     * 构建包路径 - 将包名转换为文件路径
     *
     * @param packageName 包名，如 "com.example.enums"
     * @return 文件路径，如 "/com/example/enums/"
     *
     * <p>转换示例：
     * - "com.example.enums" -> "/com/example/enums/"
     * - "org.apache.commons" -> "/org/apache/commons/"
     */
    private String buildPackagePath(String packageName) {
        // 将 . 替换为系统文件分隔符
        // 原因：不同操作系统使用不同的文件分隔符
        // Windows: \, Linux/Mac: /
        return File.separator + packageName.replace('.', File.separatorChar);
    }

    /**
     * 清理资源 - 释放模板缓存
     *
     * <p>调用时机：
     * - 在应用关闭时调用
     * - 在不再需要生成器时调用
     *
     * <p>注意：
     * 虽然 Engine 没有显式的 close 方法，但清理缓存有助于 GC
     */
    public void close() {
        if (engine != null) {
            // 清空模板缓存
            // 原因：释放内存，帮助 GC
            templateCache.clear();
            logger.info("Template cache cleared");
        }
    }
}