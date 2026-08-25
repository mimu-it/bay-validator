package com.baymax.validator.engine;

import com.baymax.App;
import com.baymax.validator.engine.constant.Const;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * YAML 配置文件加载器，负责加载 value_rules 和 common_dict 配置
 * 支持 classpath 资源和绝对路径文件加载
 *
 * 使用示例：
 * <pre>
 * YamlConfigLoader loader = new YamlConfigLoader();
 *
 * // 1. 加载 validation-rules.yml（支持多文件合并）
 * Map<String, Map<String, Object>> rules = loader.loadValueRulesYml("config/validation-rules.yml");
 * // 输出：{user={name={required=true, maxLength=30}, age={required=true, max=200}, phone=...}, order=...}
 *
 * // 2. 加载 common_dict.yml
 * Map<String, Object> dict = loader.loadCommonRuleDictYml("config/common_dict.yml");
 * // 输出：{status={1=激活, 0=禁用}, gender={M=男, F=女}}
 *
 * // 3. 加载目录下所有规则文件
 * Map<String, Map<String, Object>> dirRules = loader.loadValueRulesYmlFromDir("rules");
 * // 输出：合并 rules/ 目录下所有 .yml 文件
 *
 * // 4. 支持绝对路径
 * Map<String, Map<String, Object>> absRules = loader.loadValueRulesYml("/opt/config/validation-rules.yml");
 *
 * // 5. 检查配置文件是否存在
 * boolean exists = loader.exists("config/validation-rules.yml");
 *
 * // 6. 获取文件修改时间（用于热加载）
 * long lastModified = loader.getLastModified("/opt/config/validation-rules.yml");
 * </pre>
 */
public class YamlConfigLoader {
    // ==================== 静态成员 ====================
    /** 日志记录器，用于记录加载过程中的信息和警告 */
    private static final Logger logger = Logger.getLogger(YamlConfigLoader.class.getName());

    /** YAML 文件扩展名正则表达式，匹配 .yml 或 .yaml 结尾的文件 */
    private static final String YAML_EXTENSION_PATTERN = ".*\\.(yml|yaml)$";

    /** SnakeYAML 解析器实例，用于将 YAML 文本解析为 Java 对象 */
    private final Yaml yaml = new Yaml();

    // ==================== 公共方法 ====================

    /**
     * 加载并合并所有匹配的 YAML 配置文件
     * 支持 classpath 资源和绝对路径
     *
     * 工作原理：
     * 1. 解析路径，找到所有可能的配置源（文件系统或 classpath）
     * 2. 按顺序加载所有配置源
     * 3. 反转顺序，后加载的覆盖先加载的
     * 4. 合并所有配置
     *
     * @param ymlFilePath YAML 文件路径，支持：
     *                    - classpath 路径：如 "config/validation-rules.yml"
     *                    - 绝对路径：如 "/opt/config/validation-rules.yml"
     *                    - 相对路径：如 "./config/validation-rules.yml"
     * @return 合并后的配置 Map（用户规则覆盖默认规则）
     *         示例返回：
     *         {
     *           "user": {
     *             "name": {"required": true, "maxLength": 30, "pattern": "^[\\u4e00-\\u9fa5a-zA-Z\\s]+$"},
     *             "age": {"required": true, "min": 0, "max": 200},
     *             "phone": {"required": false, "pattern": "^1[3-9]\\d{9}$"}
     *           },
     *           "order": {
     *             "orderId": {"required": true, "pattern": "^ORD\\d{10}$"},
     *             "amount": {"required": true, "min": 0.01}
     *           }
     *         }
     */
    public Map<String, Map<String, Object>> loadValueRulesYml(String ymlFilePath) {
        // 创建线程安全的 Map 用于存储最终合并结果
        Map<String, Map<String, Object>> merged = new ConcurrentHashMap<>();

        // 第一步：解析文件路径，获取所有配置源
        // 例如：传入 "config/validation-rules.yml"
        // 可能返回：[FileConfigSource(/project/config/validation-rules.yml),
        //           UrlConfigSource(jar:file:/lib/default.jar!/META-INF/default/validation-rules.yml)]
        List<ConfigSource> configSources = resolveConfigSources(ymlFilePath);

        // 如果没有找到任何配置源，记录日志并返回空 Map
        if (configSources.isEmpty()) {
            logger.fine("No value rules files found for path: " + ymlFilePath);
            return merged;
        }

        // 第二步：加载并解析所有配置源
        // 将每个 YAML 文件解析为 Map<String, Map<String, Object>>
        List<Map<String, Map<String, Object>>> parsedConfigs = loadConfigs(configSources);

        // 第三步：合并配置（后加载的覆盖先加载的）
        mergeConfigs(parsedConfigs, merged);

        return merged;
    }

