# 채팅 Redis Cluster 배포

PR #104의 리버트를 복구하고 prod 프로필에 Redis Cluster 접속을 적용한다.

## 배포 환경변수

- `SPRING_PROFILES_ACTIVE=prod`: Cluster 설정은 prod 프로필에 있다. 로컬 기본 프로필은 기존 단일 Redis를 사용한다.
- `REDIS_CLUSTER_NODES`: 쉼표로 구분한 seed 노드 목록. 예: `redis-0:6379,redis-1:6379,redis-2:6379`. 배포에서는 필수이며 localhost 기본값을 두지 않는다.
- `REDIS_PASSWORD`: 비밀번호 인증 시 설정. 인증이 없으면 생략한다.
- `CHAT_REDIS_ENABLED=true`: 채팅 Snapshot/PubSub 경로 활성화. 기본값은 false.
- `MANAGEMENT_HEALTH_REDIS_ENABLED`: 기본 false. 전체 health에서 Redis를 제외하며 Redis 연결 성공을 보장하지 않는다. Redis 장애 시 채팅 fallback을 유지하기 위한 설정이다.

기존 timeout 기본값은 connect 2s, command 1s이며 key prefix/TTL/버퍼 기본값은 유지한다.
Spring Boot 4.1.1은 adaptive topology refresh를 기본 활성화하므로 폐기된 adaptive 속성은 설정하지 않는다. 주기 갱신은 30초, dynamic refresh sources는 true다.

BE에서 seed 노드뿐 아니라 토폴로지에 광고되는 모든 노드의 주소와 포트에 접근 가능해야 한다. TLS나 ACL 사용자명이 필요하면 `SPRING_DATA_REDIS_SSL_ENABLED=true`, `SPRING_DATA_REDIS_USERNAME`을 배포 환경에 설정한다. 인증서 신뢰 설정이 필요한 환경은 별도 확인한다.

## 배포 및 검증

1. PR의 테스트/빌드를 확인하고 develop에 머지해 새 이미지를 생성한다.
2. 클라우드 담당자가 환경변수와 실제 활성 프로필을 확인하고 새 이미지를 수동 배포한다.
3. Redis 연결/인증/토폴로지 초기화 오류 및 CROSSSLOT 오류가 없는지 확인한다. health UP만으로 판단하지 않는다.
4. BE-A에서 채팅 생성 중 연결을 끊고 같은 messageId로 BE-B에 재연결한다. snapshot, 이후 token, done 및 최종 본문을 확인한다.
5. 테스트 환경에서 노드 장애/복구 후 구독 복원과 SSE 복구를 확인한다. Redis 전체 장애 중에는 생성 서버 local fallback과 다른 서버 DB terminal fallback을 확인한다.

Lua가 사용하는 content/state/event 키는 같은 메시지의 `{messageId}` hash tag를 공유한다. 일반 PUBLISH/패턴 구독을 사용하며 sharded Pub/Sub으로 변경하지 않는다.

실제 클라우드 Cluster 통합 검증은 배포 환경에서 수행해야 한다.
