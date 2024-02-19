BayValidator的使用



# maven本地引入

在maven工程目录src/main/resources/validator中放入bay-validator-1.8.jar文件

在pom.xml中引入。

```xml
<!-- 使用自定义的验证组件 -->
<dependency>
    <groupId>com.baymax</groupId>
    <artifactId>bay-validator</artifactId>
    <version>1.8</version>
    <scope>system</scope>
    <systemPath>${basedir}/src/main/resources/validator/bay-validator-1.8.jar</systemPath>
</dependency>
```



# 部署字段规则配置文件

字段规则配置文件分为三种：

## regex_dict.yml

regex_dict.yml 文件是通过键值的方式保存常用的字符串正则表达式规则，可以对其进行修改，积累更多的规则，有利于提升开发速度。

例如：

```yaml
any_string: .*
password: ^(?![0-9]+$)(?![a-z]+$)(?![A-Z]+$)(?!([^(0-9a-zA-Z)])+$).{6,18}$
only_chinese_and_english_and_underline: ^[\u4e00-\u9fa5_a-zA-Z0-9]+$
real_name: ^[\u4E00-\u9FA5]{2,6}$
positive_integer: ^[1-9]\d*$
phone_number: ^1[3|4|5|7|8|9][0-9]{9}$
id_card: ^[1-9]\d{5}(18|19|([23]\d))\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$
simple_date: ^\d{4}(\-|\/|\.)\d{1,2}\1\d{1,2}$
date_time: ^[1-9]\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])\s+(20|21|22|23|[0-1]\d):[0-5]\d:[0-5]\d$
sfm_po: ^P\.O\.[A-Z0-9]{5,}
enum_active_status: ["active", "inactive"]
enum_gender: ["male", "female"]
```

在工程中，应先在resources目录(自行规划保存的文件夹)下新建此文件。

## value_rules_common.yml

value_rules_common.yml 文件，是一个公共字段规则，如每个表都有id字段，那么就可以在这里统一配置。

例如：

```yaml
_common:
  id:
    numeric_max: 9999999
    numeric_min: 0
    type: numeric
  is_deleted:
    numeric_max: 20
    numeric_min: 0
    type: numeric
  creator:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 31
    type: string
  created_at:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 19
    type: string
  modifier:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 31
    type: string
  updated_at:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 19
    type: string
  version:
    numeric_max: 9999999
    numeric_min: 0
    type: numeric
```

在工程中，应先在resources目录(自行规划保存的文件夹)下新建此文件。

## value_rules.yml

value_rules.yml 文件，是每个表字段的规则，可以使用代码生成方式生成默认规则，这些默认规则来源于读取对应数据表的schema的配置，可根据实际情况再进行相应调整。

例如：

```yml
sa_stock_price:
  opening_price:
    decimal_min: 0.0
    decimal_max: 8
    type: decimal
  trade_volume:
    numeric_max: 19
    numeric_min: 0
    type: numeric
  peak_price:
    decimal_min: 0.0
    decimal_max: 8
    type: decimal
  stock_name:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 8
    type: string
  turnover_rate:
    decimal_min: 0.0
    decimal_max: 3
    type: decimal
  closing_price:
    decimal_min: 0.0
    decimal_max: 8
    type: decimal
  stock_code:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 8
    type: string
  stock_name_abbreviation:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 8
    type: string
  trade_date:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 10
    type: string
  bottom_price:
    decimal_min: 0.0
    decimal_max: 8
    type: decimal
sa_stock_code:
  stock_name:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 8
    type: string
  stock_code:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 8
    type: string
sa_admin:
  password:
    string_charset: utf8
    string_length_min: 6
    string_regex_key: password
    string_length_max: 18
    type: string
  status:
    type: enum_string
    enum_values: enum_active_status
  admin_name:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 64
    type: string
  email:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 64
    type: string
sa_customer:
  gender:
    type: enum_string
    enum_values: enum_gender
  status:
    type: enum_string
    enum_values: enum_active_status
  sn:
    numeric_max: 19
    numeric_min: 0
    type: numeric
  user_name:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 64
    type: string
  real_name:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 64
    type: string
  mobile_number:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 32
    type: string
  open_platform:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 32
    type: string
  open_id:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 32
    type: string
  session_key:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 32
    type: string
  union_id:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 32
    type: string
  country:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 16
    type: string
  province:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 16
    type: string
  city:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 16
    type: string
  avatar:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 128
    type: string
  history:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 15
    type: string
sa_stock_abbreviation:
  stock_name:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 8
    type: string
  stock_name_abbreviation:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 8
    type: string
sa_options:
  option_value:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 1023
    type: string
  domain:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 64
    type: string
  option_name:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 64
    type: string
  comment:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 1023
    type: string
```



