# MSA-example

Spring Boot + Python 기반 마이크로서비스 아키텍처 예제 프로젝트입니다.
주문과 재고 관리를 **Kafka 이벤트 기반 Saga 패턴**으로 구현하고, 장애 시 **보상 트랜잭션**으로 데이터 정합성을 보장합니다.
**LangGraph 기반 AI 서비스**를 Python(FastAPI)으로 구현하여 Eureka에 등록하고 Gateway를 통해 통합합니다.
**Zipkin 분산 트레이싱**으로 Gateway부터 개별 서비스, Kafka 이벤트까지 하나의 traceId로 추적합니다.

## 아키텍처

![MSA Architecture](docs/msa-architecture.png)

## 서비스 구성

| 서비스 | 포트 | 역할 | 데이터베이스 |
|--------|------|------|-------------|
| **service-discovery** | 8761 | Eureka Server (서비스 레지스트리) | - |
| **service-gateway** | 8080 | API Gateway (JWT 검증, 라우팅) | - |
| **service-user** | 8081 | 회원가입, 로그인, JWT 발급 | H2 |
| **service-item** | 8082 | 상품 등록/조회, 재고 관리 | H2 |
| **service-order** | 8083 | 주문 생성/취소, 상태 관리 | H2 |
| **service-ai** | 8084 | LangGraph RAG 채팅, 스트리밍, Handoffs | PostgreSQL |
| **kafka** | 9092 | 메시지 브로커 | - |
| **zipkin** | 9411 | 분산 트레이싱 UI / 수집 서버 | MySQL |
| **mysql** | 3306 | Zipkin 트레이스 데이터 영구 저장 | - |

## 주문 흐름 (Saga Pattern)

### 정상 주문

```
Client → POST /buy(itemId=3)
  → OrderService: 주문 생성 (PENDING)
  → [order-events] OrderCreatedEvent 발행
  → ItemService: 재고 차감
  → [item-events] StockDecreasedEvent 발행
  → OrderService: 주문 상태 → COMPLETED
```

### 정상 취소

```
Client → POST /cancel(orderId=1)
  → OrderService: 주문 상태 → CANCELLING
  → [order-events] OrderCancelledEvent 발행
  → ItemService: 재고 복구
  → [item-events] StockIncreasedEvent 발행
  → OrderService: 주문 상태 → CANCELLED
```

## 보상 트랜잭션 (Compensating Transaction)

Kafka Consumer에서 3회 재시도 후에도 실패하면, recovery callback에서 보상 이벤트를 발행하여 데이터 정합성을 자동 복구합니다.

### Scenario 1: 재고 차감 성공 → 주문 COMPLETED 업데이트 실패

```
StockDecreasedEvent → OrderService DB 에러 → 3회 재시도 실패
  → CompensateStockDecreaseEvent 발행
  → ItemService: 재고 복구
  → StockDecreaseFailedEvent 발행
  → OrderService: 주문 → FAILED
```

### Scenario 2: 재고 복구 성공 → 주문 CANCELLED 업데이트 실패

```
StockIncreasedEvent → OrderService DB 에러 → 3회 재시도 실패
  → 로그 경고 (재고는 이미 안전한 상태)
  → Reconciliation Scheduler가 CANCELLING 상태 주문 감지 후 재처리
```

### Scenario 3: 주문 생성 이벤트 → ItemService 처리 실패

```
OrderCreatedEvent → ItemService DB 에러 → 3회 재시도 실패
  → OrderProcessingFailedEvent 발행
  → OrderService: 주문 → FAILED
```

### Scenario 4: 주문 취소 이벤트 → ItemService 처리 실패

```
OrderCancelledEvent → ItemService DB 에러 → 3회 재시도 실패
  → CancelProcessingFailedEvent 발행
  → OrderService: 주문 → CANCEL_FAILED
```

### 안전망: Reconciliation Scheduler

보상 이벤트 발행 자체가 실패하는 최악의 경우를 대비한 안전망입니다.

- **5분 주기**로 실행
- **10분 이상** PENDING/CANCELLING 상태인 주문 감지
- 해당 주문의 이벤트를 재발행하여 Saga 흐름 재시작
- 멱등성 처리(`ProcessedEvent`)로 중복 실행 방지

## 데이터 정합성 보장 메커니즘

```
1차: Kafka Retry (1초 간격, 3회)
  ↓ 실패
2차: 보상 이벤트 발행 (자동 복구)
  ↓ 실패
3차: Reconciliation Scheduler (5분 주기, 10분 이상 체류 감지)
```

