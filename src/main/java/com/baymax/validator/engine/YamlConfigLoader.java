package com.baymax.validator.engine;

import com.baymax.App;
import com.baymax.validator.engine.constant.Const;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * YAML 配置文件加载器，负责加载 value_rules 和 common_dict 配置
 */
public class YamlConfigLoader {

    private static final Logger logger = Logger.getLogger(YamlConfigLoader.class.getName());

    private final Yaml yaml = new Yaml();

    /**
     * 读取 value_rules yml 文件（支持 classpath 多 jar 合并）
     */
    /**
     * 加载并合并所有匹配的 YAML 配置文件
     *
     * 示例：加载 validation-rules.yml
     *
     * 文件结构：
     *   classpath:
     *     ├── config/validation-rules.yml          # 用户自定义规则（高优先级）
     *     └── META-INF/default/validation-rules.yml # 默认规则（低优先级）
     *
     * 【默认】规则内容：
     *   user:
     *     name: {required: true, maxLength: 20, pattern: "^[a-zA-Z\\s]+$"}
     *     age:  {required: false, min: 0, max: 150}
     *     email: {required: true, pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"}
     *   product:
     *     name: {required: true, maxLength: 100}
     *     price: {required: true, min: 0.01, max: 999999.99}
     *
     * 【用户】规则内容：
     *   user:
     *     name: {maxLength: 30, pattern: "^[\\u4e00-\\u9fa5a-zA-Z\\s]+$"}  # 覆盖默认
     *     age:  {required: true, max: 200}                                 # 覆盖默认
     *     phone: {required: false, pattern: "^1[3-9]\\d{9}$"}              # 新增字段
     *   order:                                                             # 新增模块
     *     orderId: {required: true, pattern: "^ORD\\d{10}$"}
     *     amount: {required: true, min: 0.01}
     *
     * @param ymlFilePath YAML 文件路径，如 "config/validation-rules.yml"
     * @return 合并后的配置 Map（用户规则覆盖默认规则）
     */
    public Map<String, Map<String, Object>> loadValueRulesYml(String ymlFilePath) {
        // 最终合并结果（线程安全）
        Map<String, Map<String, Object>> merged = new ConcurrentHashMap<>();
        // 存储所有解析结果
        List<Map<String, Map<String, Object>>> listMap = new ArrayList<>();

        // 1. 查找 classpath 中所有匹配的文件
        Enumeration<URL> ps;
        try {
            // 使用类加载器从 classpath 中查找所有匹配路径的资源
            // 入参: "config/validation-rules.yml"
            // 返回示例：
            //   [file:/project/target/classes/config/validation-rules.yml,
            //    jar:file:/project/lib/default-rules.jar!/META-INF/default/validation-rules.yml]
            ps = App.class.getClassLoader().getResources(ymlFilePath);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        // 2. 逐个加载并解析 YAML
        while (ps.hasMoreElements()) {
            // 第一次循环: url = file:/project/target/classes/config/validation-rules.yml
            // 第二次循环: url = jar:file:/project/target/lib/default-rules.jar!/META-INF/default/validation-rules.yml
            URL url = ps.nextElement();
            try (InputStream is = url.openStream()) {
                // 创建 Scanner 读取整个文件内容
                // \A 始终匹配整个文件的绝对开头，保证读取全部内容
                Scanner s = new Scanner(is).useDelimiter("\\A");
                try {
                    // ============ 读取文件内容 ============
                    // 第一次循环读取内容: 用户自定义规则的 YAML 文本
                    // 第二次循环读取内容: 默认规则的 YAML 文本
                    String content = s.hasNext() ? s.next() : "";
                    // 解析为 Map，如：{user={name={required=true, maxLength=20}, ...}}
                    Map<String, Map<String, Object>> map = yaml.loadAs(content, Map.class);
                    if (map != null) {
                        listMap.add(map);
                    }
                } finally {
                    s.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to load file " + url, e);
            }
        }

        if (listMap.isEmpty()) {
            logger.fine("No value rules files found for path: " + ymlFilePath);
        }

        // 3. 反转列表：后加载的覆盖先加载的
        //    原顺序：[用户规则, 默认规则] → 反转后：[默认规则, 用户规则]
        //    效果：用户规则覆盖默认规则
        if (listMap.size() > 1) {
            Collections.reverse(listMap);
        }

        // 4. 合并所有配置
        //    先加载默认规则，再用用户规则覆盖同名 key
        //    最终结果：
        //      user.name.maxLength: 20 → 30 (被覆盖)
        //      user.age.required: false → true (被覆盖)
        //      user.phone: 新增 (用户规则独有)
        //      order: 新增 (用户规则独有)
        //      product: 保留 (默认规则独有)
        listMap.forEach(merged::putAll);

        return merged;
    }

    /**
     * 读取规则字典 yml 文件（common_dict）
     */
    public Map<String, Object> loadCommonRuleDictYml(String ymlFilePath) {
        Map<String, Object> merged = new ConcurrentHashMap<>();
        List<Map<String, Object>> listMap = new ArrayList<>();

        Enumeration<URL> ps;
        try {
            ps = App.class.getClassLoader().getResources(ymlFilePath);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        while (ps.hasMoreElements()) {
            URL url = ps.nextElement();
            try (InputStream is = url.openStream()) {
                Scanner s = new Scanner(is).useDelimiter("\\A");
                try {
                    String content = s.hasNext() ? s.next() : "";
                    Map<String, Object> map = yaml.loadAs(content, Map.class);
                    if (map != null) {
                        listMap.add(map);
                    }
                } finally {
                    s.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to load file " + url, e);
            }
        }

        if (listMap.isEmpty()) {
            logger.fine("No rule dict files found for path: " + ymlFilePath);
        }

        if (listMap.size() > 1) {
            Collections.reverse(listMap);
        }
        listMap.forEach(merged::putAll);
        return merged;
    }

    /**
     * 从 rules 目录下加载所有表规则文件，合并为一个 Map
     */
    public Map<String, Map<String, Object>> loadValueRulesYmlFromDir(String rulesDir) {
        Map<String, Map<String, Object>> merged = new ConcurrentHashMap<>();

        Enumeration<URL> dirUrls;
        try {
            dirUrls = App.class.getClassLoader().getResources(rulesDir);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        List<URL> ymlUrls = new ArrayList<>();
        while (dirUrls.hasMoreElements()) {
            URL dirUrl = dirUrls.nextElement();
            try {
                File dir = new File(dirUrl.toURI());
                java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
                if (files != null) {
                    for (java.io.File f : files) {
                        ymlUrls.add(f.toURI().toURL());
                    }
                }
            } catch (Exception ignored) {
                // jar 包中无法枚举目录，降级
            }
        }

        if (ymlUrls.isEmpty()) {
            // jar 包场景降级到单文件
            try {
                return loadValueRulesYml(rulesDir + "/" + Const.VALUE_RULES_FILENAME);
            } catch (Exception ignored) {
            }
        }

        for (URL url : ymlUrls) {
            try (InputStream is = url.openStream()) {
                Scanner s = new Scanner(is).useDelimiter("\\A");
                try {
                    String content = s.hasNext() ? s.next() : "";
                    Map<String, Map<String, Object>> tableRules = yaml.loadAs(content, Map.class);
                    if (tableRules != null) {
                        merged.putAll(tableRules);
                    }
                } finally {
                    s.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to load rules from directory " + url, e);
            }
        }

        return merged;
    }
}