# ValueEnumRange

ValueEnumRange是用于保存枚举字段的值，这个值跟 value_rules.yml 中设置的字段的枚举值保持一致，其内容是代码生成的，生成的依据就是value_rules.yml中的枚举取值

示例：

```java
public final class ValueEnumRange {

  public static final class sa_stock_price {}

  public static final class sa_stock_code {}

  public static final class sa_admin {
    public static enum status {
      STRING_active(new String("active")),
      STRING_inactive(new String("inactive"));

      private final String value;

      status(final String newValue) {
        value = newValue;
      }

      public String val() {
        return value;
      }
    }
  }

  public static final class sa_customer {
    public static enum gender {
      STRING_male(new String("male")),
      STRING_female(new String("female"));

      private final String value;

      gender(final String newValue) {
        value = newValue;
      }

      public String val() {
        return value;
      }
    }

    public static enum status {
      STRING_active(new String("active")),
      STRING_inactive(new String("inactive"));

      private final String value;

      status(final String newValue) {
        value = newValue;
      }

      public String val() {
        return value;
      }
    }
  }

  public static final class sa_stock_abbreviation {}

  public static final class sa_options {}
}
```



# 代码生成

## 生成ValueEnumRange类，及value_rules.yml

```java
HxValidator.Generator.create().bindToDatabase("mysql", dataSource, databaseName)
                  .valueEnumRangeModuleTargetPath(valueEnumRangeModuleTargetPath, basePackageName + ".validator")
                  .valueRuleModuleTargetPath(valueRuleModuleTargetPath, "validator")
                  .userIgnoreKeys(new HashSet<String>() {{
                        add("id");
                        add("version");
                        add("is_deleted");
                        add("modifier");
                        add("creator");
                        add("created_at");
                        add("updated_at");
                    }}, true)
                    .generate();
```

dbType 可设置mysql、oracle

dataSource就是工程目前链接数据库的数据源

```java
DataSourceBuilder datasourcebuilder = DataSourceBuilder.create();
datasourcebuilder.driverClassName(driverClass);
datasourcebuilder.url(jdbcUrl);
datasourcebuilder.username(username);
datasourcebuilder.password(password);
DataSource dataSource = datasourcebuilder.build();
```



- databaseName 就是数据库名

- exceptTables 可以设置排除某些表的代码生成

- valueRuleModuleTargetPath  是value_rules.yml文件的生成根路径

- valueRulesDirectory value_rules.yml文件的生成根路径下可以设置这值，代表子路径

- valueEnumRangeModuleTargetPath 是ValueEnumRange.java的生成路径

- packageName 是ValueEnumRange.java的包名

- userIgnoreKeys 可以设置忽略字段，不生成对应字段的规则

- customUseSnake 是设置这个忽略字段是下划线格式，还是驼峰格式




每次生成value_rules.yml时，会先备份原value_rules.yml，备份方式是在文件名后加一串序号，然后读取原value_rules.yml的配置来作为新的value_rules.yml的依据。





## hxValidator.js的生成

hxValidator.js 是用于在前端使用字段规则的方法，前端从后端获取字段验证规则，使用hxValidator.js中的方法加载这些规则来验证前端字段是否符合要求。

示例代码如下：

```java
public void publishJs() throws FileNotFoundException {
        /** basePath: /Volumes/HD-FOR-MAC/DEV_ENV/projects/webApp/ideaProjects/qy-oa-parent/qy-oa-api/target/classes*/
        final File basePath = new File(ResourceUtils.getURL("classpath:").getPath());
        Path webappPath = Paths.get(basePath.getPath(), "..", "..", "src", "main", "webapp");
        Path jsPublicDirectory = Paths.get(webappPath.toString(), "js", hxValidator);

        File file = new File(jsPublicDirectory.toString());
        if(!file.exists()) {
            file.mkdirs();
        }

        /**
         * bayValidator中自带了 hxValidator.js
         */
        String jsFileName = hxValidator + ".js";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(jsFileName);
             OutputStream out = new FileOutputStream(new File(file.getPath() + File.separator + jsFileName))){
            byte[] buffer = new byte[128];
            int len;

            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        file = new File(Paths.get(jsPublicDirectory.toString(), jsFileName).toString());
        if(!file.exists()) {
            throw new IllegalStateException("Publish hxValidator.js failed.");
        }
    }
```

