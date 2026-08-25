# bay-validator

基于 YAML 配置的校验引擎，支持 Java Bean 校验、前端 JS 校验以及代码生成。

## 目录

- [快速开始](#快速开始)
- [校验规则配置](#校验规则配置)
- [从数据库自动生成校验规则](#从数据库自动生成校验规则)
- [Spring Boot 集成](#spring-boot-集成)
- [前端 Vue 集成](#前端-vue-集成)
- [代码生成](#代码生成)
- [校验规则类型](#校验规则类型)
- [高级用法](#高级用法)

---

## 快速开始

### 1. 引入依赖

#### 方式一：Maven 中央仓库（推荐）

```xml
<dependency>
    <groupId>com.baymax</groupId>
    <artifactId>bay-validator</artifactId>
    <version>1.8-SNAPSHOT</version>
</dependency>
```


```
mvn install:install-file \
                  -Dfile=bay-validator-2.0.jar \
                  -DgroupId=com.baymax \
                  -DartifactId=bay-validator \
                  -Dversion=2.0 \
                  -Dpackaging=jar
```


#### 方式二：本地 jar 包

如果项目不方便使用 Maven 仓库，也可以直接用编译好的 jar 包：

**编译 jar 包：**

```bash
# 在 bay-validator 项目根目录执行
mvn clean package -DskipTests

# 编译产物在 target/bay-validator-1.8-SNAPSHOT.jar
```

**在项目中引用：**

将 `bay-validator-1.8-SNAPSHOT.jar` 放入项目的 `lib/` 目录，然后在 `pom.xml` 中添加 system scope 依赖：

```xml
<dependency>
    <groupId>com.baymax</groupId>
    <artifactId>bay-validator</artifactId>
    <version>1.8-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/bay-validator-1.8-SNAPSHOT.jar</systemPath>
</dependency>
```

> **注意**：`scope=system` 在打包时需要额外配置 spring-boot-maven-plugin 的 `includeSystemScope`，详见下方。

或者先安装到本地 Maven 仓库，再以普通依赖引用：

```bash
mvn install:install-file \
    -Dfile=target/bay-validator-1.8-SNAPSHOT.jar \
    -DgroupId=com.baymax \
    -DartifactId=bay-validator \
    -Dversion=1.8-SNAPSHOT \
    -Dpackaging=jar
```

然后按[方式一](#方式一maven-中央仓库推荐)正常引用即可。

##### Spring Boot 打包时包含 system scope 依赖

如果使用 `scope=system`，Spring Boot 默认不会将 system scope 的依赖打入 fat jar，需要在 `pom.xml` 中显式配置：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <includeSystemScope>true</includeSystemScope>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2. 准备配置文件

在 `resources/` 目录下创建 `rules/` 目录，按表名分文件存放规则：

```yaml
# resources/rules/student.yml
student:
  id:
    type: numeric
    numeric_min: 1
    numeric_max: 1000000
  name:
    type: string
    string_regex_key: only_chinese_and_english_and_underline
    string_length_min: 2
    string_length_max: 50
    string_charset: utf8
  gender:
    type: enum_string
    enum_values:
      - male
      - female
  phone:
    type: string
    string_regex_key: phone_number
    string_length_min: 11
    string_length_max: 11
  money:
    type: decimal
    decimal_min: 0.00
    decimal_max: 999999.99
  birthday:
    type: date
    begin_at: 1900-01-01
    end_at: 2100-12-31
  created_at:
    type: datetime
    begin_at: 2020-01-01 00:00:00
```

```yaml
# resources/rules/order.yml
order:
  status:
    type: enum_numeric
    enum_values:
      - 1
      - 2
      - 3
  total_amount:
    type: decimal
    decimal_min: 0.01
    decimal_max: 1000000.00
```

```yaml
# resources/common_dict.yml
# 内置正则规则键（部分列举），完整列表见源码 common_dict.yml
any_string: .*
only_chinese_and_english_and_underline: ^[\u4e00-\u9fa5_a-zA-Z0-9]+$
positive_integer: ^[1-9]\d*$
phone_number: ^1[3|4|5|7|8][0-9]{9}$
e_mail: ^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$
id_card: ^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$
url: ^https?://[\w\-]+(\.[\w\-]+)+[/#?]?.*$
ipv4: ^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$
```

### 3. 初始化并校验

```java
// 从 rules 目录加载（推荐方式）
HxValidator.Engine.create()
    .rulesDir("rules")
    .ruleDict("common_dict.yml")
    .init();

// 校验单个字段
boolean ok = HxValidator.builder()
    .validate("student.phone", "13800138000");
// ok = true

// 单字段校验（不抛异常，直接返回 boolean）
boolean valid = ValidatorEngine.INSTANCE.validate("student.id", 100);
```

---

## Spring Boot 集成

### 1. 配置类

```java
@Configuration
public class ValidatorConfig {

    @PostConstruct
    public void initValidator() {
        HxValidator.Engine.create()
                .dbType(DataBaseType.mysql)          // mysql / oracle
                .rulesDir("rules")                    // 按表分文件的规则目录
                .ruleDict("common_dict.yml")           // 正则词典
                .ignoreKeys(new HashSet<>(Arrays.asList(
                    "id", "version", "is_deleted",
                    "gmt_created", "gmt_modified")))
                .keyMode(KeyMode.snake)               // 驼峰→下划线自动转换
                .init();
    }
}
```

### 2. Controller 层校验

```java
@RestController
@RequestMapping("/student")
public class StudentController {

    @PostMapping
    public Result createStudent(@RequestBody StudentVO vo) {
        // 校验整个 Bean
        List<String> errors = HxValidator.builder()
                .with(vo)
                .bind("student")
                .nullableKeys(new String[]{"remark"})
                .validate();

        if (!errors.isEmpty()) {
            return Result.error("校验失败: " + String.join(", ", errors));
        }
        // 业务逻辑...
    }

    @GetMapping("/validate")
    public Result validateField(@RequestParam String field,
                                @RequestParam String value) {
        try {
            HxValidator.builder().validate(field, value);
            return Result.ok();
        } catch (IllegalValueException e) {
            return Result.error("字段 " + field + " 校验失败");
        }
    }
}
```

### 3. Service 层校验

```java
@Service
public class StudentService {

    public void updateStudent(StudentPO po) {
        // 链式校验
        HxValidator.builder()
            .validate("student.id", po.getId())
            .validate("student.name", po.getName())
            .validateIfNonnull("student.phone", po.getPhone())
            .validateIfNonnull("student.money", po.getMoney());
    }
}
```

### 4. 获取规则 JSON（供前端使用）

```java
@RestController
@RequestMapping("/validator")
public class ValidatorController {

    @GetMapping("/rules")
    public Result getRules(@RequestParam String table) {
        // 该接口返回前端 hxValidator.js 所需要的配置 JSON
        // 前端根据这些配置在浏览器端做预校验
        Map<String, Object> rules = new HashMap<>();

        // 获取指定表的所有字段规则
        ValidatorEngine engine = ValidatorEngine.INSTANCE;
        engine.valueRulesMap.get(table).keySet().forEach(field -> {
            String fieldKey = table + "." + field;
            Map<String, Object> rule = engine.getFieldValidatorRulesJson(fieldKey);
            if (rule != null) {
                rules.put(field, rule);
            }
        });

        return Result.ok(rules);
    }
}
```

---

## 前端 Vue 集成

### 1. 将 hxValidator.js 放入项目

将 `resources/hxValidator.js` 复制到 Vue 项目的 `src/utils/` 目录下。

### 2. 封装校验工具

```javascript
// src/utils/validator.js
import { HxValidator } from './hxValidator';

// 缓存后端获取的规则配置
let ruleCache = null;
let hxValidator = null;

/**
 * 从后端加载校验规则
 */
export async function loadRules(tableName) {
    const res = await fetch(`/api/validator/rules?table=${tableName}`);
    const data = await res.json();

    // 构造前端需要的 fieldConfig 结构
    const fieldConfig = {};
    Object.entries(data).forEach(([field, rule]) => {
        fieldConfig[`${tableName}.${field}`] = rule;
    });

    ruleCache = fieldConfig;
    hxValidator = new HxValidator(fieldConfig);
}

/**
 * 校验单个字段
 * @param {string} fieldKey - 如 "student.name"
 * @param {*} value - 待校验的值
 * @returns {string} 空字符串表示通过，非空为错误信息
 */
export function validateField(fieldKey, value) {
    if (!hxValidator) {
        return "校验规则未加载";
    }
    return hxValidator.validate(fieldKey, value);
}

/**
 * 校验整个表单
 * @param {string} tableName - 表名
 * @param {Object} formData - 表单数据
 * @returns {Object} { valid: boolean, errors: { [field]: string } }
 */
export function validateForm(tableName, formData) {
    const errors = {};
    Object.entries(formData).forEach(([field, value]) => {
        const fieldKey = `${tableName}.${field}`;
        const error = validateField(fieldKey, value);
        if (error) {
            errors[field] = error;
        }
    });
    return {
        valid: Object.keys(errors).length === 0,
        errors
    };
}
```

### 3. Vue 组件中使用

```vue
<template>
  <el-form :model="form" :rules="rules" ref="formRef">
    <el-form-item label="姓名" prop="name">
      <el-input v-model="form.name" />
    </el-form-item>
    <el-form-item label="手机号" prop="phone">
      <el-input v-model="form.phone" />
    </el-form-item>
    <el-form-item label="性别" prop="gender">
      <el-select v-model="form.gender">
        <el-option label="男" value="male" />
        <el-option label="女" value="female" />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button @click="submit">提交</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { loadRules, validateForm } from '@/utils/validator';

const form = reactive({
  name: '',
  phone: '',
  gender: '',
  money: ''
});

const formRef = ref(null);
const rules = reactive({});

onMounted(async () => {
  await loadRules('student');

  // 构造前端校验规则（Element Plus 格式）
  // 这里可以直接使用 hxValidator.js 中 validateField 做自定义校验
  rules.name = [{
    validator: (rule, value, callback) => {
      const error = validateField('student.name', value);
      if (error) callback(new Error(error));
      else callback();
    },
    trigger: 'blur'
  }];
  rules.phone = [{
    validator: (rule, value, callback) => {
      const error = validateField('student.phone', value);
      if (error) callback(new Error(error));
      else callback();
    },
    trigger: 'blur'
  }];
});

function submit() {
  // 前端预校验
  const { valid, errors } = validateForm('student', form);
  if (!valid) {
    console.log('校验失败:', errors);
    return;
  }
  // 提交到后端...
}
</script>
```

### 4. 使用 `testHxValidator` 在纯 JS 中校验

```javascript
import { testHxValidator } from '@/utils/hxValidator';

// 当后端直接返回 JSON 字符串时
const configStr = JSON.stringify({
    "student.phone": {
        "type": "string",
        "stringLengthMin": 11,
        "stringLengthMax": 11,
        "stringCharset": "utf8",
        "regexStr": "^1[3|4|5|7|8][0-9]{9}$",
        "lengthMode": "byte"
    }
});

const result = testHxValidator(configStr, "student.phone", '"13800138000"');
console.log(result); // ""（空字符串表示通过）
```

### 5. 关于 `lengthMode`

校验规则 JSON 中的 `lengthMode` 字段指示前端使用哪种长度计算方式：

| 值 | 含义 | 示例 |
|----|------|------|
| `"char"` | 按字符数计算 | `student.description`（无 charset） |
| `"byte"` | 按字节数计算 | `student.name`（charset=utf8） |

```javascript
// lengthMode="char" 时：直接使用字符串长度
strLength = value.length;

// lengthMode="byte" 时：逐字符计算编码后字节数
// 使用 _byteLength() 方法，utf8 下优先使用 TextEncoder
```

---

## 从数据库自动生成校验规则

bay-validator 支持连接数据库，根据表结构自动生成默认的校验规则文件，避免手动编写大量重复配置。

### 1. 配置数据源

在 Spring Boot 的 `application.yml` 中配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 2. 编写生成代码

`valueRuleModuleTargetPath` 传入的是模块的 `target/classes` 路径，生成器内部通过 `../../src/main/resources` 定位回源码目录。

```java
import com.baymax.validator.engine.HxValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class RuleGenerator {

    @Autowired
    private DataSource dataSource;

    /**
     * 生成校验规则到 src/main/resources/rules/ 目录
     *
     * @param databaseName   数据库名称，如 "my_app_db"
     * @param exceptTables   排除的表名列表（如系统表），可为 null
     * @param packageName    生成的 Java 枚举类的包名
     *                      （如 "com.myapp.validator.values"）
     * @param moduleTargetPath 所在模块的 target/classes 目录绝对路径
     */
    public void generate(String databaseName, List<String> exceptTables,
                         String packageName, String moduleTargetPath) {
        HxValidator.Generator.create()
                .bindToDatabase("mysql", dataSource, databaseName, exceptTables)
                .valueRuleModulePath(
                        moduleTargetPath + "/target/classes", "rules")
                .valueEnumRangeModulePath(
                        moduleTargetPath + "/target/classes", packageName)
                .userIgnoreKeys(new HashSet<>(Arrays.asList(
                        "id", "version", "is_deleted",
                        "gmt_created", "gmt_modified")), true)
                .generate();
    }
}
```

> **路径解析原理**：`ValidatorCodeGenerator` 内部将 `target/classes` 路径通过 `Path.get("../../src/main/resources")` 解析，定位到源码的 resources 目录。所以无论你的项目是单体还是多模块，传模块的 `target/classes` 路径即可。

### 3. 执行生成

在测试或启动时调用。`moduleTargetPath` 传模块根目录的绝对路径，生成器通过 `../../src/main/resources` 定位到真实的 resources 目录：

```java
@SpringBootTest
class RuleGeneratorTest {

    @Autowired
    private RuleGenerator ruleGenerator;

    @Test
    void testGenerateRules() {
        ruleGenerator.generate("my_app_db",
            Arrays.asList("flyway_schema_history", "audit_log"),
            "com.myapp.validator.values",
            // 模块根目录路径，生成器内部会找 ../../src/main/resources
            System.getProperty("user.dir"));
    }
}
```

### 4. 生成的目录结构

执行后会在 `src/main/resources/rules/` 目录下生成按表名分文件的规则：

```
resources/
  rules/
    student.yml              # 自动生成：student 表的字段规则
    order.yml                # 自动生成：order 表的字段规则
    product.yml              # 自动生成：product 表的字段规则
    ...
  common_dict.yml            # 预制的正则词典
  hxValidator.js             # 前端校验 JS
```

同时还会在指定包下生成 `ValueEnumRange.java` 枚举类，包含表中所有枚举字段的取值常量。

### 5. 自动生成的规则示例

```yaml
# 自动生成的 student.yml
student:
  name:
    type: string
    string_regex_key: any_string
    string_length_min: 1
    string_length_max: 50     # varchar(50) 自动识别
  age:
    type: numeric
    numeric_min: 0
    numeric_max: 9223372036854775807
```

### 6. 规则合并逻辑

每次重新生成时，**不会覆盖已有手工配置**：

| 场景 | 行为 |
|------|------|
| 旧配置中已存在的字段 | **保留旧配置**，不覆盖（你的手工修改安全） |
| 数据库新增的字段 | **追加新规则** |
| 数据库中已删除的字段 | **自动移除** |
| 整个 rules 目录 | 生成前将旧目录加时间戳重命名备份（如 `rules_1681234567/`）

| 类型 | 说明 | 校验条件 | 示例 |
|------|------|---------|------|
| `numeric` | 整数 | `numeric_min ≤ 值 ≤ numeric_max` | `{ type: numeric, numeric_min: 1, numeric_max: 128 }` |
| `decimal` | 浮点数 | `decimal_min ≤ 值 ≤ decimal_max` | `{ type: decimal, decimal_min: 0.00, decimal_max: 300.00 }` |
| `string` | 字符串 | 长度校验 + 正则匹配 | `{ type: string, string_charset: utf8, string_regex_key: phone_number, string_length_min: 11, string_length_max: 11 }` |
| `enum_string` | 字符串枚举 | 值在给定列表中 | `{ type: enum_string, enum_values: [male, female] }` |
| `enum_numeric` | 整数枚举 | 值在给定列表中 | `{ type: enum_numeric, enum_values: [1, 2, 100] }` |
| `enum_decimal` | 浮点数枚举 | 值在给定列表中 | `{ type: enum_decimal, enum_values: [3.11, 4000000000.123456] }` |
| `date` | 日期 | `beginAt ≤ 值 < endAt` | `{ type: date, begin_at: 2020-01-01, end_at: 2030-12-31 }` |
| `datetime` | 日期时间 | `beginAt ≤ 值 < endAt` | `{ type: datetime, begin_at: 2020-01-01 00:00:00, end_at: 2030-12-31 23:59:59 }` |

---

## 高级用法

### 通用规则（`_common` 前缀）

如果某个字段在所有表中都有相同的校验规则，可以放在 `_common` 下：

```yaml
# 在 value_rules.yml 或公共规则文件中
_common:
  status:
    type: enum_numeric
    enum_values:
      - 1
      - 2
      - 3
```

校验时可以直接传字段名，无需指定表名：

```java
HxValidator.builder().validate("status", "2");
// 自动查找 "_common.status" 的规则
```

### 引用 common_dict 中的枚举/NULL

```yaml
# common_dict.yml 中定义
domain_set:
  - bearing
  - pad
  - rotor
order_status:
  - 1
  - 2
  - 3
  - 4
  - 5
  - 6

# value_rules.yml 中引用
student:
  domain:
    type: enum_string
    enum_values: domain_set    # 引用 common_dict 中的 list
order:
  status:
    type: enum_numeric
    enum_values: order_status  # 引用 common_dict 中的 list
```

### 数据库类型对长度校验的影响

- **MySQL**：`varchar(n)` 的 `n` 为字符数，无论中英文每个字符计为 1
- **Oracle**：`varchar2(n)` 的 `n` 为字节数，"你好" 在 utf8 下计为 6 字节

配置时通过 `string_charset` 控制：

```yaml
# mysql：不设 charset，按字符数校验
mysql_field:
  type: string
  string_length_min: 1
  string_length_max: 50

# oracle：设 charset，按字节数校验
oracle_field:
  type: string
  string_charset: utf8    # 显式指定编码，前端按字节计算
  string_length_min: 1
  string_length_max: 150  # → 可存 50 个汉字(3×50=150)
```

### 使用 `ValidatorEngine` 直接校验

```java
// 简单初始化
ValidatorEngine.INSTANCE.init("value_rules.yml");

// 获取规则的 JSON 形式
Map<String, Object> rule = ValidatorEngine.INSTANCE
    .getFieldValidatorRulesJson("student.phone");
System.out.println(rule.get("regexStr"));
// ^1[3|4|5|7|8][0-9]{9}$

// 获取枚举值的列表
List<Object> enumValues = ValidatorEngine.INSTANCE
    .getEnumValues("student.gender");
// ["male", "female"]
```

### 启用驼峰命名模式

Bean 的属性名默认自动转换为下划线模式去匹配规则。如果 Bean 的字段名本身就是下划线模式，或想禁用转换：

```java
HxValidator.Engine.create()
    .keyMode(KeyMode.camel)    // 不使用自动转换
    .init();
```

---

## 快速使用步骤总结

1. **编译项目** — `mvn clean package -DskipTests`，得到 jar 包
2. **引入依赖** — 将 jar 包添加到你的项目中
3. **编写校验规则** — 在 `resources/rules/` 下手写 YAML 规则（也可连接数据库自动生成，详见「从数据库自动生成校验规则」）
4. **初始化引擎** — 在项目启动时调用 `HxValidator.Engine.create().rulesDir("rules").init()`
5. **后端校验** — 使用 `HxValidator.builder().validate(key, val)` 校验单个字段，`HxValidator.builder().with(bean).bind("table").validate()` 校验整个 Bean
6. **前端校验** — 将 `hxValidator.js` 放入前端项目，后端提供规则 JSON 接口，前端实时预校验
