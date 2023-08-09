package com.baymax.validator.engine;

import com.baymax.App;
import com.baymax.validator.engine.constant.Const;
import com.baymax.validator.engine.generator.JavaEnum;
import com.baymax.validator.engine.generator.JavaEnumTemplateRender;
import com.baymax.validator.engine.generator.formatter.IFormatter;
import com.baymax.validator.engine.generator.kit.TableMetaKit;
import com.baymax.validator.engine.generator.meta.ColumnMeta;
import com.baymax.validator.engine.generator.meta.TableMeta;
import com.baymax.validator.engine.model.FieldRule;
import com.baymax.validator.engine.model.sub.*;
import com.baymax.validator.engine.utils.BeanUtil;
import com.baymax.validator.engine.utils.FileWriter;
import com.baymax.validator.engine.utils.NameUtil;
import com.baymax.validator.engine.utils.ParamUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfinal.kit.Kv;
import com.jfinal.template.Engine;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.yaml.snakeyaml.nodes.Tag;

import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * @author xiao.hu
 * mysql varchar(50) 不管中文 还是英文 都是存50个的 不管 tinyint
 *         后面的数字是多少，它存储长度=2^（1字节）=2^8，即存储范围是 -2^7 到 2^7 - 1。==> 127
 * 
 *         JS的数据类型只有字符串值，数值，布尔值，数组，对象，
 *         对于参数而言，我们只取字符串值和数值，因为boolean类型也可以用true和false表示
 */
public enum ValidatorEngine {
	/**
	 * 单例
	 */
	INSTANCE;

	private static ObjectMapper mapper = new ObjectMapper();

	enum DbType {
		/**
		 * 数据库类型，不同类型对于字段长度的校验是有区别的
		 */
		mysql,
		oracle
	}


	enum RuleKey {
		/**
		 * yml文件的规则键值
		 */
		type,
		numeric_min,
		numeric_max,
		decimal_min,
		decimal_max,
		string_charset,
		string_regex_key,
		string_length_min,
		string_length_max,
		enum_values,
		enum_dict
	}

	public enum RuleType {
		/**
		 * yml校验规则的类型
		 */
		numeric,
		decimal,
		string,
		enum_string,
		enum_numeric,
		enum_decimal;

		public static boolean isEnum(String type) {
			return enum_string.name().equals(type)
					|| enum_numeric.name().equals(type)
					|| enum_decimal.name().equals(type);
		}
	}

	/**
	 * Map的形式保存了value_rules.yml的配置
	 */
	Map<String, Map<String, Object>> valueRulesMap;
	Map<String, Map<String, Object>> commonValueRulesMap;

	/**
	 * 决定键校验时使用驼峰还是下划线模式
	 * @param isSnakeKeyMode
	 */
	private boolean isSnakeKeyMode = true;

	/**
	 * 默认数据库类型是mysql
	 */
	private static DbType dbType = null;

	public static DbType getDbType() {
		return dbType;
	}

	public static void initDbType(String dbTypeName) {
		if(DbType.oracle.name().equals(dbTypeName)) {
			dbType = DbType.oracle;
		}
		else {
			dbType = DbType.mysql;
		}
	}

	/**
	 * 可被自定义
	 */
	private static Set<String> ignoreKeys = new HashSet<>();
	static {
		ignoreKeys.add("id");
		ignoreKeys.add("gmt_created");
		ignoreKeys.add("creator");
		ignoreKeys.add("gmt_modified");
		ignoreKeys.add("modifier");
		ignoreKeys.add("is_deleted");
		ignoreKeys.add("version");
	}

	public static boolean containIgnoreKeys(String key) {
		return ignoreKeys.contains(key);
	}

	/**
	 * 代码格式化插件
	 */
	private IFormatter formatter = null;

	public void setFormatter(IFormatter formatter) {
		this.formatter = formatter;
	}

	/**
	 * {{ 入口方法 }}
	 *
	 *
	 * 根据给予的value_rules.yml类进行初始化
	 * 配置样例如下：
	 * student:
	 *   id:
	 *     type: int
	 *     numeric_min: 1
	 *     numeric_max: 128
	 *     string_charset: utf8
	 *     string_regex: positive_integer
	 *     string_length_min: 0
	 *     string_length_max: 128
	 *     enum_values: null
	 *   gender:
	 *     type: enum
	 *     numeric_min: null
	 *     numeric_max: null
	 *     string_charset: null
	 *     string_regex: null
	 *     string_length_min: null
	 *     string_length_max: null
	 *     enum_values:
	 *     - male
	 *     - female
	 *
	 *   结构为 map -> map -> {String, Integer, ArrayList}
	 */
	public void init0(String dbType, String valueRulesYmlFilePath, String regexDictYmlFilePath) {
		initDbType(dbType);
		CommonDict.INSTANCE.init(regexDictYmlFilePath);
		this.valueRulesMap = loadValueRulesYml(valueRulesYmlFilePath);
	}

	public void init(String valueRulesYmlFilePath) {
		initDbType(DbType.mysql.name());
		CommonDict.INSTANCE.init("");
		this.valueRulesMap = loadValueRulesYml(valueRulesYmlFilePath);
	}

