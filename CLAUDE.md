# CLAUDE.md — Java Project Guidelines

## Project Overview

This is a Java project. Follow these conventions and guidelines when working on the codebase.

## Build & Run

```bash
# Build
mvn clean compile          # Maven
./gradlew build            # Gradle

# Test
mvn test                   # Run all tests
mvn test -Dtest=ClassName  # Run a specific test class
./gradlew test             # Gradle equivalent

# Package
mvn package -DskipTests    # Build JAR/WAR without tests
./gradlew jar              # Gradle equivalent

# Run
java -jar target/*.jar     # Run packaged JAR
mvn spring-boot:run        # Spring Boot (Maven)
./gradlew bootRun          # Spring Boot (Gradle)

# Lint / Static Analysis
mvn checkstyle:check       # Checkstyle
mvn spotbugs:check         # SpotBugs
./gradlew spotlessCheck    # Spotless (Gradle)
```

## Code Style & Conventions

### General
- **Java version**: 17+ (use modern language features: records, sealed classes, pattern matching, text blocks)
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) unless project `.editorconfig` or Checkstyle config says otherwise
- Max line length: 120 characters
- Indentation: 4 spaces (no tabs)
- Always use braces for `if`, `for`, `while`, even for single-line bodies

### Naming
- **Classes/Interfaces**: `PascalCase` — `UserService`, `OrderRepository`
- **Methods/Variables**: `camelCase` — `getUserById`, `orderCount`
- **Constants**: `UPPER_SNAKE_CASE` — `MAX_RETRY_COUNT`
- **Packages**: all lowercase, no underscores — `com.example.userservice`
- **Test classes**: suffix with `Test` — `UserServiceTest`

### Class Organization
Order members within a class as follows:
1. Static fields
2. Instance fields
3. Constructors
4. Public methods
5. Package-private / protected methods
6. Private methods
7. Inner classes / enums

### Imports
- No wildcard imports (`import java.util.*` ❌)
- Order: `java.*`, `javax.*`, third-party, project-internal (separated by blank lines)
- Remove unused imports

## Architecture & Design

### Patterns
- Favor **composition over inheritance**
- Use **dependency injection** (constructor injection preferred over field injection)
- Follow **SOLID** principles
- Prefer **immutable objects** — use `record` types where appropriate
- Use the **Builder pattern** for objects with many optional parameters
- Use **Optional** for return types that may be absent; never use `null` as a return value for collections (return empty collections instead)

### Package Structure
```
src/main/java/com/example/project/
├── config/          # Configuration classes
├── controller/      # REST controllers / API layer
├── service/         # Business logic
├── repository/      # Data access layer
├── model/           # Domain entities / DTOs
│   ├── entity/      # JPA / persistence entities
│   └── dto/         # Data transfer objects
├── exception/       # Custom exceptions & global handler
└── util/            # Utility / helper classes
```

### API Design (REST)
- Use proper HTTP methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`
- Return appropriate status codes (200, 201, 204, 400, 404, 409, 500)
- Use `ResponseEntity<>` for explicit control over responses
- Validate request bodies with `@Valid` and Bean Validation annotations

## Error Handling

- Use **custom exception classes** that extend `RuntimeException` for domain errors
- Implement a `@RestControllerAdvice` global exception handler
- Never swallow exceptions silently — log them at minimum
- Use specific exception types, not generic `Exception` or `RuntimeException`
- Include meaningful error messages and context

```java
// Good
throw new OrderNotFoundException("Order not found with id: " + orderId);

// Bad
throw new RuntimeException("not found");
```

## Testing

### Conventions
- Test class mirrors source class: `UserService` → `UserServiceTest`
- Test method naming: `shouldReturnUser_whenValidIdProvided()` or `givenValidId_whenGetUser_thenReturnsUser()`
- Use **JUnit 5** (`@Test`, `@BeforeEach`, `@DisplayName`, `@Nested`)
- Use **Mockito** for mocking dependencies
- Use **AssertJ** for fluent assertions (preferred over JUnit assertions)

### Structure
- Follow **Arrange-Act-Assert** (AAA) pattern
- Each test should test **one behavior**
- Use `@Nested` classes to group related tests
- Use `@ParameterizedTest` for testing multiple inputs

### Test Types
- **Unit tests**: Test individual classes in isolation with mocked dependencies
- **Integration tests**: Use `@SpringBootTest` or `@DataJpaTest` for DB-layer tests
- **Controller tests**: Use `@WebMvcTest` with `MockMvc`

## Logging

- Use **SLF4J** (`org.slf4j.Logger`) — never `System.out.println`
- Use parameterized logging: `log.info("Processing order: {}", orderId)`
- Log levels:
    - `ERROR` — unexpected failures requiring attention
    - `WARN` — recoverable issues or degraded behavior
    - `INFO` — significant business events
    - `DEBUG` — detailed diagnostic info (off in production)

## Dependencies & Libraries

### Preferred Libraries
| Purpose              | Library                        |
|----------------------|--------------------------------|
| HTTP / REST          | Spring Web / Spring WebFlux    |
| Persistence          | Spring Data JPA / Hibernate    |
| Validation           | Jakarta Bean Validation        |
| JSON                 | Jackson                        |
| Logging              | SLF4J + Logback                |
| Testing              | JUnit 5 + Mockito + AssertJ    |
| Utility              | Apache Commons, Guava          |
| API Docs             | SpringDoc OpenAPI (Swagger)    |
| Mapping              | MapStruct                      |

### Dependency Management
- Pin dependency versions explicitly
- Use Maven BOM / Gradle platform for version alignment
- Keep dependencies up to date; check for vulnerabilities

## Common Pitfalls to Avoid

- ❌ Field injection (`@Autowired` on fields) — use constructor injection
- ❌ Returning `null` from methods that return collections — return `Collections.emptyList()`
- ❌ Catching `Exception` or `Throwable` broadly — catch specific types
- ❌ Mutable DTOs passed across layers — use records or immutable objects
- ❌ Business logic in controllers — keep controllers thin, logic in services
- ❌ N+1 query problems — use `@EntityGraph` or `JOIN FETCH`
- ❌ Missing `@Transactional` on service methods that modify data
- ❌ Hardcoded configuration values — use `@Value` or `@ConfigurationProperties`

## Git & PR Conventions

- **Branch names**: `feature/add-user-endpoint`, `fix/order-validation-bug`
- **Commit messages**: imperative mood, concise — `Add user registration endpoint`
- Keep PRs focused and small; one feature or fix per PR
- All tests must pass before merging