# RAG 检索评测脚本：登录 -> 逐条调用 /api/kb/search -> 统计 Recall@5 与拒答率
# 用法：powershell -ExecutionPolicy Bypass -File .\run-rag-eval.ps1
# 说明：中文请求/响应必须按 UTF-8 字节收发，否则会出现 ???? 导致全部 no-hit

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "kb_swagger_test",
    [string]$Password = "12345678",
    [int]$TopK = 5,
    # 阈值：0 表示不传、使用服务端默认值；调参时显式传入便于对照
    [double]$MinScore = 0,
    [string]$EvalFile = "$PSScriptRoot\rag-eval-v1.json"
)

# 统一按 UTF-8 解码响应体，规避 PowerShell 5 默认按 ISO-8859-1 解码导致的乱码
function Invoke-JsonUtf8 {
    param(
        [string]$Uri,
        [string]$Method,
        [string]$Body,
        [hashtable]$Headers = @{}
    )
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
    $resp = Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers `
        -ContentType "application/json; charset=utf-8" -Body $bytes -UseBasicParsing
    $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    return $text | ConvertFrom-Json
}

# 1. 登录拿 token（单端互踢下每次评测都重新登录，避免旧 token 失效）
$login = Invoke-JsonUtf8 -Uri "$BaseUrl/api/auth/login" -Method "Post" `
    -Body (@{ username = $Username; password = $Password } | ConvertTo-Json -Compress)
if ($login.code -ne 200) {
    throw "登录失败: $($login.message)"
}
$authHeader = @{ Authorization = "Bearer $($login.data)" }

# 2. 读取评测集（UTF-8，避免中文标题被读坏）
$cases = Get-Content -Path $EvalFile -Raw -Encoding UTF8 | ConvertFrom-Json

# 3. 逐条检索（同时记录单次耗时，用于统计 P50/P95）
$recallPass = 0
$recallTotal = 0
$rejectPass = 0
$rejectTotal = 0
$failures = @()
$times = @()

foreach ($case in $cases) {
    $bodyHash = @{ query = $case.question; topK = $TopK }
    if ($MinScore -gt 0) { $bodyHash["minScore"] = $MinScore }
    $body = $bodyHash | ConvertTo-Json -Compress
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $res = Invoke-JsonUtf8 -Uri "$BaseUrl/api/kb/search" -Method "Post" -Body $body -Headers $authHeader
    $sw.Stop()
    $times += $sw.ElapsedMilliseconds
    $data = $res.data

    if ($case.type -eq "answerable") {
        $recallTotal++
        $rank = -1
        for ($i = 0; $i -lt $data.results.Count; $i++) {
            if ($data.results[$i].documentTitle -eq $case.expectedTitle) {
                $rank = $i + 1
                break
            }
        }
        if ($rank -gt 0) {
            $recallPass++
        } else {
            $failures += "[#$($case.id)] 未命中期望文档『$($case.expectedTitle)』 | 问题：$($case.question) | 实际：$(($data.results | ForEach-Object { $_.documentTitle }) -join ', ')"
        }
    } else {
        $rejectTotal++
        if (-not $data.hit) {
            $rejectPass++
        } else {
            $failures += "[#$($case.id)] 负样本仍返回命中 | 问题：$($case.question) | 实际：$(($data.results | ForEach-Object { $_.documentTitle }) -join ', ')"
        }
    }
}

# 4. 汇总
$recall = if ($recallTotal -gt 0) { [math]::Round($recallPass / $recallTotal, 4) } else { 0 }
$reject = if ($rejectTotal -gt 0) { [math]::Round($rejectPass / $rejectTotal, 4) } else { 0 }

# 延迟分位数：升序后取最近秩（ceil(p*n)-1），样本量小也不至于越界
function Get-Percentile {
    param([double[]]$Values, [double]$P)
    if ($Values.Count -eq 0) { return 0 }
    $sorted = $Values | Sort-Object
    $idx = [math]::Ceiling($P * $sorted.Count) - 1
    if ($idx -lt 0) { $idx = 0 }
    if ($idx -ge $sorted.Count) { $idx = $sorted.Count - 1 }
    return [int]$sorted[$idx]
}
$p50 = Get-Percentile -Values $times -P 0.50
$p95 = Get-Percentile -Values $times -P 0.95
$max = if ($times.Count -gt 0) { ($times | Measure-Object -Maximum).Maximum } else { 0 }

Write-Host ""
Write-Host "==== RAG 评测结果 (topK=$TopK) ===="
Write-Host ("Recall@$TopK : {0}/{1} = {2}" -f $recallPass, $recallTotal, $recall)
Write-Host ("拒答率      : {0}/{1} = {2}" -f $rejectPass, $rejectTotal, $reject)
Write-Host ("延迟(ms)    : P50={0} P95={1} Max={2} (n={3})" -f $p50, $p95, $max, $times.Count)
if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "==== 未通过用例 ===="
    $failures | ForEach-Object { Write-Host $_ }
}

# 有失败用例时返回非 0，便于后续接入 CI
if ($failures.Count -gt 0) { exit 1 } else { exit 0 }
