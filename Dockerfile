# syntax=docker/dockerfile:1

# =========================
# Build stage
# - Gradle 테스트 실행
# - Spring Boot bootJar 생성
# - jar를 레이어별로 추출
# =========================
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /build

# Gradle 설정 파일을 먼저 복사해 의존성 다운로드 레이어를 캐시한다.
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# gradlew 실행 권한을 보장하고 의존성을 미리 받아둔다.
RUN chmod +x gradlew \
 && ./gradlew dependencies --no-daemon

# 소스 코드는 의존성 설치 후 복사해 캐시 효율을 높인다.
COPY src src

# CI와 CD에서 최종 이미지를 빌드할 때 반드시 테스트를 실행한다.
# 테스트가 실패하면 builder stage가 실패하므로 runtime 이미지를 만들 수 없다.
# bootJar 산출물 이름은 build.gradle에서 app.jar로 고정한다.
RUN ./gradlew test bootJar --no-daemon

# Spring Boot jar를 레이어별로 분리한다.
# 애플리케이션 코드만 바뀌면 application 레이어 위주로 다시 전송된다.
RUN java -Djarmode=tools \
    -jar build/libs/lookddak-be.jar \
    extract \
    --layers \
    --launcher \
    --destination extracted

# =========================
# Runtime stage
# - JRE 기반 최종 실행 이미지
# - JDK와 Gradle을 포함하지 않음
# =========================
FROM eclipse-temurin:25-jre-noble AS runtime

# 변경: CD에서 전달한 전체 Git SHA를 이미지 내부 메타데이터에 기록한다.
# 7자리 이미지 태그와 별개로 어떤 원본 커밋에서 만들어졌는지 추적할 수 있다.
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.revision="${GIT_SHA}"

# 변경: Compose 헬스체크에서 사용하는 bash와 grep이 이미지에 있는지 확인한다.
# 현재 헬스체크는 curl을 사용하지 않으므로 curl은 설치하지 않는다.
# 베이스 이미지가 바뀌어 두 명령이 사라지면 이미지 빌드 단계에서 바로 실패한다.
RUN command -v bash > /dev/null \
 && command -v grep > /dev/null \
 && groupadd --system --gid 1001 spring \
 && useradd --system --uid 1001 --gid spring spring

WORKDIR /app

# 변경 빈도가 낮은 레이어부터 복사해 Docker layer cache 효율을 높인다.
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# Spring 애플리케이션을 root가 아닌 전용 사용자로 실행한다.
USER spring

# 문서화용 포트다. 실제 외부 노출은 Compose 또는 ECS에서 결정한다.
EXPOSE 8080

# 메모리와 timezone 설정은 JAVA_TOOL_OPTIONS로 외부에서 주입한다.
# 레이어 추출 방식이므로 app.jar 대신 Spring Boot JarLauncher를 실행한다.
ENTRYPOINT [ \
  "java", \
  "-XX:+ExitOnOutOfMemoryError", \
  "org.springframework.boot.loader.launch.JarLauncher" \
]