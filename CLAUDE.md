# CLAUDE.md - Trade Validator Service

> Documentation for AI assistants working with the Trade Validator codebase

## Project Overview

**Trade Validator** is a Spring Boot microservice that validates financial trade details using configurable business rules. The application follows a clean, modular architecture with dependency injection, parallel processing, and runtime configurability via JMX.

**Key Facts**:
- **Language**: Java 8+
- **Build Tool**: Gradle 5.6.4 (wrapper included)
- **Framework**: Spring Framework 4.3.3.RELEASE, Spring Boot 2.1.18.RELEASE
- **Architecture**: Multi-module Gradle project with 5 modules
- **Testing**: JUnit 4, Mockito, Hamcrest
- **API Documentation**: Swagger UI
- **Management**: JMX MBeans, Spring Boot Actuator

---

## Module Architecture

The project uses a **layered module architecture** with clear separation of concerns:

```
trade-validator-model (domain models)
       ↓
trade-validator-api (public interfaces)
       ↓
trade-validators + trade-validator-core (implementations + orchestration)
       ↓
trade-validator-app (REST API + Spring Boot application)
```

### Module Breakdown

| Module | Location | Purpose | Key Classes |
|--------|----------|---------|-------------|
| **trade-validator-model** | `/trade-validator-model/` | Domain models and value objects | `Trade`, `ValidationResult`, `TradeValidationResult`, `ValidationError` |
| **trade-validator-api** | `/trade-validator-api/` | Public interfaces for extensibility | `TradeValidator`, `CurrencyHolidayService` |
| **trade-validators** | `/trade-validators/` | Business rule implementations (7 validators) | `CustomerValidator`, `LegalEntityValidator`, `CurrencyValidator`, `SpotForwardValidator`, `OptionTypeValidator`, `ValueDateWeekendValidator`, `TradeDateValueValidator` |
| **trade-validator-core** | `/trade-validator-core/` | Validation orchestration | `ValidationCore` |
| **trade-validator-app** | `/trade-validator-app/` | REST API and Spring Boot main class | `TradeValidatorAppApplication`, `ValidationController`, `ShutdownController` |

---

## Directory Structure

```
/home/user/trade-validator/
├── gradle/wrapper/              # Gradle wrapper files
├── raw_trades/                  # Sample JSON trade files for testing
│   ├── single_trade.json
│   └── bulk_trades.json
├── trade-validator-model/       # Domain models module
│   └── src/main/java/com/tradevalidator/model/
├── trade-validator-api/         # API interfaces module
│   └── src/main/java/com/tradevalidator/validator/
├── trade-validators/            # Validator implementations module
│   ├── src/main/java/com/tradevalidator/validators/
│   └── src/test/java/           # Unit tests for validators
├── trade-validator-core/        # Core orchestration module
│   └── src/main/java/com/tradevalidator/core/
├── trade-validator-app/         # Spring Boot application module
│   ├── src/main/java/com/tradevalidator/
│   ├── src/main/resources/
│   │   ├── application.properties  # Main configuration file
│   │   └── static/index.html
│   └── src/test/java/           # Integration tests
├── build.gradle                 # Root build configuration
├── settings.gradle              # Multi-module project definition
├── gradlew & gradlew.bat       # Gradle wrapper scripts
└── README.md                    # User documentation
```

---

## Key Design Patterns

### 1. Strategy Pattern (Validators)
Each validator implements the `TradeValidator` interface:
```java
public interface TradeValidator {
    ValidationResult validate(Trade trade);
}
```

All validators are Spring-managed beans (`@Component`) and are auto-discovered via component scanning.

### 2. Fluent Builder Pattern
All model classes use fluent builders for construction:
```java
Trade trade = Trade.newTrade()
    .customer("PLUTO1")
    .ccyPair("EURUSD")
    .build();

ValidationError error = ValidationError.validationError()
    .field("valueDate")
    .message("Value date is invalid")
    .build();
```

### 3. Dependency Injection
- **Constructor injection** for required dependencies
- **@Autowired collection injection** in `ValidationCore` to auto-discover all validators
- **Interface-based design** for extensibility

### 4. Parallel Processing
`ValidationCore` uses Java 8 parallel streams to run all validators concurrently:
```java
validators.parallelStream()
    .map(validator -> validator.validate(trade))
    .collect(Collectors.toList());
```

Thread-safe collections (`ConcurrentHashMap`) used in `TradeValidationResult`.

