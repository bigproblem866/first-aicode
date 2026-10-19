# =============================================================================
#  重置本机 MySQL 8 的 root 密码（完整版 / 自验证）
# =============================================================================
#
#  用法
#  ----
#  以【管理员身份】打开 PowerShell（开始菜单搜 PowerShell → 右键 → 以管理员身份运行），执行：
#
#      powershell -ExecutionPolicy Bypass -File "E:\AI-Cladue\AI_MyErp\tools\reset-mysql-root-password.ps1"
#
#  它会做什么
#  ----------
#   1. 检查权限与路径
#   2. 清理残留的 mysqld 进程（上一次失败留下的进程会占着数据目录，导致后续全部失败）
#   3. 停止 MySQL80 服务
#   4. 用 --init-file 参数临时启动一次 mysqld，让它执行「建/改 root 账号 + 授权」
#   5. 结束临时进程，等数据目录真正释放，再启动 MySQL80 服务
#   6. 用新密码实际连接一次来验证，并明确报告成功或失败
#
#  数据不受影响，只改 mysql.user 里的凭证记录。
#
#  ---------------------------------------------------------------------------
#  这个脚本踩过的坑都写在注释里，改动时请先读完，否则很容易重现同样的问题：
#
#  [坑1] 必须传 --defaults-file
#        直接运行 mysqld.exe 时，它不会去找 ProgramData 下的 my.ini
#        （那份配置是 Windows 服务用 --defaults-file 传进去的）。不传就会用
#        默认数据目录 <安装目录>\data —— MSI 装出来的那个目录根本不存在，
#        于是启动即失败：Failed to set datadir ... (OS errno: 2)
#
#  [坑2] 必须等数据目录释放
#        mysqld 被强制结束后，InnoDB 对数据目录的文件句柄不会立刻释放。
#        这时启动服务会失败，而 PowerShell 只报一句"无法启动服务"，
#        真实原因（datadir 被占用）只写在 .err 日志里。
#
#  [坑3] mysql.exe 的参数要写成 --xxx=yyy 并且加引号
#        写成 -h127.0.0.1 会被 PowerShell 拆坏（它会收到 '127'），
#        报 Unknown MySQL server host '127'。用 --host=127.0.0.1 且加引号最稳。
#
#  [坑4] 原生命令写 stderr 时不能开 ErrorActionPreference=Stop
#        PowerShell 5.1 下会让非零退出码变成终止性错误，脚本直接中断，
#        后面的失败处理逻辑根本执行不到。
#
#  [坑5] .ps1 必须带 UTF-8 BOM
#        Windows PowerShell 5.1 读没有 BOM 的 .ps1 时按系统 ANSI 代码页（GBK）解析，
#        中文会乱码甚至破坏语法。同理不要用 .bat 装中文（cmd 的解析更脆弱）。
# =============================================================================

$ErrorActionPreference = 'Stop'

# ----------------------------------------------------------------- 配置项
$MysqldPath   = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe'
$MysqlPath    = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$DefaultsFile = 'C:\ProgramData\MySQL\MySQL Server 8.0\my.ini'
$ServiceName  = 'MySQL80'
$DataDir      = 'C:\ProgramData\MySQL\MySQL Server 8.0\Data'
$InitFile     = Join-Path $env:TEMP 'mysql-root-reset.sql'
$TempLog      = Join-Path $env:TEMP 'mysql-root-reset.out.log'
$TempErrLog   = Join-Path $env:TEMP 'mysql-root-reset.err.log'

# 目标账号。绝大部分 MySQL 都是 root@localhost；改这里可以重置别的账号。
$TargetUser = 'root'
$TargetHost = 'localhost'

# ----------------------------------------------------------------- 小工具
function Say-Step($t) { Write-Host "`n=== $t" -ForegroundColor Cyan }
function Say-Ok($t)   { Write-Host "    [OK] $t" -ForegroundColor Green }
function Say-Bad($t)  { Write-Host "    [!!] $t" -ForegroundColor Red }
function Say-Info($t) { Write-Host "         $t" -ForegroundColor Gray }

