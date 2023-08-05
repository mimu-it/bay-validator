BayValidator的使用



# maven本地引入

在maven工程目录src/main/resources/validator中放入bay-validator-1.7.jar文件

在pom.xml中引入。

```
<!-- 使用自定义的验证组件 -->
<dependency>
    <groupId>com.baymax</groupId>
    <artifactId>bay-validator</artifactId>
    <version>1.7</version>
    <scope>system</scope>
    <systemPath>${basedir}/src/main/resources/validator/bay-validator-1.7.jar</systemPath>
</dependency>
```



# 部署字段规则配置文件

字段规则配置文件分为三种：

## regex_dict.yml

regex_dict.yml 文件是通过键值的方式保存常用的字符串正则表达式规则，可以对其进行修改，积累更多的规则，有利于提升开发速度。

例如：

```
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

```
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

```
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

```
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

```
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

```
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

```
public void publishJs() throws FileNotFoundException {
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
    }public void publishJs() throws FileNotFoundException {
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