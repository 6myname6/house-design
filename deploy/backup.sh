#!/bin/bash
# MySQL 每日备份（部署文档 §7.4，第 7 步④）
# 策略：容器启动即备份一次（顺带即时验证链路），随后每 24h 一次；
#       --single-transaction 保证 InnoDB 一致性快照不锁表；
#       保留最近 KEEP 份，超出轮转删除。
set -u

BACKUP_DIR="/backups"
KEEP=7
export MYSQL_PWD="${MYSQL_ROOT_PASSWORD}"

dump() {
  local ts out rc
  ts="$(date +%Y%m%d_%H%M%S)"
  out="$BACKUP_DIR/house_design_$ts.sql.gz"

  # --databases 必带：导出 CREATE DATABASE + USE 语句，空服务器可直接恢复
  # （恢复演练实测：不带时 dump 无 USE，导入报 No database selected）
  mysqldump -h mysql -uroot \
    --single-transaction --routines --triggers --default-character-set=utf8mb4 \
    --databases house_design | gzip > "$out"
  rc=${PIPESTATUS[0]}
  if [ "$rc" -ne 0 ]; then
    rm -f "$out"  # 导出中断时清理残缺文件，避免污染保留策略
    echo "[backup] mysqldump failed rc=$rc, partial file removed"
    return 1
  fi

  # 保留最近 KEEP 份：按文件名（时间戳）升序，删掉最旧的超出部分
  find "$BACKUP_DIR" -name 'house_design_*.sql.gz' | sort | head -n -"$KEEP" | xargs -r rm -f
  echo "[backup] done: house_design_$ts.sql.gz"
}

while true; do
  if ! dump; then
    echo "[backup] will retry next cycle"
  fi
  sleep 86400
done
