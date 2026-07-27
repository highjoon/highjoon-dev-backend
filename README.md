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
- Neon `development` 브랜치를 가리킴. `production`은 배포 전용
    - 콘솔 Connect의 기본 선택이 `production`이므로 브랜치를 바꿔서 복사
- Neon 콘솔 문자열은 세 곳 수정 필요: `jdbc:` 접두사 추가, `channel_binding=require` 제거, 호스트의 `-pooler` 제거

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<endpoint>.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
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
