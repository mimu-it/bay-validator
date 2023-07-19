package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.utils.StrUtil;

import java.math.BigInteger;

/**
 * BigInteger 理论上能表示无限大的数
 * @author xiao.hu
 * @date 2022-03-30
 * @apiNote
 */
public class NumericFieldRule extends FieldRule {

    @Override
    public boolean validate(String value) {
        if(!StrUtil.isNumber(value)) {
            return false;
        }

        BigInteger realVal;
        try {
            realVal = new BigInteger(value);
        } catch (Exception e) {
            return false;
        }

        Comparable<BigInteger> min = super.getNumericMin();
        Comparable<BigInteger> max = super.getNumericMax();

        //返回 负整数、零或正整数，根据此对象是小于、等于还是大于指定对象。
        return (min.compareTo(realVal) <= 0) && (max.compareTo(realVal) >= 0);
    }
}
