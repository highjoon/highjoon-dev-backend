# 자바 21 JRE 알파인 이미지
FROM eclipse-temurin:21-jre-alpine
# 비루트 유저 생성 (root 실행 방지)
RUN adduser -D appuser
# 이후 명령어 실행 기준 디렉터리
WORKDIR /app
# 스프링 부트 빌드 결과물을 이미지 내의 app.jar로 복사
COPY build/libs/*.jar app.jar
# 8080 포트 사용을 문서화
EXPOSE 8080
# 유저 지정
USER appuser
# 컨테이너 시작 시 실행하는 명령어. 스프링 부트 앱 시작
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-jar", "/app/app.jar"]
