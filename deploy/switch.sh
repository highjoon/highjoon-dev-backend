#!/usr/bin/env bash
# 활성 색 전환. 인자로 받은 포트로 nginx upstream을 바꾸고 reload
# 사용법: switch.sh 8082
set -euo pipefail

UPSTREAM_FILE=/etc/nginx/conf.d/backend-upstream.conf
TARGET_PORT="${1:?포트 필요}"

case "$TARGET_PORT" in
    8081 | 8082) ;;
    *)
        echo "허용되지 않은 포트: ${TARGET_PORT}" >&2
        exit 1
        ;;
esac

# 검사 실패 시 되돌릴 수 있게 현재 파일 보관
sudo cp "$UPSTREAM_FILE" "${UPSTREAM_FILE}.prev"

sudo tee "$UPSTREAM_FILE" >/dev/null <<EOF
# 배포 스크립트가 자동 생성. 직접 수정 금지
# blue=8081, green=8082
upstream backend {
    server 127.0.0.1:${TARGET_PORT} max_fails=0;
}
EOF

# 깨진 파일을 conf.d에 남기면 이후 certbot 인증서 갱신까지 막힘
if ! sudo nginx -t; then
    sudo cp "${UPSTREAM_FILE}.prev" "$UPSTREAM_FILE"
    echo "nginx 설정 오류. 원복함" >&2
    exit 1
fi

# reload는 기존 워커가 처리 중인 요청을 끝낸 뒤 교체. 끊김 없음
sudo systemctl reload nginx
echo "전환 완료: ${TARGET_PORT}"