	public void init(String valueRulesYmlFilePath, String regexDictYmlFilePath) {
		initDbType(DbType.mysql.name());
		CommonDict.INSTANCE.init(regexDictYmlFilePath);
		this.valueRulesMap = loadValueRulesYml(valueRulesYmlFilePath);
	}

	/**
	 * 使用了通用配置
	 * @param valueRulesYmlFilePath
	 * @param commonValueRulesYmlFilePath
	 */
	public void init(String dbType, String valueRulesYmlFilePath,
					 String commonValueRulesYmlFilePath, String regexDictYmlFilePath) {
		init0(dbType, valueRulesYmlFilePath, regexDictYmlFilePath);
		if(commonValueRulesYmlFilePath != null) {
			this.commonValueRulesMap = loadValueRulesYml(commonValueRulesYmlFilePath);
		}
	}

	/**
	 * 使用了通用配置
	 * @param valueRulesYmlFilePath
	 * @param commonValueRulesYmlFilePath
	 */
	public void init(String dbType, String valueRulesYmlFilePath, String commonValueRulesYmlFilePath,
					 String regexDictYmlFilePath,
					 Set<String> userIgnoreKeys, boolean customUseSnake) {
		this.isSnakeKeyMode = customUseSnake;
		ignoreKeys = userIgnoreKeys;
		checkIgnoreKeysByUseSnake();
		init(dbType, valueRulesYmlFilePath, commonValueRulesYmlFilePath, regexDictYmlFilePath);
	}

	/**
	 * 用户可以自定义忽略的字段
	 * @param valueRulesYmlFilePath
	 * @param userIgnoreKeys
	 */
	public void init(String dbType, String valueRulesYmlFilePath, String regexDictYmlFilePath,
					 Set<String> userIgnoreKeys, boolean customUseSnake) {
		this.isSnakeKeyMode = customUseSnake;
		ignoreKeys = userIgnoreKeys;
		checkIgnoreKeysByUseSnake();

		init0(dbType, valueRulesYmlFilePath, regexDictYmlFilePath);
	}


	/**
	 * 判断当前预设的忽略字段是否也满足键的模式
	 */
	private void checkIgnoreKeysByUseSnake() {
		for(String key : ignoreKeys) {
			if(this.isSnakeKeyMode) {
				char[] chars = key.toCharArray();
				for(char s : chars) {
					if(Character.isUpperCase(s)) {
						throw new IllegalArgumentException("ignore keys must be snake naming mode");
					}
				}
			}
			else {
				char[] chars = key.toCharArray();
				for(char s : chars) {
					if(s == '_') {
						throw new IllegalArgumentException("ignore keys must be camel naming mode");
					}
				}
			}
		}
	}

