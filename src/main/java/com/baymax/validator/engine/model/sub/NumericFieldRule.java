package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.utils.ParamUtil;
import com.baymax.validator.engine.utils.StrUtil;

import java.math.BigInteger;
import java.util.Map;

/**
 * BigInteger 理论上能表示无限大的数
 * @author xiao.hu
 * @date 2022-03-30
 * @apiNote
 */
public class NumericFieldRule extends FieldRule {

    @Override
    public void build(String fieldKey, String type, Map<String, Object> rulesMap) {
        /**
         * 数值型相关配置
         * yaml会根据配置数值的大小，自动匹配int long BigInteger类型
         * 因为假设numericMin为1时，yaml将其匹配为int型，而numericMax为大数，匹配为long型
         * 比较起来需要做类型转换，所以统一使用BigInteger类型进行全兼容
         */
        BigInteger numericMin = ParamUtil.getBigInteger(rulesMap, RuleKey.numeric_min.name());
        BigInteger numericMax = ParamUtil.getBigInteger(rulesMap, RuleKey.numeric_max.name());

        this.setFieldKey(fieldKey);
        this.setType(type);
        this.setNumericMin(numericMin);
        this.setNumericMax(numericMax);
    }

	@Override
	public boolean validate(Object value) {
		String valueStr = String.valueOf(value);
		if(!StrUtil.isNumber(valueStr)) {
			return false;
		}

		BigInteger realVal;
		try {
			realVal = new BigInteger(valueStr);
		} catch (Exception e) {
			return false;
		}

		BigInteger min = super.getNumericMin();
		BigInteger max = super.getNumericMax();

		if(min != null && min.compareTo(realVal) > 0) {
			return false;
		}

		if(max != null && max.compareTo(realVal) < 0) {
			return false;
		}

		return true;
	}


}
