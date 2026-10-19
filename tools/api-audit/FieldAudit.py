"""前后端接口对账：字段级。

发现的问题类型：字段名写错导致的**静默失效** ——
比 404 更危险，因为接口调通了、页面也正常打开，只是数据不对：
  - 前端传 queryParams.bizDateStart，后端 DTO 里叫 bizDateBegin
    → 后端收不到这个参数，当成没传，筛选条件被忽略
  - 前端表格写 prop="materialCode"，后端 VO 里没这个字段
    → 这一列渲染成空

做法：把后端 OpenAPI 文档里所有 schema 的属性名收集成一个集合，
再看前端的 queryParams.X 与 prop="X" 有没有不在集合里的。

注意这是**筛查**不是证明：用"全体字段的并集"而不是"精确到某个接口的字段"，
会有假阳性（某个字段在别的模块合法）。但假阴性少，命中后逐条人工确认即可。
要彻底消除假阳性，得把每个页面映射到它调用的那个 DTO，成本高得多，MVP 阶段不值得。

用法：
    python -I FieldAudit.py <前端src目录> <后端地址>
"""
import json
import re
import sys
import urllib.request
from collections import defaultdict
from pathlib import Path

GROUPS = ["system", "mdm", "inventory", "purchase"]

# PageQuery 提供的通用分页参数，不在各模块 DTO 里重复定义
COMMON = {"pageNum", "pageSize", "orderByColumn", "isAsc"}


def backend_fields(base_url: str) -> set[str]:
    fields: set[str] = set()
    for g in GROUPS:
        with urllib.request.urlopen(f"{base_url}/v3/api-docs/{g}", timeout=20) as resp:
            doc = json.loads(resp.read().decode("utf-8"))
        for schema in doc.get("components", {}).get("schemas", {}).values():
            fields.update(schema.get("properties", {}).keys())
        # 简单类型的查询参数不会出现在 components.schemas 里，单独收一遍
        for ops in doc.get("paths", {}).values():
            for op in ops.values():
                for pm in op.get("parameters", []) or []:
                    if pm.get("in") == "query":
                        fields.add(pm["name"])
    return fields


def main() -> None:
    web_src = Path(sys.argv[1])
    base_url = sys.argv[2].rstrip("/")

    be = backend_fields(base_url) | COMMON
    print(f"后端字段名总数（全部 schema 并集）: {len(be)}\n")

    unknown_query: dict[str, set[str]] = defaultdict(set)
    unknown_prop: dict[str, set[str]] = defaultdict(set)
    vue_count = q_count = p_count = 0

    for vue in sorted(web_src.glob("views/**/*.vue")):
        vue_count += 1
        text = vue.read_text(encoding="utf-8")
        rel = str(vue.relative_to(web_src)).replace("\\", "/")

        for name in re.findall(r"queryParams\.(\w+)", text):
            q_count += 1
            if name not in be:
                unknown_query[name].add(rel)

        for name in re.findall(r'prop="(\w+)"', text):
            p_count += 1
            if name not in be:
                unknown_prop[name].add(rel)

    # 打印扫描量：避免"脚本没扫到文件"被误读成"全部通过"
    print(f"扫描页面 {vue_count} 个，queryParams 引用 {q_count} 处，prop 引用 {p_count} 处\n")

    print("=" * 70)
    print("[1] 前端 queryParams 有、后端任何 schema 都没有的字段")
    print("    => 该筛选条件后端收不到，会被静默忽略")
    print("=" * 70)
    if not unknown_query:
        print("    无")
    for name, files in sorted(unknown_query.items()):
        print(f"    {name:24s} <- {', '.join(sorted(files))}")

    print()
    print("=" * 70)
    print("[2] 前端表格 prop 有、后端任何 schema 都没有的字段")
    print("    => 这一列会渲染成空（不报错）")
    print("=" * 70)
    if not unknown_prop:
        print("    无")
    for name, files in sorted(unknown_prop.items()):
        print(f"    {name:24s} <- {', '.join(sorted(files))}")

    sys.exit(1 if (unknown_query or unknown_prop) else 0)


if __name__ == "__main__":
    main()
