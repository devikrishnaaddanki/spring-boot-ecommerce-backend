# 🛒 Spring Boot E-commerce Backend

A RESTful e-commerce backend application built using **Java 21 and Spring Boot**. The project implements secure authentication, product management, shopping cart operations, order processing, caching, and event-driven communication.

The goal of this project is to demonstrate practical backend development skills, including REST API design, database integration, security, transaction management, and asynchronous event processing.

## 🚀 Features

### 🔐 Authentication and Authorization

* User registration and login.
* Spring Security integration.
* JWT-based authentication.
* Password hashing using BCrypt.
* Role-based access control.
* Request validation and centralized exception handling.

### 📦 Product Management

* Create, retrieve, update, and delete products.
* Retrieve products using pagination and sorting.
* Search and filter products.
* Validate product-related requests.

### 🛍️ Shopping Cart

* Add products to the shopping cart.
* Update cart item quantities.
* Retrieve cart information.
* Remove or manage cart items.

### 📋 Order Management

* Place orders using cart items.
* Validate product stock before completing an order.
* Calculate order totals using product prices.
* Store order and order-item information.
* Maintain order status.
* Retrieve order information.
* Handle insufficient stock and other order-related errors.
* Use transaction management to maintain data consistency during order processing.

### ⚡ Redis Caching

* Integrate Redis with Spring Boot.
* Configure JSON serialization for cached values.
* Cache eligible application data to reduce repeated database access.

### 📨 Kafka Event Processing

* Publish order-created events.
* Process events using a Kafka consumer.
* Use Spring application events and an after-commit listener to trigger event processing after a successful database transaction.

### 🧪 Testing

* Unit tests using JUnit 5 and Mockito.
* Integration tests for application functionality.
* Tests covering product operations and order processing.
* Validation of order behavior when stock is insufficient.

---

## 🛠️ Technology Stack

| Technology      | Purpose                                    |
| --------------- | ------------------------------------------ |
| Java 21         | Programming language                       |
| Spring Boot     | Application framework                      |
| Spring Web      | REST API development                       |
| Spring Security | Authentication and authorization           |
| JWT             | Token-based authentication                 |
| Spring Data JPA | Database access                            |
| Hibernate       | ORM implementation                         |
| MySQL           | Relational database                        |
| Redis           | Caching                                    |
| Apache Kafka    | Event-driven communication                 |
| Maven           | Dependency management and build automation |
| JUnit 5         | Testing                                    |
| Mockito         | Mocking dependencies in unit tests         |
| Postman         | API testing                                |
| Git and GitHub  | Version control and source-code hosting    |

---

## 🏗️ Project Architecture

The application follows a layered architecture to separate HTTP handling, business logic, persistence, and security responsibilities.

```text
                   Client / Postman
                          |
                          v
                    REST Controllers
                          |
                          v
                      Services
                          |
              +-----------+-----------+
              |                       |
              v                       v
         Repositories           Redis Cache
              |
              v
            MySQL

       Security Layer
              |
              v
     JWT Authentication Filter

       Order Processing
              |
              v
     Database Transaction
              |
              v
       After-Commit Event
              |
              v
         Kafka Producer
              |
              v
         Kafka Topic
              |
              v
         Kafka Consumer
```

**Note:** This diagram illustrates the main architectural components and their intended relationships. Redis and Kafka are infrastructure components that must be configured and running for the corresponding features to work.

---

## 🔑 Authentication Flow

The application uses Spring Security and JWT to authenticate users.

```text
User
 |
 | POST /auth/login
 v
AuthController
 |
 v
UserService
 |
 v
AuthenticationManager
 |
 v
AuthenticationProvider
 |
 v
CustomUserDetailsService
 |
 v
UserRepository
 |
 v
MySQL
 |
 v
BCrypt Password Verification
 |
 v
Successful Authentication
 |
 v
JWT Generation
 |
 v
JWT Returned to Client
```

For protected endpoints, the client sends the token in the HTTP authorization header:

```http
Authorization: Bearer <your-jwt-token>
```

The JWT authentication filter validates the token and establishes the authenticated user's security context.

### Security Practices

* Passwords are hashed rather than stored as plaintext.
* Authentication uses signed JWTs.
* Protected endpoints are secured through Spring Security.
* Sensitive configuration values are supplied through environment variables.

---

## 📡 API Overview

The following table summarizes the main API areas implemented in the project.

| Module         | Endpoint         | Method | Purpose                  |
| -------------- | ---------------- | ------ | ------------------------ |
| Authentication | `/auth/register` | POST   | Register a user          |
| Authentication | `/auth/login`    | POST   | Authenticate a user      |
| Products       | `/products`      | GET    | Retrieve products        |
| Products       | `/products`      | POST   | Create a product         |
| Products       | `/products/{id}` | GET    | Retrieve a product by ID |
| Products       | `/products/{id}` | PUT    | Update a product         |
| Products       | `/products/{id}` | DELETE | Delete a product         |

The exact product endpoint paths, request parameters, authorization rules, and response formats should be verified against the current controller implementations.

**Cart and order APIs:** The project also includes `CartController` and `OrderController`. Refer to these controllers for the exact routes and request formats.

### Example: User Registration

**Request**

```http
POST /auth/register
Content-Type: application/json
```

Example request body (adjust the fields to match `RegisterRequest`):

```json
{
  "username": "demo_user",
  "password": "StrongPassword123"
}
```

### Example: User Login

**Request**

```http
POST /auth/login
Content-Type: application/json
```

Example request body (adjust the fields to match `LoginRequest`):

```json
{
  "username": "demo_user",
  "password": "StrongPassword123"
}
```

Use the JWT returned by the login endpoint when calling protected APIs.

---

## ⚙️ Prerequisites

Before running the application, install or configure:

* Java Development Kit (JDK) 21
* MySQL
* Redis
* Apache Kafka
* Git

Maven is available through the project's Maven Wrapper, so a separate Maven installation is not required.

---

## 💻 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/devikrishnaaddanki/spring-boot-ecommerce-backend.git
```

Navigate to the project directory:

```bash
cd spring-boot-ecommerce-backend
```

### 2. Configure MySQL

Create the application database:

```sql
CREATE DATABASE jwt_security;
```

Configure a MySQL user with the necessary database permissions.

The application uses environment variables for database credentials.

### 3. Configure Environment Variables

The application expects these environment variables:

| Variable      | Description              |
| ------------- | ------------------------ |
| `DB_USERNAME` | MySQL username           |
| `DB_PASSWORD` | MySQL password           |
| `JWT_SECRET`  | Secret used to sign JWTs |

In PowerShell, set them for the current terminal session:

```powershell
$env:DB_USERNAME = "your_mysql_username"
$env:DB_PASSWORD = "your_mysql_password"
$env:JWT_SECRET = "your_long_random_secret"
```

Replace the example values with your local configuration.

**Security:** Never commit actual passwords, production JWT secrets, or other credentials to GitHub. Use a sufficiently long, randomly generated JWT secret compatible with the signing algorithm configured in the application.

### 4. Start Redis

Start your Redis server using your preferred local installation or container setup.

The current application configuration expects Redis at:

```text
localhost:6379
```

### 5. Start Apache Kafka

Start Kafka using your local installation or contai