### 5. Runtime Configuration
Three-tier configuration approach:
1. **Hardcoded defaults** in validator constructors
2. **Spring @Value injection** from `application.properties`
3. **JMX MBeans** for runtime updates via `@ManagedResource`

---

## Package Structure

### Main Packages

- **`com.tradevalidator.model`** - Domain models (`Trade`, `ValidationResult`, etc.)
- **`com.tradevalidator.validator`** - Public API interfaces
- **`com.tradevalidator.validators`** - Validator implementations
- **`com.tradevalidator.core`** - Core orchestration service
- **`com.tradevalidator`** - Spring Boot application entry point
- **`com.tradevalidator.rest`** - REST controllers
- **`com.tradevalidator.util`** - Utilities (exception handlers, info contributors)

---

## Important Files

### Configuration
**`/trade-validator-app/src/main/resources/application.properties`**
```properties
# Customer validation
validator.customer.validCustomers=PLUTO1,PLUTO2

# Today date for spot/forward calculation (yyyy-MM-dd format)
validator.todayDate=2016-09-10

# Legal entities
validator.legalEntities=CS Zurich

# Spot/Forward types
validator.soptforward.spotTypes=Spot
validator.soptforward.forwardTypes=Forward

# Option configuration
validator.options.optionType=VanillaOption
validator.options.europeanStyles=EUROPEAN
validator.options.americanStyles=AMERICAN
```

### Build Configuration
**`/build.gradle`** - Root build file with:
- Spring version: 4.3.3.RELEASE
- Group: `com.tradevalidator`
- Version: `1.0.0-SNAPSHOT`
- Java compatibility: 1.8
- Common test dependencies

**`/settings.gradle`** - Multi-module project definition (module order matters for dependency resolution)

---

## API Endpoints

### Validation Endpoints
- **`POST /api/validate`** - Validate a single trade
  - Request: Single `Trade` JSON object
  - Response: `TradeValidationResult` with errors (if any)

- **`POST /api/validateBulk`** - Validate multiple trades
  - Request: Array of `Trade` JSON objects
  - Response: Array of `TradeValidationResult` objects

### Management Endpoints
- **`GET /api/shutdown`** - Check shutdown status
- **`POST /api/shutdown`** - Initiate graceful shutdown (stops accepting validation requests)
- **`DELETE /api/shutdown`** - Cancel pending shutdown
- **`GET /info`** - Service information (includes shutdown status)
- **`GET /metrics`** - Application metrics (Spring Boot Actuator)

### Documentation & Monitoring
- **`/swagger-ui.html`** - Interactive API documentation
- **`/jmx/*`** - JMX console (JMinix) for runtime management
- **`/logs`** - Logback status messages
- **`/`** - Static landing page

---

## Validation Rules

### 1. CustomerValidator
- **Rule**: Customer must be in approved list
- **Default**: PLUTO1, PLUTO2
- **Config**: `validator.customer.validCustomers` (comma-separated)
- **Location**: `trade-validators/src/main/java/com/tradevalidator/validators/CustomerValidator.java:24`

### 2. LegalEntityValidator
- **Rule**: Legal entity must be valid
- **Default**: CS Zurich
- **Config**: `validator.legalEntities` (comma-separated)
- **Location**: `trade-validators/src/main/java/com/tradevalidator/validators/LegalEntityValidator.java:24`

### 3. CurrencyValidator
- **Rules**:
  - Currency pair must be 6 characters (e.g., EURUSD)
  - Both currencies must be valid ISO codes
  - Value date cannot fall on currency holidays
- **Dependencies**: Uses `CurrencyHolidayService`
- **Location**: `trade-validators/src/main/java/com/tradevalidator/validators/CurrencyValidator.java:28`

### 4. SpotForwardValidator
- **Rules**:
  - **Spot trades**: Value date must be exactly +2 days from "today"
  - **Forward trades**: Value date must be more than 2 days from "today"
- **Config**:
  - `validator.todayDate` (yyyy-MM-dd format)
  - `validator.soptforward.spotTypes`
  - `validator.soptforward.forwardTypes`
- **Location**: `trade-validators/src/main/java/com/tradevalidator/validators/SpotForwardValidator.java:33`

### 5. OptionTypeValidator
- **Rules**:
  - Style must be AMERICAN or EUROPEAN
  - Expiry date and premium date must be before delivery date
  - American options must have excerciseStartDate
  - ExcerciseStartDate must be between trade date and expiry date
- **Config**: `validator.options.*` properties
- **Location**: `trade-validators/src/main/java/com/tradevalidator/validators/OptionTypeValidator.java:29`

