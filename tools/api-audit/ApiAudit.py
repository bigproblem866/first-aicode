"""前后端接口对账：路由级。

发现的问题类型：前端调了一个后端根本不存在的接口（404/405）。

做法：后台路由清单不靠 grep 注解，而是从**运行中的应用**的 OpenAPI 文档里取 ——
那是唯一权威来源（能正确处理类级 @RequestMapping 前缀、路径拼接、动态参数）。
前端则解析 api/*.js 里的 `const BASE = '...'` + 模板字符串拼出的实际路径。

比对 (HTTP方法, 路径) 二元组。只比路径会漏掉"路径对但方法错"，那同样是 405。

路径变量名两边经常不一样（前端 ${id} vs 后端 {userId}），所以统一归一化成 *。

用法：
    # 先启动后端（不需要数据库，见 README 的说明），然后
    python -I ApiAudit.py <前端src目录> <后端地址>
    例：python -I ApiAudit.py E:/AI-Cladue/AI_MyErp/erp-web/src http://localhost:8080
"""
import json
import re
import sys
import urllib.request
from pathlib import Path

GROUPS = ["system", "mdm", "inventory", "purchase"]
VAR_RE = re.compile(r"\{[^}]*\}")


def canon(path: str) -> str:
    """把路径变量统一成 *，忽略变量名差异"""
    return VAR_RE.sub("*", path)


def backend_routes(base_url: str) -> set[tuple[str, str]]:
    routes: set[tuple[str, str]] = set()
    for g in GROUPS:
        with urllib.request.urlopen(f"{base_url}/v3/api-docs/{g}", timeout=20) as resp:
            doc = json.loads(resp.read().decode("utf-8"))
        for path, ops in doc.get("paths", {}).items():
            # 去掉 /api 前缀，与前端 baseURL='/api' 的相对路径对齐
            rel = re.sub(r"^/api", "", path)
            for method in ops:
                routes.add((method.upper(), canon(rel)))
    return routes


def frontend_routes(web_src: Path) -> dict[tuple[str, str], str]:
    routes: dict[tuple[str, str], str] = {}
    for js in sorted(web_src.glob("api/**/*.js")):
        text = js.read_text(encoding="utf-8")
        consts = dict(re.findall(r"const\s+(\w+)\s*=\s*'([^']*)'", text))

        # 按 request( 切块，每块取 url 与 method（两者在同一个对象字面量里，顺序不定）
        for chunk in text.split("request(")[1:]:
            chunk = chunk[:600]
            url_m = re.search(r"url:\s*(`[^`]*`|'[^']*'|[A-Za-z_]\w*)", chunk)
            method_m = re.search(r"method:\s*'(\w+)'", chunk)
            if not url_m or not method_m:
                continue
            url = url_m.group(1).strip("`'")
            # url 可能写成裸标识符（url: BASE），查表展开
            if url in consts:
                url = consts[url]
            for name, value in consts.items():
                url = url.replace("${" + name + "}", value)
            url = re.sub(r"\$\{[^}]+\}", "{v}", url)
            if not url.startswith("/"):
                continue
            routes[(method_m.group(1).upper(), canon(url))] = js.name
    return routes


def main() -> None:
    web_src = Path(sys.argv[1])
    base_url = sys.argv[2].rstrip("/")

    be = backend_routes(base_url)
    fe = frontend_routes(web_src)
    print(f"后端注册 (方法,路径): {len(be)} 条")
    print(f"前端调用 (方法,路径): {len(fe)} 条\n")

    missing = sorted((k, v) for k, v in fe.items() if k not in be)
    print(f"[OK]      对得上: {len(fe) - len(missing)} 条")
    print(f"[MISSING] 前端调了但后端没有（会 404/405）: {len(missing)} 条")
    for (method, path), f in missing:
        print(f"   {method:6s} {path:42s} <- {f}")

    unused = sorted(k for k in be if k not in fe)
    print(f"\n[INFO]    后端有、前端未调用: {len(unused)} 条")
    for method, path in unused:
        print(f"   {method:6s} {path}")

    sys.exit(1 if missing else 0)


if __name__ == "__main__":
    main()
