package com.baymax.validator.engine.common;

import com.baymax.validator.engine.CommonDict;
import com.baymax.validator.engine.preset.RuleKey;

import java.util.List;
import java.util.Map;

/**
 * 验证引擎通用工具类，提供枚举值获取和枚举字典解析功能
 *
 * <p>该类主要用于从校验规则配置中提取枚举相关的数据，支持两种配置方式：
 * <ul>
 *   <li>直接配置枚举值列表（List类型）</li>
 *   <li>通过字典KEY引用预定义的枚举值列表（String类型）</li>
 * </ul>
 *
 * @author xiao.hu
 * @date 2024-02-19
 * @apiNote 该类是验证引擎的核心工具类，用于处理字段的枚举值校验
 */
public class Common {

    /**
     * 从规则配置中获取枚举值列表
     *
     * <p>支持两种配置方式：
     * <ol>
     *   <li>直接配置枚举值列表：配置为List类型时，直接返回该列表</li>
     *   <li>通过字典KEY引用：配置为String类型时，通过CommonDict获取预定义的枚举列表</li>
     * </ol>
     *
     * <p><b>数据变化示例：</b>
     * <ul>
     *   <li>输入：rulesMap中 enum_values = ["PENDING", "APPROVED", "REJECTED"]（List类型）
     *       <br>输出：["PENDING", "APPROVED", "REJECTED"]</li>
     *   <li>输入：rulesMap中 enum_values = "order_status"（String类型）
     *       <br>若CommonDict中预定义了 "order_status" -> ["待支付", "已支付", "已发货", "已完成"]
     *       <br>输出：["待支付", "已支付", "已发货", "已完成"]</li>
     *   <li>输入：rulesMap中 enum_values = [1, 2, 3, 4, 5]（数字List类型）
     *       <br>输出：[1, 2, 3, 4, 5]</li>
     *   <li>输入：rulesMap中没有 enum_values 配置
     *       <br>输出：null</li>
     * </ul>
     *
     * @param rulesMap 规则配置映射，包含枚举值相关的配置项
     *                 键为 {@link RuleKey#enum_values}
     * @return 枚举值列表，如果没有配置或类型不支持则返回null
     *         <ul>
     *           <li>List类型：直接返回原列表</li>
     *           <li>String类型：通过CommonDict获取对应的预定义列表</li>
     *           <li>其他类型（null、数字、布尔值等）：返回null</li>
     *         </ul>
     */
    public static List<Object> getEnumValues(Map<String, Object> rulesMap) {
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
     * 获取枚举值的转义字典，主要用于数字类型枚举值的显示文本映射
     *
     * <p>当枚举值为数字类型时，可以通过该字典将数字值映射为可读的文本描述，
     * 用于前端展示或错误提示信息。
     *
     * <p><b>数据变化示例：</b>
     * <ul>
     *   <li>输入：rulesMap中 enum_dict = {0="待处理", 1="处理中", 2="已完成", -1="已取消"}（Map类型）
     *       <br>输出：{0="待处理", 1="处理中", 2="已完成", -1="已取消"}</li>
     *   <li>输入：rulesMap中 enum_dict = {1="男", 2="女", 0="未知"}（Map类型）
     *       <br>输出：{1="男", 2="女", 0="未知"}</li>
     *   <li>输入：rulesMap中 enum_dict = "status_dict"（String类型，非Map）
     *       <br>输出：null（不支持String类型）</li>
     *   <li>输入：rulesMap中没有 enum_dict 配置
     *       <br>输出：null</li>
     * </ul>
     *
     * @param rulesMap 规则配置映射，包含枚举字典相关的配置项
     *                 键为 {@link RuleKey#enum_dict}
     * @return 枚举值到显示文本的映射字典，key为枚举值（通常为数字），value为对应的显示文本
     *         <ul>
     *           <li>Map类型：直接返回该Map</li>
     *           <li>非Map类型（String、List等）：返回null</li>
     *           <li>没有配置：返回null</li>
     *         </ul>
     * @see RuleKey#enum_dict 枚举字典的配置键名
     */
    public static Map<Object, String> getEnumDict(Map<String, Object> rulesMap) {
        Object enumDict = rulesMap.get(RuleKey.enum_dict.name());
        if(enumDict instanceof Map) {
            return (Map<Object, String>) enumDict;
        }
        return null;
    }
}