	/**
	 * 读取 value_rules.yml 文件
	 * 这个文件应该放在foundation工程中
	 * @param ymlFilePath
	 * @return
	 */
	public static Map<String, Object> loadYml(String ymlFilePath) {
		Yaml yaml = new Yaml();

		Map<String, Object> map = new HashMap<>(2);

		List<Map<String, Object>> listMap = new ArrayList<>();
		Enumeration<URL> ps;
		try {
			ps = App.class.getClassLoader().getResources(ymlFilePath);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		while(ps.hasMoreElements()) {
			URL url = ps.nextElement();
			try (InputStream is = url.openStream()) {
				/**
				 *  读取所有输入,包括回车换行符
				 *  \\A为正则表达式,表示从字符头开始
				 */
				Scanner s = new Scanner(is).useDelimiter("\\A");
				String originValueRulesStr = s.hasNext() ? s.next() : "";

				Map<String, Object> mapValueDict = yaml.loadAs(originValueRulesStr, Map.class);
				listMap.add(mapValueDict);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		if(listMap.size() > 1) {
			/**
			 * 最后加载的是bay-validator本身的配置文件
			 */
			Collections.reverse(listMap);
		}

		listMap.forEach((itemMap) -> map.putAll(itemMap));

		return map;
	}

	/**
	 * 读取 value_rules.yml 文件
	 * 这个文件应该放在foundation工程中
	 * @param valueRulesYmlFilePath
	 * @return
	 */
	public static Map<String, Map<String, Object>> loadValueRulesYml(String valueRulesYmlFilePath) {
		Yaml yaml = new Yaml();

		Map<String, Map<String, Object>> map = new HashMap<>(2);

		List<Map<String, Map<String, Object>>> listMap = new ArrayList<>();
 		Enumeration<URL> ps;
		try {
			ps = App.class.getClassLoader().getResources(valueRulesYmlFilePath);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		while(ps.hasMoreElements()) {
			URL url = ps.nextElement();

			try (InputStream is = url.openStream()) {
				/**
				 *  读取所有输入,包括回车换行符
				 *  \\A为正则表达式,表示从字符头开始
				 */
				Scanner s = new Scanner(is).useDelimiter("\\A");
				String originValueRulesStr = s.hasNext() ? s.next() : "";

				Map<String, Map<String, Object>> mapValueDict = yaml.loadAs(originValueRulesStr, Map.class);
				listMap.add(mapValueDict);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		if(listMap.size() > 1) {
			/**
			 * 最后加载的是bay-validator本身的配置文件
			 */
			Collections.reverse(listMap);
		}

		listMap.forEach((itemMap) -> map.putAll(itemMap));

		return map;
	}

	/**
	 * 判断规则是否存在
	 * 
	 * @param tableName
	 * @param fieldName
	 * @return
	 */
	public boolean isRuleExisted(String tableName, String fieldName) {
		Map<String, Object> fieldsMap = this.valueRulesMap.get(tableName);
		if(fieldsMap == null) {
			return false;
		}

		Map<String, Object> rulesMap = (Map<String, Object>) fieldsMap.get(fieldName);
		if(rulesMap == null) {
			return false;
		}

		return rulesMap.size() > 0;
	}

	/**
	 * 获得配置的FieldRule形式校验规则
	 *
	 * @param fieldKey
	 * @return
	 */
	public FieldRule getFieldRules(String fieldKey) {
		Map<String, Object> rulesMap = getRulesMap(fieldKey);
		return (rulesMap == null) ? null : this.buildFieldRule(fieldKey, rulesMap);
	}

	/**
	 * 通过fieldKey获得配置的Map形式校验规则
	 * @param fieldKey
	 * @return
	 */
	private Map<String, Object> getRulesMap(String fieldKey) {
		String[] keys = secureFieldKey(fieldKey);

		String tableName = keys[0];
		String fieldName = keys[1];

		return getRuleMap(tableName, fieldName);
	}

	/**
	 * 校验并处理field key
	 * @param fieldKey
	 * @return
	 */
	private String[] secureFieldKey(String fieldKey) {
		if(fieldKey == null) {
			throw new IllegalArgumentException("fieldKey is illegal");
		}

		String[] keys = fieldKey.split("\\.");
		if(keys.length != 2) {
			keys = new String[]{"_common", keys[0]};
		}
		return keys;
	}

	/**
	 * 通过tableName， fieldName获得配置的Map形式校验规则
	 * @param tableName
	 * @param fieldName
	 * @return
	 */
	private Map<String, Object> getRuleMap(String tableName, String fieldName) {
		Map<String, Object> fieldsMap = this.valueRulesMap.get(tableName);
		if(fieldsMap == null) {
			if(this.commonValueRulesMap != null) {
				fieldsMap = this.commonValueRulesMap.get(tableName);
				if(fieldsMap == null) {
					return null;
				}
			}
			else {
				return null;
			}
		}

		Map<String, Object> rulesMap = (Map<String, Object>) fieldsMap.get(fieldName);
		if(rulesMap == null) {
			return null;
		}
		return rulesMap;
	}

	/**
	 * 根据yml的配置建立FieldRule对象
	 * @param rulesMap
	 * @return
	 */
	private FieldRule buildFieldRule(String fieldKey, Map<String, Object> rulesMap) {
		String type = (String) rulesMap.get(RuleKey.type.name());

		/**
		 * 数值型相关配置
		 * yaml会根据配置数值的大小，自动匹配int long BigInteger类型
		 * 因为假设numericMin为1时，yaml将其匹配为int型，而numericMax为大数，匹配为long型
		 * 比较起来需要做类型转换，所以统一使用BigInteger类型进行全兼容
		 */
		BigInteger numericMin = ParamUtil.getBigInteger(rulesMap, RuleKey.numeric_min.name());
		BigInteger numericMax = ParamUtil.getBigInteger(rulesMap, RuleKey.numeric_max.name());

		/**
		 * decimal的相关配置
		 */
		BigDecimal decimalMin = ParamUtil.getBigDecimal(rulesMap, RuleKey.decimal_min.name());
		BigDecimal decimalMax = ParamUtil.getBigDecimal(rulesMap, RuleKey.decimal_max.name());

		/**
		 * string的相关配置
		 */
		String stringCharset = (String) rulesMap.get(RuleKey.string_charset.name());
		String stringRegexKey = (String) rulesMap.get(RuleKey.string_regex_key.name());
		Integer stringLengthMin = (Integer) rulesMap.get(RuleKey.string_length_min.name());
		Integer stringLengthMax = (Integer) rulesMap.get(RuleKey.string_length_max.name());

		/**
		 * enum的相关配置
		 */
		List<Object> enumValuesList = this.getEnumValues(rulesMap);
		Map<Object, String> enumDict = this.getEnumDict(rulesMap);

		/**
		 * 工厂方法
		 */
		if(RuleType.numeric.name().equals(type)) {
			FieldRule fr = new NumericFieldRule();
			fr.setFieldKey(fieldKey);
			fr.setType(type);
			fr.setNumericMin(numericMin);
			fr.setNumericMax(numericMax);
			return fr;
		}
		if(RuleType.decimal.name().equals(type)) {
			FieldRule fr = new DecimalFieldRule();
			fr.setFieldKey(fieldKey);
			fr.setType(type);
			fr.setDecimalMin(decimalMin);
			fr.setDecimalMax(decimalMax);
			return fr;
		}
		else if(RuleType.string.name().equals(type)) {
			FieldRule fr = new StringRegexFieldRule();
			fr.setFieldKey(fieldKey);
			fr.setType(type);
			fr.setStringCharset(stringCharset);
			fr.setStringRegexKey(stringRegexKey);
			fr.setStringLengthMin(stringLengthMin);
			fr.setStringLengthMax(stringLengthMax);
			return fr;
		}
		else if(RuleType.enum_string.name().equals(type)) {
			FieldRule fr = new EnumStringFieldRule();
			fr.setFieldKey(fieldKey);
			fr.setType(type);
			fr.setEnumValues(enumValuesList);
			return fr;
		}
		else if(RuleType.enum_numeric.name().equals(type)) {
			FieldRule fr = new EnumNumericFieldRule<>(BigInteger.class);
			fr.setFieldKey(fieldKey);
			fr.setType(type);
			fr.setEnumValues(enumValuesList);
			fr.setEnumDict(enumDict);
			return fr;
		}
		else if(RuleType.enum_decimal.name().equals(type)) {
			FieldRule fr = new EnumNumericFieldRule<>(BigDecimal.class);
			fr.setFieldKey(fieldKey);
			fr.setType(type);
			fr.setEnumValues(enumValuesList);
			fr.setEnumDict(enumDict);
			return fr;
		}

		return null;
	}

	private List<Object> getEnumValues(Map<String, Object> rulesMap) {
		Object enumValues = rulesMap.get(RuleKey.enum_values.name());
		List<Object> enumValuesList = null;
		if(enumValues instanceof List) {
			enumValuesList = (List<Object>) enumValues;
		}
		else if(enumValues instanceof String) {
			enumValuesList = CommonDict.INSTANCE.getList((String) enumValues);
		}
		return enumValuesList;
	}


	/**
	 * 对于 number 类型的枚举值，可以在 enum_dict 中配置转义字典
	 * @param rulesMap
	 * @return
	 */
	private Map<Object, String> getEnumDict(Map<String, Object> rulesMap) {
		Object enumDict = rulesMap.get(RuleKey.enum_dict.name());
		if(enumDict instanceof Map) {
			return (Map<Object, String>) enumDict;
		}
		return null;
	}


	/**
	 * 获得配置的校验规则项
	 * @return
	 */
	public Map<String, Object> getFieldValidatorRules(String fieldKey) {
		Map<String, Object> map = new HashMap<>(1);
		map.put(fieldKey, this.getFieldValidatorRulesJson(fieldKey));
		return map;
	}

	/**
	 * 获得配置的校验规则项的字符串形式
	 * @return
	 */
	public String getFieldValidatorRulesStr(String fieldKey) {
		try {
			return mapper.writeValueAsString(this.getFieldValidatorRules(fieldKey));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}


    /**
     * 获得配置的校验规则的json map
	 * 就是FieldRule的Map表达
     * @param fieldKey
     * @return
     */
    public Map<String, Object> getFieldValidatorRulesJson(String fieldKey) {
        FieldRule fieldRule = this.getFieldRules(fieldKey);
        if (fieldRule == null) {
        	return null;
		}

		/**
		 * 不需要返回fieldKey
		 */
		fieldRule.setFieldKey("");

        try {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String jsonStr = mapper.writeValueAsString(fieldRule);

            if(!RuleType.string.name().equals(fieldRule.getType())) {
                /**
                 * 如果不是string类型，则不需要补充正则表达式
                 */
                return mapper.readValue(jsonStr, Map.class);
            }

            String regexKey = fieldRule.getStringRegexKey();
            String regexStr = (String) CommonDict.INSTANCE.getRule(regexKey);
            if(StringUtils.isBlank(regexStr)) {
                throw new IllegalStateException(String.format("regex is blank, regexKey is %s", regexKey));
            }

            /**
             * 如果校验类型是string，则需要取出正则表达式
             */
            Map m = mapper.readValue(jsonStr, Map.class);
            m.put("regexStr", regexStr);
            return m;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }


	/**
	 * 获得配置的校验规则的json字符串
	 * 
	 * @param fieldKey
	 * @return
	 * @throws JsonProcessingException 
	 */
	public String getFieldValidatorRulesJsonStr(String fieldKey) {
        try {
            return mapper.writeValueAsString(this.getFieldValidatorRulesJson(fieldKey));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

	/**
	 * 获取某个字段的枚举配置，如果不是枚举类型，则返回null
	 * @param fieldKey
	 * @return
	 */
    public List<Object> getEnumValues(String fieldKey) {
		Map<String, Object> ruleJson = this.getFieldValidatorRulesJson(fieldKey);
		if(ruleJson == null) {
			return null;
		}

		String type = (String) ruleJson.get(RuleKey.type.name());
		if(RuleType.isEnum(type)) {
			return (List<Object>) ruleJson.get(NameUtil.lineToHump(RuleKey.enum_values.name()));
		}

		return null;
	}

	/**
	 * 校验
	 * @param validatorKey
	 * @param paramValue
	 * @return
	 */
	public boolean validate(String validatorKey, Object paramValue) {
		FieldRule fr = this.getFieldRules(validatorKey);
		if(fr == null) {
			fr = this.getFieldRules("_common." + validatorKey);
			if(fr == null) {
				throw new IllegalStateException(String.format("No field(%s) rule found", validatorKey));
			}
		}
		return fr.validate(String.valueOf(paramValue));
	}


	/**
	 * JavaScript的数据类型，共有9种：
	 * 值类型(基本类型)：字符串（String）、数字(Number)、布尔(Boolean)、空（Null）、未定义（Undefined）、Symbol。
	 * 引用数据类型：    对象(Object)、数组(Array)、函数(Function)。
	 *
	 * Function<T,R>可以允许我们自定一个函数，通过给定参数T返回结果R
	 *
	 * @param bean
	 * @param prefix
	 * @param customIgnoreKeys
	 * @param nullableKeys
	 * @return
	 */
	public List<String> validate(Object bean, String prefix,
							String[] customIgnoreKeys, String[] nullableKeys) {
		List<String> errorKeys = new ArrayList<>();
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
			PropertyDescriptor[] proDescriptors = beanInfo.getPropertyDescriptors();

			List<String> setMethodCache = new ArrayList<>();
			Method[] names = bean.getClass().getDeclaredMethods();
			for(Method method : names) {
				if(method.getName().startsWith("set")) {
					setMethodCache.add(method.getName());
				}
			}

			if (proDescriptors != null && proDescriptors.length > 0) {
				for (PropertyDescriptor propDesc : proDescriptors) {


					/**
					 * 如果不存在写属性，说明不是get 和 set 方法
					 *
					 * 只有 void  setXXX 才会被认为是WriteMethod
					 */
					if (propDesc.getWriteMethod() == null) {
						/**
						 * 针对有些直接使用po传递controller参数的时候，set方法可能返回值不是void的情况
						 * 虽然这样写不够纯粹，但是方便。不用重复定义类似的对象
						 */
						String poSetMethod = "s" + propDesc.getReadMethod().getName().substring(1);
						if(!setMethodCache.contains(poSetMethod)) {
							continue;
						}
					}

					String name = propDesc.getName();

					/**
					 * 可以使用驼峰或者下划线
					 */
					if(isSnakeKeyMode) {
						name = NameUtil.humpToLine(name);
					}

					Method method = propDesc.getReadMethod();
					Object value = method.invoke(bean);

					/**
					 * 初始化时设定的可以忽略的键
					 */
					if(ignoreKeys.contains(name)) {
						/** 忽略值的处理 */
						continue;
					}

					/**
					 * 用户定义的可以忽略的键
					 */
					if(customIgnoreKeys != null) {
						if( Arrays.asList(customIgnoreKeys).contains(name)) {
							/** 忽略值的处理 */
							continue;
						}
					}

					if((value != null) && !(value instanceof String) && !(value instanceof Number)
							&& (!(value instanceof Boolean))) {
						throw new UnsupportedOperationException(
								String.format("The type of parameter is not supported, name: %s, value: %s"
								, name, mapper.writeValueAsString(value)));
					}

					String valueStr = value == null ? "" : String.valueOf(value);
					if(StringUtils.isBlank(valueStr)) {
						/**
						 * 非强制校验的键
						 */
						if(nullableKeys != null) {
							if(Arrays.asList(nullableKeys).contains(name)) {
								/** 忽略值的处理 */
								continue;
							}
						}

						errorKeys.add(name);
						continue;
					}

					String prefixName = prefix + "." + name;
					boolean isOK = INSTANCE.validate(prefixName, valueStr);
					if (!isOK) {
						errorKeys.add(name);
					}
				}
			}

			return errorKeys;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


	/**
	 * 根据yml中的配置，将enum类型的配置生成java代码，供后端使用
	 * 特别注意，只生成有确定取值的字段，
	 * 如果所有字段都是String或者Integer类型，则该类中内容可能只剩下表名
	 * @param packageName
	 */
	public String generateJavaEnumCode(String packageName) {
		Engine.setFastMode(true);
		Engine engine = Engine.use();
		engine.setDevMode(true);
		engine.setToClassPathSourceFactory();

		if(this.valueRulesMap == null) {
			throw new IllegalStateException("this.valueRulesMap is null");
		}

		Iterator<Map.Entry<String, Map<String, Object>>> itTable = this.valueRulesMap.entrySet().iterator();

		List<String> tableTemplateList = new ArrayList<>();
		Set<String> importList = new HashSet<>();
		while(itTable.hasNext()) {
			/**
			 * 遍历所有的表名
			 */
			Map.Entry<String, Map<String, Object>> entryTable = itTable.next();
			String tableName = entryTable.getKey();
			Map<String, Object> tableRuleMap = entryTable.getValue();
			if(tableRuleMap == null) {
				throw new IllegalStateException(String.format("yaml's format is illegal, table name is %s", tableName));
			}

			Iterator<Map.Entry<String, Object>> itField = tableRuleMap.entrySet().iterator();

			List<String> fieldRuleTemplateList = new ArrayList<>();
			while(itField.hasNext()) {
				Map.Entry<String, Object> entryField = itField.next();
				String fieldName = entryField.getKey();
				Object fieldRuleMapObj = entryField.getValue();
				if(!(fieldRuleMapObj instanceof Map)) {
					throw new IllegalStateException(String.format(
							"yaml's format is illegal, field name is %s.%s", tableName, fieldName));
				}

				Map<String, Object> fieldRuleMap = (Map<String, Object>)fieldRuleMapObj;
				String type = (String) fieldRuleMap.get(RuleKey.type.name());
				if(!RuleType.isEnum(type)) {
					continue;
				}

				List<Object> enumValues = this.getEnumValues(fieldRuleMap);
				Map<Object, String> enumDict = this.getEnumDict(fieldRuleMap);

				JavaEnum je = JavaEnumTemplateRender.build(fieldName, type, enumValues, enumDict);
				importList.add("import " + je.getCanonicalJavaType());

				Kv cond = Kv.by(Const.TemplateKey.fieldName.name(), fieldName)
						.set(Const.TemplateKey.enumValues.name(), je.getEnumValues())
						.set(Const.TemplateKey.javaType.name(), je.getJavaType());
				String template = engine.getTemplate(Const.ENUM_FIELD_RULE_FILENAME).renderToString(cond);
				//System.out.println(template);
				fieldRuleTemplateList.add(template);
			}


			Kv cond = Kv.by(Const.TemplateKey.tableName.name(), tableName)
					.set(Const.TemplateKey.fieldRuleList.name(), fieldRuleTemplateList);
			String tableTemplate = engine.getTemplate(Const.TABLE_FILENAME).renderToString(cond);
			// System.out.println(tableTemplate);
			tableTemplateList.add(tableTemplate);
		}

		String importStr = StringUtils.join(importList, ";") + ";";
		Kv cond = Kv.by(Const.TemplateKey.tableList.name(), tableTemplateList)
				.set("package", packageName)
				.set("import", importStr)
				.set(Const.TemplateKey.generateTime.name(), DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
		String valueRangeTemplate = engine.getTemplate(Const.VALUE_ENUM_RANGE_FILENAME).renderToString(cond);

		String formatSource = formatJava(valueRangeTemplate);
		System.out.println(formatSource);
		return formatSource;
	}


	/**
	 *
	 * @param oldYmlPath
	 * @param dataSource
	 * @param exceptTables
	 * @return
	 * @throws SQLException
	 */
	public String generateDefaultYml(String oldYmlPath, DataSource dataSource, String databaseName, List<String> exceptTables)
			throws SQLException {
		List<String> tables = TableMetaKit.getTables(dataSource, databaseName, exceptTables);
		if(tables == null || tables.isEmpty()) {
			return "";
		}

		Engine ve = Engine.create("Huxiao.coder");

		Map<String, TableMeta> tablesWithColumnMetaMapping = makeStringTableMetaMap(dataSource, tables);

		List<FieldRule> list = new ArrayList<>();
		Iterator<Map.Entry<String, TableMeta>> it = tablesWithColumnMetaMapping.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, TableMeta> entry = it.next();
			String tableName = entry.getKey();
			TableMeta tableMeta = entry.getValue();

			/**
			 * columnName: is_deleted, clazzName:String
			 * columnName: parent_id, clazzName:Long
			 * columnName: name, clazzName:String
			 * columnName: password, clazzName:String
			 * columnName: name_cn, clazzName:String
			 * columnName: user_number, clazzName:String
			 * columnName: is_admin, clazzName:String
			 * columnName: ticket, clazzName:String
			 * columnName: version, clazzName:Integer
			 */
			List<ColumnMeta> columnMetaList =  tableMeta.getColumnMetaList();
			for(ColumnMeta meta : columnMetaList) {
				String columnName = meta.getName();
				String clazzName = meta.getOriginClass();
				Integer displaySize = meta.getDisplaySize();

				if(ignoreKeys.contains(columnName)) {
					continue;
				}

				if(String.class.getName().equals(clazzName)
						|| Date.class.getName().equals(clazzName)) {
					makeAnyStringRule(list, tableName, columnName, displaySize);
				}
				else {
					makeNumericRule(list, tableName, columnName, clazzName, displaySize);
				}
			}
		}

		return generateDefaultYml(oldYmlPath, list);
	}

	public static Map<String, TableMeta> makeStringTableMetaMap(DataSource dataSource, List<String> tables) throws SQLException {
		Map<String, TableMeta> tablesWithColumnMetaMapping = new HashMap<>();
		//连接不能在循环里面，不然有可能占用很多的链接
		Connection con = dataSource.getConnection();
		for(String tableName : tables) {
			List<ColumnMeta> columnsMetaList = TableMetaKit.getColumnsMeta(con, tableName);
			if(columnsMetaList == null || columnsMetaList.isEmpty()) {
				continue;
			}

			TableMeta oTableMeta = new TableMeta();
			oTableMeta.setTableName(tableName);
			oTableMeta.setColumnMetaList(columnsMetaList);

			tablesWithColumnMetaMapping.put(tableName, oTableMeta);
		}
		return tablesWithColumnMetaMapping;
	}

	/**
	 *
	 * @param list
	 * @param tableName
	 * @param columnName
	 * @param clazzName
	 * @param displaySize
	 */
	public static void makeNumericRule(List<FieldRule> list, String tableName,
									   String columnName, String clazzName, Integer displaySize) {
		if(displaySize == null) {
			/**
			 * 不必要取整型最大值
			 */
			displaySize = 9999;
		}

		if(Integer.class.getName().equals(clazzName)
				|| Long.class.getName().equals(clazzName)
		        || BigInteger.class.getName().equals(clazzName)) {
			FieldRule fr = new NumericFieldRule();
			fr.setFieldKey(tableName + "." + columnName);
			fr.setType(RuleType.numeric.name());
			fr.setNumericMin(new BigInteger("0"));
			fr.setNumericMax(new BigInteger(String.valueOf(displaySize)));
			list.add(fr);
		}
		else if(BigDecimal.class.getName().equals(clazzName)) {
			FieldRule fr = new DecimalFieldRule();
			fr.setFieldKey(tableName + "." + columnName);
			fr.setType(RuleType.decimal.name());
			fr.setDecimalMin(new BigDecimal("0.00"));
			fr.setDecimalMax(new BigDecimal(String.valueOf(displaySize)));
			list.add(fr);
		}
	}

	/**
	 * 指定字符串类型的规则
	 *
	 * mysql mariadb而言
	 * 对于varchar类型的长度，"你好"和"nh"是一个概念，都是2
	 * 对于varchar(2)这样的数据类型，不能插入’123’或者’你好吗’这样的字符串，
	 * 但是可以插入’12’,’你好’这样的字符串，我们知道在utf8字符集下两个汉字占用6个字节的大小。
	 *
	 * 对于int(2)这样的数据 类型，是可以插入数字123的，但是最大不能超过int存储范围的最大值
	 *
	 * Oracle：
	 * 1.      Varchar2的字段，保存汉字量是长度/3， 即 varchar2 (30) 的字段，必能保存10个汉字。
	 * 2.      nvarchar2的字段，保存汉字是1：1的，即 nvarchar2 (30) 的字段，必能保存30个汉字。
	 * @param list
	 * @param tableName
	 * @param columnName
	 * @param displaySize
	 */
	public static void makeAnyStringRule(List<FieldRule> list, String tableName, String columnName, Integer displaySize) {
		if(displaySize == null) {
			displaySize = 128;
		}

		FieldRule rule = new StringRegexFieldRule();
		rule.setFieldKey(tableName + "." + columnName);
		rule.setStringRegexKey("any_string");
		rule.setStringLengthMin(1);
		rule.setStringLengthMax(displaySize);

		if(getDbType() == DbType.oracle) {
			rule.setStringCharset("utf8");
		}

		rule.setType(RuleType.string.name());
		list.add(rule);
	}

	/**
	 * yml读取到java中的是Map结构是：
	 * "student" -> Map
	 *               |
	 *              "id" -> Map
	 *                       |
	 *                      "type" -> "numeric"
	 *                      "numeric_min" -> 1
	 *                      ...
	 * @param oldYmlPath
	 * @param list
	 */
	public String generateDefaultYml(String oldYmlPath, List<FieldRule> list) {
		/**
		 * 读取旧配置文件中的已有配置
		 */
		this.valueRulesMap = loadValueRulesYml(oldYmlPath);
		Map<String, Map<String, Object>> oldConfigMap = this.valueRulesMap;

		/**
		 * 根据的读取到的表的元数据
		 */
		Map<String, Object> tableMap = new HashMap<>();
		if(oldConfigMap != null) {
			tableMap.putAll(oldConfigMap);
		}

		for(FieldRule rule : list) {
			/**
			 * 处理 _common.status, sfm_template_summary_list.status
			 */
			String[] keys = secureFieldKey(rule.getFieldKey());

			String tableName = keys[0];
			String fieldName = keys[1];

			Map<String, Object> fieldMap;
			if(oldConfigMap != null) {
				fieldMap = oldConfigMap.get(tableName);
				if (fieldMap != null) {
					Map<String, Object> rulesMap = (Map<String, Object>) fieldMap.get(fieldName);
					if (rulesMap != null && rulesMap.size() > 0) {
						System.out.println("fieldName exist: " + fieldName);
						/**
						 * 如果该字段已存在旧的配置，则使用旧的配置
						 */
						continue;
					}
				}
			}

			/**
			 * 如果不存在旧的配置，则根据FieldRule新生成配置
			 */
			fieldMap = (Map<String, Object>) tableMap.get(tableName);
			if(fieldMap == null) {
				fieldMap = new HashMap<>();
				tableMap.put(tableName, fieldMap);
			}

			Map<String, Object> beanMap = BeanUtil.beanToMap(rule);
			Map<String, Object> ruleMap = new HashMap<>();
			Iterator<Map.Entry<String, Object>> it = beanMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Object> entry = it.next();
				String key = entry.getKey();
				Object val = entry.getValue();

				if("fieldKey".equals(key)) {
					continue;
				}
				ruleMap.put(NameUtil.humpToLine(key), val);
			}

			fieldMap.put(fieldName, ruleMap);
		}

		/**
		 * 过滤掉在table中已经不存在的字段
		 */
		removeDeletedRule(tableMap, list);

		Yaml yaml = new Yaml();
		String dumpCleanStr = yaml.dumpAs(ValidatorEngine.cleanCopyOfMap(tableMap), Tag.MAP, DumperOptions.FlowStyle.BLOCK);
		//System.out.println("dumpCleanStr:\n" + dumpCleanStr);
		return dumpCleanStr;
	}

	/**
	 * 过滤掉在table中已经不存在的字段
	 * @param tableMap
	 * @param listFromTable
	 */
	private void removeDeletedRule(Map<String, Object> tableMap, List<FieldRule> listFromTable) {
		Set<String> fieldIncludeUnused = new HashSet<>();
		tableMap.forEach((tableName, v) -> {
			Map<String, Object> fieldMap = (Map<String, Object> )v;
			fieldMap.forEach((fieldName, rule) -> {
				fieldIncludeUnused.add(String.format("%s.%s", tableName, fieldName));
			});
		});

		Set<String> fieldForOutput = new HashSet<>();
		listFromTable.forEach((rule) -> {
			fieldForOutput.add(rule.getFieldKey());
		});

		Set<String> fieldToBeFilter = new HashSet<>();
		fieldIncludeUnused.forEach((fieldName) -> {
			if(!fieldForOutput.contains(fieldName)) {
				fieldToBeFilter.add(fieldName);
			}
		});

		fieldToBeFilter.forEach((fieldKey) -> {
			String[] keys = secureFieldKey(fieldKey);

			String tableName = keys[0];
			String fieldName = keys[1];

			Map<String, Object> fieldMap = (Map<String, Object>) tableMap.get(tableName);
			if(fieldMap.containsKey(fieldName)) {
				fieldMap.remove(fieldName);
			}

			if(fieldMap.isEmpty()) {
				tableMap.remove(tableName);
			}
		});
	}

	/**
	 * 格式化java代码
	 * 如果格式化过程异常，则原样输出
	 * @param valueRangeTemplate
	 * @return
	 */
	public String formatJava(String valueRangeTemplate) {
		if(formatter == null) {
			/** 没有设置格式化插件就直接原样返回 */
			return valueRangeTemplate;
		}

		try {
			return formatter.formatJava(valueRangeTemplate);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return valueRangeTemplate;
	}

	/**
	 * 如果是使用此类的main方法 this.getClass().getClassLoader().getResource("").getPath()
	 * ==>
	 * /Volumes/HD-FOR-MAC/DEV_ENV/projects/webApp/workspace_for_maven/law-doc-
	 * parent/law-doc-validator/target/classes/
	 * 
	 * this.getClass().getResource("").getPath() ==>
	 * /Volumes/HD-FOR-MAC/DEV_ENV/projects/webApp/workspace_for_maven/law-doc-
	 * parent/law-doc-validator/target/classes/com/baymax/law/doc/validator/
	 * engine/
	 */
	public void writeToFile(String fileName, String packageName, String content, boolean toSrcTest) {
		// /Volumes/HD-FOR-MAC/DEV_ENV/projects/webApp/workspace_for_maven/law-doc-parent/law-doc-validator/target/classes/
		String targetClassesPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		String srcPath = toSrcTest ? targetClassesPath + "../../src/test/java"
				: targetClassesPath + "../../src/main/java";

		String packagePath = File.separator + packageName.replaceAll("\\.", File.separator);
		//System.out.println(packagePath);

		if(toSrcTest) {
			FileWriter.write(srcPath + packagePath, fileName, Const.FileType.java.name(), content);
		} else {
			FileWriter.backupAndWrite(srcPath + packagePath, fileName, Const.FileType.java.name(), content);
		}
	}

	/**
	 * 将hxValidator.js发布到指定路径下，可方便远程加载
	 * @param filePath
	 */
	public void publishHxValidatorJS(String filePath) {
		//得到配置文件路径
		String path = Thread.currentThread().getContextClassLoader()
				.getResource(Const.HX_VALIDATOR + "." + Const.FileType.js.name()).getPath();
		try {
			String content = new String(Files.readAllBytes(Paths.get(path)));
			FileWriter.write(filePath, Const.HX_VALIDATOR, Const.FileType.js.name(), content);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}


	/**
	 * 去掉map中值为null的数据
	 * @param map
	 */
	public static Map<String, Object> cleanCopyOfMap(Map<String, Object> map) {
		Map<String, Object> copyMap = new HashMap<>(map.size());
		copyMap.putAll(map);
		return deepCleanMap(copyMap);
	}

	/**
	 * 递归清理map中的各项value为null的键值对
	 * @param copyMap
	 * @return
	 */
	private static Map<String, Object> deepCleanMap(Map<String, Object> copyMap) {
		copyMap.entrySet().removeIf((entry) ->{
			if(entry.getValue() instanceof Map) {
				/**
				 * 递归清理
				 */
				deepCleanMap((Map<String, Object>) entry.getValue());
			}
			return entry.getValue() == null;
		});
		return copyMap;
	}


}
