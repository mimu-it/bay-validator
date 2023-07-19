package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.utils.StrUtil;

import java.math.BigDecimal;

/**
 * @author xiao.hu
 *
 * @date 2022-03-30
 * @apiNote
 */
public class DecimalFieldRule extends FieldRule {

    /**
     * 因为js的浮点精度不准确，所以约定传入的浮点数需要以字符串的形式出现
     */
    @Override
    public boolean validate(String value) {
        if(!StrUtil.isNumber(value)) {
            return false;
        }

        BigDecimal realVal = new BigDecimal(value);
        Comparable<BigDecimal> min = super.getDecimalMin();
        Comparable<BigDecimal> max = super.getDecimalMax();

        //返回 负整数、零或正整数，根据此对象是小于、等于还是大于指定对象。
        return (min.compareTo(realVal) <= 0) && (max.compareTo(realVal) >= 0);
    }
}
