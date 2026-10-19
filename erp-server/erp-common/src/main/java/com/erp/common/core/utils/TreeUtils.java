package com.erp.common.core.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 扁平列表 → 树结构。
 * <p>
 * 部门、菜单、物料分类都是「一张表 + parent_id」的树形结构，前端要的是嵌套 JSON。
 * 这个工具统一处理，避免每个 Service 各写一遍递归。
 * <p>
 * 用方法引用取字段，所以实体不需要实现任何接口：
 * <pre>
 *   List&lt;SysDept&gt; tree = TreeUtils.build(deptList, SysDept::getId, SysDept::getParentId,
 *                                      SysDept::setChildren, 0L);
 * </pre>
 * 实现是「建索引 + 挂父子」两趟遍历，不做嵌套循环匹配 —— 菜单几百个节点时差异明显。
 */
public final class TreeUtils {

    private TreeUtils() {
    }

    /**
     * @param nodes          扁平节点列表，需已按期望顺序排好（如 order_num）
     * @param idGetter       取节点自身 ID
     * @param parentIdGetter 取父节点 ID
     * @param childrenSetter 设置子节点列表
     * @param rootParentId   根节点的父 ID（一般为 0L）
     * @return 根节点列表（每个节点的 children 都会被赋值，无子节点时为空列表而非 null）
     */
    public static <T> List<T> build(List<T> nodes,
                                    Function<T, Long> idGetter,
                                    Function<T, Long> parentIdGetter,
                                    BiConsumer<T, List<T>> childrenSetter,
                                    Long rootParentId) {
        List<T> roots = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return roots;
        }

        // 第一趟：建 ID → 节点索引
        Map<Long, T> idToNode = new LinkedHashMap<>(nodes.size());
        for (T node : nodes) {
            Long id = idGetter.apply(node);
            if (id != null) {
                idToNode.put(id, node);
            }
        }

        // 第二趟：按父 ID 归组。这里不能边遍历边挂到节点上，
        // 否则每个节点只能保留最后一个子节点（早期版本就是这么错的）。
        Map<Long, List<T>> childrenMap = new LinkedHashMap<>();
        for (T node : nodes) {
            Long parentId = parentIdGetter.apply(node);
            if (parentId == null || parentId.equals(rootParentId)) {
                roots.add(node);
                continue;
            }
            if (!idToNode.containsKey(parentId)) {
                // 孤儿节点：父级不在结果集里（按权限过滤后很常见：
                // 有子菜单权限但没有父目录权限）。挂不上，直接丢弃。
                continue;
            }
            childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
        }

        // 第三趟：统一赋值 children。空列表而不是 null，
        // 前端 v-for 拿到 [] 和 null 的行为不同，统一给 [] 少一处判空。
        for (Map.Entry<Long, T> entry : idToNode.entrySet()) {
            List<T> children = childrenMap.getOrDefault(entry.getKey(), new ArrayList<>());
            childrenSetter.accept(entry.getValue(), children);
        }
        return roots;
    }

    /**
     * 收集某节点及其全部后代的 ID，用于「删除父级前校验是否存在下级」。
     * <p>
     * 用迭代而不是递归：层级异常（数据被改坏成环）时递归会栈溢出且难以定位，
     * 迭代版本遇到环只是循环结束、结果略多而已。
     */
    public static <T> List<Long> collectSelfAndDescendantIds(List<T> nodes,
                                                             Function<T, Long> idGetter,
                                                             Function<T, Long> parentIdGetter,
                                                             Long rootId) {
        List<Long> result = new ArrayList<>();
        if (rootId == null) {
            return result;
        }
        result.add(rootId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (T node : nodes) {
                Long id = idGetter.apply(node);
                Long parentId = parentIdGetter.apply(node);
                if (id != null && parentId != null
                        && result.contains(parentId) && !result.contains(id)) {
                    result.add(id);
                    changed = true;
                }
            }
        }
        return result;
    }
}
