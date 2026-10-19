package com.erp.common.core.utils;

import cn.hutool.core.bean.BeanUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 实体 / DTO / VO 之间的属性拷贝。基于 Hutool，按字段名+类型匹配。
 * <p>
 * 为什么不用 MapStruct：本工程字段命名高度一致（entity 与 VO 同名同类型），
 * 用 MapStruct 要额外引入注解处理器并处理它与 Lombok 的处理顺序问题，
 * 骨架阶段性价比不高。将来实体字段变复杂（改名、类型转换、多来源拼装）时，
 * 再针对那几张表单独换成 MapStruct，不影响其他代码。
 * <p>
 * 注意：拷贝是「按名字覆盖」，target 中同名但 source 为 null 的字段会被置为 null。
 */
public final class BeanCopyUtils {

    private BeanCopyUtils() {
    }

    /** 单对象拷贝，targetClass 必须有无参构造 */
    public static <T> T copy(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return BeanUtil.copyProperties(source, targetClass);
    }

    /** 拷贝到已存在的实例上（常用于把 DTO 的修改内容合并进数据库查出来的实体） */
    public static void copy(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanUtil.copyProperties(source, target);
    }

    /** 列表拷贝，null / 空集合安全 */
    public static <T> List<T> copyList(Collection<?> source, Class<T> targetClass) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return BeanUtil.copyToList(source, targetClass);
    }
}
