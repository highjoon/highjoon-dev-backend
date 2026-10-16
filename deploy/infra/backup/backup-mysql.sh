#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-/opt/highjoon-dev-infra/backup.env}"
RETENTION_DAYS=7

aws_s3() {
    docker run --rm -i --env-file "$ENV_FILE" \
        -e AWS_REQUEST_CHECKSUM_CALCULATION=when_required \
        -e AWS_RESPONSE_CHECKSUM_VALIDATION=when_required \
        -v /var/tmp:/var/tmp:ro \
        amazon/aws-cli:2.22.35 --endpoint-url "$BACKUP_S3_ENDPOINT" "$@"
}

set -a
. "$ENV_FILE"
set +a
: "${BACKUP_S3_ENDPOINT:?}" "${BACKUP_S3_BUCKET:?}" "${MYSQL_USER:?}" "${MYSQL_PASSWORD:?}"

key="highjoon_dev-$(date -u +%Y%m%d-%H%M%S).sql.gz"
tmp=$(mktemp /var/tmp/highjoon_dev-XXXXXX.sql.gz)
trap 'rm -f "$tmp"' EXIT

docker exec -e MYSQL_PWD="$MYSQL_PASSWORD" highjoon-dev-mysql \
    mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces \
    --default-character-set=utf8mb4 highjoon_dev | gzip -c >"$tmp"

gzip -t "$tmp"
zcat "$tmp" | tail -n 1 | grep -q '^-- Dump completed'

aws_s3 s3 cp "$tmp" "s3://${BACKUP_S3_BUCKET}/${key}" </dev/null
echo "업로드 완료: ${key}"

cutoff=$(date -u -d "$((RETENTION_DAYS - 1)) days ago" +%Y%m%d)
aws_s3 s3 ls "s3://${BACKUP_S3_BUCKET}/" </dev/null | awk '{print $4}' |
    grep -E '^highjoon_dev-[0-9]{8}-[0-9]{6}\.sql\.gz$' |
    while read -r old; do
        day=${old#highjoon_dev-}
        day=${day%%-*}
        if [ "$day" -lt "$cutoff" ]; then
            aws_s3 s3 rm "s3://${BACKUP_S3_BUCKET}/${old}" </dev/null
            echo "삭제: ${old}"
        fi
    done
