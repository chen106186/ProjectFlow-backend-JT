# 本地打镜像 + 导出为 tar，方便传到服务器
param(
    [string]$Tag = "latest"
)

$ErrorActionPreference = "Stop"

$ROOT = Split-Path -Parent $MyInvocation.MyCommand.Definition
$FRONTEND = "$ROOT\..\ProjectFlow-frontend-JT"
$OUT = "$ROOT\dist-images"

New-Item -ItemType Directory -Force -Path $OUT | Out-Null

Write-Host ">>> 构建后端镜像..." -ForegroundColor Cyan
docker build -t "projectflow-backend:$Tag" $ROOT
if ($LASTEXITCODE -ne 0) { throw "后端镜像构建失败" }

Write-Host ">>> 构建前端镜像..." -ForegroundColor Cyan
docker build -t "projectflow-frontend:$Tag" $FRONTEND
if ($LASTEXITCODE -ne 0) { throw "前端镜像构建失败" }

Write-Host ">>> 导出镜像到 dist-images/ ..." -ForegroundColor Cyan
docker save projectflow-backend:$Tag  | gzip > "$OUT\projectflow-backend.tar.gz"
docker save projectflow-frontend:$Tag | gzip > "$OUT\projectflow-frontend.tar.gz"

Copy-Item "$ROOT\docker-compose.yml" "$OUT\docker-compose.yml" -Force
Copy-Item "$ROOT\.env.example"       "$OUT\.env.example"       -Force

Write-Host ""
Write-Host "=== 完成 ===" -ForegroundColor Green
Write-Host "产物目录: $OUT"
Write-Host ""
Write-Host "传到服务器:"
Write-Host "  scp dist-images/* user@your-server:/opt/projectflow/"
Write-Host ""
Write-Host "服务器上执行:"
Write-Host "  cd /opt/projectflow"
Write-Host "  docker load < projectflow-backend.tar.gz"
Write-Host "  docker load < projectflow-frontend.tar.gz"
Write-Host "  cp .env.example .env  && vi .env   # 填写 MySQL/Redis 连接信息"
Write-Host "  docker compose up -d"
