package com.erp.common.core.utils;

import cn.hutool.core.util.StrUtil;

import java.util.Collection;
import java.util.Map;

/**
 * 字符串工具。绝大多数方法直接委托给 Hutool，这里只做一层薄封装，
 * 目的是让业务代码统一用 {@code StringUtils.isBlank()} 一套写法，
 * 想要更全的能力直接用 {@code StrUtil} 也可以。
 */
public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isEmpty(String str) {
        return StrUtil.isEmpty(str);
    }

    public static boolean isNotEmpty(String str) {
        return StrUtil.isNotEmpty(str);
    }

    public static boolean isBlank(String str) {
        return StrUtil.isBlank(str);
    }

    public static boolean isNotBlank(String str) {
        return StrUtil.isNotBlank(str);
    }

    /** 所有入参都非空白才算 true */
    public static boolean isNotBlank(String... strs) {
        return StrUtil.isAllNotBlank(strs);
    }

    public static boolean isBlank(String... strs) {
        return StrUtil.isAllBlank(strs);
    }

    /** null 安全，返回去空格后的值或默认值 */
    public static String defaultIfBlank(String str, String defaultValue) {
        return StrUtil.isBlank(str) ? defaultValue : str;
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }

    public static boolean isNotEmpty(Object[] arr) {
        return !isEmpty(arr);
    }

    /** 把逗号分隔的字符串切成 Long 列表，常用于 {@code DELETE /{ids}} 里的 "1,2,3" */
    public static java.util.List<Long> splitToLong(String str) {
        return StrUtil.split(str, ',').stream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(Long::valueOf)
                .toList();
    }

    /** 两端补 padding，用于让编码/单据号 SQL 里能直接 LIKE 匹配 */
    public static String padLeadingZero(long value, int length) {
        return StrUtil.padPre(String.valueOf(value), length, '0');
    }
}