# 打印错误日志：失败时的关键证据，别删
function Show-ErrorLogs {
    foreach ($f in @($TempLog, $TempErrLog)) {
        if (Test-Path $f) {
            Say-Info "临时 mysqld 输出（$f）末尾："
            Get-Content $f -Tail 25 -ErrorAction SilentlyContinue |
                ForEach-Object { Say-Info "  $_" }
        }
    }
    $err = Get-ChildItem (Join-Path $DataDir '*.err') -ErrorAction SilentlyContinue |
           Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($err) {
        Say-Info "my.ini 指定的错误日志（$($err.FullName)）末尾："
        Get-Content $err.FullName -Tail 30 -ErrorAction SilentlyContinue |
            ForEach-Object { Say-Info "  $_" }
    } else {
        Say-Info "在 $DataDir 下没找到 .err 日志文件。"
    }
}

# 结束所有 mysqld 并等它们真正退出。返回 $true 表示已清干净。
function Clear-AllMysqld([int]$TimeoutSeconds = 40) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $procs = @(Get-Process mysqld -ErrorAction SilentlyContinue)
        if ($procs.Count -eq 0) { return $true }
        foreach ($p in $procs) {
            try { Stop-Process -Id $p.Id -Force -ErrorAction Stop } catch { }
        }
        Start-Sleep -Seconds 2
    }
    return ((@(Get-Process mysqld -ErrorAction SilentlyContinue)).Count -eq 0)
}

# ------------------------------------------------------------ 0. 前置检查
Say-Step '前置检查'

