# highjoon-dev-backend

## 도메인

[api.highjoon-dev.com](api.highjoon-dev.com)

- Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL

## 사전 준비

- Docker 실행
- `gradle.properties`(gitignore, 로컬 전용)가 Gradle 실행 JDK를 21로 고정

## 실행

```bash
docker compose up -d          # 로컬 Postgres 기동
./gradlew bootRun             # 앱 기동 (http://localhost:8080)
```

- Health check: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`

## 테스트

```bash
docker compose up -d          # 테스트는 로컬 Postgres 실행을 전제로 함
./gradlew test
```

## 포맷

```bash
./gradlew spotlessApply       # palantir-java-format 일괄 적용
./gradlew build               # spotlessCheck 자동 포함 (포맷 강제)
```

## 종료

```bash
docker compose down           # 컨테이너 정지 (데이터는 볼륨에 유지)
```
