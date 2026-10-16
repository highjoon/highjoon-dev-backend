# highjoon-dev-backend

[api.highjoon-dev.com](https://api.highjoon-dev.com)

- Java 21, Spring Boot 3.5, Spring Data JPA
- MySQL 8.4, Flyway
- Oracle Cloud A1, Docker Compose, nginx, GitHub Actions
- Prometheus, Grafana, Loki

## 로컬 개발

DB 접속 정보는 루트의 `application-local.yml`에서 관리 (gitignore)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/highjoon_dev
    username: root
    password: root
```

```bash
./gradlew bootRun         # http://localhost:8080
./gradlew test            # Testcontainers(mysql:8.4) 사용, DB 설정 불필요
./gradlew spotlessApply   # 포맷
```

## 스키마

- `src/main/resources/db/migration/`의 Flyway 파일이 생성. 스키마 전용
- Hibernate는 `ddl-auto: validate`
- 적용된 파일은 수정하지 않고 새 버전 추가

## 배포

```
main push → GitHub Actions(arm64 빌드) → GHCR → Oracle A1 blue/green 전환
```

- 앱 위치: Oracle A1 `/opt/highjoon-dev`, blue `8081`, green `8082`
- blue/green은 nginx `/etc/nginx/conf.d/backend-upstream.conf`에서 관리
- `deploy/nginx/`는 certbot이 관리하는 파일의 참고용 스냅샷. 배포 대상 아님

## 운영

A1 `/opt/highjoon-dev-infra`에서 `deploy/infra/`로 관리 (MySQL, Loki, Prometheus, node-exporter, Grafana)

| 항목     | 내용                                                                                                           |
| -------- | -------------------------------------------------------------------------------------------------------------- |
| 대시보드 | [grafana.highjoon-dev.com](https://grafana.highjoon-dev.com). 설정 원본은 `deploy/infra/grafana/provisioning/` |
| 알림     | ERROR 로그, blue green 모두 다운, 디스크 80% 초과 → Discord `#서버`                                            |
| 로그     | 앱 → Fluent Bit → Loki. Grafana Explore에서 `{service="highjoon-dev-backend"} \| json \| traceId="..."`        |
| 백업     | 매일 03:17 KST mysqldump → OCI Object Storage, 7일 보관. 복구 검증은 `deploy/infra/backup/restore-verify.sh`   |
