#!/usr/bin/env bash
set -euo pipefail

DUMP="${1:?덤프 파일 경로 필요}"
: "${MYSQL_USER:?}" "${MYSQL_PASSWORD:?}"

NAME=highjoon-dev-restore-check
TMP_ROOT_PW=restore-check
TABLES="category tag post post_tag post_view_stats"

docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" -e MYSQL_ROOT_PASSWORD="$TMP_ROOT_PW" -e MYSQL_DATABASE=highjoon_dev \
    mysql:8.4 --character-set-server=utf8mb4 >/dev/null
trap 'docker rm -f "$NAME" >/dev/null 2>&1 || true' EXIT

until docker exec "$NAME" mysqladmin ping -h 127.0.0.1 -uroot -p"$TMP_ROOT_PW" --silent 2>/dev/null; do
    sleep 2
done

gunzip -c "$DUMP" | docker exec -i "$NAME" mysql -uroot -p"$TMP_ROOT_PW" --default-character-set=utf8mb4 highjoon_dev

fail=0
for t in $TABLES; do
    live=$(docker exec -e MYSQL_PWD="$MYSQL_PASSWORD" highjoon-dev-mysql \
        mysql -u"$MYSQL_USER" -N -B highjoon_dev -e "SELECT COUNT(*) FROM ${t}")
    restored=$(docker exec "$NAME" mysql -uroot -p"$TMP_ROOT_PW" -N -B highjoon_dev -e "SELECT COUNT(*) FROM ${t}" 2>/dev/null)
    if [ "$live" = "$restored" ]; then
        printf 'ok   %-16s live=%s restored=%s\n' "$t" "$live" "$restored"
    else
        printf 'FAIL %-16s live=%s restored=%s\n' "$t" "$live" "$restored"
        fail=1
    fi
done
exit "$fail"
