package com.baymax.validator.engine;

import java.util.List;
import java.util.Map;

/**
 * 枚举方式实现单例
 */
public enum RegexDict {

    INSTANCE;

    Map<String, Object> dict;

    /**
     * 初始化
     */
    public void init() {
        dict = ValidatorEngine.loadYml("regex_dict.yml");
        if(dict == null) {
            throw new IllegalStateException("load regex_dict.yml failed");
        }
    }

    /**
     * 通过键值获得正则表达式
     * @param regexKey
     * @return
     */
    public String getRegex(String regexKey) {
        return (String) dict.get(regexKey);
    }

    /**
     * 通过键值可以直接获得List类型的值
     * @param dictKey
     * @return
     */
    public List<Object> getList(String dictKey) {
        return (List<Object>) dict.get(dictKey);
    }
}

