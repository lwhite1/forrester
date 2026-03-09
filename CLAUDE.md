# CLAUDE.md — Java Project Guidelines

## Project Overview

This is a Java project. Follow these conventions and guidelines when working on the codebase.

## INVARIANTS
- Before committing, always execute every test 
- Before testing, always perform clean builds of the entire system. NEVER build a module in isolation.
- When auditing code, do a thorough code audit and quality assessment for the entire project. 
  Create agents to do a deep analysis of the entire codebase. Run StopBugs. Run JaCoCo for coverage. 
  For every problem found, create an issue in github unless one already exists. 
  Create a summary report in the docs/quality folder using the date and timestamp for the ile name. 
  If it's slow use more agents. Don't skip any part of this process.


```bash
# Build
mvn clean compile          # Maven

# Test
mvn test                   # Run all tests
mvn test -Dtest=ClassName  # Run a specific test class

# Package
mvn package -DskipTests    # Build JAR/WAR without tests

# Run
java -jar target/*.jar     # Run packaged JAR

# Lint / Static Analysis
mvn checkstyle:check       # Checkstyle
mvn spotbugs:check         # SpotBugs
```

## Code Style & Conventions

### General
- **Java version**: 25 (use modern language features: records, sealed classes, pattern matching, text blocks)
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
- Always run every unit test for the entire system before committing

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

### UI Testability
- **Isolate UI from logic**: Separate business logic and state management from UI components. This allows traditional unit testing of non-UI parts using JUnit without requiring a running JavaFX application
- **Use observable properties**: Store application state in observable classes (e.g., JavaFX `Property` types) and bind UI elements to these properties. Tests can interact with and verify the state of the model directly, treating the UI as a reliable black box
- **Use IDs for nodes**: Assign unique `fx:id` or CSS IDs to JavaFX scene graph nodes (e.g., `#myButton`). This makes it straightforward for testing frameworks like TestFX to locate and interact with specific elements programmatically
- **Use TestFX for UI interaction tests**: When testing UI behavior that is easier to verify through actual user interaction (clicking buttons, filling forms, verifying dialog behavior) than through unit tests alone, write TestFX tests. JUnit 5 remains the primary framework for logic and state testing; TestFX augments it for cases where simulating real user interaction provides better coverage

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
| Purpose              | Library                     |
|----------------------|-----------------------------|
| JSON                 | Jackson                     |
| Logging              | SLF4J + Logback             |
| Testing              | JUnit 5 + Mockito + AssertJ |
| Utility              | Apache Commons, Guava       |
| API Docs             | OpenAPI (Swagger)           |
| Mapping              | MapStruct                   |

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
- No mentions of Claude in commit message descriptions or summaries
- No mention of Claude in pull requests