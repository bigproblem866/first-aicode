# 静态校验一个 PowerShell 脚本：
#   1. 语法解析
#   2. 用到的每个命令是否存在（含脚本内自定义函数）
#   3. 用到的每个命名参数是否真属于那个命令
#   4. 字符串形式的时间字面量是否合法（如 '00:00:60' 是非法 TimeSpan，语法检查查不出来）
# 目的：把"运行到那一行才报错"的问题提前筛掉。
param([Parameter(Mandatory=$true)][string]$Path)

$fail = 0

$tokens = $null; $errors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile($Path, [ref]$tokens, [ref]$errors)

Write-Host '=== 1. 语法解析 ==='
if ($errors -and $errors.Count -gt 0) {
    $fail++
    $errors | ForEach-Object { Write-Host ("  [!!] 行 " + $_.Extent.StartLineNumber + ": " + $_.Message) }
} else {
    Write-Host '  [OK] 0 个语法错误'
}

# 脚本内定义的函数，视为已知命令
$localFuncs = @()
$ast.FindAll({ param($n) $n -is [System.Management.Automation.Language.FunctionDefinitionAst] }, $true) |
    ForEach-Object { $localFuncs += $_.Name }
Write-Host ("  [i] 脚本内自定义函数: " + ($localFuncs -join ', '))

# ---------------------------------------------------------------- 命令与参数
$cmdAsts = $ast.FindAll({ param($n) $n -is [System.Management.Automation.Language.CommandAst] }, $true)

Write-Host ''
Write-Host '=== 2. 命令与参数校验 ==='
$seen = @{}
foreach ($c in $cmdAsts) {
    $name = $c.GetCommandName()
    if ([string]::IsNullOrWhiteSpace($name)) { continue }   # 形如 & $var 的动态调用，跳过

    # 注意：这里刻意【不去重】。同一个命令可能出现多次，每次用的参数都不同，
    # 只检查第一次会漏掉后面所有调用的坏参数 —— 这个漏洞让校验器少抓了一个错误。
    $seen[$name] = $true

    # 自定义函数
    if ($localFuncs -contains $name) { continue }

    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if (-not $cmd) {
        $fail++
        Write-Host ("  [!!] 命令不存在: $name  (行 " + $c.Extent.StartLineNumber + ")")
        continue
    }

    # 该命令支持的所有参数名
    $validParams = @()
    if ($cmd.Parameters) { $validParams = $cmd.Parameters.Keys }

    # 收集这次调用里用到的命名参数（-Foo / -Foo:bar / -Foo bar）
    $used = @()
    foreach ($el in $c.CommandElements) {
        if ($el -is [System.Management.Automation.Language.CommandParameterAst]) {
            $used += $el.ParameterName
        }
    }
    $bad = @()
    foreach ($p in ($used | Select-Object -Unique)) {
        if ($validParams -notcontains $p) { $bad += $p }
    }
    if ($bad.Count -gt 0) {
        $fail++
        Write-Host ("  [!!] $name 不支持这些参数: " + ($bad -join ', ') + "  (行 " + $c.Extent.StartLineNumber + ")")
    }
}
Write-Host ("  [i] 命令调用共 " + $cmdAsts.Count + " 处，涉及 " + $seen.Count + " 个不同的命令")

# -------------------------------------------------------------- 时间字面量
Write-Host ''
Write-Host '=== 3. 字符串形式的时间字面量 ==='
$timeLit = 0
foreach ($t in $tokens) {
    if ($t.Kind -ne 'StringLiteral' -and $t.Kind -ne 'StringExpandable') { continue }
    $v = $t.Value
    if ($v -notmatch '^\d{1,2}:\d{1,2}(:\d{1,2})?(\.\d+)?$') { continue }
    $timeLit++
    try {
        $ts = [TimeSpan]$v
        Write-Host ("  [OK] '$v' = " + $ts.TotalSeconds + " 秒")
    } catch {
        $fail++
        Write-Host ("  [!!] 非法 TimeSpan: '$v'  (行 " + $t.Extent.StartLineNumber + ")")
    }
}
if ($timeLit -eq 0) { Write-Host '  [i] 没有发现字符串形式的时间字面量' }

# ---------------------------------------------------------------- 结论
Write-Host ''
if ($fail -eq 0) {
    Write-Host '===== 校验通过：0 个问题 =====' -ForegroundColor Green
    exit 0
} else {
    Write-Host "===== 发现 $fail 处问题 =====" -ForegroundColor Red
    exit 1
}
