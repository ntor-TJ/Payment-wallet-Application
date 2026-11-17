# Payment Wallet Application - Hackathon Assessment & Capstone Challenge

Welcome to the **Payment Wallet Application Capstone Assessment**. This repository contains a Spring Boot-based RESTful payment wallet system that has been intentionally sabotaged and configured with architectural flaws, missing validation gates, broken JPA mappings, and stripped enterprise infrastructure.

Your objective as a candidate is to audit the codebase, diagnose the issues, and implement robust, production-ready solutions across the 5 core capstone tasks outlined below.

---

## 🎯 Candidate Tasks Overview

| # | Task Area | Core Focus |
|---|---|---|
| **1** | **JPA Mapping & Performance** | Fix broken entity relationships and eliminate N+1 query bottlenecks (`FetchType.LAZY` / Join Fetch). |
| **2** | **Input Validation & JSR-380** | Prevent negative money transfers and bad payloads using `@Valid` and Bean Validation constraints. |
| **3** | **Global Exception Handling** | Re-architect a centralized `@RestControllerAdvice` error handling system with proper HTTP status codes. |
| **4** | **Notification / Email Service** | Implement a decoupled notification/email service for critical financial transactions. |
| **5** | **Production Readiness & Observability** | Integrate Spring Boot Actuator, configure monitoring endpoints, and implement custom health indicators. |

---

## 📋 Detailed Task Specifications

### 🔹 Task 1: Fix JPA Entity Relationships & Eliminate N+1 Performance Issues
- **Background**:
  - The application currently experiences entity mapping errors on startup and database operations.
  - The `Wallet` entity has been altered to use eager fetching (`FetchType.EAGER`) across associated collections (`bankAccount`, `transactions`, `billpayment`), resulting in severe N+1 query execution problems when loading wallet records.
  - A relationship mapping between `Wallet` and `BankAccount` is broken (`mappedBy = "walletAccount"` mismatch).
- **Deliverables**:
  1. Fix the broken JPA relationship in `Wallet.java` so that `mappedBy` aligns accurately with the corresponding owning entity (`BankAccount.java`).
  2. Eliminate the N+1 problem:
     - Replace unnecessary `FetchType.EAGER` configurations on collections with `FetchType.LAZY`.
     - Where eager data loading is genuinely needed (e.g., fetching a wallet with its transactions or bank accounts), implement optimized queries using `JOIN FETCH` or Spring Data `@EntityGraph`.
  3. Verify bidirectional consistency and cascade definitions (`CascadeType`) across `Wallet`, `Customer`, `BankAccount`, and `Transaction`.

---

### 🔹 Task 2: Implement Input Validation & JSR-380 Constraints (@Valid)
- **Background**:
  - Validation rules have been stripped out. Currently, negative monetary transfers (e.g., transferring `-$500`) are accepted and processed by `WalletController.java` and `WalletServiceImpl.java`, allowing malicious balance inflation or negative balances.
  - Endpoints lack `@Valid` and proper payload contract enforcement.
- **Deliverables**:
  1. **Block Negative & Zero Money Transfers**:
     - Ensure money transfers, deposits, and bill payments strictly reject non-positive amounts (`amount <= 0`).
  2. **Introduce / Refactor Request DTOs**:
     - Create dedicated request DTOs (e.g., `FundTransferRequestDTO`, `WalletBalanceDTO`, `TransactionDTO`) rather than relying solely on raw `@PathVariable` inputs for complex operations.
     - Apply standard JSR-380 annotations:
       - `@Positive(message = "Amount must be strictly greater than zero")` / `@Min(1)`
       - `@NotNull(message = "Field cannot be null")`
       - `@Pattern(regexp = "[0-9]{10}", message = "Mobile number must be a valid 10-digit number")`
  3. **Controller Enforcement**:
     - Annotate all incoming request payloads in `WalletController`, `BankAccountController`, `CustomerController`, and `BeneficiaryController` with `@Valid`.
  4. **Service-Level Defensive Checks**:
     - Ensure `WalletServiceImpl.java` guards against illegal balance states, insufficient funds, and negative arguments.

---

### 🔹 Task 3: Centralized Global Exception Handling
- **Background**:
  - Centralized exception handlers were completely removed (`GlobalExceptionHandler` was deleted).
  - Unhandled exceptions (`InsuficientBalance`, `BankAccountNotFound`, `WalletNotFound`, `MethodArgumentNotValidException`, etc.) result in raw stack traces or default 500 error responses that leak internal implementation details and provide a poor client experience.
