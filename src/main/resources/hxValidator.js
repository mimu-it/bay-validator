function testHxValidator(fieldConfigStr, key, valueStr) {
    /**
     * 必须使用字符串传入配置，
     * 不然Object.prototype.toString.call(obj)将返回java对象，而不是js对象
     * @type {never}
     */
    let fieldConfig = (new Function("", "return " + fieldConfigStr ))();
    let value = (new Function("", "return " + valueStr ))();
    let oHxValidator = new HxValidator(fieldConfig);
    return oHxValidator.validate(key, value);
}


function testStringLength(fieldConfigStr, key, valueStr) {
    let fieldConfig = (new Function("", "return " + fieldConfigStr ))();
    let value = (new Function("", "return " + valueStr ))();
    let oHxValidator = new HxValidator(fieldConfig);

    let configJson = (fieldConfig)[key];

    if (!configJson) {
        return "No config, the field is " + key;
    }

    return oHxValidator.string_length__checker(configJson, value);
}


function HxValidator(fieldConfig) {
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
        debug : function(str) {
            //print(str); 不能在页面使用，不然会触发打印功能
        },
        isString : function(obj) {
            return Object.prototype.toString.call(obj) === "[object String]" ;    // [object String]
        },
        isNumber : function(obj) {
            return Object.prototype.toString.call(obj) === "[object Number]" ;    // [object Number]
        },
        isArray: function(obj) {
            return Object.prototype.toString.call(obj) == "[object Array]";
        },
        whatType: function(obj) {
            return Object.prototype.toString.call(obj);
        },
        isJson: function(obj) {
            return (typeof(obj) == "object" && Object.prototype.toString.call(obj).toLowerCase() == "[object object]" && !obj.length);
        },
        isFunction: function(fn) {
            return Object.prototype.toString.call(fn) === '[object Function]';
        },
        replace: function(k, v) {
            if (typeof v === 'function') {
                return Function.prototype.toString.call(v)
            }
            return v
        },
        /** 判断数组中的各项元素的类型，如果存在不同类型，会返回空类型，都是同类型则返回类型值 */
        checkArrayItemsType: function(sourceArr) {
            let arrayType = "";
            for(let i = 0; i < sourceArr.length; i++) {
                let item = sourceArr[i];
                let itemType = Object.prototype.toString.call(item);
                if(arrayType === "") {
                    /** 首个元素为"" */
                    arrayType = itemType;
                }
                else if(arrayType != itemType) {
                    /** 如果一个数组中存在不同类型的元素，则返回"" */
                    return "";
                }
            }
            return arrayType;
        },
        /** targetArr是否是sourceArr的子集 */
        isSubset: function(sourceArr, targetArr){
            return targetArr.every(function(v) {
                return sourceArr.some(function(item){
                    return item === v;
                })
            })
        },
        /** 是否包含 */
        contains: function(sourceArr, targetItem) {
            return sourceArr.some(function(item){
                return item === targetItem;
            })
        },
        /** 是否是中文字符 */
        isChineseCharacter: function(c) {
            return (c.charCodeAt(0) > 255);
        }
    },
    validate: function(key, value) {
        let configJson = (this.fieldConfig)[key];

        if (!configJson) {
            return "No config, the field is " + key;
        }

        this.kit.debug("configJson.type : " + configJson.type);
        if(configJson.type === 'enum_numeric' || configJson.type === 'enum_decimal'
        || configJson.type === 'enum_string') {
            /** 如果是枚举类型取值范围检测 */
            let errorCode = this.value_range__checker.call(this, configJson, value);
            if (errorCode.length != 0) {
                return errorCode;
            }
            return ""
        }

        if(configJson.type === 'string') {
            /** 如果是字符串检测 */
            /** 数值型和字符串型都进行下列两组检测 */
            let checkList = [this.string_length__checker, this.regex__checker];
            for (let i = 0; i < checkList.length; i++) {
                let checker = checkList[i];
                let errorCode = checker.call(this, configJson, value);
                if (errorCode.length != 0) {
                    return errorCode;
                }
            }

            return "";
        }

        if(configJson.type === "decimal") {
            let errorCode = this.decimal_range__checker.call(this, configJson, value);
            if (errorCode.length != 0) {
                return errorCode;
            }
            return "";
        }


        /** 数值型检测 */
        let checkList = [this.range__checker];
        for (let i = 0; i < checkList.length; i++) {
            let checker = checkList[i];
            let errorCode = checker.call(this, configJson, value);
            if (errorCode.length != 0) {
                return errorCode;
            }
        }
        return "";
    },
    range__checker: function(config, value) {
        let numericMin = config.numericMin;
        let numericMax = config.numericMax;

        if(numericMin == undefined || numericMax == undefined) {
            return "数值型大小校验规则不完整";
        }

        let valueNumber = Number(value);
        if (valueNumber >= numericMin && valueNumber < numericMax) {
            return "";
        }

        return "数值型大小校验规则不符合规则";
    },
    decimal_range__checker: function(config, value) {
        let numericMin = config.decimalMin;
        let numericMax = config.decimalMax;

        if(numericMin == undefined || numericMax == undefined) {
            return "数值型大小校验规则不完整";
        }

        let valueNumber = Number(value);
        if (valueNumber >= numericMin && valueNumber < numericMax) {
            return "";
        }

        return "数值型大小校验规则不符合规则";
    },
    /** 字符串长度检测 */
    string_length__checker: function(config, value) {
        let kit = this.kit;
        let stringCharset = config.stringCharset;
        let stringLengthMin = config.stringLengthMin;
        let stringLengthMax = config.stringLengthMax;

        if(stringCharset == undefined || stringLengthMin == undefined
        || stringLengthMax == undefined) {
            kit.debug("stringCharset: " + stringCharset + ", stringLengthMin: " + stringLengthMin
            + ", stringLengthMax:" + stringLengthMax);
            return "字符串长度校验规则不完整";
        }

        /** 英文字母：
         *     字节数 : 1;编码：GB2312
         *     字节数 : 1;编码：GBK
         *     字节数 : 1;编码：GB18030
         *     字节数 : 1;编码：ISO-8859-1
         *     字节数 : 1;编码：UTF-8
         *     字节数 : 4;编码：UTF-16
         *     字节数 : 2;编码：UTF-16BE
         *     字节数 : 2;编码：UTF-16LE
         *  中文汉字：
         *     字节数 : 2;编码：GB2312
         *     字节数 : 2;编码：GBK
         *     字节数 : 2;编码：GB18030
         *     字节数 : 1;编码：ISO-8859-1
         *     字节数 : 3;编码：UTF-8
         *     字节数 : 4;编码：UTF-16
         *     字节数 : 2;编码：UTF-16BE
         *     字节数 : 2;编码：UTF-16LE
         */
        let strLength = 0;
        for(let i = 0; i < value.length; i++) {
            let c = value.charAt(i);
            if(!kit.isChineseCharacter(c)) {
                /** 非中文 */
                if(kit.contains(["GB2312", "GBK", "GB18030", "ISO-8859-1", "UTF-8", "utf8"], stringCharset)) {
                    strLength ++;
                }
                else if(kit.contains(["UTF-16"], stringCharset)) {
                    strLength = strLength + 4;
                }
                else if(kit.contains(["UTF-16BE", "UTF-16LE"], stringCharset)) {
                    strLength = strLength + 2;
                }

                continue;
            }
            else {
                /** 中文 */
                if(kit.contains(["GB2312", "GBK", "GB18030", "UTF-16BE", "UTF-16LE"], stringCharset)) {
                    strLength = strLength + 2;
                }
                else if(kit.contains(["ISO-8859-1"], stringCharset)) {
                    strLength ++;
                }
                else if(kit.contains(["UTF-8", "utf8"], stringCharset)) {
                    strLength = strLength + 3;
                }
                else if(kit.contains(["UTF-16"], stringCharset)) {
                    strLength = strLength + 4;
                }

                continue;
            }

            throw "字符类型不明，字符:" + c;
        }

        kit.debug("stringLengthMin: " + stringLengthMin);
        kit.debug("stringLengthMax: " + stringLengthMax);
        kit.debug("strLength: " + strLength);
        kit.debug("result: " + (stringLengthMin <= strLength && strLength < stringLengthMax) );
        if(stringLengthMin <= strLength && strLength < stringLengthMax) {
            return "";
        }
        return "参数字符长度不符合规则";
    },
    /** 枚举类型检测取值范围 */
    regex__checker: function(config, value) {
        let kit = this.kit;
        let rule = config.regexStr;
        if (rule != undefined && kit.whatType(rule) == "[object String]") {
            /* RegExp适用于字符串表达式
             * eg. var re = new RegExp("\\w+");
             *     等价于：
             *     var re = /\w+/;
             */
            let valueStr = value + "";
            let reg = new RegExp(rule);
            if (!reg.test(valueStr)) {
                return "参数字符不符合规则";
            }
            return "";
        }
        return "正则校验规则不完整";
    },
    /** 枚举类型检测取值范围 */
    value_range__checker: function(config, value) {
        this.kit.debug("step in value_range__checker");
        let kit = this.kit;
        let values = config.enumValues;

        kit.debug("kit.isArray(values) : " + kit.isArray(values));
        if (values != undefined && kit.isArray(values)) {
            kit.debug("isArray : " + kit.isArray(value));
            if(kit.isArray(value)) {

                /**
                 * 如果是数组比较，就是多选的情况
                 * @type {*|string}
                 */
                let srcArrayType = kit.checkArrayItemsType(values);
                let targetArrayType = kit.checkArrayItemsType(value);

                if(srcArrayType === "") {
                    return "参照数组元素类型不一致";
                }

                if(targetArrayType === "") {
                    return "数组元素类型不一致";
                }

                if(srcArrayType != targetArrayType) {
                    return "数组元素类型与参照数组元素类型不一致，数组类型为: " + targetArrayType
                    + ", 参照数组元素类型: " + srcArrayType;
                }

                /* 如果取值是多选 */
                if(kit.isSubset(values, value)) {
                    return "";
                }
            }
            else {
                kit.debug("kit.isNumber(value) : " + kit.isNumber(value));
                if( (config.type === 'enum_numeric' && kit.isNumber(value))
                || (config.type === 'enum_string' && kit.isString(value))
                || (config.type === 'enum_decimal' && kit.isString(value))) {
                    if(this.kit.contains(values, value)) {
                        return "";
                    }
                }
                else if(config.type === 'enum_decimal' && kit.isNumber(value)) {
                    /** 考虑到js的浮点类型的精度丢失问题，所以统一使用字符串比较 */
                    return "浮点类型请使用字符串参数";
                }
            }

            return "参数不符合规范";
        }

        return "未配置枚举取值范围";
    }
}