| 메커니즘 | 설명 |
|---------|------|
| **이벤트 멱등성** | `ProcessedEvent` 테이블로 중복 처리 방지 |
| **비관적 잠금** | `SELECT FOR UPDATE`로 재고 동시성 제어 |
| **보상 트랜잭션** | retry 소진 시 보상 이벤트로 자동 롤백 |
| **Reconciliation** | Scheduler가 장기 체류 주문을 주기적으로 재처리 |

## 분산 트레이싱 (Distributed Tracing)

모든 서비스에 Zipkin 분산 트레이싱이 적용되어 있습니다. Gateway에서 시작된 요청이 어떤 서비스를 거치고, Kafka를 통해 어떻게 전파되는지 **하나의 traceId**로 추적할 수 있습니다.

### 구성 요소

| 서비스 | 트레이싱 라이브러리 | 역할 |
|--------|---------------------|------|
| **Java 서비스** (Gateway, User, Item, Order) | Micrometer Tracing + Brave | span 생성, B3 헤더 전파, Zipkin 리포트 |
| **Python 서비스** (AI) | OpenTelemetry + Zipkin Exporter | span 생성, B3 헤더 전파, Zipkin 리포트 |
| **Zipkin** | - | span 수집, 저장, UI 시각화 |

### 동작 방식

```
1. Gateway에서 요청 수신 → Brave가 traceId(groupID) 자동 생성
2. 다운스트림 서비스 호출 시 B3 헤더로 traceId 전파
3. Kafka 이벤트 발행/소비 시 Kafka 헤더로 traceId 전파
4. 각 서비스가 span을 Zipkin 서버(http://localhost:9411)로 비동기 전송
5. Zipkin UI에서 전체 트레이스 시각화
```

### Zipkin UI 확인

```
http://localhost:9411
```

Saga 흐름 예시: `POST /api/service-order/buy` 요청 시 Zipkin에서 다음 트레이스를 확인할 수 있습니다.

```
service-gateway → service-order → kafka(order-events) → service-item → kafka(item-events) → service-order
```

## 인증 흐름

```
1. POST /api/service-user/register  →  회원가입
2. POST /api/service-user/login     →  JWT를 HttpOnly Cookie로 발급
3. 이후 요청 시 Gateway가 Cookie에서 JWT 검증
4. 검증 성공 → X-User-Id 헤더 추가 후 서비스로 라우팅
```

## 실행 방법

### 1. 인프라 실행 (Kafka, Zipkin, MySQL)

```bash
docker-compose up -d
```

### 2. 서비스 실행 (순서대로)

```bash
# 1) Eureka Server
cd service-discovery && ./gradlew bootRun

# 2) Gateway
cd service-gateway && ./gradlew bootRun

# 3) User / Item / Order / AI (순서 무관)
cd service-user && ./gradlew bootRun
cd service-item && ./gradlew bootRun
cd service-order && ./gradlew bootRun
cd service-ai && uv run uvicorn app.main:app --host 0.0.0.0 --port 8084
```

### 3. API 테스트

```bash
# 회원가입
curl -X POST "http://localhost:8080/api/service-user/register" \
  -H "Content-Type: application/json" \
  -d '{"id": "user1", "pwd": "1234"}'

# 로그인
curl -c cookies.txt -X POST "http://localhost:8080/api/service-user/login" \
  -H "Content-Type: application/json" \
  -d '{"id": "user1", "pwd": "1234"}'

# 상품 등록
curl -b cookies.txt -X POST "http://localhost:8080/api/service-item/register" \
  -H "Content-Type: application/json" \
  -d '{"name": "테스트상품", "price": 10000, "quantity": 100}'

# 주문
curl -b cookies.txt -X POST "http://localhost:8080/api/service-order/buy?item_id=1"

# 주문 조회
curl -b cookies.txt "http://localhost:8080/api/service-order/search"

# 주문 취소
curl -b cookies.txt -X POST "http://localhost:8080/api/service-order/cancel?order_id=1"

# AI 채팅
curl -b cookies.txt -X POST "http://localhost:8080/api/service-ai/v1/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": "안녕하세요"}'
```

## 기술 스택

- **Java 17**, **Spring Boot 3.5**
- **Spring Cloud** — Gateway, Eureka
- **Apache Kafka** — 이벤트 기반 비동기 통신
- **Spring Data JPA** + **H2** — 데이터 접근
- **Auth0 java-jwt** — JWT 인증
- **Python 3.12**, **FastAPI** — AI 서비스
- **LangGraph** + **LangChain** — RAG, Multi-Agent Handoffs
- **py-eureka-client** — Python 서비스 Eureka 등록
- **Micrometer Tracing** + **Brave** — Java 분산 트레이싱
- **OpenTelemetry** — Python 분산 트레이싱
- **Zipkin** — 트레이스 수집 / 시각화
- **Gradle** / **uv** — 빌드 도구
- **Docker Compose** — Kafka, Zipkin, MySQL 인프라
