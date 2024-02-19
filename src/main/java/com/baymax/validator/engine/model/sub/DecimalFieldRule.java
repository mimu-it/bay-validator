package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.utils.ParamUtil;
import com.baymax.validator.engine.utils.StrUtil;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author xiao.hu
 *
 * @date 2022-03-30
 * @apiNote
 */
public class DecimalFieldRule extends FieldRule {

    @Override
    public void build(String fieldKey, String type, Map<String, Object> rulesMap) {
        /**
         * decimal的相关配置
         */
        BigDecimal decimalMin = ParamUtil.getBigDecimal(rulesMap, RuleKey.decimal_min.name());
        BigDecimal decimalMax = ParamUtil.getBigDecimal(rulesMap, RuleKey.decimal_max.name());

        this.setFieldKey(fieldKey);
        this.setType(type);
        this.setDecimalMin(decimalMin);
        this.setDecimalMax(decimalMax);
    }

    /**
     * 因为js的浮点精度不准确，所以约定传入的浮点数需要以字符串的形式出现
     */
    @Override
    public boolean validate(Object value) {
        String valueStr = String.valueOf(value);
        if(!StrUtil.isNumber(valueStr)) {
            return false;
        }

        BigDecimal realVal = new BigDecimal(valueStr);
        Comparable<BigDecimal> min = super.getDecimalMin();
        Comparable<BigDecimal> max = super.getDecimalMax();

        //返回 负整数、零或正整数，根据此对象是小于、等于还是大于指定对象。
        return (min.compareTo(realVal) <= 0) && (max.compareTo(realVal) >= 0);
    }


}
