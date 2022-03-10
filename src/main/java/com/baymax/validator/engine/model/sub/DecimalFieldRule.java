package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;

import java.math.BigDecimal;

public class DecimalFieldRule extends FieldRule {

    @Override
    public boolean validate(Object value) {
        if(value == null) { return false; }
        /**
         * 因为js的浮点精度不准确，所以约定传入的浮点数需要以字符串的形式出现
         */
        if(!(value instanceof String)) {
            return false;
        }

        BigDecimal realVal = new BigDecimal((String)value);
        //System.out.println("realVal.toString()" + realVal.toString());
        Comparable<BigDecimal> min = super.getDecimalMin();
        Comparable<BigDecimal> max = super.getDecimalMax();

        //返回 负整数、零或正整数，根据此对象是小于、等于还是大于指定对象。
        return ((min.compareTo(realVal) < 0) // min < realVal
                || (min.compareTo(realVal) == 0)) // min == realVal
                && (max.compareTo(realVal) > 0); // max > realVal;
    }
}