$isAdmin = ([Security.Principal.WindowsPrincipal] `
            [Security.Principal.WindowsIdentity]::GetCurrent()
           ).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Say-Bad '需要管理员权限。'
    Say-Info '请以管理员身份打开 PowerShell 后重新运行：'
    Say-Info '  开始菜单搜 PowerShell -> 右键 -> 以管理员身份运行'
    exit 1
}

foreach ($pair in @(@('mysqld.exe', $MysqldPath), @('mysql.exe', $MysqlPath))) {
    if (-not (Test-Path $pair[1])) {
        Say-Bad "找不到 $($pair[0])：$($pair[1])"
        Say-Info '请修改脚本顶部的 $MysqldPath / $MysqlPath 后重试。'
        exit 1
    }
}
if (-not (Test-Path $DefaultsFile)) {
    Say-Bad "找不到 MySQL 配置文件：$DefaultsFile"
    Say-Info '想确认服务实际用的是哪个配置文件，执行：'
    Say-Info '  sc qc MySQL80'
    Say-Info '把 BINARY_PATH_NAME 里的 --defaults-file=... 抄到脚本顶部。'
    exit 1
}
Say-Ok '管理员权限、mysqld.exe、mysql.exe、my.ini 均正常'

# ------------------------------------------------------------- 1. 取新密码
Say-Step '设置新密码'
Write-Host ''
Write-Host '  建议先用【字母+数字】的简单密码（如 NewPass123），'
Write-Host '  成功登录后再改成你习惯的复杂密码 —— 特殊字符可能被转义。'
Write-Host ''
$pwd1 = Read-Host '  请输入新的 root 密码'
if ([string]::IsNullOrWhiteSpace($pwd1)) {
    Say-Bad '密码不能为空。'
    exit 1
}
$pwd2 = Read-Host '  请再输入一次确认'
if ($pwd1 -ne $pwd2) {
    Say-Bad '两次输入不一致，已退出（请重新运行）。'
    exit 1
}
Say-Ok '两次输入一致'

# --------------------------------------------------------- 2. 清理残留进程
Say-Step '清理残留的 mysqld 进程'
$before = @(Get-Process mysqld -ErrorAction SilentlyContinue)
if ($before.Count -gt 0) {
    Say-Info "发现 $($before.Count) 个：$(($before | ForEach-Object { $_.Id }) -join ', ')"
} else {
    Say-Info '没有发现'
}

# ------------------------------------------------------------- 3. 停服务
Say-Step "停止服务 $ServiceName"
$svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $svc) {
    Say-Bad "找不到名为 $ServiceName 的服务。"
    Say-Info '先确认服务名：Get-Service *mysql*'
    Say-Info '然后把脚本顶部的 $ServiceName 改成实际名称。'
    exit 1
}
# 先判断状态再停：如果服务已经是停止的（例如上一次失败留下的），
# 直接 Stop-Service 会抛异常，容易被误判成"服务名不对"而错失继续执行的机会。
if ($svc.Status -eq 'Running') {
    try {
        Stop-Service -Name $ServiceName -Force -ErrorAction Stop
        # 用 FromSeconds 而不是字符串 '00:01:00'：
        # 字符串形式很容易写错（比如写 '00:00:60' —— 秒的取值范围是 00-59，
        # 这不是合法 TimeSpan，而且语法检查查不出来，只有执行到才报错）。
        $svc.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(60))
        Say-Ok '服务已停止'
    } catch {
        Say-Bad "停止服务失败：$($_.Exception.Message)"
        exit 1
    }
} else {
    Say-Ok "服务当前状态为 $($svc.Status)，无需停止"
}

# 服务停掉后还活着的 mysqld 一律是残留进程，必须清掉，否则会占着数据目录
if (-not (Clear-AllMysqld)) {
    Say-Bad '有 mysqld 进程无法结束。'
    Say-Info '请在任务管理器中手动结束后，重新运行本脚本。'
    exit 1
}
Say-Ok '没有残留的 mysqld 进程'
Start-Sleep -Seconds 2

# -------------------------------------------------- 4. 写 init SQL 并执行
Say-Step '写入账号规范化 SQL'
# 不只改密码，还确保账号存在且权限完整：
#   CREATE USER IF NOT EXISTS —— 万一这台机器上根本没有 root@localhost
#   GRANT ALL ... WITH GRANT OPTION —— 保证能建库、能授权（后面建库脚本要用）
# 三条一起执行，无论初始状态如何，最后都能得到一个可用的超级管理员。
$initSql = @(
    "CREATE USER IF NOT EXISTS '$TargetUser'@'$TargetHost' IDENTIFIED BY '$pwd1';",
    "ALTER USER '$TargetUser'@'$TargetHost' IDENTIFIED BY '$pwd1';",
    "GRANT ALL PRIVILEGES ON *.* TO '$TargetUser'@'$TargetHost' WITH GRANT OPTION;",
    "FLUSH PRIVILEGES;"
)
Set-Content -Path $InitFile -Value $initSql -Encoding ascii
Say-Ok "已写入 $InitFile"

Say-Step '临时启动 mysqld 以应用新密码（最多等 40 秒）'
Remove-Item $TempLog, $TempErrLog -ErrorAction SilentlyContinue

# --defaults-file 必须在第一个（坑1）
$proc = Start-Process -FilePath $MysqldPath `
                      -ArgumentList "--defaults-file=`"$DefaultsFile`"",
                                    "--init-file=`"$InitFile`"",
                                    '--console' `
                      -PassThru -NoNewWindow `
                      -RedirectStandardOutput $TempLog `
                      -RedirectStandardError $TempErrLog

$deadline = (Get-Date).AddSeconds(40)
$listening = $false
while ((Get-Date) -lt $deadline) {
    if ($proc.HasExited) { break }
    $conn = Test-NetConnection -ComputerName '127.0.0.1' -Port 3306 `
                               -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($conn) { $listening = $true; break }
    Start-Sleep -Milliseconds 800
}

