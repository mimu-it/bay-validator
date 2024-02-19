package com.baymax.validator.engine.model.sub;

import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.preset.RuleKey;
import com.baymax.validator.engine.utils.ParamUtil;
import com.baymax.validator.engine.utils.StrUtil;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author xiao.hu
 * @date 2024-02-19
 * @apiNote
 */
public class DatetimeFieldRule extends FieldRule {
    private static SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static {
        /** yaml.loadAs 默认会把日期字符串转换为Date  会变成 08:00:00 */
        datetimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void build(String fieldKey, String type, Map<String, Object> rulesMap) {
        /**
         * decimal的相关配置
         */
        Date beginAt = ParamUtil.getDatetime(rulesMap, RuleKey.begin_at.name());
        Date endAt = ParamUtil.getDatetime(rulesMap, RuleKey.end_at.name());

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
                date = datetimeFormat.parse(valueStr);
            } catch (Exception e) {
                return false;
            }
        }
        else if (value instanceof Date) {
            date = (Date)value;
        }
        else if(value instanceof LocalDateTime) {
            /**
             * 可以使用java.time.LocalDateTime类将日期和时间表示为不包含任何时区信息的本地值。
             * 如果要将其转换为特定时区下的日期对象（java.util.Date），则需要进行额外的处理。
             */
            LocalDateTime localDateTime = (LocalDateTime)value;

            // 获取当前系统默认的时区ID
            ZoneId zoneId = ZoneId.systemDefault();
            // 根据时区创建ZonedDateTime对象
            ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
            // 转换为时间戳
            long timestamp = zonedDateTime.toInstant().toEpochMilli();
            // 转换为Date对象
            date = new Date(timestamp);
        }

        if(date == null) {
            return false;
        }

        Date beginAt = super.getBeginAt();
        Date endAt = super.getEndAt();

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
}
