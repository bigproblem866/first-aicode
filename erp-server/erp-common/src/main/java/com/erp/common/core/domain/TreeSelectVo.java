package com.erp.common.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 通用树形下拉选项，供前端 el-tree-select / el-cascader 使用。
 * <p>
 * 放在 erp-common 而不是某个业务模块里，是因为它被多个模块共用
 * （部门树、菜单树、物料分类树），而业务模块之间不允许互相依赖。
 * <p>
 * 早期版本在 erp-system 和 erp-mdm 里各建了一份同名类，看起来"符合模块独立"，
 * 实际上有两个问题：
 * <ol>
 *   <li>两份结构会各自演化。哪天给其中一份加了字段，两个接口返回的形状就不一致了，
 *       前端同一套渲染逻辑会莫名其妙在某个页面失效。</li>
 *   <li>MyBatis-Plus 的 {@code type-aliases-package} 会把 domain 包下的类按<b>简单类名</b>
 *       注册别名，两个同名类直接导致应用启动失败：
 *       {@code The alias 'TreeSelectVo' is already mapped to ...}。</li>
 * </ol>
 * 「模块之间不互相依赖」指的是不要依赖对方的业务能力，不是不能共享数据结构 ——
 * 共享结构就该放 common。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TreeSelectVo {

    /** 节点唯一标识。用字符串而不是 Long：前端 el-tree 的 node-key 用字符串更稳 */
    private String id;

    /** 显示文本 */
    private String label;

    /** 子节点。无子节点时为 null，序列化时被 NON_NULL 过滤掉 */
    private List<TreeSelectVo> children;

    public TreeSelectVo() {
    }

    public TreeSelectVo(String id, String label, List<TreeSelectVo> children) {
        this.id = id;
        this.label = label;
        this.children = children;
    }
}
