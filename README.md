# highjoon-dev-backend

[api.highjoon-dev.com](api.highjoon-dev.com)

- Java 21
- Spring Boot 3.5
- Spring Data JPA
- PostgreSQL(NeonDB)

## 준비

- JDK 21 (`gradle.properties`)
- Docker (`Testcontainers` + `Postgres`에서만 사용)
- DB 접속 정보는 프로젝트 루트의 `application-local.yml`에서 관리 (gitignore)
- Neon `development` 브랜치를 가리킴. `main`은 배포 전용
    - 콘솔 Connect의 기본 선택이 `main`이므로 브랜치를 바꿔서 복사
- Neon 콘솔 문자열은 세 곳 수정 필요: `jdbc:` 접두사 추가, `channel_binding=require` 제거, 호스트의 `-pooler` 제거

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<endpoint>.us-west-2.aws.neon.tech/neondb?sslmode=require
    username: <role>
    password: <password>
  jpa:
    show-sql: true          # 로컬 전용. 배포 시엔 꺼진 상태로 나간다
    properties:
      hibernate:
        format_sql: true
```

배포 시에는 이 파일 없이 `SPRING_DATASOURCE_URL` 등 환경변수로 주입한다.

## 실행

```bash
./gradlew bootRun     # http://localhost:8080
```

- `curl localhost:8080/actuator/health` → `{"status":"UP"}`

## 테스트

```bash
./gradlew test        # Testcontainers가 Postgres를 띄운다
```

- 빈 컨테이너에 Flyway가 마이그레이션을 실행 → 마이그레이션 SQL도 매번 검증
- Neon에 붙지 않으므로 `application-local.yml` 없이 실행 가능

## 스키마

- 테이블은 Flyway 마이그레이션 파일이 생성
- Hibernate는 `ddl-auto: validate`로 엔티티와 실제 스키마가 맞는지만 확인

```
src/main/resources/db/migration/V1__create_category.sql
```

- 파일명은 `V<번호>__<설명>.sql` 형식
- 엔티티 변경과 마이그레이션 파일은 같은 커밋에 포함
- 적용이 끝난 파일은 고치지 않으며 변경이 필요하면 새 버전 생성

## 포맷

```bash
./gradlew spotlessApply   # palantir-java-format
./gradlew build           # spotlessCheck 포함
```

## 로깅

```
앱(stdout) → Docker json-file → Fluent Bit → GCP Cloud Logging
```

- 로컬은 평문, 배포 환경은 ECS JSON. `.env`의 `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`로 구분
- 로컬에서 JSON을 보려면 `./gradlew bootRun --args='--logging.structured.format.console=ecs'` 실행
- 설정은 `deploy/fluent-bit/`에 있고 배포 때 `/opt/highjoon-dev/fluent-bit/`로 동기화
- 로컬 로그 파일은 10MB × 3개로 로테이션

### 로깅 대상

| 대상    | 레벨                        |
|-------|---------------------------|
| 모든 요청 | INFO 한 줄                  |
| 4xx   | 추가 로그 없음 (요청 완료 INFO는 기록) |
| 5xx   | ERROR + 스택트레이스            |

- `/actuator`는 필터에서 제외
- 요청 본문은 제외

### traceId

`RequestLoggingFilter`가 요청마다 8자리 ID를 발급해 MDC에 넣고 응답 헤더 `X-Trace-Id`로 반환

| 필드          | 비고                |
|-------------|-------------------|
| `traceId`   | 요청 식별자            |
| `clientIp`  | 마지막 자리를 `0`으로 마스킹 |
| `userAgent` | 256자 초과 시 절단      |

### 조회

```bash
gcloud logging read 'resource.type="gce_instance" AND jsonPayload.traceId="a1b2c3d4"' --limit=10
```

#### Logs Explorer 쿼리

```
severity>=ERROR                        에러만
jsonPayload.clientIp="1.2.3.0"         특정 대역
jsonPayload.message=~"[0-9]{4}ms"      1초 넘는 요청
```

### 알림

| 이름            | 조건                                                       |
|---------------|----------------------------------------------------------|
| 앱 ERROR 로그 발생 | 5분간 `severity>=ERROR` 1건 이상                              |
| API 헬스체크 실패   | `/actuator/health` 1분 간격, 5분 연속 실패 (OOM 등으로 앱이 죽는 경우 대비) |
