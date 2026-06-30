/**
 * 通过字符串方式创建配置和值，进行校验（Java 后端调用 JS 时使用）。
 *
 * 注意：fieldConfigStr 和 valueStr 必须是受信任的来源（后端序列化输出），
 * 禁止直接拼接用户输入调用此函数，否则存在 XSS 风险。
 *
 * @param {string} fieldConfigStr - JSON 格式的字段配置字符串
 * @param {string} key - 字段 key
 * @param {string} valueStr - JS 表达式形式的参数值字符串
 */
export function testHxValidator(fieldConfigStr, key, valueStr) {
    var fieldConfig = JSON.parse(fieldConfigStr);
    var value = JSON.parse(valueStr);
    var oHxValidator = new HxValidator(fieldConfig);
    return oHxValidator.validate(key, value);
}

export function HxValidator(fieldConfig) {
    this.fieldConfig = fieldConfig;
    return this;
}

/**
 * 1.js大整数的精度丢失和浮点数本质上是一样的，尾数位最大是 52 位，因此 JS 中能精准表示的最大整数是 Math.pow(2, 53)，
 * 十进制即 9007199254740992。大于 9007199254740992 的可能会丢失精度
 */
HxValidator.prototype = {
    constructor: HxValidator,
    kit: {
        /** 判断值是否为 null 或 undefined */
        isNone: function (o) {
            return o === null || o === undefined;
        },
        /** 判断是否为字符串类型 */
        isString: function (obj) {
            return Object.prototype.toString.call(obj) === "[object String]";
        },
        /** 判断是否为数字类型 */
        isNumber: function (obj) {
            return Object.prototype.toString.call(obj) === "[object Number]";
        },
        /** 判断是否为数组 */
        isArray: function (obj) {
            return Object.prototype.toString.call(obj) === "[object Array]";
        },
        /** 校验日期字符串格式 (yyyy-MM-dd) */
        checkDateStr: function (strDate) {
            if (strDate === null || strDate === undefined) {
                return false;
            }
            return /^(\d{4})-(\d{2})-(\d{2})$/.test(strDate);
        },
        /** 校验日期时间字符串格式 (yyyy-MM-dd HH:mm:ss) */
        checkDatetimeStr: function (strDate) {
            if (strDate === null || strDate === undefined) {
                return false;
            }
            return /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.test(strDate);
        },
        /** 判断 Date 对象是否有效（new Date("2018-22-33") 是 invalid Date） */
        isValidDate: function (date) {
            if (date === null || date === undefined) {
                return false;
            }
            return date instanceof Date && !Number.isNaN(date.getTime());
        },
        /** 获取对象的类型字符串，如 "[object String]" */
        whatType: function (obj) {
            return Object.prototype.toString.call(obj);
        },
        /** 判断数组中的各项元素的类型，如果存在不同类型，则返回空字符串 */
        checkArrayItemsType: function (sourceArr) {
            var arrayType = "";
            for (var i = 0; i < sourceArr.length; i++) {
                var item = sourceArr[i];
                var itemType = Object.prototype.toString.call(item);
                if (arrayType === "") {
                    arrayType = itemType;
                } else if (arrayType !== itemType) {
                    return "";
                }
            }
            return arrayType;
        },
        /** targetArr 是否是 sourceArr 的子集 */
        isSubset: function (sourceArr, targetArr) {
            return targetArr.every(function (v) {
                return sourceArr.some(function (item) {
                    return item === v;
                });
            });
        },
        /** 数组中是否包含目标元素 */
        contains: function (sourceArr, targetItem) {
            return sourceArr.some(function (item) {
                return item === targetItem;
            });
        },
        /**
         * 判断字符是否为 CJK（中日韩统一表意文字）字符。
         * Unicode 范围：U+4E00 ~ U+9FFF 为基本区，U+3400 ~ U+4DBF 为扩展A区。
         */
        isCJKCharacter: function (c) {
            var code = c.charCodeAt(0);
            return (code >= 0x4E00 && code <= 0x9FFF) || (code >= 0x3400 && code <= 0x4DBF);
        },
        /** 判断字符串是否为 null、undefined 或纯空白 */
        isBlank: function (value) {
            return (value === null || value === undefined || value.trim() === "");
        }
    },
    /** 根据配置和值进行字段校验 */
    validate: function (key, value) {
        var configJson = this.fieldConfig[key];

        if (!configJson) {
            return "No config, the field is " + key;
        }

        if (configJson.type === "enum_numeric" || configJson.type === "enum_decimal"
                || configJson.type === "enum_string") {
            var errorCode = this.value_range__checker.call(this, configJson, value);
            if (errorCode.length !== 0) {
                return errorCode;
            }
            return "";
        }

        if (configJson.type === "string") {
            var checkList = [this.string_length__checker, this.regex__checker];
            for (var i = 0; i < checkList.length; i++) {
                var checker = checkList[i];
                var err = checker.call(this, configJson, value);
                if (err.length !== 0) {
                    return err;
                }
            }
            return "";
        }

        if (configJson.type === "decimal") {
            var errorCode = this.decimal_range__checker.call(this, configJson, value);
            if (errorCode.length !== 0) {
                return errorCode;
            }
            return "";
        }

        if (configJson.type === "numeric") {
            var errorCode = this.range__checker.call(this, configJson, value);
            if (errorCode.length !== 0) {
                return errorCode;
            }
            return "";
        }

        if (configJson.type === "date" || configJson.type === "datetime") {
            var errorCode = this.date__checker.call(this, configJson, value);
            if (errorCode.length !== 0) {
                return errorCode;
            }
            return "";
        }

        return "";
    },
    /** 数值范围校验 */
    range__checker: function (config, value) {
        var numericMin = config.numericMin;
        var numericMax = config.numericMax;

        if (numericMin === undefined || numericMax === undefined) {
            return "数值型大小校验规则不完整";
        }

        var valueNumber = Number(value);
        if (valueNumber >= numericMin && valueNumber <= numericMax) {
            return "";
        }

        return "数值型大小校验规则不符合规则";
    },
    /** 浮点数范围校验 */
    decimal_range__checker: function (config, value) {
        var numericMin = config.decimalMin;
        var numericMax = config.decimalMax;

        if (numericMin === undefined || numericMax === undefined) {
            return "数值型大小校验规则不完整";
        }

        var valueNumber = Number(value);
        if (valueNumber >= numericMin && valueNumber <= numericMax) {
            return "";
        }

        return "数值型大小校验规则不符合规则";
    },
    /** 字符串长度检测，支持按字符集计算字节长度 */
    string_length__checker: function (config, value) {
        var kit = this.kit;
        var stringCharset = config.stringCharset;
        var stringLengthMin = config.stringLengthMin;
        var stringLengthMax = config.stringLengthMax;

        if (stringLengthMin === undefined || stringLengthMax === undefined) {
            return "字符串长度校验规则不完整";
        }

        var strLength = 0;
        if (value === null || value === undefined) {
            if (stringLengthMin <= 0 && 0 <= stringLengthMax) {
                return "";
            }
            return "参数字符长度不符合规则";
        }

        // lengthMode 明确指示长度计算方式："char"=字符数, "byte"=字节数
        var lengthMode = config.lengthMode || "char";

        if (lengthMode === "char") {
            // 字符数
            strLength = value.length;
        } else {
            // 字节数：按字符集计算
            var charset = (kit.isBlank(stringCharset)) ? "utf8" : stringCharset;
            strLength = this._byteLength(value, charset);
        }

        if (stringLengthMin <= strLength && strLength <= stringLengthMax) {
            return "";
        }
        return "参数字符长度不符合规则";
    },
    /**
     * 计算字符串在指定字符集下的字节长度。
     * 使用 Blob + TextEncoder 方案（浏览器环境）或 encodeURIComponent 回退。
     */
    _byteLength: function (value, charset) {
        // 在浏览器环境中优先使用 TextEncoder
        if (typeof TextEncoder !== "undefined") {
            var encoder;
            if (charset.toLowerCase() === "utf8" || charset.toLowerCase() === "utf-8") {
                encoder = new TextEncoder();
                return encoder.encode(value).length;
            }
            // 非 UTF-8 编码无法使用 TextEncoder，回退到字符映射表
        }

        // 回退：逐字符映射（覆盖常用编码）
        var length = 0;
        for (var i = 0; i < value.length; i++) {
            var c = value.charAt(i);
            if (!kit.isCJKCharacter(c)) {
                // 非 CJK 字符
                if (kit.contains(["GB2312", "GBK", "GB18030", "ISO-8859-1", "UTF-8", "utf8"], charset)) {
                    length += 1;
                } else if (kit.contains(["UTF-16"], charset)) {
                    length += 4;
                } else if (kit.contains(["UTF-16BE", "UTF-16LE"], charset)) {
                    length += 2;
                }
            } else {
                // CJK 字符
                if (kit.contains(["GB2312", "GBK", "GB18030", "UTF-16BE", "UTF-16LE"], charset)) {
                    length += 2;
                } else if (kit.contains(["ISO-8859-1"], charset)) {
                    length += 1;
                } else if (kit.contains(["UTF-8", "utf8"], charset)) {
                    length += 3;
                } else if (kit.contains(["UTF-16"], charset)) {
                    length += 4;
                }
            }
        }
        return length;
    },
    /** 正则表达式校验 */
    regex__checker: function (config, value) {
        var rule = config.regexStr;
        if (rule !== undefined && Object.prototype.toString.call(rule) === "[object String]") {
            var valueStr = String(value);
            try {
                var reg = new RegExp(rule);
                if (!reg.test(valueStr)) {
                    return "参数字符不符合规则";
                }
                return "";
            } catch (e) {
                return "正则表达式格式错误";
            }
        }
        return "正则校验规则不完整";
    },
    /** 枚举类型取值范围检测 */
    value_range__checker: function (config, value) {
        var kit = this.kit;
        var values = config.enumValues;

        if (values === undefined || !kit.isArray(values)) {
            return "未配置枚举取值范围";
        }

        if (kit.isArray(value)) {
            // 多选（数组比较）
            var srcArrayType = kit.checkArrayItemsType(values);
            var targetArrayType = kit.checkArrayItemsType(value);

            if (srcArrayType === "") {
                return "参照数组元素类型不一致";
            }

            if (targetArrayType === "") {
                return "数组元素类型不一致";
            }

            if (srcArrayType !== targetArrayType) {
                return "数组元素类型与参照数组元素类型不一致";
            }

            if (kit.isSubset(values, value)) {
                return "";
            }

            return "参数不符合规范";
        }

        // 单选
        if ((config.type === "enum_numeric" && kit.isNumber(value))
                || (config.type === "enum_string" && kit.isString(value))
                || (config.type === "enum_decimal" && kit.isString(value))) {
            if (kit.contains(values, value)) {
                return "";
            }
            return "参数不符合规范";
        }

        // enum_numeric / enum_decimal 兼容字符串数字
        if ((config.type === "enum_numeric" && kit.isString(value))
                || (config.type === "enum_decimal" && kit.isString(value))) {
            var parsed = Number(value);
            if (!Number.isNaN(parsed) && kit.contains(values, parsed)) {
                return "";
            }
            if (kit.contains(values, value)) {
                return "";
            }
            return "参数不符合规范";
        }

        // enum_decimal 类型传入浮点数时的提示
        if (config.type === "enum_decimal" && kit.isNumber(value)) {
            return "浮点类型请使用字符串参数";
        }

        return "参数不符合规范";
    },
    /** 日期类型校验检测 */
    date__checker: function (config, value) {
        var kit = this.kit;
        if (kit.isNone(value)) {
            return "日期值为空";
        }

        if (typeof value === "string") {
            if (config.type === "date" && !kit.checkDateStr(value)) {
                return "日期值不规范";
            } else if (config.type === "datetime" && !kit.checkDatetimeStr(value)) {
                return "日期值不规范";
            }
        }

        var beginAt = config.beginAt && new Date(config.beginAt);
        var endAt = config.endAt && new Date(config.endAt);
        var valueDate = new Date(value);

        if (kit.isValidDate(beginAt) && kit.isValidDate(endAt)) {
            if (valueDate.getTime() >= beginAt.getTime() && valueDate.getTime() < endAt.getTime()) {
                return "";
            }
            return "日期值校验失败";
        }

        if (kit.isValidDate(beginAt)) {
            if (valueDate.getTime() >= beginAt.getTime()) {
                return "";
            }
            return "日期值校验失败";
        }

        if (kit.isValidDate(endAt)) {
            if (valueDate.getTime() < endAt.getTime()) {
                return "";
            }
            return "日期值校验失败";
        }

        return "日期值校验失败";
    }
};
