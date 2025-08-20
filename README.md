# Trade Validation Service

A microservice that validates trade details using configurable business rules.

## Technology Stack
- **Build Tool**: Gradle
- **Framework**: Spring 4.3.3 with Spring Boot 1.4.1
- **Architecture**: Modular design with separated concerns

## Module Structure

| Module | Description |
|--------|-------------|
| `trade-validator-model` | Model classes for trades and validation errors |
| `trade-validator-api` | API interfaces for extending validation functionality |
| `trade-validator-core` | Core validation services and orchestration |
| `trade-validator-app` | REST API endpoints for trade validation |
| `trade-validators` | Business rule implementations |

## Architecture Overview

- **Validation Rules**: Each rule implements the `TradeValidator` interface as a Spring-managed bean
- **Dependency Injection**: Spring automatically injects available validators into `ValidationCore` at startup
- **Request Flow**: REST controllers → `ValidationCore` → Individual validators → Response
- **Configuration**: Validators support hardcoded defaults, Spring configuration, and runtime JMX management 



## API Endpoints

### Validation Endpoints
- **`POST /api/validate`** - Validate a single trade
  - Returns: Trade object with validation errors (if any)
- **`POST /api/validateBulk`** - Validate multiple trades
  - Returns: Array of trades, each with their validation errors

### Management Endpoints
- **`GET /api/shutdown`** - Check shutdown status
- **`POST /api/shutdown`** - Initiate graceful shutdown
- **`DELETE /api/shutdown`** - Cancel pending shutdown
- **`GET /info`** - Service information including shutdown status
- **`GET /metrics`** - Application metrics

### Documentation
- **`/swagger-ui.html#/validation-controller`** - Interactive API documentation

> **Note**: When shutdown is initiated, the service stops accepting new validation requests.



## Usage Examples

### Single Trade Validation

**Request:**
```bash
curl -H "Content-Type: application/json" \
     -X POST \
     --data "@./raw_trades/single_trade.json" \
     localhost:8080/api/validate
```

**Response:**
```json
{
  "trade": {
    "tradeDate": "2016-08-11",
    "valueDate": "2016-08-15",
    "customer": "PLUTO1",
    "ccyPair": "EURUSD",
    "type": "Spot",
    "legalEntity": "CS Zurich"
  },
  "invalidFields": {
    "valueDate": ["On spot trades valueDate should be +2 days from today date"]
  },
  "haveErrors": true
}
```

### Bulk Trade Validation

**Request:**
```bash
curl -H "Content-Type: application/json" \
     -X POST \
     --data "@./raw_trades/bulk_trades.json" \
     localhost:8080/api/validateBulk
```

**Response:**
```json
[
  {
    "trade": {
      "tradeDate": "2016-08-11",
      "valueDate": "2016-08-15",
      "customer": "PLUTO1",
      "ccyPair": "EURUSD",
      "type": "Spot",
      "legalEntity": "CS Zurich"
    },
    "invalidFields": {
      "valueDate": ["On spot trades valueDate should be +2 days from today date"]
    },
    "haveErrors": true
  }
]
```

## Common Validation Rules

- **Spot Trades**: Value date must be +2 business days from trade date
- **Forward Trades**: Value date must be more than 2 days from trade date
- **Options**: Expiry date must be before delivery date
- **Customer Validation**: Customer must be in approved list
- **Holiday Validation**: Value dates cannot fall on holidays
- **Date Logic**: Value date cannot be before trade date

## Build & Run

### Prerequisites
- Java 8 or higher
- Gradle (wrapper included)

### Building the Project
```bash
# In Git Bash (since Java is available there)
./gradlew build
```

### Running the Application
```bash
./gradlew :trade-validator-app:bootRun
```

### Troubleshooting

**Restlet Dependencies Issue:**
If you encounter Restlet dependency errors, the build.gradle files have been updated to:
- Remove problematic `spring-boot-starter-remote-shell` dependency
- Add Restlet Maven repository: `https://maven.restlet.talend.com`

**Java Not Found:**
- Ensure Java 8+ is installed and available in PATH
- Use Git Bash if Java is configured there
- Set JAVA_HOME environment variable if needed

### Management Endpoints Examples

**Check shutdown status:**
```bash
curl http://localhost:8080/api/shutdown
```

**Initiate shutdown:**
```bash
curl -X POST http://localhost:8080/api/shutdown
```
