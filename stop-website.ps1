# 农产品溯源销售系统 - 一键停止脚本

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "SilentlyContinue"

Write-Host ""
Write-Host "  ==================================================" -ForegroundColor Red
Write-Host "      农产品溯源销售系统 - 一键停止工具" -ForegroundColor Red
Write-Host "  ==================================================" -ForegroundColor Red
Write-Host ""
Write-Host "[INFO] 正在停止所有服务..." -ForegroundColor Cyan
Write-Host ""

# ===== 停止后端 =====
Write-Host "[1/2] 停止后端 SpringBoot 服务..." -ForegroundColor White
$stopped = $false
$ports = @(8080)
foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($conn in $conns) {
        $procId = $conn.OwningProcess
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Write-Host "[OK] 已终止后端进程 (PID: $procId)" -ForegroundColor Green
        $stopped = $true
    }
}
# 也通过窗口标题关闭
Get-Process | Where-Object { $_.MainWindowTitle -like "*后端服务*" -or $_.MainWindowTitle -like "*SpringBoot*" } | ForEach-Object {
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    Write-Host "[OK] 已关闭后端窗口 (PID: $($_.Id))" -ForegroundColor Green
    $stopped = $true
}
if (-not $stopped) {
    Write-Host "[INFO] 后端服务未在运行" -ForegroundColor Gray
}
Write-Host ""

# ===== 停止前端 =====
Write-Host "[2/2] 停止前端 Vite 服务..." -ForegroundColor White
$stopped2 = $false
$conns2 = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
foreach ($conn in $conns2) {
    $procId = $conn.OwningProcess
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    Write-Host "[OK] 已终止前端进程 (PID: $procId)" -ForegroundColor Green
    $stopped2 = $true
}
# 也通过窗口标题关闭
Get-Process | Where-Object { $_.MainWindowTitle -like "*前端服务*" -or $_.MainWindowTitle -like "*Vite*" } | ForEach-Object {
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    Write-Host "[OK] 已关闭前端窗口 (PID: $($_.Id))" -ForegroundColor Green
    $stopped2 = $true
}
if (-not $stopped2) {
    Write-Host "[INFO] 前端服务未在运行" -ForegroundColor Gray
}

Write-Host ""
Write-Host "  ==================================================" -ForegroundColor Red
Write-Host "  [所有服务已停止！]" -ForegroundColor Green
Write-Host "  ==================================================" -ForegroundColor Red
Write-Host ""
Start-Sleep -Seconds 3