目的就是在工程的 src/main/webapp 目录下生成 hxValidator.js 文件

前端jsp或者vue页面可以引入这段js



# 在springboot中使用

启动后立即初始化HxValidator

```java
@Component
@Order(1)
public class AppStartedCallback implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("App started. preparing data.");

        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)
                .commonRules("validator/value_rules_common.yml")
                .rules("validator/value_rules.yml")
                .regexDict("validator/regex_dict.yml")
                .ignoreKeys(BoneApiApplication.ignoreKeys)
                .keyMode(KeyMode.snake)
                .init();
    }
}
```

value_rules_common.yml, value_rules.yml, regex_dict.yml 都放置在src/main/resources/validator文件夹中。



假如需要校验一个Pojo类，可以这样使用

```java
List<String> illegalProps = HxValidator.builder().with(item).bind(tableName)
        .nullableKeys(nullableKeys.toArray(new String[0])).validate();

if(!illegalProps.isEmpty()) {
    return RespResult.failure(ErrorCode.illegal_argument.name(), illegalProps);
}
```

illegalProps 中保存了所有验证失败的属性名。



也可以这样使用, 验证属性不对会抛出异常

```java
HxValidator.builder().validate("student.float_card", 3.11f)
        .validateIfNonnull("student.gender", "male");
```





# 新建一个controller类为前端提供验证规则

通过访问类似如下路径

```
http://localhost:8080/validator/rules?fields=[%22estate.id%22,%22estate.version%22,%22estate.name%22]
```

可以得到如下规则

```json
{"state":true,"data":{"estate.id":{"fieldKey":"","type":"numeric","numericMin":0,"numericMax":9999999},"estate.name":{"fieldKey":"","type":"string","stringRegexKey":"any_string","stringLengthMin":1,"stringLengthMax":7,"regexStr":".*"},"estate.version":{"fieldKey":"","type":"numeric","numericMin":0,"numericMax":9999999}}}
```

修改 estate.name 的规则配置，因为是string，所以增加stringCharset属性，此属性在验证长度的时候是必须的

```yaml
qy_estate:
  name:
    string_length_min: 1
    string_regex_key: any_string
    string_length_max: 7
    type: string
    string_charset: utf8
```

此时请求返回的响应json是

```json
{
    "state": true,
    "data": {
        "estate.id": {
            "fieldKey": "",
            "type": "numeric",
            "numericMin": 0,
            "numericMax": 9999999
        },
        "estate.name": {
            "fieldKey": "",
            "type": "string",
            "stringCharset": "utf8",
            "stringRegexKey": "any_string",
            "stringLengthMin": 1,
            "stringLengthMax": 7,
            "regexStr": ".*"
        },
        "estate.version": {
            "fieldKey": "",
            "type": "numeric",
            "numericMin": 0,
            "numericMax": 9999999
        }
    }
}
```



ValidatorController.java 参考代码如下：