    /**
     * 加载 common_dict 配置文件
     *
     * common_dict 用于存储公共字典数据，如状态码、枚举值等
     *
     * 示例 YAML 内容：
     * <pre>
     * status:
     *   1: 激活
     *   0: 禁用
     * gender:
     *   M: 男
     *   F: 女
     * </pre>
     *
     * @param ymlFilePath 文件路径，支持 classpath 或绝对路径
     * @return 合并后的配置 Map
     *         示例返回：
     *         {
     *           "status": {"1": "激活", "0": "禁用"},
     *           "gender": {"M": "男", "F": "女"}
     *         }
     */
    public Map<String, Object> loadCommonRuleDictYml(String ymlFilePath) {
        // 创建线程安全的 Map 用于存储最终合并结果
        Map<String, Object> merged = new ConcurrentHashMap<>();

        // 解析文件路径，获取所有配置源
        List<ConfigSource> configSources = resolveConfigSources(ymlFilePath);

        // 如果没有找到任何配置源，记录日志并返回空 Map
        if (configSources.isEmpty()) {
            logger.fine("No rule dict files found for path: " + ymlFilePath);
            return merged;
        }

        // 加载并解析所有配置源为 Map<String, Object>
        List<Map<String, Object>> parsedConfigs = loadGenericConfigs(configSources);

        // 反转顺序：后加载的覆盖先加载的
        // 例如：如果有 user_rules.yml 和 default_rules.yml
        // 先加载 default，再加载 user，user 覆盖 default
        if (parsedConfigs.size() > 1) {
            Collections.reverse(parsedConfigs);
        }

        // 将所有配置合并到 merged Map 中
        // 如果 key 相同，后面的会覆盖前面的
        parsedConfigs.forEach(merged::putAll);

        return merged;
    }

    /**
     * 从 rules 目录下加载所有表规则文件，合并为一个 Map
     *
     * 该方法会扫描指定目录下的所有 .yml 和 .yaml 文件，
     * 并将它们的内容合并到一个 Map 中。
     *
     * 使用场景：
     * - 将不同表的验证规则放在不同文件中，便于管理
     * - 例如：user-rules.yml, order-rules.yml, product-rules.yml
     *
     * 目录结构示例：
     * <pre>
     * rules/
     *   ├── user-rules.yml      # 用户表规则
     *   ├── order-rules.yml     # 订单表规则
     *   └── product-rules.yml   # 产品表规则
     * </pre>
     *
     * @param rulesDir 目录路径，支持 classpath 或绝对路径
     *                 例如："rules" 或 "/opt/config/rules"
     * @return 合并后的配置 Map，key 为表名，value 为该表的验证规则
     *         示例返回：
     *         {
     *           "user": {"name": {...}, "age": {...}},
     *           "order": {"orderId": {...}, "amount": {...}},
     *           "product": {"name": {...}, "price": {...}}
     *         }
     */
    public Map<String, Map<String, Object>> loadValueRulesYmlFromDir(String rulesDir) {
        // 创建线程安全的 Map 用于存储最终合并结果
        Map<String, Map<String, Object>> merged = new ConcurrentHashMap<>();

        // 解析目录路径，获取所有目录配置源
        // 第二个参数 true 表示这是目录而不是文件
        List<ConfigSource> dirSources = resolveConfigSources(rulesDir);
        logger.info("loadValueRulesYmlFromDir rulesDir: " + rulesDir);
        logger.info("dirSources: " + dirSources);

        // 如果没有找到任何目录，尝试降级到单文件加载
        if (dirSources.isEmpty()) {
            // 构建默认的文件路径：rulesDir + "/" + 默认规则文件名
            String filePath = rulesDir + "/" + Const.VALUE_RULES_FILENAME;
            try {
                // 调用单文件加载方法
                return loadValueRulesYml(filePath);
            } catch (Exception e) {
                // 如果加载失败，记录警告并返回空 Map
                logger.log(Level.WARNING, "Failed to load rules from directory: " + rulesDir, e);
                return merged;
            }
        }

        // 从目录中收集所有 YAML 文件
        // 例如：从 rules/ 目录中找出 user-rules.yml, order-rules.yml 等
        List<ConfigSource> yamlFiles = collectYamlFiles(dirSources);

        // 如果没有找到任何 YAML 文件，记录日志并返回空 Map
        if (yamlFiles.isEmpty()) {
            logger.fine("No YAML files found in directory: " + rulesDir);
            return merged;
        }

        // 加载并合并所有 YAML 文件
        List<Map<String, Map<String, Object>>> parsedConfigs = loadConfigs(yamlFiles);
        // 将每个文件的配置合并到 merged 中
        // 如果多个文件有相同的表名，后面的会覆盖前面的
        parsedConfigs.forEach(merged::putAll);

        return merged;
    }

