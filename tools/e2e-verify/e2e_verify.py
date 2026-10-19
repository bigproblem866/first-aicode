"""ERP 端到端验证：认证 → 权限 → 主数据 → 库存闭环 → 采购入库闭环。

用法: python -I E2E.py
"""
import json
import urllib.error
import urllib.request
from datetime import datetime
from decimal import Decimal

BASE = "http://localhost:8080"
# 每次运行用唯一编码，这样脚本可以反复跑而不会撞唯一索引
SUF = datetime.now().strftime("%H%M%S")
MC, WC, SC, BUYER = f"E2E-M{SUF}", f"E2E-W{SUF}", f"E2E-S{SUF}", f"e2e_buyer_{SUF}"
results = []


def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode("utf-8") if body is not None else None
    try:
        with urllib.request.urlopen(req, data, timeout=25) as r:
            raw = r.read().decode("utf-8")
            return r.status, (json.loads(raw) if raw else {})
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            return e.code, (json.loads(raw) if raw else {})
        except json.JSONDecodeError:
            return e.code, {"raw": raw}


def check(name, ok, detail=""):
    results.append((ok, name, detail))
    print(("  [PASS] " if ok else "  [FAIL] ") + name + (f"   -> {detail}" if detail else ""))


def body_code(resp):
    return (resp or {}).get("code")


def data_of(resp):
    return (resp or {}).get("data")


print("=" * 72)
print("1. 基础可用性与鉴权")
print("=" * 72)

st, r = call("GET", "/api/health")
check("健康检查返回 200", st == 200 and body_code(r) == 200, f"HTTP={st} code={body_code(r)}")

st, r = call("GET", "/api/system/user/list")
check("不带 Token 调受保护接口返回 401", st == 401 and body_code(r) == 401, f"HTTP={st} code={body_code(r)}")

st, r = call("POST", "/api/auth/login", {"username": "admin", "password": "123456"})
token = data_of(r) or {}
token = token.get("token") if isinstance(token, dict) else None
check("admin/123456 登录成功并拿到 token", body_code(r) == 200 and bool(token), f"code={body_code(r)}")

st, r = call("POST", "/api/auth/login", {"username": "admin", "password": "wrong-password"})
check("错误密码被拒绝", body_code(r) != 200, f"code={body_code(r)} msg={(r or {}).get('msg')}")

st, r = call("GET", "/api/auth/getInfo", token=token)
info = data_of(r) or {}
perms = (info.get("permissions") or []) if isinstance(info, dict) else []
check("getInfo 返回用户信息", body_code(r) == 200 and info.get("user"),
      f"roles={info.get('roles')} 权限数={len(perms)}")
check("超级管理员拿到通配权限 *:*:*", "*:*:*" in perms, f"permissions={perms}")

st, r = call("GET", "/api/auth/routers", token=token)
routers = data_of(r) or []
top = [x.get("path") for x in routers] if isinstance(routers, list) else []
check("动态路由返回 3 个一级菜单（系统管理 / 基础资料 / 供应链）",
      isinstance(routers, list) and len(routers) == 3, f"{top}")
if isinstance(routers, list) and routers:
    first = routers[0]
    check("一级菜单用 Layout 组件、子菜单有 component",
          first.get("component") == "Layout" and any(c.get("component") for c in (first.get("children") or [])),
          f"{first.get('path')} -> {first.get('component')}")

st, r = call("GET", "/api/system/dict/data/type/sys_normal_disable", token=token)
dict_rows = data_of(r) or []
check("字典接口可按类型取数（前端下拉依赖）", body_code(r) == 200 and len(dict_rows) >= 2,
      f"条数={len(dict_rows)}")

print()
print("=" * 72)
print("2. 主数据：物料 / 仓库 / 供应商")
print("=" * 72)