### 6. ValueDateWeekendValidator
- **Rule**: Value date cannot fall on weekend
- **Default**: Saturday, Sunday
- **Config**: JMX runtime configuration
- **Location**: `trade-validators/src/main/java/com/tradevalidator/validators/ValueDateWeekendValidator.java:22`

### 7. TradeDateValueValidator
- **Rule**: Value date cannot be before trade date
- **Config**: None (always enforced)
- **Location**: `trade-validators/src/main/java/com/tradevalidator/validators/TradeDateValueValidator.java:16`

---

## Development Workflows

### Building the Project
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :trade-validator-app:build

# Clean build
./gradlew clean build

# Skip tests
./gradlew build -x test
```

### Running the Application
```bash
# Run with Gradle
./gradlew :trade-validator-app:bootRun

# Run JAR directly (after build)
java -jar trade-validator-app/build/libs/trade-validator-app-1.0.0-SNAPSHOT.jar
```

**Default URL**: http://localhost:8080

### Running Tests
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :trade-validators:test

# Run integration tests
./gradlew :trade-validator-app:test
```

### Testing the API Manually
```bash
# Validate single trade
curl -H "Content-Type: application/json" \
     -X POST \
     --data "@./raw_trades/single_trade.json" \
     http://localhost:8080/api/validate

# Validate bulk trades
curl -H "Content-Type: application/json" \
     -X POST \
     --data "@./raw_trades/bulk_trades.json" \
     http://localhost:8080/api/validateBulk
```

---

## Testing Strategy

### Unit Tests
**Location**: `/trade-validators/src/test/java/`

**Pattern**:
- One test class per validator
- Test valid scenarios
- Test invalid data
- Test missing/null data
- Use Hamcrest matchers for assertions
- Mock dependencies with Mockito

**Example test structure**:
```java
@Test
public void shouldValidateCorrectCustomer() {
    // Given
    Trade trade = Trade.newTrade().customer("PLUTO1").build();

    // When
    ValidationResult result = validator.validate(trade);

    // Then
    assertThat(result.hasErrors(), is(false));
}
```

### Integration Tests
**Location**: `/trade-validator-app/src/test/java/`

**Pattern**:
- Full Spring Boot context loaded (`@SpringBootTest`)
- Tests REST endpoints end-to-end
- Uses `TestRestTemplate`
- Validates JSON serialization/deserialization
- Uses real test data from resources

**Test data location**: `/trade-validator-app/src/test/resources/`

---

## Adding New Validators

### Step-by-Step Guide

1. **Create validator class** in `trade-validators/src/main/java/com/tradevalidator/validators/`
   ```java
   @Component
   @ManagedResource(description = "My New Validator")
   public class MyNewValidator implements TradeValidator {

       @Override
       public ValidationResult validate(Trade trade) {
           ValidationResult result = ValidationResult.validationResult();

           // Add validation logic
           if (/* invalid condition */) {
               result.withError(ValidationError.validationError()
                   .field("fieldName")
                   .message("Error message")
                   .build());
           }

           return result;
       }
   }
   ```

2. **Add configuration** (optional) in `application.properties`:
   ```properties
   validator.mynew.someProperty=value
   ```

3. **Inject configuration** (optional):
   ```java
   private String configValue = "default";

   @Value("${validator.mynew.someProperty:default}")
   @ManagedAttribute
   public void setConfigValue(String value) {
       this.configValue = value;
   }
   ```

4. **Create unit test** in `trade-validators/src/test/java/com/tradevalidator/validators/`:
   ```java
   public class MyNewValidatorTest {
       private MyNewValidator validator;

       @Before
       public void setUp() {
           validator = new MyNewValidator();
       }

       @Test
       public void shouldValidateCorrectTrade() {
           // Test implementation
       }
   }
   ```

5. **Build and test**:
   ```bash
   ./gradlew :trade-validators:test
   ./gradlew build
   ```

**Note**: Spring will automatically discover the new validator via component scanning. No additional registration needed!

---

## Extending the Application

### Adding New Holiday Data Source

Implement `CurrencyHolidayService` interface:
```java
@Component
public class ExternalHolidayService implements CurrencyHolidayService {
    @Override
    public Optional<Set<Date>> fetchHolidays(Currency currency) {
        // Fetch from external API or database
        return Optional.of(holidays);
    }
}
```

### Adding New REST Endpoints

