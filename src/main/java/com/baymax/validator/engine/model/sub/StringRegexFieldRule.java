package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.CommonDict;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.utils.StrUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xiao.hu
 * @date 2022-03-30
 * @apiNote
 */
public class StringRegexFieldRule extends FieldRule {

    @Override
    public void build(String fieldKey, String type, Map<String, Object> rulesMap) {
        /**
         * string的相关配置
         */
        String stringCharset = (String) rulesMap.get(RuleKey.string_charset.name());
        String stringRegexKey = (String) rulesMap.get(RuleKey.string_regex_key.name());
        Integer stringLengthMin = (Integer) rulesMap.get(RuleKey.string_length_min.name());
        Integer stringLengthMax = (Integer) rulesMap.get(RuleKey.string_length_max.name());

        this.setFieldKey(fieldKey);
        this.setType(type);
        this.setStringCharset(stringCharset);
        this.setStringRegexKey(stringRegexKey);
        this.setStringLengthMin(stringLengthMin);
        this.setStringLengthMax(stringLengthMax);
    }

    @Override
    public boolean validate(Object value) {
        String realVal = String.valueOf(value);
        /**
         * 验证字符串长度是否符合规范
         * 如果没有配置，则默认符合长度规范
         */
        String charset = super.getStringCharset();
        Integer stringLengthMin = super.getStringLengthMin();
        Integer stringLengthMax = super.getStringLengthMax();
        if(StringUtils.isNotBlank(charset) && stringLengthMin != null && stringLengthMax != null) {
            if(StrUtil.isBlank(charset)) {
                /**
                 * charset 为空意味着是使用mysql的字段长度规则
                 */
                int length = realVal.length();
                if(length > stringLengthMax || length < stringLengthMin) {
                    return false;
                }
            }
            else {
                /**
                 * charset 不为空意味着是需要使用byte验证字段长度规则
                 */
                try {
                    int length = realVal.getBytes(charset).length;
                    if(length > stringLengthMax || length < stringLengthMin) {
                        return false;
                    }
                } catch (UnsupportedEncodingException e) {
                    return false;
                }
            }
        }

        /**
         * 使用正则表达式验证字符串
         */
        String regexDictKey = super.getStringRegexKey();
        if(StringUtils.isBlank(regexDictKey)) {
            throw new IllegalStateException(String.format("common dict key '%s' is null", regexDictKey));
        }

        String regexStr = (String) CommonDict.INSTANCE.getRule(regexDictKey);
        Pattern pattern = Pattern.compile(regexStr);
        Matcher matcher = pattern.matcher(realVal);
        // 字符串是否与正则表达式相匹配
        return matcher.matches();
    }
}