st, r = call("POST", "/api/mdm/material",
             {"materialCode": MC, "materialName": "验证用物料", "unit": "1",
              "materialType": "1", "safetyStock": 10, "purchasePrice": 25.5,
              "salePrice": 39.9, "taxRate": 0.13, "status": "0"}, token=token)
check("新增物料", body_code(r) == 200, f"code={body_code(r)} msg={(r or {}).get('msg')}")

st, r = call("POST", "/api/mdm/material",
             {"materialCode": MC, "materialName": "重复编码"}, token=token)
check("重复编码被拒绝且提示可读", body_code(r) != 200 and "已存在" in str((r or {}).get("msg", "")),
      f"msg={(r or {}).get('msg')}")

st, r = call("GET", f"/api/mdm/material/list?pageNum=1&pageSize=10&materialCode={MC}", token=token)
page = data_of(r) or {}
mats = page.get("rows") or []
material_id = mats[0].get("id") if mats else None
check("物料列表能查到新增的物料", body_code(r) == 200 and len(mats) == 1,
      f"total={page.get('total')} id={material_id}")
check("id 是字符串（防 JS 精度丢失）", isinstance(material_id, str), f"type={type(material_id).__name__}")

st, r = call("POST", "/api/mdm/warehouse",
             {"warehouseCode": WC, "warehouseName": "验证用仓库", "status": "0"}, token=token)
check("新增仓库", body_code(r) == 200, f"msg={(r or {}).get('msg')}")
st, r = call("GET", f"/api/mdm/warehouse/list?pageNum=1&pageSize=10&warehouseCode={WC}", token=token)
whs = (data_of(r) or {}).get("rows") or []
warehouse_id = whs[0].get("id") if whs else None
check("仓库列表能查到", bool(warehouse_id), f"id={warehouse_id}")

st, r = call("POST", "/api/mdm/supplier",
             {"supplierCode": SC, "supplierName": "验证用供应商", "status": "0"}, token=token)
check("新增供应商", body_code(r) == 200, f"msg={(r or {}).get('msg')}")
st, r = call("GET", f"/api/mdm/supplier/list?pageNum=1&pageSize=10&supplierCode={SC}", token=token)
sups = (data_of(r) or {}).get("rows") or []
supplier_id = sups[0].get("id") if sups else None
check("供应商列表能查到", bool(supplier_id), f"id={supplier_id}")

if not all([material_id, warehouse_id, supplier_id]):
    print("\n[中止] 主数据准备失败，后续流程无法进行。")
    for ok, n, d in results:
        if not ok:
            print("  失败项:", n, d)
    raise SystemExit(1)

print()
print("=" * 72)
print("3. 库存闭环：入库单 → 审核 → 台账 + 流水")
print("=" * 72)

IN_QTY = 100
st, r = call("POST", "/api/inv/order",
             {"orderType": "IN", "warehouseId": warehouse_id, "bizDate": "2026-10-16",
              "remark": "E2E 入库", "details": [{"materialId": material_id, "qty": IN_QTY, "price": 25.5}]},
             token=token)
check("新增入库单（单头+明细）", body_code(r) == 200, f"msg={(r or {}).get('msg')}")

st, r = call("GET", "/api/inv/order/list?pageNum=1&pageSize=10&orderType=IN", token=token)
rows = (data_of(r) or {}).get("rows") or []
in_order = rows[0] if rows else None
in_order_id = in_order.get("id") if in_order else None
check("入库单列表能查到（草稿）", bool(in_order_id) and str(in_order.get("status")) == "0",
      f"id={in_order_id} status={in_order.get('status')}")

st, r = call("GET", f"/api/inv/order/{in_order_id}", token=token)
det = (data_of(r) or {}).get("details") or []
check("单据详情返回明细", len(det) == 1, f"明细数={len(det)}")

st, r = call("GET", f"/api/inv/stock/list?pageNum=1&pageSize=10&warehouseId={warehouse_id}&materialId={material_id}",
             token=token)
