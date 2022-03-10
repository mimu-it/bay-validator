package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;

import java.math.BigInteger;

/**
 * BigInteger 理论上能表示无限大的数
 */
public class NumericFieldRule extends FieldRule {

    @Override
    public boolean validate(Object value) {
        if(value == null) { return false; }

        BigInteger realVal = new BigInteger(String.valueOf(value));

        Comparable<BigInteger> min = super.getNumericMin();
        Comparable<BigInteger> max = super.getNumericMax();

        //返回 负整数、零或正整数，根据此对象是小于、等于还是大于指定对象。
        return ((min.compareTo(realVal) < 0) // min < realVal
                || (min.compareTo(realVal) == 0)) // min == realVal
                && (max.compareTo(realVal) > 0); // max > realVal;
    }
}
