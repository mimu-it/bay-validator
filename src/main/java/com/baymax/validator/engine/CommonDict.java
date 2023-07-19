package com.baymax.validator.engine;

import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.utils.StrUtil;

import java.io.File;
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

        dict = ValidatorEngine.loadYml(regexDictYmlFilePath);
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
    public List<Object> getList(String dictKey) {
        return (List<Object>) dict.get(dictKey);
    }
}

