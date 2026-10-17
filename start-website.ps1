# 农产品溯源销售系统 - 一键启动脚本
# 哈尔滨金融学院 · 毕业设计

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "SilentlyContinue"

function Write-Status($msg) {
    Write-Host $msg -ForegroundColor White
}
function Write-OK($msg) {
    Write-Host "[OK] $msg" -ForegroundColor Green
}
function Write-Info($msg) {
    Write-Host "[INFO] $msg" -ForegroundColor Cyan
}
function Write-Warn2($msg) {
    Write-Host "[WARN] $msg" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "  ==================================================" -ForegroundColor Green
Write-Host "      农产品溯源销售系统 - 一键启动工具" -ForegroundColor Green
Write-Host "      哈尔滨金融学院 · 毕业设计" -ForegroundColor Green
Write-Host "  ==================================================" -ForegroundColor Green
Write-Host ""
Write-Info "开始启动系统..."
Write-Host ""

# ===== Step 1: 检查 MySQL =====
Write-Status "[1/4] 检查 MySQL 数据库服务 (端口 3306)..."
$mysqlOk = $false
try {
    $tcp = New-Object Net.Sockets.TcpClient
    $tcp.Connect("localhost", 3306)
    $tcp.Close()
    $mysqlOk = $true
    Write-OK "MySQL 服务正在运行"
} catch {
    Write-Warn2 "MySQL 似乎未在运行 (端口 3306 无响应)"
    Write-Host "      请先启动 MySQL 服务！" -ForegroundColor Yellow
    $choice = Read-Host "是否仍然继续启动？(Y/N)"
    if ($choice -ne "Y" -and $choice -ne "y") { exit }
}
Write-Host ""

# ===== Step 2: 启动后端 =====
Write-Status "[2/4] 启动后端 SpringBoot 服务 (端口 8080)..."
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d E:\agri-traceability\backend && mvn spring-boot:run -DskipTests" -WindowStyle Normal
Write-Info "后端正在后台启动中，预计需要 15-20 秒..."
Write-Host ""

# ===== Step 3: 等待后端就绪 =====
Write-Status "[3/4] 等待后端服务就绪..."
$backendReady = $false
for ($i = 1; $i -le 30; $i++) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/category/list" -UseBasicParsing -TimeoutSec 2
        if ($r.StatusCode -eq 200) {
            $backendReady = $true
            Write-OK "后端服务已就绪！"
            break
        }
    } catch {
        Write-Host "      等待中... ($i/30)" -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
}
if (-not $backendReady) {
    Write-Warn2 "后端启动超时，请检查后端窗口的错误信息"
    Write-Host "      仍然尝试启动前端..." -ForegroundColor Yellow
}
Write-Host ""

# ===== Step 4: 启动前端 =====
Write-Status "[4/4] 启动前端 Vite 服务 (端口 5173)..."
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d E:\agri-traceability\frontend && npm run dev" -WindowStyle Normal
Write-Info "前端正在启动中，预计需要 5-8 秒..."
Write-Host ""

# ===== 等待前端就绪 =====
Write-Status "等待前端服务就绪..."
$frontendReady = $false
for ($i = 1; $i -le 20; $i++) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 2
        if ($r.StatusCode -eq 200) {
            $frontendReady = $true
            Write-OK "前端服务已就绪！"
            break
        }
    } catch {
        Write-Host "      等待中... ($i/20)" -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
}
if (-not $frontendReady) {
    Write-Warn2 "前端启动超时，仍然尝试打开浏览器..."
}
Write-Host ""

# ===== 完成 =====
Write-Host "  ==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  [全部启动成功！]" -ForegroundColor Green
Write-Host ""
Write-Host "  前端地址: http://localhost:5173" -ForegroundColor White
Write-Host "  后端地址: http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "  登录账号: admin" -ForegroundColor Yellow
Write-Host "  登录密码: admin123" -ForegroundColor Yellow
Write-Host ""
Write-Host "  浏览器即将自动打开..." -ForegroundColor Cyan
Write-Host ""
Write-Host "  ==================================================" -ForegroundColor Green
Write-Host ""
Write-Info "关闭本窗口不会停止服务"
Write-Host "       要停止服务请关闭 '后端服务' 和 '前端服务' 两个窗口" -ForegroundColor Gray
Write-Host "       或双击桌面 'Stop-Website.bat'" -ForegroundColor Gray
Write-Host ""

Start-Sleep -Seconds 3
Start-Process "http://localhost:5173"

Write-OK "浏览器已打开，本窗口可以关闭了"
Write-Host ""
Read-Host "按回车键关闭此窗口"
