# daejung-dist-backend

홍게 판매 사이트를 위한 Spring Boot 백엔드 프로젝트입니다.

## 기술 스택

- Java 17
- Spring Boot 4.0.8
- Spring Data JPA
- Spring Security
- MySQL 8.4
- Docker / Docker Compose

## 현재 구현된 기능

- 홍게 상품 조회 API
- 기본 홍게 상품 시드 데이터 자동 등록
- 로컬용 MySQL 실행 환경
- 로컬 실행 스크립트
- 전체 요청 허용 보안 설정

## API

### 홍게 상세 조회

- `GET /api/v1/hongges/{id}`

응답:
- 성공: 홍게 정보 반환
- 실패: `404 Not Found`

예시

```bash
curl http://localhost:8080/api/v1/hongges/1
```

## 로컬 실행 방법

### 1. 환경 변수 준비

`.env.example`을 복사해 `.env` 파일을 생성합니다.

```bash
cp .env.example .env
```

`.env` 예시:

```env
MYSQL_ROOT_PASSWORD=change_me
MYSQL_DATABASE=daejung_dist
MYSQL_USER=app_user
MYSQL_PASSWORD=change_me
```

### 2. 실행

```bash
./scripts/local-run.sh
```

이 스크립트는 다음 순서로 동작합니다.

1. `.env` 파일 확인
2. 환경 변수 로드
3. Docker Compose로 MySQL 실행
4. `local` 프로필로 Spring Boot 실행

## 직접 실행할 경우

### MySQL만 먼저 띄우기

```bash
docker compose up -d
```

### 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 테스트

```bash
./gradlew test
```

## 설정 파일

- `src/main/resources/application.properties`: 공통 설정
- `src/main/resources/application-local.properties`: 로컬 MySQL 설정
- `docker-compose.yml`: MySQL 컨테이너 설정

## 프로젝트 구조

```text
src/main/java/shop/daejung/daejungdistbackend
├── global/config
│   └── SecurityConfig.java
└── modules/hongge
    ├── config
    │   └── HonggeDataInitializer.java
    ├── controller
    │   └── HonggeController.java
    ├── domain
    │   └── Hongge.java
    ├── repository
    │   └── HonggeRepository.java
    └── service
        └── HonggeService.java
```
