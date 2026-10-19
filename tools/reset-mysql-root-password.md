# 重置本机 MySQL 8 的 root 密码

配套脚本：`reset-mysql-root-password.bat`（右键 → 以管理员身份运行）

---

## 为什么不直接执行，而要你自己动手

两件事决定了必须由你在本机操作：

1. 修改 Windows 服务需要**管理员提权**，得由你确认；
2. 这一改动会影响你本机的 MySQL 实例，不该由工具静默完成。

---

## 这个脚本做了什么

MySQL 支持在启动时执行一个 SQL 文件（`--init-file` 参数）。脚本的做法是：

1. 停止 `MySQL80` 服务
2. 以 `--init-file` 参数临时启动一次 `mysqld`，它会执行里面那一条 `ALTER USER` 把密码改掉
3. 关掉这个临时进程
4. 重新启动 `MySQL80` 服务

**数据不会受影响**，只是改了 `mysql.user` 表里的一条凭证记录。

### 为什么不用 `--skip-grant-tables`

网上更常见的做法是 `--skip-grant-tables`（跳过权限校验启动），但它在重置期间会让
**任何能连到 3306 的人都能以任意身份操作数据库**，存在一个真实的敞开窗口。
`--init-file` 没有这个窗口，所以用它。

---

## 为什么脚本里全是英文

`cmd.exe` 是**按系统 ANSI 代码页（中文 Windows 上是 GBK）逐字节读取 .bat 文件**的。
如果文件里存的是 UTF-8 中文，某些多字节序列会「吃掉」后面的 `\r`，导致**两行被合并**，
原本的 `::` 注释行失去注释符、被当成命令执行，报出这样的错：

```
'xxxx' is not recognized as an internal or external command
```

在文件开头写 `chcp 65001` **救不了**——那行生效之前，文件已经在用错误的代码页被读取了。

所以这个脚本保持纯 ASCII，在任何代码页下都能跑；中文说明放在本文档里。

> 顺带一提，脚本还必须用 **CRLF** 行尾。LF-only 的批处理文件在解析多行
> `if (...)` 块时可能出错。这两个坑（编码、行尾）都已处理。

---

## 手动执行方式（不想用脚本时）

以管理员身份打开 CMD，逐条执行：

```cmd
net stop MySQL80

echo ALTER USER 'root'@'localhost' IDENTIFIED BY 'NewPass123' > %TEMP%\reset.sql

"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --defaults-file="C:\ProgramData\MySQL\MySQL Server 8.0\my.ini" --init-file="%TEMP%\reset.sql" --console
```

> ⚠ **`--defaults-file` 不能省。** 它必须是第一个参数。原因见下一节。

第三条会占住当前窗口并开始输出日志。等它打印出 `ready for connections` 之后，
按 **Ctrl+C** 结束它，然后执行：

```cmd
net start MySQL80
```

验证：

```cmd
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p
```

能进去就说明重置成功。

---

## 重置过程中实际踩到的三个坑（都是真实报错）

### 1. 直接运行 mysqld 会因数据目录不存在而启动失败

```
[ERROR] [MY-013276] [Server] Failed to set datadir to
        'C:\Program Files\MySQL\MySQL Server 8.0\data\' (OS errno: 2 - No such file or directory)
[ERROR] [MY-010119] [Server] Aborting
```

**原因**：`mysqld.exe` 直接运行时**不会**去 `C:\ProgramData\MySQL\MySQL Server 8.0\` 找 `my.ini` —
那份配置是 Windows 服务通过 `--defaults-file` 参数传给它的。不传这个参数，
mysqld 就用默认数据目录 `<安装目录>\data`，而 MSI 安装的真实数据目录在
`C:\ProgramData\MySQL\MySQL Server 8.0\Data`，默认那个根本不存在。

**修法**：把 `--defaults-file` 作为第一个参数显式传进去（脚本已处理）。

**排查命令**：想知道服务到底怎么启动的，执行
```
sc qc MySQL80
```
看 `BINARY_PATH_NAME` 里的启动参数。

### 2. `.bat` 脚本里的中文会让注释行变成命令

cmd.exe 按系统 ANSI 代码页（中文 Windows 上是 GBK）**逐字节**解析 .bat 文件。
UTF-8 的中文字节被当 GBK 读时，某些双字节序列会吃掉后面的 `\r`，导致**两行被合并**，
`::` 注释行失去注释符而被当命令执行：

```
'xxxx' is not recognized as an internal or external command
```

开头写 `chcp 65001` **没用**——那行生效之前文件已经用错代码页读完了。
所以用脚本的话选 **PowerShell 版**；如果非要用 .bat，必须纯 ASCII + CRLF 行尾。

### 3. 临时 mysqld 没被清理时会一直占着数据目录

第一次尝试失败后，出现了第二个 `mysqld.exe` 进程，它不监听任何端口，但占着数据目录，
会让后续的临时 mysqld 起不来——表现为"重置怎么试都无效"。

所以脚本现在会**在停止服务后清掉所有残留 mysqld**，并且只杀自己启动的那个进程
（用 `Start-Process -PassThru` 拿 PID），而不是 `taskkill /IM mysqld.exe` 无差别全杀。

---

## 设计教训（为什么脚本最后一定要自己验证）

第一版 `.bat` 脚本在临时 mysqld 启动失败的情况下**依然打印了"完成"** ——
因为它无法判断 `start /B mysqld` 到底起没起来，也无法判断 `ALTER USER` 是否执行。

结果就是：用户以为重置成功了，实际什么都没改，两边白折腾几轮。

**任何"执行了一串命令就算完成"的脚本都不可信**，必须有一个可观测的成功判据。
这里有三个：
1. 临时 mysqld 进程是否还活着（提前退出 = 失败）
2. 端口 3306 是否真的可连
3. **最后用新密码实际连一次数据库**（最有力的一条）

### 注意事项

- **新密码先用字母+数字**（如 `NewPass123`）。特殊字符可能被命令行转义，导致脚本失败。
  进去之后再改成你习惯的复杂密码。
- `mysqld.exe` 的路径按你的实际安装位置调整。
- 如果 `net start MySQL80` 失败，看
  `C:\ProgramData\MySQL\MySQL Server 8.0\` 下的 `.err` 日志文件
  （常见原因是 `my.ini` 或数据目录权限）。

---

## 重置之后

把新密码记到密码管理器里。然后回到项目根目录执行建库脚本（需要 root，只这一次）：

```bash
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -p < "E:/AI-Cladue/AI_MyErp/sql/00_bootstrap.sql"
```

它会创建 `erp_dev` 库和应用账号 `erp`。之后建表由 Flyway 在应用启动时自动完成，
**不再需要 root**。
