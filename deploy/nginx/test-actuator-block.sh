#!/usr/bin/env bash
# actuator 차단 snippet 검증. 사용법: test-actuator-block.sh
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
T=$(mktemp -d)
NAME=hjd-nginx-test

cat > "$T/nginx.conf" <<'EOF'
events {}
http {
    upstream backend { server 127.0.0.1:8099; }
    # 가짜 백엔드
    server {
        listen 8099;
        location / { return 200 "UPSTREAM\n"; }
    }
    server {
        listen 8080;
        include /etc/nginx/snippets/highjoon-dev-actuator.conf;
        location / { proxy_pass http://backend; }
    }
}
EOF

docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" -p 127.0.0.1:18080:8080 \
    -v "$T/nginx.conf:/etc/nginx/nginx.conf:ro" \
    -v "$DIR/snippets:/etc/nginx/snippets:ro" nginx:1.27-alpine >/dev/null
trap 'docker rm -f "$NAME" >/dev/null 2>&1 || true' EXIT
sleep 2

fail=0
check() {
    local expected="$1" path="$2" actual
    actual=$(curl -s -o /dev/null -w '%{http_code}' --path-as-is "http://127.0.0.1:18080${path}")
    if [ "$actual" = "$expected" ]; then
        printf 'ok   %s %s\n' "$actual" "$path"
    else
        printf 'FAIL %s (기대 %s) %s\n' "$actual" "$expected" "$path"
        fail=1
    fi
}

check 200 /actuator/health
check 200 /api/v1/categories
check 404 /actuator/prometheus
check 404 /actuator/prometheus/
check 404 /actuator/Prometheus
check 404 //actuator/prometheus
check 404 '/actuator;x=1/prometheus'
check 404 /actuator/%70rometheus
check 404 '/actuator/health;x=1'
check 404 /actuator/env

exit "$fail"
