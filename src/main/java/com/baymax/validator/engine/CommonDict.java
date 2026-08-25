package com.baymax.validator.engine;

import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.utils.StrUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举方式实现单例
 *
 * @author xiao.hu
 */
public enum CommonDict {

    /**
     * 单例实例
     */
    INSTANCE;

    private final YamlConfigLoader configLoader = new YamlConfigLoader();

    Map<String, Object> dict;

    /**
     * 初始化
     */
    public void init() {
        this.init("");
    }

    public void init(String regexDictYmlFilePath) {
        regexDictYmlFilePath = StrUtil.isBlank(regexDictYmlFilePath) ?
                Const.COMMON_DICT_FILENAME : regexDictYmlFilePath;

        Map<String, Object> dictDefault = configLoader.loadCommonRuleDictYml(Const.COMMON_DICT_FILENAME);

        dict = new HashMap<>(dictDefault.size());
        dict.putAll(dictDefault);

        if(!regexDictYmlFilePath.equals(Const.COMMON_DICT_FILENAME)) {
            // 加载自定的枚举字典
            Map<String, Object> dictFromOther = configLoader.loadCommonRuleDictYml(regexDictYmlFilePath);
            dict.putAll(dictFromOther);
        }

        if(dict == null) {
            throw new IllegalStateException("load " + Const.COMMON_DICT_FILENAME + " failed");
        }
    }

    /**
     * 通过键值获得正则表达式
     * @param regexKey
     * @return
     */
    public Object getRule(String regexKey) {
        return dict.get(regexKey);
    }

    /**
     * 通过键值可以直接获得List类型的值
     * @param dictKey
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Object> getList(String dictKey) {
        Object val = dict.get(dictKey);
        if(val instanceof List) {
            return (List<Object>) val;
        }
        return null;
    }
}