    // ==================== 私有方法 - 路径解析 ====================

    /**
     * 解析配置源
     *
     * 该方法按以下优先级查找配置：
     * 1. 绝对路径文件系统
     * 2. classpath 资源
     * 3. 如果绝对路径不存在，尝试作为 classpath 资源
     *
     * @param path      文件或目录路径
     * @return 配置源列表
     */
    private List<ConfigSource> resolveConfigSources(String path) {
        // 创建列表存储找到的配置源
        List<ConfigSource> sources = new ArrayList<>();

        // ===== 第一步：尝试作为文件系统路径 =====
        // 使用 Paths.get() 创建 Path 对象
        // 例如："/opt/config/validation-rules.yml" -> 绝对路径
        //       "config/validation-rules.yml" -> 相对路径
        Path filePath = Paths.get(path);

        // 检查是否为绝对路径（以 / 开头或包含盘符，如 C:\）
        if (filePath.isAbsolute()) {
            logger.info("isAbsolute");
            // 将 Path 转换为 File 对象
            File file = filePath.toFile();

            // 检查文件或目录是否存在
            if (file.exists()) {
                sources.add(new FileConfigSource(file));
                return sources; // 找到后立即返回
            }
        }

        // ===== 第二步：尝试作为 classpath 资源 =====
        try {
            // 使用 App.class 的类加载器获取所有匹配的资源
            // 例如：查找 "config/validation-rules.yml"
            // 可能找到多个：用户自定义的、jar包中的默认配置
            Enumeration<URL> resources = App.class.getClassLoader().getResources(path);

            // 遍历所有找到的资源
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                // 关键：判断 URL 协议类型
                if (url.getProtocol().equals("file")) {
                    logger.info("classpath file:" + url);
                    // 文件系统路径 → 用 File API
                    // 可以遍历内部的文件
                    File dir = new File(url.toURI());
                    if (dir.exists()) {
                        sources.add(new FileConfigSource(dir));
                    }
                }
                else if (url.getProtocol().equals("jar")) {
                    logger.info("classpath jar:" + url);
                    // JAR 包内的资源 → 用 JarFile API
                    // 将 URL 包装为 UrlConfigSource
                    // 比如后续组装成value_rules.yml
                    sources.add(new UrlConfigSource(url));
                }
            }
        } catch (IOException e) {
            // 如果加载 classpath 资源失败，记录警告
            logger.log(Level.WARNING, "Failed to load classpath resources for: " + path, e);
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // ===== 第三步：降级处理 - 绝对路径不存在时尝试 classpath =====
        // 如果上面没有找到任何资源，且路径是绝对路径，尝试作为 classpath 资源加载
        // 这种情况可能发生在：路径是绝对路径格式，但实际文件在 classpath 中
        if (sources.isEmpty() && filePath.isAbsolute()) {
            try {
                // 从 classpath 中获取资源
                // 注意：这里会去掉绝对路径的前导 /，因为 classpath 不需要
                URL resource = App.class.getClassLoader().getResource(path);
                logger.info("降级处理:" + resource);
                if (resource != null) {
                    sources.add(new UrlConfigSource(resource));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load resource as classpath: " + path, e);
            }
        }
        logger.info("sources:" + sources);
        logger.info("sources size:" + sources.size());
        return sources;
    }

    // ==================== 私有方法 - 文件收集 ====================

    /**
     * 从目录源中收集所有 YAML 文件
     *
     * 该方法处理两种类型的目录源：
     * 1. FileConfigSource：文件系统目录，可以遍历文件列表
     * 2. UrlConfigSource：URL 资源（如 jar 包），无法遍历，只能尝试加载默认文件
     *
     * @param dirSources 目录配置源列表
     * @return 所有 YAML 文件的配置源列表
     */
    private List<ConfigSource> collectYamlFiles(List<ConfigSource> dirSources) {
        // 创建列表存储找到的 YAML 文件
        List<ConfigSource> yamlFiles = new ArrayList<>();

        // 遍历所有目录源
        for (ConfigSource source : dirSources) {
            logger.info("collectYamlFiles source:" + source);
            logger.info("collectYamlFiles source class:" + source.getClass());
            // ===== 处理文件系统目录 =====
            if (source instanceof FileConfigSource) {
                // 获取目录的 File 对象
                File fileOrDir = ((FileConfigSource) source).getFile();
                if(fileOrDir.isDirectory()) {
                    // 列出目录下所有匹配 YAML 扩展名的文件
                    // 使用 FileFilter 过滤：只保留 .yml 或 .yaml 结尾的文件
                    File[] files = fileOrDir.listFiles((d, name) -> name.matches(YAML_EXTENSION_PATTERN));

                    // 如果找到了文件，将它们添加到结果列表
                    if (files != null) {
                        for (File file : files) {
                            yamlFiles.add(new FileConfigSource(file));
                            logger.info("FileConfigSource: " + file.getAbsolutePath());
                        }
                    }
                }
                else {
                    yamlFiles.add(new FileConfigSource(fileOrDir));
                }
            }
            // ===== 处理 URL 资源（如 jar 包中的目录） =====
            else if (source instanceof UrlConfigSource) {
                URL url = ((UrlConfigSource)source).getUrl();
                String urlPath = url.toString();
                if (!urlPath.endsWith("/")) {
                    urlPath += "/";
                }

                if ("jar".equals(url.getProtocol())) {
                    JarURLConnection conn = null;

                    try {
                        conn = (JarURLConnection)url.openConnection();
                        JarFile jarFile = conn.getJarFile();
                        Enumeration<JarEntry> entries = jarFile.entries();
                        String dirName = conn.getEntryName();
                        if (!dirName.endsWith("/")) {
                            dirName = dirName + "/";
                        }

                        while(entries.hasMoreElements()) {
                            JarEntry entry = (JarEntry)entries.nextElement();
                            String entryName = entry.getName();
                            if (entryName.startsWith(dirName) && !entry.isDirectory()) {
                                String relativePath = entryName.substring(dirName.length());
                                if (!relativePath.contains("/")) {
                                    logger.info("文件名: " + relativePath);
                                    logger.info("文件名entryName: " + entryName);

                                    String ymlFile = urlPath + relativePath;
                                    logger.info("UrlConfigSource: " + ymlFile);

                                    URL fileUrl = new URL(ymlFile);
                                    // 将文件 URL 添加到结果列表
                                    yamlFiles.add(new UrlConfigSource(fileUrl));
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return yamlFiles;
    }

    // ==================== 私有方法 - 配置加载 ====================

    /**
     * 加载并解析配置（返回 Map<String, Map<String, Object>> 类型）
     *
     * 该方法用于加载验证规则配置，返回的 Map 结构为：
     * {表名: {字段名: {验证规则}}}
     *
     * @param configSources 配置源列表
     * @return 解析后的配置列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Map<String, Object>>> loadConfigs(List<ConfigSource> configSources) {
        // 创建列表存储解析后的配置
        List<Map<String, Map<String, Object>>> parsedConfigs = new ArrayList<>();

        // 遍历所有配置源
        for (ConfigSource source : configSources) {
            try {
                // 读取配置源的内容（YAML 文本）
                String content = source.readContent();

                // 如果内容不为空，进行解析
                if (!content.isEmpty()) {
                    // 使用 SnakeYAML 将 YAML 文本解析为 Map
                    // 由于 YAML 结构为：{表名: {字段名: {验证规则}}}
                    // 所以类型为 Map<String, Map<String, Object>>
                    Map<String, Map<String, Object>> map = yaml.loadAs(content, Map.class);
                    logger.info("[loadConfigs] load rules: " + map);

                    // 如果解析结果不为 null，添加到列表
                    if (map != null) {
                        parsedConfigs.add(map);
                    }
                }
            } catch (IOException e) {
                // 如果加载失败，记录警告日志
                logger.log(Level.WARNING, "Failed to load config from " + source, e);
            }
        }

        return parsedConfigs;
    }

    /**
     * 加载并解析通用配置（返回 Map<String, Object> 类型）
     *
     * 该方法用于加载字典配置，返回的 Map 结构为：
     * {字典名: {key: value}}
     *
     * @param configSources 配置源列表
     * @return 解析后的配置列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadGenericConfigs(List<ConfigSource> configSources) {
        // 创建列表存储解析后的配置
        List<Map<String, Object>> parsedConfigs = new ArrayList<>();

        // 遍历所有配置源
        for (ConfigSource source : configSources) {
            try {
                // 读取配置源的内容（YAML 文本）
                String content = source.readContent();

                // 如果内容不为空，进行解析
                if (!content.isEmpty()) {
                    // 使用 SnakeYAML 将 YAML 文本解析为 Map
                    // 通用配置类型为 Map<String, Object>
                    Map<String, Object> map = yaml.loadAs(content, Map.class);

                    // 如果解析结果不为 null，添加到列表
                    if (map != null) {
                        parsedConfigs.add(map);
                    }
                }
            } catch (IOException e) {
                // 如果加载失败，记录警告日志
                logger.log(Level.WARNING, "Failed to load config from " + source, e);
            }
        }

        return parsedConfigs;
    }

    /**
     * 合并配置列表
     *
     * 合并规则：
     * 1. 反转配置列表，使后加载的配置排在前面
     * 2. 按顺序将配置合并到目标 Map
     * 3. 如果 key 相同，后面的配置覆盖前面的
     *
     * @param configs 配置列表
     * @param target  目标 Map
     */
    private void mergeConfigs(List<Map<String, Map<String, Object>>> configs,
                              Map<String, Map<String, Object>> target) {
        // 如果配置列表为空，直接返回
        if (configs.isEmpty()) {
            return;
        }

        // 反转顺序：后加载的覆盖先加载的
        // 例如：configs = [默认规则, 用户规则]
        // 反转后：[用户规则, 默认规则]
        // 这样用户规则会覆盖默认规则
        if (configs.size() > 1) {
            Collections.reverse(configs);
        }

        // 将所有配置合并到 target
        // 如果 key 相同，后面的会覆盖前面的
        configs.forEach(target::putAll);
    }

    // ==================== 内部类 - 配置源抽象 ====================

    /**
     * 配置源接口
     *
     * 抽象了配置的来源，可以是文件系统、classpath、jar 包等
     * 所有配置源都必须实现 readContent() 方法
     */
    private interface ConfigSource {
        /**
         * 读取配置内容
         *
         * @return YAML 格式的文本内容
         * @throws IOException 读取失败时抛出
         */
        String readContent() throws IOException;
    }

    /**
     * 文件配置源
     *
     * 表示文件系统中的配置文件
     */
    private static class FileConfigSource implements ConfigSource {
        /** 文件对象 */
        private final File file;

        /**
         * 构造函数
         *
         * @param file 文件对象
         */
        FileConfigSource(File file) {
            this.file = file;
        }

        /**
         * 获取文件对象
         *
         * @return 文件对象
         */
        File getFile() {
            return file;
        }

        /**
         * 读取文件内容
         *
         * @return 文件内容
         * @throws IOException 读取失败时抛出
         */
        @Override
        public String readContent() throws IOException {
            logger.info("读取文件内容: " + file.getAbsolutePath());
            // 使用 FileInputStream 读取文件
            try (FileInputStream is = new FileInputStream(file)) {
                // 调用公共方法读取输入流
                return readInputStream(is);
            }
        }

        /**
         * toString 方法，用于日志输出
         */
        @Override
        public String toString() {
            return "FileConfigSource{" + file.getAbsolutePath() + "}";
        }
    }

    /**
     * URL 配置源（支持 classpath、jar 等）
     *
     * 表示 URL 资源，可以来自 classpath、jar 包、网络等
     */
    private static class UrlConfigSource implements ConfigSource {
        /** URL 对象 */
        private final URL url;

        /**
         * 构造函数
         *
         * @param url URL 对象
         */
        UrlConfigSource(URL url) {
            this.url = url;
        }

        /**
         * 获取 URL 对象
         *
         * @return URL 对象
         */
        URL getUrl() {
            return url;
        }

        /**
         * 读取 URL 内容
         *
         * @return URL 内容
         * @throws IOException 读取失败时抛出
         */
        @Override
        public String readContent() throws IOException {
            logger.info("读取资源 url 内容: " + url.getPath());
            // 使用 URL.openStream() 打开连接并读取
            try (InputStream is = url.openStream()) {
                // 调用公共方法读取输入流
                return readInputStream(is);
            }
        }

        /**
         * toString 方法，用于日志输出
         */
        @Override
        public String toString() {
            return "UrlConfigSource{" + url + "}";
        }
    }

    // ==================== 私有方法 - 工具方法 ====================

    /**
     * 从输入流读取内容
     *
     * 使用 Scanner 读取整个输入流的内容
     * \A 是正则表达式，匹配输入的开头，确保读取全部内容
     *
     * @param is 输入流
     * @return 字符串内容
     */
    private static String readInputStream(InputStream is) {
        // 创建 Scanner，使用 \A 作为分隔符
        // \A 表示输入的开头，所以会读取整个输入流
        try (Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
            // 如果有内容，返回全部内容；否则返回空字符串
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    // ==================== 公共方法 - 工具方法 ====================

    /**
     * 工具方法：加载单个配置文件（便捷方法）
     *
     * 与 loadValueRulesYml 的区别：
     * - loadValueRulesYml：加载所有匹配的配置文件并合并
     * - loadSingleValueRulesYml：只加载第一个找到的配置文件
     *
     * 使用场景：当确定只有一个配置文件时使用
     *
     * @param ymlFilePath YAML 文件路径
     * @return 加载的配置 Map
     *         示例返回：
     *         {
     *           "user": {
     *             "name": {"required": true, "maxLength": 20},
     *             "age": {"required": false, "min": 0, "max": 150}
     *           }
     *         }
     */
    public Map<String, Map<String, Object>> loadSingleValueRulesYml(String ymlFilePath) {
        // 创建 HashMap 存储结果
        Map<String, Map<String, Object>> result = new HashMap<>();

        // 解析路径，获取配置源
        List<ConfigSource> sources = resolveConfigSources(ymlFilePath);

        // 如果没有找到配置源，记录警告并返回空 Map
        if (sources.isEmpty()) {
            logger.warning("Configuration file not found: " + ymlFilePath);
            return result;
        }

        // 只取第一个找到的配置源
        ConfigSource source = sources.get(0);
        try {
            // 读取配置内容
            String content = source.readContent();
            // 如果内容不为空，解析 YAML
            if (!content.isEmpty()) {
                Map<String, Map<String, Object>> map = yaml.loadAs(content, Map.class);
                if (map != null) {
                    result.putAll(map);
                }
            }
        } catch (IOException e) {
            // 加载失败时记录警告
            logger.log(Level.WARNING, "Failed to load config from " + source, e);
        }

        return result;
    }

    /**
     * 检查配置文件是否存在
     *
     * 使用场景：在加载配置前检查文件是否存在
     *
     * 示例：
     * <pre>
     * if (loader.exists("config/validation-rules.yml")) {
     *     Map rules = loader.loadValueRulesYml("config/validation-rules.yml");
     * }
     * </pre>
     *
     * @param path 文件路径
     * @return true: 存在, false: 不存在
     */
    public boolean exists(String path) {
        // 解析路径，如果找到任何配置源，说明文件存在
        List<ConfigSource> sources = resolveConfigSources(path);
        return !sources.isEmpty();
    }

    /**
     * 获取文件最后修改时间（仅支持文件系统路径）
     *
     * 使用场景：实现配置文件热加载
     * 通过比较文件的最后修改时间，判断是否需要重新加载
     *
     * 示例：
     * <pre>
     * long lastModified = loader.getLastModified("/opt/config/validation-rules.yml");
     * if (lastModified > cachedTime) {
     *     // 重新加载配置
     * }
     * </pre>
     *
     * @param path 文件路径（必须是文件系统路径）
     * @return 最后修改时间（毫秒），如果无法获取则返回 0
     */
    public long getLastModified(String path) {
        // 将路径转换为 Path 对象
        Path filePath = Paths.get(path);

        // 只支持绝对路径的文件系统文件
        if (filePath.isAbsolute()) {
            try {
                // 使用 Files.getLastModifiedTime 获取文件修改时间
                // 返回的是 FileTime 对象，调用 toMillis() 转换为毫秒
                return Files.getLastModifiedTime(filePath).toMillis();
            } catch (IOException e) {
                // 获取失败时记录调试日志
                logger.log(Level.FINE, "Failed to get last modified time for: " + path, e);
            }
        }
        // 如果是 classpath 资源，无法获取修改时间，返回 0
        return 0L;
    }

    // ==================== 示例数据演示 ====================

    /**
     * 主方法 - 演示各种使用场景
     */
    public static void main(String[] args) {
        // 创建加载器实例
        YamlConfigLoader loader = new YamlConfigLoader();

        logger.info("========== YAML 配置加载器示例 ==========\n");

        // 示例 1: 加载单文件验证规则
        logger.info("示例 1: 加载验证规则");
        Map<String, Map<String, Object>> rules = loader.loadValueRulesYml("config/validation-rules.yml");
        if (!rules.isEmpty()) {
            logger.info("加载的规则: " + rules);
            // 输出示例：
            // {user={name={required=true, maxLength=20, pattern=^[a-zA-Z\\s]+$}, age={required=false, min=0, max=150}}}
        }

        // 示例 2: 加载公共字典
        logger.info("示例 2: 加载公共字典");
        Map<String, Object> dict = loader.loadCommonRuleDictYml("config/common_dict.yml");
        if (!dict.isEmpty()) {
            logger.info("加载的字典: " + dict);
            // 输出示例：
            // {status={1=激活, 0=禁用}, gender={M=男, F=女}}
        }

        // 示例 3: 加载目录下所有规则
        logger.info("示例 3: 加载目录下所有规则");
        Map<String, Map<String, Object>> dirRules = loader.loadValueRulesYmlFromDir("rules");
        if (!dirRules.isEmpty()) {
            logger.info("目录加载的规则: " + dirRules);
            // 输出示例：
            // {user={name={required=true, maxLength=30}}, order={orderId={required=true}}}
        }

        // 示例 4: 支持绝对路径
        logger.info("示例 4: 使用绝对路径加载");
        Map<String, Map<String, Object>> absRules = loader.loadValueRulesYml("/opt/config/validation-rules.yml");
        if (!absRules.isEmpty()) {
            logger.info("绝对路径加载的规则: " + absRules);
        } else {
            logger.info("绝对路径文件不存在，尝试使用示例数据演示");

            // 创建示例数据
            Map<String, Map<String, Object>> exampleRules = new HashMap<>();
            Map<String, Object> userRules = new HashMap<>();
            Map<String, Object> nameRule = new HashMap<>();
            nameRule.put("required", true);
            nameRule.put("maxLength", 30);
            userRules.put("name", nameRule);
            exampleRules.put("user", userRules);

            logger.info("示例数据: " + exampleRules);
        }

        // 示例 5: 检查文件是否存在
        logger.info("示例 5: 检查文件是否存在");
        String testPath = "config/validation-rules.yml";
        boolean exists = loader.exists(testPath);
        logger.info("文件 '" + testPath + "' 是否存在: " + exists);

        // 示例 6: 获取文件修改时间
        logger.info("示例 6: 获取文件修改时间");
        String absPath = "/opt/config/validation-rules.yml";
        long lastModified = loader.getLastModified(absPath);
        if (lastModified > 0) {
            logger.info("文件 '" + absPath + "' 最后修改时间: " + new Date(lastModified));
        } else {
            logger.info("无法获取文件 '" + absPath + "' 的修改时间");
        }

        // 示例 7: 加载单个配置文件
        logger.info("示例 7: 加载单个配置文件");
        Map<String, Map<String, Object>> singleRules = loader.loadSingleValueRulesYml("config/validation-rules.yml");
        if (!singleRules.isEmpty()) {
            logger.info("单个文件加载的规则: " + singleRules);
        }

        logger.info("========== 示例演示完成 ==========");
    }
}