if (-not $listening) {
    Say-Bad '临时 mysqld 没能就绪，ALTER USER 未执行，密码未改动。'
    Show-ErrorLogs
    Write-Host ''
    Say-Info '正在把 MySQL80 服务恢复回去...'
    Clear-AllMysqld | Out-Null
    try { Start-Service $ServiceName; Say-Ok '服务已恢复运行' }
    catch { Say-Bad "服务也没能启动，请手动执行：net start $ServiceName" }
    exit 1
}
Say-Ok '临时 mysqld 已就绪，SQL 已执行'

# -------------------------------------------------- 5. 收尾并重启服务
Say-Step '结束临时 mysqld，等待数据目录释放'
if (-not (Clear-AllMysqld)) {
    Say-Bad 'mysqld 进程无法结束。'
    Say-Info '请在任务管理器中手动结束后，执行：net start MySQL80'
    exit 1
}
Say-Ok '没有 mysqld 进程了（坑2：必须等它退出，否则服务启动会失败）'
Start-Sleep -Seconds 3

Say-Step "重新启动服务 $ServiceName"
try {
    Start-Service $ServiceName
    (Get-Service $ServiceName).WaitForStatus('Running', [TimeSpan]::FromSeconds(60))
    Say-Ok '服务已启动'
} catch {
    Say-Bad "启动服务失败：$($_.Exception.Message)"
    Say-Info '真实原因在错误日志里（PowerShell 报的只是"无法启动服务"）：'
    Show-ErrorLogs
    Write-Host ''
    Say-Info '手动重试：'
    Say-Info "  Get-Process mysqld -EA SilentlyContinue | Stop-Process -Force"
    Say-Info "  Start-Sleep -Seconds 5"
    Say-Info "  Start-Service $ServiceName"
    exit 1
}

# --------------------------------------------------- 6. 用新密码真连一次
Say-Step '用新密码实际连接以验证'

# 坑3：参数用 --xxx=yyy 并加引号，别写 -h127.0.0.1
# 坑4：原生命令会往 stderr 写字，此时必须关掉 Stop，否则脚本会在这里中断
$savedEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$env:MYSQL_PWD = $pwd1        # 用环境变量传密码，不出现在命令行里
$verifyOut = & $MysqlPath '--user=root' '--host=127.0.0.1' '--port=3306' `
                          '--connect-timeout=10' `
                          '-e' 'SELECT VERSION() AS mysql_version;' 2>&1
$verifyCode = $LASTEXITCODE
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
$ErrorActionPreference = $savedEap
Remove-Item $InitFile -ErrorAction SilentlyContinue

if ($verifyCode -eq 0) {
    Write-Host ''
    Write-Host '============================================================' -ForegroundColor Green
    Write-Host '  重置成功，新密码已验证可用' -ForegroundColor Green
    Write-Host '============================================================' -ForegroundColor Green
    $verifyOut | ForEach-Object { Write-Host "  $_" }
    Write-Host ''
    Write-Host '  下一步：建库（只需这一次 root）'
    Write-Host '  在 Git Bash 里执行：'
    Write-Host '    "/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -p < "E:/AI-Cladue/AI_MyErp/sql/00_bootstrap.sql"'
    Write-Host ''
    Write-Host '  记得把新密码记到密码管理器里。'
    exit 0
}

Say-Bad '服务已启动，但用新密码仍然连不上。输出如下：'
$verifyOut | ForEach-Object { Say-Info "  $_" }
Write-Host ''
Say-Info '这种情况通常是：这个 MySQL 上负责管理的账号名不是 root。'
Say-Info '下一步可以放开权限校验启动一次，直接查 mysql.user 看有哪些账号：'
Say-Info "  net stop $ServiceName"
Say-Info "  `"$MysqldPath`" --defaults-file=`"$DefaultsFile`" --skip-grant-tables --console"
Say-Info '  （另开一个窗口）mysql.exe --user=root -e "SELECT user,host FROM mysql.user;"'
exit 1
