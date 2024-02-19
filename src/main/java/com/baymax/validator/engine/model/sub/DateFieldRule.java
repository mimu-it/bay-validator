package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.utils.ParamUtil;
import com.baymax.validator.engine.utils.StrUtil;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 *
 * GMT是前世界标准时，UTC是现世界标准时。
 *
 * UTC 比 GMT更精准，以原子时计时，适应现代社会的精确计时。
 *
 * UTC（Coodinated Universal Time），协调世界时，又称世界统一时间、世界标准时间、国际协调时间。由于英文（CUT）和法文（TUC）的缩写不同，作为妥协，简称UTC。
 * UTC 是现在全球通用的时间标准，全球各地都同意将各自的时间进行同步协调。UTC 时间是经过平均太阳时（以格林威治时间GMT为准）、地轴运动修正后的新时标以及以秒为单位的国际原子时所综合精算而成。
 *
 * @author xiao.hu
 * @date 2024-02-19
 * @apiNote
 */
public class DateFieldRule extends FieldRule {
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    static {
        /** yaml.loadAs 默认会把日期字符串转换为Date  会变成 08:00:00 */
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        //dateFormat.setTimeZone(TimeZone.getDefault());
    }

    @Override
    public void build(String fieldKey, String type, Map<String, Object> rulesMap) {
        /**
         * decimal的相关配置
         */
        Date beginAt = ParamUtil.getDate(rulesMap, RuleKey.begin_at.name());
        Date endAt = ParamUtil.getDate(rulesMap, RuleKey.end_at.name());

        this.setFieldKey(fieldKey);
        this.setType(type);
        this.setBeginAt(beginAt);
        this.setEndAt(endAt);
    }

    @Override
    public boolean validate(Object value) {
        Date date = null;
        if(value instanceof String) {
            String valueStr = (String)value;
            if(StrUtil.isBlank(valueStr)) {
                return false;
            }

            try {
                date = dateFormat.parse(valueStr);
            } catch (Exception e) {
                return false;
            }
        }
        else if (value instanceof Date) {
            date = (Date)value;
        }
        else if(value instanceof LocalDate) {
            LocalDate localDate = (LocalDate)value;
            date = Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        }

        if(date == null) {
            return false;
        }

        Date beginAt = zeroTime(super.getBeginAt());
        Date endAt = zeroTime(super.getEndAt());

        if(beginAt != null && endAt != null) {
            return (date.equals(beginAt) || date.after(beginAt)) && (date.before(endAt));
        }

        if(beginAt != null) {
            return date.equals(beginAt) || date.after(beginAt);
        }

        if(endAt != null) {
            return date.before(endAt);
        }

        return false;
    }


    private Date zeroTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
