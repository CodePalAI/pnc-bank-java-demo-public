# PNC Bank Demo Application

This Java Spring Boot application demonstrates key banking functionalities with enterprise-grade design patterns, reliability, security, and maintainability. It was built as a showcase for PNC Bank stakeholders.

## Features

- **Core Banking Operations**
  - Account Management: Create, view, update, and delete customer accounts
  - Transaction Processing: Deposits, withdrawals, and inter-account transfers with real-time balance updates
  - Balance Inquiry & History: View current balances and paginated transaction history

- **Technical Highlights**
  - Layered Architecture with separate packages for controllers, services, repositories, and models
  - RESTful API endpoints following REST conventions
  - Entity mapping with Spring Data JPA
  - Database migration management with Flyway
  - DTOs and Mappers using MapStruct
  - Server-side web UI with Thymeleaf
  - Security with Spring Security
  - Global exception handling and input validation
  - Scheduled reporting

## Getting Started

### Prerequisites

- Java 17 or later
- Maven or Gradle (Maven is used in the examples below)

### Running in Demo Mode

The application comes with a demo mode that preloads sample data for demonstration purposes. This is the easiest way to get started:

```bash
# Clone this repository
git clone https://github.com/yourusername/pnc-bank-demo.git
cd pnc-bank-demo

# Build the application
mvn clean install

# Run in demo mode (default)
mvn spring-boot:run
```

Then navigate to http://localhost:8080 in your browser.

### Login Credentials

The application is secured with basic authentication:

- Regular User: `user` / `password`
- Admin User: `admin` / `password`

### Running in Production Mode

To run in production mode with an external database:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Note: In production mode, you'll need to configure your PostgreSQL database settings in `application-prod.properties`.

## API Documentation

The application provides RESTful API endpoints for all banking operations. Here are some example curl commands:

### Account Management

#### List all accounts
```bash
curl -X GET "http://localhost:8080/api/accounts" -u admin:password
```

#### Get account by ID
```bash
curl -X GET "http://localhost:8080/api/accounts/1" -u admin:password
```


#### Create a new account
```bash
curl -X POST "http://localhost:8080/api/accounts" \
  -H "Content-Type: application/json" \
  -u admin:password \
  -d '{
    "accountNumber": "1234567890",
    "accountHolder": "John Doe",
    "accountType": "CHECKING",
    "initialDeposit": 1000.00
  }'
```

### Transaction Processing

#### Deposit
```bash
curl -X POST "http://localhost:8080/api/transactions/deposit" \
  -H "Content-Type: application/json" \
  -u admin:password \
  -d '{
    "accountNumber": "1234567890",
    "amount": 500.00,
    "description": "Deposit via API"
  }'
```

#### Withdraw
```bash
curl -X POST "http://localhost:8080/api/transactions/withdraw" \
  -H "Content-Type: application/json" \
  -u admin:password \
  -d '{
    "accountNumber": "1234567890",
    "amount": 200.00,
    "description": "Withdrawal via API"
  }'
```

#### Transfer
```bash
curl -X POST "http://localhost:8080/api/transactions/transfer" \
  -H "Content-Type: application/json" \
  -u admin:password \
  -d '{
    "fromAccountNumber": "1234567890",
    "toAccountNumber": "0987654321",
    "amount": 300.00,
    "description": "Transfer via API"
  }'
```

## Project Structure

```
src/main/java/io/pnc/bank/demo/
├── BankDemoApplication.java
├── controller/                  # REST and web controllers
├── service/                     # Business logic
│   └── impl/                    # Service implementations
├── repository/                  # Data access
├── model/                       # Entity classes
├── dto/                         # Data Transfer Objects
├── mapper/                      # Object mappers
├── exception/                   # Custom exceptions
├── config/                      # Configuration classes
└── util/                        # Utility classes

src/main/resources/
├── application.properties       # Common properties
├── application-demo.properties  # Demo mode properties
├── application-prod.properties  # Production mode properties
├── db/migration/                # Flyway database migrations
├── static/                      # Static resources
└── templates/                   # Thymeleaf templates
```

## Configuration Options

The application offers several configuration options:

- **Profiles**: `demo` (default) and `prod`
- **Database**: H2 in-memory database (demo) or PostgreSQL (prod)
- **Demo Data**: Sample data loading can be enabled/disabled via `demo.data.initialize` property
- **Sample Size**: Number of demo accounts can be configured via `demo.account.count` property

## License

This project is licensed under the MIT License - see the LICENSE file for details.