- **Deliverables**:
  1. Re-implement a production-grade centralized exception handler using `@RestControllerAdvice` (or `@ControllerAdvice`).
  2. Provide standard, structured JSON error responses with fields such as:
     - `timestamp` (`LocalDateTime`)
     - `statusCode` (`int`)
     - `error` (`String` - HTTP reason phrase)
     - `message` (`String` - clear exception description)
     - `path` / `details` (`String` - URI path or request details)
  3. Implement specific `@ExceptionHandler` methods returning standard HTTP statuses:
     - `BankAccountNotFound`, `WalletNotFound`, `UserNotFoundException` ➡️ **404 NOT FOUND**
     - `InsuficientBalance`, `InvalidDetailsException`, `BankAccountAlreadyExist` ➡️ **400 BAD REQUEST** or **409 CONFLICT**
     - `MethodArgumentNotValidException` ➡️ **400 BAD REQUEST** (with detailed field-by-field validation failure messages)
     - Generic `Exception.class` fallback ➡️ **500 INTERNAL SERVER ERROR**

---

### 🔹 Task 4: Implement Transactional Notification / Email Service
- **Background**:
  - The payment wallet lacks an automated notification and receipting system. Customers receive no notification when funds are credited, debited, or transferred.
- **Deliverables**:
  1. Design and implement an `EmailService` / `NotificationService` interface in `com.masai.service` (with a corresponding `Impl` class).
  2. Integrate the notification service into:
     - `fundTransfer` (notify both sender and recipient)
     - `addMoney` (credit notification)
     - `depositAmount` (debit/deposit notification)
  3. Notification payloads should include:
     - Source customer & target customer names
     - Transaction amount and currency
     - Transaction timestamp and reference ID
     - Updated wallet balance
  4. **Resilience & Best Practices**:
     - Implement mock email sending / logging or integrate with Spring's `JavaMailSender`.
     - Ensure notifications are asynchronous (`@Async`) or wrapped in fail-safe error handling so an email delivery failure does not roll back an otherwise successful monetary transaction.

---

### 🔹 Task 5: Production Readiness & Observability with Spring Boot Actuator
- **Background**:
  - The application lacks health checking, metrics, and production monitoring dependencies. Operations teams cannot monitor system uptime or verify database connectivity.
- **Deliverables**:
  1. Add `spring-boot-starter-actuator` to `pom.xml`.
  2. Configure `application.properties` to expose monitoring endpoints:
     ```properties
     management.endpoints.web.exposure.include=health,info,metrics
     management.endpoint.health.show-details=always
     ```
  3. Implement a custom `HealthIndicator` (e.g., `WalletServiceHealthIndicator`):
     - Validates database connectivity and wallet service operational readiness.
     - Reports custom status (`UP` / `DOWN`) along with diagnostic details.
  4. Verify that `/actuator/health` and `/actuator/metrics` return valid JSON telemetry.

---

## 🛠️ Technology Stack & Prerequisites

- **Java**: JDK 8+ (JDK 11 or 17 recommended)
- **Framework**: Spring Boot 2.7.x (Spring MVC, Spring Data JPA, Hibernate, Bean Validation)
- **Database**: MySQL 8.0+
- **Build Tool**: Apache Maven 3.6+
- **Documentation**: Swagger UI / OpenAPI 3.0

---

## 🚀 Local Setup & Execution Guide

### 1. Database Configuration
Create a database named `walletdb` in your local MySQL instance:
```sql
CREATE DATABASE walletdb;
```

Update credentials in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/walletdb?useSSL=false&serverTimezone=UTC
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 2. Build and Run
From the project root:
```bash
# Navigate to the Spring Boot module directory
cd "Project unit-6/PaymentWalletApplication"

# Compile and package
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.
- **Swagger Documentation**: `http://localhost:8080/swagger-ui/index.html`
- **Actuator Health (once implemented)**: `http://localhost:8080/actuator/health`

---

## 🧪 Testing Your Fixes

1. **Negative Money Transfer Test**:
   - Send a POST request to `/fundtransfer/-500/{sourceMobile}/{targetMobile}`.
   - **Expected Result**: HTTP 400 Bad Request with a clear validation error message. It must NOT succeed or deduct/add funds.
2. **N+1 Query Inspection**:
   - Enable SQL logging (`spring.jpa.show-sql=true`) and verify that querying wallets does not execute N individual SELECT queries for bank accounts and transactions.
3. **Global Exception Handling Test**:
   - Attempt a transfer with balance < transfer amount.
   - **Expected Result**: HTTP 400 with a structured JSON payload containing timestamp, status code, and `Balance is not Sufficient in Source Wallet`.
4. **Health Check Test**:
   - Request `GET /actuator/health`.
   - **Expected Result**: HTTP 200 OK with `{"status": "UP", ...}`.

---

## 📂 Git Branching & Submission Instructions

1. **Active Branch**: All work must be completed and committed on the `Ax_hackathon` branch:
   ```bash
   git checkout Ax_hackathon
   ```
2. **Commit Guidelines**:
   - Commit changes logically per task (e.g., `feat(jpa): fix relationship mapping and lazy fetching`, `feat(validation): enforce JSR-380 constraints on fund transfer`).
3. **Submission**:
   - Push your branch to the remote repository:
     ```bash
     git push origin Ax_hackathon
     ```
   - Open a Pull Request from `Ax_hackathon` targeting `main` with detailed notes on your fixes.