```java
package com.baymax.bone.api.controller;

import cn.hutool.core.util.CharUtil;
import com.baymax.bone.api.url.ControllerUrl;
import com.baymax.bone.common.constant.Const;
import com.baymax.bone.common.controller.RespResult;
import com.baymax.bone.common.exception.ErrorCode;
import com.baymax.bone.common.i18n.MessageUtils;
import com.baymax.bone.common.util.JsonUtil;
import com.baymax.validator.engine.ValidatorEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xiao.hu
 * @date 2022-03-22
 * @apiNote
 */
@RestController
@RequestMapping(ControllerUrl.VALIDATOR)
public class ValidatorController {

    @Value("${validator.namespace-prefix}")
    String validatorNamespacePrefix;

    String validatorCommonPrefix = "_common";

    public static final class Url {
        public static final String SELECT_OPTIONS = "/select-options";
        public static final String RULES = "/rules";
    }

    /**
     * 获取 select 下拉选项
     * @param fields
     * @return
     */
    @GetMapping(Url.SELECT_OPTIONS)
    @ResponseBody
    public RespResult selectOptions(@RequestParam(name=Const.Param.FIELDS) String fields) {
        Map<String, List<Map<String, Object>>> optionsMap = new HashMap<>(4);

        List<String> parseArray = JsonUtil.forCamelKey().readValue(fields, new TypeReference<List<String>>() {});
        for (String fieldKey : parseArray) {
            List<Map<String, Object>> optionList = new ArrayList<>();
            List<Object> values = getEnumValues(fieldKey);
            if(values != null) {
                for(Object v : values) {
                    Map<String, Object> options = new HashMap<>(2);
                    options.put(Const.SelectOptions.LABEL, MessageUtils.get(String.valueOf(v)));
                    options.put(Const.SelectOptions.VALUE, v);
                    optionList.add(options);
                }
            }
            optionsMap.put(fieldKey, optionList);
        }

        return RespResult.success(optionsMap);
    }

    /**
     * 获取枚举值
     * @param key
     * @return
     */
    private List<Object> getEnumValues(String key) {
        return ValidatorEngine.INSTANCE.getEnumValues(this.rectifyFieldKey(key));
    }

    /**
     * 获取校验规则
     * @param fields
     * @return
     */
    @GetMapping(Url.RULES)
    @ResponseBody
    public RespResult rules(@RequestParam(name=Const.Param.FIELDS) String fields) {
        if (StringUtils.isBlank(fields)) {
            return RespResult.failure(ErrorCode.illegal_argument.name(), fields);
        }

        List<String> parseArray = JsonUtil.forCamelKey().readValue(fields, new TypeReference<List<String>>() {});

        Map<String, Map<String, Object>> ruleMap = new HashMap<>(parseArray.size());
        for (String fieldKey : parseArray) {
            String rectifyFieldKey = this.rectifyFieldKey(fieldKey);
            Map<String, Object> rule = ValidatorEngine.INSTANCE.getFieldValidatorRulesJson(rectifyFieldKey);
            if(rule == null) {
                /** 尝试使用_common */
                String commonFieldKey = this.rectifyCommonFieldKey(fieldKey);
                rule = ValidatorEngine.INSTANCE.getFieldValidatorRulesJson(commonFieldKey);
            }

            ruleMap.put(fieldKey, rule);
        }
        return RespResult.success(ruleMap);
    }

    /**
     * 格式化需被校验的字段值
     * @param fieldKey
     * @return
     */
    private String rectifyFieldKey(String fieldKey) {
        String rectifyFieldKey = fieldKey;
        /*如id，version这类通用值，则可判断是否存在"."，这样可以自动添加_common作为前缀*/
        if(fieldKey.indexOf(CharUtil.DOT) == -1) {
            rectifyFieldKey = validatorCommonPrefix + CharUtil.DOT + rectifyFieldKey;
        }
        else {
            /*如果不是ld_前缀，则尝试加上，以等于表名，因为valueRange的生成策略其中就是表名开头*/
            if(!rectifyFieldKey.startsWith(validatorNamespacePrefix)) {
                rectifyFieldKey = validatorNamespacePrefix + rectifyFieldKey;
            }
        }
        return rectifyFieldKey;
    }

    /**
     * 使用 _common 前缀格式化校验字段
     * @param fieldKey
     * @return
     */
    private String rectifyCommonFieldKey(String fieldKey) {
        int dotIndex = fieldKey.indexOf(CharUtil.DOT);
        if(dotIndex != -1) {
            fieldKey = validatorCommonPrefix + CharUtil.DOT +fieldKey.substring(dotIndex + 1);
        }
        else {
            fieldKey = validatorNamespacePrefix + CharUtil.DOT + fieldKey;
        }
        return fieldKey;
    }
}
```



# 配合前端vue的使用参考示例

## 引入hxValidator.js

在vue工程 public/index.html 中引入hxValidator.js,  hxValidator.js保存在目录public/js/hxValidator/中

```html
<!DOCTYPE html>
<html lang="">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width,initial-scale=1.0">
    <link rel="icon" href="<%= BASE_URL %>favicon.ico">
    <title><%= htmlWebpackPlugin.options.title %></title>
    <script src="<%= BASE_URL %>js/hxValidator/hxValidator.js" type="text/javascript"></script>
  </head>
  <body>
    <noscript>
      <strong>We're sorry but <%= htmlWebpackPlugin.options.title %> doesn't work properly without JavaScript enabled. Please enable it to continue.</strong>
    </noscript>
    <div id="app"></div>
    <!-- built files will be auto injected -->
  </body>
</html>
```



