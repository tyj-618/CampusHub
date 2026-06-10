# CampusHub

CampusHub is a pure backend project for a lightweight campus forum. It focuses on common backend engineering capabilities for an internship resume: authentication, user profiles, categories, posts, comments, likes, in-site notices, admin operations, Redis ranking, RocketMQ event decoupling, batched view-count flushing, unified responses, exception handling, and integration tests.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Web
- Spring Validation
- Spring JDBC
- MySQL
- Redis, optional
- RocketMQ, optional
- H2 for tests
- JUnit / Spring Boot Test

## Architecture

The project uses a clear backend layering style:

```text
Controller -> Service -> Mapper -> Database
```

- Controller: exposes REST APIs and receives request parameters.
- Service: handles business rules, login checks, permission checks, events, and cache/ranking coordination.
- Mapper: executes SQL through Spring JDBC and maps query results.
- Common/Exception: provides `ApiResponse`, `PageResponse`, `ErrorCode`, `BusinessException`, and global exception handling.

## Modules

| Module | Description |
| --- | --- |
| auth | Register, login, logout, token storage, current user lookup |
| user | Current user profile, public profile, profile update |
| category | Enabled post category query |
| post | Post creation, list, detail, update, delete, user posts, hot posts |
| comment | Comment creation, list, delete, my comments |
| like | Like, unlike, like status |
| notice | Comment/like notices, unread count, mark read |
| admin | Hide/restore posts, disable/enable users |
| event | Domain event abstraction, sync handling, RocketMQ extension |
| common | Unified response, pagination response, error codes |
| exception | Business exception and global exception handler |

## Engineering Highlights

- Uses `Controller-Service-Mapper` layering to keep HTTP handling, business rules, and SQL access separated.
- Uses BCrypt to hash user passwords and token-based login state management.
- Provides both in-memory and Redis token stores through the `TokenStore` abstraction.
- Uses Redis ZSet for the hot post ranking when the `redis` profile is enabled:
  - `member = postId`
  - `score = hot_score`
  - MySQL `post_stat.hot_score` remains the persistent source of truth.
  - Redis ranking can be rebuilt from MySQL when empty or inconsistent.
- Uses RocketMQ profile-based event publishing to decouple comments/likes from notice generation.
- Uses `ConcurrentHashMap + LongAdder + ScheduledExecutorService` to batch post view-count updates and reduce database write pressure.
- Uses unified response objects, error codes, business exceptions, and global exception handling to keep API behavior consistent.
- Uses H2 integration tests to verify core API flows and boundary cases without requiring local MySQL, Redis, or RocketMQ.

## Project Structure

```text
src/main/java/com/tyj/campushub
├── admin
├── auth
├── category
├── comment
├── common
├── event
├── exception
├── like
├── notice
├── post
└── user
```

Important resources:

```text
src/main/resources/application.yaml
src/main/resources/application-rocketmq.yaml
src/main/resources/db/schema.sql
src/main/resources/db/data.sql
src/test/resources/application-test.yaml
src/test/resources/schema.sql
src/test/resources/data.sql
```

## Database Initialization

Create the database and tables:

```sql
source src/main/resources/db/schema.sql;
```

Initialize category data:

```sql
source src/main/resources/db/data.sql;
```

When running from a MySQL shell on Windows, you can use absolute paths:

```sql
source D:/GitCode/CampusHub/src/main/resources/db/schema.sql;
source D:/GitCode/CampusHub/src/main/resources/db/data.sql;
```

## Local Run

Default startup only requires MySQL.

Common environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Backend server port |
| `CAMPUSHUB_DB_URL` | `jdbc:mysql://localhost:3306/campushub?...` | MySQL URL |
| `CAMPUSHUB_DB_USERNAME` | `root` | MySQL username |
| `CAMPUSHUB_DB_PASSWORD` | empty | MySQL password |
| `CAMPUSHUB_VIEW_COUNT_FLUSH_INTERVAL_SECONDS` | `10` | View-count batch flush interval |

Start with Maven Wrapper:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Default base URL:

```text
http://localhost:8080
```

## Redis Profile

The default profile does not require Redis:

- `InMemoryTokenStore` stores login tokens.
- `NoOpHotPostRankStore` reads hot posts directly from MySQL.

Enable the `redis` profile to use Redis:

- `RedisTokenStore` stores tokens in Redis with TTL.
- `RedisHotPostRankStore` uses Redis ZSet for hot post ranking.

Redis environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `CAMPUSHUB_REDIS_HOST` | `localhost` | Redis host |
| `CAMPUSHUB_REDIS_PORT` | `6379` | Redis port |
| `CAMPUSHUB_REDIS_PASSWORD` | empty | Redis password |
| `CAMPUSHUB_REDIS_DATABASE` | `0` | Redis database |

Run with Redis:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=redis"
```

## RocketMQ Profile

The default profile handles domain events synchronously. Enable `rocketmq` to publish and consume comment/like events asynchronously.

RocketMQ settings are in:

```text
src/main/resources/application-rocketmq.yaml
```

Common environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `CAMPUSHUB_ROCKETMQ_NAME_SERVER` | `localhost:9876` | RocketMQ NameServer |
| `CAMPUSHUB_ROCKETMQ_PRODUCER_GROUP` | `campushub-producer-group` | Producer group |
| `CAMPUSHUB_COMMENT_TOPIC` | `campushub-comment-event` | Comment event topic |
| `CAMPUSHUB_LIKE_TOPIC` | `campushub-like-event` | Like event topic |

Run with RocketMQ:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=rocketmq
```

Run with Redis and RocketMQ:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis,rocketmq
```

## Tests

Tests use H2 and do not require local MySQL, Redis, or RocketMQ.

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Current integration tests cover:

- Spring context startup
- Register and login
- Category query
- Post creation
- Comment creation and listing
- Like and unlike behavior
- Notice query and unread count
- Permission and validation boundary cases

## Typical API Examples

Register:

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "123456",
  "nickname": "Alice"
}
```

Login:

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "123456"
}
```

Create post:

```http
POST /api/posts
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "categoryId": 1,
  "title": "How should I prepare for final exams?",
  "content": "Any useful study plan or resource recommendations?"
}
```

Query hot posts:

```http
GET /api/posts/hot?limit=10
```

## Resume Summary

CampusHub is a pure Spring Boot backend project for a campus forum. It implements authentication, user profiles, posts, comments, likes, notices, admin operations, Redis ZSet hot ranking, RocketMQ event decoupling, batched view-count flushing, unified API responses, global exception handling, and H2 integration tests. The project is designed as a compact but complete backend system suitable for internship resume presentation and technical interviews.
