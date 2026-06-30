package com.baymax.validator.engine;

import com.baymax.App;
import com.baymax.validator.engine.constant.Const;

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

    private static final Logger LOG = Logger.getLogger(YamlConfigLoader.class.getName());

    private final org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();

    /**
     * 读取 value_rules yml 文件（支持 classpath 多 jar 合并）
     */
    public Map<String, Map<String, Object>> loadValueRulesYml(String ymlFilePath) {
        Map<String, Map<String, Object>> merged = new ConcurrentHashMap<>();
        List<Map<String, Map<String, Object>>> listMap = new ArrayList<>();

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
                    Map<String, Map<String, Object>> map = yaml.loadAs(content, Map.class);
                    if (map != null) {
                        listMap.add(map);
                    }
                } finally {
                    s.close();
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to load file " + url, e);
            }
        }

        if (listMap.isEmpty()) {
            LOG.fine("No value rules files found for path: " + ymlFilePath);
        }

        if (listMap.size() > 1) {
            Collections.reverse(listMap);
        }
        listMap.forEach(merged::putAll);
        return merged;
    }

    /**
     * 读取规则字典 yml 文件（common_dict）
     */
    public Map<String, Object> loadRuleDictYml(String ymlFilePath) {
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
                LOG.log(Level.WARNING, "Failed to load file " + url, e);
            }
        }

        if (listMap.isEmpty()) {
            LOG.fine("No rule dict files found for path: " + ymlFilePath);
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
                java.io.File dir = new java.io.File(dirUrl.toURI());
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
                LOG.log(Level.WARNING, "Failed to load rules from directory " + url, e);
            }
        }

        return merged;
    }
}