Create controller in `trade-validator-app/src/main/java/com/tradevalidator/rest/`:
```java
@RestController
@RequestMapping("/api")
public class MyNewController {

    @Autowired
    private ValidationCore validationCore;

    @GetMapping("/myendpoint")
    public ResponseEntity<?> myEndpoint() {
        // Implementation
    }
}
```

### Modifying Trade Model

1. Add fields to `Trade.java` in `trade-validator-model` module
2. Add Jackson annotations for JSON serialization: `@JsonProperty`
3. Add to builder pattern with fluent setter
4. Update validators that need to check the new field
5. Update test data in `raw_trades/` and test resources

---

## Common Patterns and Conventions

### Date Handling
- **Format**: Always use `yyyy-MM-dd` pattern
- **Parsing**: Use `SimpleDateFormat` or Java 8 `LocalDate`
- **Comparison**: Be aware of timezone issues
- **Example**: `2016-08-11`

### Null Safety
- **Always check for nulls** in validators before accessing trade fields
- Use `Optional` for method returns when value may not exist
- Early return pattern for null checks:
  ```java
  if (trade.getCustomer() == null) {
      return ValidationResult.validationResult();
  }
  ```

### Error Messages
- Be descriptive and user-friendly
- Include expected vs actual values when relevant
- Example: "On spot trades valueDate should be +2 days from today date"

### Logging
- Use SLF4J with Logback
- Log at appropriate levels:
  - **DEBUG**: Detailed flow information
  - **INFO**: Important business events
  - **WARN**: Recoverable issues
  - **ERROR**: Serious problems
- Example: `log.debug("Validating trade with customer: {}", trade.getCustomer());`

### Thread Safety
- Validators should be **stateless** (no instance variables modified during validation)
- Use thread-safe collections when needed (`ConcurrentHashMap`)
- Parallel processing relies on validators being thread-safe

---

## Important Gotchas

### 1. Module Dependencies
When adding dependencies, respect the module hierarchy:
- Never create circular dependencies
- Lower layers should not depend on upper layers
- Use API module for shared interfaces

### 2. Gradle Module References
When adding inter-module dependencies:
```gradle
dependencies {
    implementation project(':trade-validator-model')
}
```

### 3. Date Format Consistency
The "today date" in `application.properties` (`validator.todayDate=2016-09-10`) is used for spot/forward validation. Update this when testing or change the property injection to use `LocalDate.now()` for production.

### 4. Shutdown Behavior
When `ValidationCore.shutdown()` is called:
- All validation requests throw `CoreShutdownException`
- Returns HTTP 403 to clients
- Can be cancelled with `cancelShutdown()`

### 5. JMX Configuration Changes
Changes made via JMX are **runtime only** and will be lost on restart. For permanent changes, update `application.properties`.

### 6. Currency Holiday Service
Default implementation in `TradeValidatorAppApplication` returns hardcoded USD holidays. For production, implement a real external service.

### 7. Parallel Streams Performance
Validators run in parallel via `parallelStream()`. If you have many validators or they're I/O bound, consider:
- Making validators async-compatible
- Using a custom thread pool
- Implementing timeout mechanisms

---

## Code Quality Standards

### Java Conventions
- Use **camelCase** for methods and variables
- Use **PascalCase** for classes
- Use **UPPER_SNAKE_CASE** for constants
- Keep methods small and focused (Single Responsibility Principle)
- Prefer composition over inheritance

### Testing Standards
- Test coverage should be maintained for all validators
- Each validator must have corresponding unit tests
- Integration tests should cover happy path and error cases
- Use descriptive test method names: `shouldValidateCorrectCustomer()`

### Documentation
- Use JavaDoc for public APIs
- Document complex business logic
- Keep README.md updated with new endpoints or features
- Update this CLAUDE.md when architecture changes

---

## Technology Stack Reference

| Component | Technology | Version | Notes |
|-----------|-----------|---------|-------|
| **Build** | Gradle | 5.6.4 | Wrapper included |
| **Java** | JDK | 8+ | Language level 8 |
| **Framework** | Spring Framework | 4.3.3.RELEASE | Core DI container |
| **Boot** | Spring Boot | 2.1.18.RELEASE | Auto-configuration |
| **Web** | Spring MVC | 4.3.3.RELEASE | REST endpoints |
| **JSON** | Jackson | 2.8.3 | Serialization |
| **Logging** | Logback | 1.1.6 | SLF4J backend |
| **API Docs** | Springfox Swagger | 2.6.0 | `/swagger-ui.html` |
| **JMX** | JMinix | 1.2.0 | `/jmx/*` console |
| **Actuator** | Spring Boot Actuator | 2.1.18 | `/metrics`, `/info` |
| **Testing** | JUnit | 4.12 | Test framework |
| **Mocking** | Mockito | 1.10.19 | Mock objects |
| **Assertions** | Hamcrest | 1.3 | Matcher library |
| **Utils** | Apache Commons Lang3 | 3.4 | Utility methods |