before_rows = (data_of(r) or {}).get("rows") or []
check("审核前台账里没有该物料库存（还没审核）", len(before_rows) == 0, f"台账行数={len(before_rows)}")

st, r = call("POST", f"/api/inv/order/audit/{in_order_id}", token=token)
check("审核入库单", body_code(r) == 200, f"msg={(r or {}).get('msg')}")

st, r = call("POST", f"/api/inv/order/audit/{in_order_id}", token=token)
check("重复审核被拒绝（不重复加库存）", body_code(r) != 200, f"msg={(r or {}).get('msg')}")

st, r = call("GET", f"/api/inv/stock/list?pageNum=1&pageSize=10&warehouseId={warehouse_id}&materialId={material_id}",
             token=token)
rows = (data_of(r) or {}).get("rows") or []
qty_after = Decimal(str(rows[0]["qty"])) if rows else Decimal(0)
check(f"库存台账数量 = {IN_QTY}", rows and qty_after == Decimal(IN_QTY), f"实际={qty_after}")

st, r = call("GET", f"/api/inv/log/list?pageNum=1&pageSize=10&warehouseId={warehouse_id}&materialId={material_id}",
             token=token)
logs = (data_of(r) or {}).get("rows") or []
check("写入了库存流水", len(logs) == 1, f"流水数={len(logs)}")
if logs:
    lg = logs[0]
    # 用数值比较：后端序列化 BigDecimal 可能是 0 也可能是 0.0，字符串比较会误判
    before_ok = Decimal(str(lg.get("beforeQty"))) == Decimal(0)
    after_ok = Decimal(str(lg.get("afterQty"))) == Decimal(IN_QTY)
    check("流水的变动前/后数量正确", before_ok and after_ok,
          f"bizType={lg.get('bizType')} qty={lg.get('qty')} before={lg.get('beforeQty')} after={lg.get('afterQty')}")
    check("流水能追溯到来源单据",
          lg.get("sourceType") == "INV_ORDER" and bool(lg.get("sourceNo")),
          f"sourceType={lg.get('sourceType')} sourceNo={lg.get('sourceNo')}")
    check("流水带出了物料编码/名称（跨模块取主数据）",
          bool(lg.get("materialCode")) and bool(lg.get("materialName")),
          f"{lg.get('materialCode')} / {lg.get('materialName')}")

# 出库：库存不足必须被拦住
st, r = call("POST", "/api/inv/order",
             {"orderType": "OUT", "warehouseId": warehouse_id, "bizDate": "2026-10-16",
              "details": [{"materialId": material_id, "qty": IN_QTY + 1}]}, token=token)
st2, r2 = call("GET", "/api/inv/order/list?pageNum=1&pageSize=10&orderType=OUT", token=token)
out_rows = (data_of(r2) or {}).get("rows") or []
out_id = out_rows[0].get("id") if out_rows else None
st3, r3 = call("POST", f"/api/inv/order/audit/{out_id}", token=token)
check("库存不足时出库审核被拒绝", body_code(r3) != 200, f"msg={(r3 or {}).get('msg')}")

st, r = call("GET", f"/api/inv/stock/list?pageNum=1&pageSize=10&warehouseId={warehouse_id}&materialId={material_id}",
             token=token)
rows = (data_of(r) or {}).get("rows") or []
check("失败的出库没有改动库存（事务回滚）",
      rows and Decimal(str(rows[0]["qty"])) == Decimal(IN_QTY), f"实际={rows[0]['qty'] if rows else 'N/A'}")

print()
print("=" * 72)
print("4. 采购入库闭环：订单 → 审核 → 收货 → 确认 → 自动入库")
print("=" * 72)

PO_QTY = 60
st, r = call("POST", "/api/pur/order",
             {"supplierId": supplier_id, "orderDate": "2026-10-16", "expectDate": "2026-10-20",
              "details": [{"materialId": material_id, "qty": PO_QTY, "price": 25.0}]}, token=token)
