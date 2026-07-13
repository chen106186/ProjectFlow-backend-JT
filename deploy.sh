#!/bin/bash
# 服务器端：加载镜像并启动
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"

echo ">>> 加载镜像..."
docker load < "$DIR/projectflow-backend.tar.gz"
docker load < "$DIR/projectflow-frontend.tar.gz"

if [ ! -f "$DIR/.env" ]; then
  cp "$DIR/.env.example" "$DIR/.env"
  echo ""
  echo "!!! 请先编辑 .env 填写 MySQL / Redis 连接信息，然后重新执行此脚本"
  exit 1
fi

echo ">>> 启动服务..."
docker compose -f "$DIR/docker-compose.yml" --env-file "$DIR/.env" up -d

echo ""
echo "=== 启动完成 ==="
docker compose -f "$DIR/docker-compose.yml" ps