---

## Troubleshooting

### Gradle Build Issues

**Problem**: Restlet dependency errors
**Solution**: The `build.gradle` includes Restlet repository: `https://maven.restlet.talend.com`

**Problem**: `./gradlew` permission denied
**Solution**: `chmod +x gradlew`

**Problem**: Java not found
**Solution**: Ensure `JAVA_HOME` is set and Java 8+ is in PATH

### Runtime Issues

**Problem**: Port 8080 already in use
**Solution**:
- Stop other service on 8080
- Or change port: `--server.port=8081`

**Problem**: Validators not being discovered
**Solution**:
- Ensure validator class has `@Component` annotation
- Check package is under `com.tradevalidator`
- Verify module has dependency on `trade-validator-api`

**Problem**: Configuration not loading
**Solution**:
- Check property name matches exactly
- Verify `application.properties` is in `src/main/resources/`
- Check for typos in property keys

### Test Issues

**Problem**: Integration tests failing
**Solution**:
- Ensure Spring Boot application starts successfully
- Check test resources are in correct location
- Verify test data JSON is valid

---

## Quick Reference Commands

```bash
# Build
./gradlew build                              # Full build
./gradlew clean build                        # Clean build
./gradlew build -x test                      # Build without tests

# Run
./gradlew :trade-validator-app:bootRun       # Run application

# Test
./gradlew test                               # All tests
./gradlew :trade-validators:test             # Unit tests only
./gradlew :trade-validator-app:test          # Integration tests only

# Check dependencies
./gradlew dependencies                        # Show dependency tree
./gradlew :trade-validator-app:dependencies  # Module dependencies

# Clean
./gradlew clean                              # Clean build artifacts
```

---

## Git Workflow

### Branch Strategy
- **Main branch**: Production-ready code
- **Feature branches**: Use `claude/` prefix for AI-generated work
- **Current branch**: `claude/claude-md-mhy1tgnd9i79vkdk-01RGZow4KTAs7jKWHcVKUCHc`

### Commit Standards
- Write clear, descriptive commit messages
- Reference issue numbers when applicable
- Group related changes in single commits
- Examples:
  - "Add currency pair length validator"
  - "Fix weekend date validation for null dates"
  - "Update application.properties with new legal entities"

### Pushing Changes
Always push to the designated branch:
```bash
git push -u origin claude/claude-md-mhy1tgnd9i79vkdk-01RGZow4KTAs7jKWHcVKUCHc
```

---

## For AI Assistants: Best Practices

### When Making Changes

1. **Understand the module structure** - Changes should be in the appropriate module
2. **Follow existing patterns** - Use fluent builders, component scanning, etc.
3. **Maintain thread safety** - Validators run in parallel
4. **Add tests** - Unit tests for validators, integration tests for endpoints
5. **Update configuration** - Add properties if needed
6. **Respect dependencies** - Don't create circular module dependencies
7. **Document changes** - Update README.md and this file when needed

### When Adding Features

1. **Start with the model** - Update `Trade` or `ValidationResult` if needed
2. **Define interfaces** - Add to API module if creating extension points
3. **Implement validators** - In `trade-validators` module
4. **Add configuration** - In `application.properties`
5. **Wire up in app** - REST endpoints in `trade-validator-app`
6. **Test thoroughly** - Unit + integration tests
7. **Document** - API docs, README, examples

### When Debugging

1. **Check logs** - Logback provides detailed output
2. **Use JMX** - Runtime inspection via `/jmx/*`
3. **Test in isolation** - Unit tests for individual validators
4. **Verify configuration** - Check property loading via debug logs
5. **Use Swagger UI** - Test endpoints interactively

---

## Support and Resources

- **README.md** - User-facing documentation with examples
- **Swagger UI** - Interactive API documentation at `/swagger-ui.html`
- **Source code** - Well-commented with clear structure
- **Tests** - Comprehensive examples of usage patterns
- **Sample data** - `raw_trades/` directory contains example trades

---

*Last updated: 2025-11-13*
*Project version: 1.0.0-SNAPSHOT*
