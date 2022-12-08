package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.RegexDict;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.utils.StrUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class StringRegexFieldRule extends FieldRule {

    @Override
    public boolean validate(Object value) {
        String realVal = (String) value;

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
                    e.printStackTrace();
                    return false;
                }
            }
        }

        /**
         * 使用正则表达式验证字符串
         */
        String regexDictKey = super.getStringRegexKey();
        if(StringUtils.isBlank(regexDictKey)) {
            throw new IllegalStateException(String.format("regex dict key '%s' is null", regexDictKey));
        }

        String regexStr = RegexDict.INSTANCE.getRegex(regexDictKey);
        Pattern pattern = Pattern.compile(regexStr);
        Matcher matcher = pattern.matcher(realVal);
        // 字符串是否与正则表达式相匹配
        return matcher.matches();
    }
}