check("新增采购订单", body_code(r) == 200, f"msg={(r or {}).get('msg')}")

st, r = call("GET", "/api/pur/order/list?pageNum=1&pageSize=10", token=token)
po_rows = (data_of(r) or {}).get("rows") or []
po = po_rows[0] if po_rows else None
po_id = po.get("id") if po else None
total_amount = po.get("totalAmount") if po else None
check("订单总金额由后端按明细汇总（未从前端接收）",
      total_amount is not None and Decimal(str(total_amount)) == Decimal(PO_QTY) * Decimal("25.0"),
      f"totalAmount={total_amount} 期望={PO_QTY * 25}")

st, r = call("POST", f"/api/pur/order/audit/{po_id}", token=token)
check("审核采购订单", body_code(r) == 200, f"msg={(r or {}).get('msg')}")

st, r = call("GET", "/api/pur/order/{0}".format(po_id), token=token)
po_detail = data_of(r) or {}
po_det = (po_detail.get("details") or [])
order_detail_id = po_det[0].get("id") if po_det else None
check("订单详情带出明细 id（收货要指回明细行）", bool(order_detail_id), f"orderDetailId={order_detail_id}")

st, r = call("GET", "/api/pur/receipt/pendingOrders", token=token)
pending = data_of(r) or []
check("待收货订单列表能查到该订单", any(str(x.get("id")) == str(po_id) for x in pending),
      f"待收订单数={len(pending)}")

st, r = call("POST", "/api/pur/receipt",
             {"orderId": po_id, "warehouseId": warehouse_id, "receiptDate": "2026-10-16",
              "details": [{"orderDetailId": order_detail_id, "qty": PO_QTY}]}, token=token)
check("新增采购收货单", body_code(r) == 200, f"msg={(r or {}).get('msg')}")

st, r = call("GET", "/api/pur/receipt/list?pageNum=1&pageSize=10", token=token)
rc_rows = (data_of(r) or {}).get("rows") or []
rc = rc_rows[0] if rc_rows else None
rc_id = rc.get("id") if rc else None
check("收货单列表能查到（草稿）", bool(rc_id) and str(rc.get("status")) == "0", f"id={rc_id} status={rc.get('status')}")

st, r = call("POST", f"/api/pur/receipt/confirm/{rc_id}", token=token)
check("确认收货（触发跨模块入库）", body_code(r) == 200, f"msg={(r or {}).get('msg')}")

st, r = call("POST", f"/api/pur/receipt/confirm/{rc_id}", token=token)
check("重复确认被拒绝（不重复入库）", body_code(r) != 200, f"msg={(r or {}).get('msg')}")

st, r = call("GET", f"/api/pur/receipt/{rc_id}", token=token)
rc_detail = data_of(r) or {}
check("收货单回写了生成的入库单号", bool(rc_detail.get("invOrderNo")), f"invOrderNo={rc_detail.get('invOrderNo')}")

st, r = call("GET", f"/api/inv/stock/list?pageNum=1&pageSize=10&warehouseId={warehouse_id}&materialId={material_id}",
             token=token)
rows = (data_of(r) or {}).get("rows") or []
expected = Decimal(IN_QTY) + Decimal(PO_QTY)
actual = Decimal(str(rows[0]["qty"])) if rows else Decimal(-1)
check(f"库存 = 入库{IN_QTY} + 采购收货{PO_QTY} = {expected}", actual == expected, f"实际={actual}")

st, r = call("GET", "/api/inv/order/list?pageNum=1&pageSize=20&orderType=IN", token=token)
in_rows = (data_of(r) or {}).get("rows") or []
# 不能断言"恰好 1 张"：数据库会累积历次运行的数据。
# 应当断言"这张收货单生成的那张入库单确实存在且可追溯"。
gen_no = rc_detail.get("invOrderNo")
from_receipt = [x for x in in_rows
                if x.get("sourceType") == "PUR_RECEIPT" and x.get("orderNo") == gen_no]