## 封装一个参考工具 validator.js

```javascript
import {API} from '@/axios/api';
import Util from '@/util/util.js';

/**
 * 加载校验规则
 */
function loadValidatorRules(hxValidatorHolder, namespace, form, special, callback) {
    hxValidatorHolder.fields = hxValidatorHolder.fields || [];
    for(let item in form) {
        if(!Util.isNone(special) && !Util.isNone(special[item])) {
            hxValidatorHolder.fields.push(special[item]);
        }
        else {
            hxValidatorHolder.fields.push(namespace + item);
        }
    }

    /*创建的时候开始加载校验配置*/
    API.getValidatorRules({
        fields: JSON.stringify(hxValidatorHolder.fields)
    }).then((response) => {
        let fieldConfig = response.data;
        /**
         * index.html页面已经引入
         * <script src="js/hxValidator/hxValidator.js">
         */
        hxValidatorHolder.instance = new HxValidator(fieldConfig);

        if(Util.isFunction(callback)) {
            callback();
        }

    }).catch((error) => {
        console.log(error);
    });
}

/**
 * 校验的通用方法
 */
function validateCommon(oHxValidator, validatorKey, rule, value, callback) {
    console.log("validate %s", validatorKey);
    let errorCode = oHxValidator.validate(validatorKey, value);
    if (!Util.isFunction(callback)) {
        return errorCode;
    }

    if (errorCode) {
        callback(new Error(errorCode));
    }
}

// 暴露出这些属性和方法
export default {
    loadValidatorRules,
    validateCommon
}
```



## 使用示例

以封装 el-dialog 为例

```html
<template>
    <el-dialog :title="title"
               :visible.sync="dialogVisible"
               modal-append-to-body
               :close-on-click-modal="false"
               @close="onClose">
        <div>
            <el-form :model="formIU" ref="formIU" :rules="validatorRules" size="small" v-if="formReady">
                <el-form-item :label="$t('message.term.name')"
                              :label-width="formLabelWidth" prop="name">
                    <el-input v-model="formIU.name" autocomplete="off"></el-input>
                </el-form-item>
            </el-form>
        </div>
    </el-dialog>
</template>
<script>
    import {API} from '@/axios/api';
    import Validator from '@/util/validator.js';

    export default {
        name: 'EstateIUDialog',
        components: {

        },
        props: {
            visible: {
                type: Boolean,
                default() {
                    return false;
                }
            },
            title: {
                type: String,
                default() {
                    return "";
                }
            }
        },
        computed: {
            dialogVisible: {
                get: function() {
                    return this.visible;
                },
                set: function(val) {
                    /** 配合 :visible.sync 使用 */
                    this.$emit('update:visible', val);
                }
            },
            /**
             * 与form进行绑定，实现对输入参数的校验
             */
            validatorRules() {
                let rules = {};
                this.oHxValidator.fields.forEach((item) => {
                    let key = item;
                    let dotIndex = item.indexOf(".");
                    if (dotIndex > 0) {
                        key = item.substring(item.indexOf(".") + 1);
                    }

                    let requiredRule = [
                        {required: true, message: this.$t('message.term.required'), trigger: 'blur'},
                        {
                            validator: (rule, value, callback) => {
                                Validator.validateCommon(this.oHxValidator.instance,
                                    item, rule, value, callback)
                            },
                            trigger: 'blur'
                        }
                    ];

                    rules[key] = requiredRule;
                })

                return rules;
            }
        },
        data() {
            return {
                namespace: "estate.",
                formLabelWidth: "100",
                /** 新增及编辑对话框对应的表单 */
                formReady: false,
                formIU: {
                    id: '',
                    version: '',
                    name: ''
                },
                /** 验证相关 */
                oHxValidator: {
                    instance: null,
                    fields: []
                }
            }
        },
        watch: {

        },
        created() {

        },
        mounted() {
            this.$nextTick(() => {
                /* 恢复数据 */
                Validator.loadValidatorRules(this.oHxValidator, this.namespace, this.formIU, null, () => {
                    /** 校验规则加载成功才显示form */
                    this.formReady = true;
                })
            });
        },
        methods: {
            onClose() {
                this.$emit("onClose");
            }
        }
    }
</script>
```