check("采购收货自动生成了入库单（sourceType=PUR_RECEIPT 且单号可追溯）",
      bool(gen_no) and len(from_receipt) == 1,
      f"receipt.invOrderNo={gen_no} 在入库单列表中匹配数={len(from_receipt)}")

st, r = call("GET", "/api/pur/order/list?pageNum=1&pageSize=10", token=token)
po_rows = (data_of(r) or {}).get("rows") or []
check("收满后采购订单状态变为已完成(3)", po_rows and str(po_rows[0].get("status")) == "3",
      f"status={po_rows[0].get('status') if po_rows else '-'}")

st, r = call("GET", f"/api/inv/log/list?pageNum=1&pageSize=20&warehouseId={warehouse_id}&materialId={material_id}",
             token=token)
logs = (data_of(r) or {}).get("rows") or []
check("流水共 2 条（手工入库 + 采购收货）", len(logs) == 2, f"流水数={len(logs)}")

print()
print("=" * 72)
print("5. 权限隔离：采购员角色")
print("=" * 72)

st, r = call("POST", "/api/system/user",
             {"username": BUYER, "nickName": "验证用采购员", "password": "buyer123",
              "deptId": 102, "sex": "2", "status": "0", "roleIds": [2]}, token=token)
check("创建采购员用户并分配 purchaser 角色(id=2)", body_code(r) == 200, f"msg={(r or {}).get('msg')}")

st, r = call("POST", "/api/auth/login", {"username": BUYER, "password": "buyer123"})
buyer_token = (data_of(r) or {}).get("token") if body_code(r) == 200 else None
check("采购员能登录", bool(buyer_token), f"code={body_code(r)} msg={(r or {}).get('msg')}")

if buyer_token:
    st, r = call("GET", "/api/auth/routers", token=buyer_token)
    b_routers = data_of(r) or []
    paths = [x.get("path") for x in b_routers] if isinstance(b_routers, list) else []
    # 采购员只被授予：2(基础资料) / 5(供应链) / 3(库存管理) / 4(采购管理) / 204(仓库) …
    # 链路是 5 → 3 → 204 与 5 → 4 → 400/401，任何一层父节点没授权都会被拼树丢掉。
    supply_menu = next(
        (x for x in b_routers if x.get("path") == "/supply"), {}
    ) if isinstance(b_routers, list) else {}
    supply_children = [c.get("path") for c in (supply_menu.get("children") or [])]
    inventory_menu = next(
        (c for c in (supply_menu.get("children") or []) if c.get("path") == "inventory"), {}
    )
    inventory_children = [c.get("path") for c in (inventory_menu.get("children") or [])]

    check("采购员只看到 基础资料 + 供应链（库存下仅仓库、采购全部）",
          isinstance(b_routers, list) and len(b_routers) == 2
          and "/mdm" in paths and "/supply" in paths
          and supply_children == ["inventory", "purchase"]
          and inventory_children == ["warehouse"],
          f"top={paths} supply={supply_children} inventory={inventory_children}")

    st, r = call("GET", "/api/system/user/list", token=buyer_token)
    check("采购员调系统管理接口返回 HTTP 403（契约要求真实状态码）",
          st == 403 and body_code(r) == 403, f"HTTP={st} code={body_code(r)}")

    st, r = call("GET", "/api/mdm/material/list?pageNum=1&pageSize=5", token=buyer_token)
    check("采购员能访问主数据（有权限）", body_code(r) == 200, f"code={body_code(r)}")

print()
print("=" * 72)
failed = [x for x in results if not x[0]]
print(f"总计 {len(results)} 项：通过 {len(results) - len(failed)}，失败 {len(failed)}")
if failed:
    print("\n失败明细：")
    for _, n, d in failed:
        print(f"  - {n}   {d}")
print("=" * 72)
raise SystemExit(1 if failed else 0)
