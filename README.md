# 🌾 Smart Agriculture Management System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)
![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white)

## 📖 Project Overview

**Smart Agriculture Management System** is a robust backend solution engineered to empower farmers with data-driven decision-making. By leveraging advanced software patterns and automated systems, it facilitates comprehensive crop lifecycle management, granular expense tracking, and intelligent agricultural advisories based on environmental factors and pre-defined agronomic rules.

## ✨ Key Features

- **Robust Security & Role-Based Access Control**: Secure endpoints using JWT authentication, distinguishing access levels between `Admin` and `Farmer` roles.
- **Comprehensive Crop Management**: Full CRUD operations for managing crop lifecycles, from planting to harvesting.
- **Granular Expense Tracking**: Real-time financial monitoring capabilities tied to specific crops and farming activities.
- **Intelligent Rule-Based Advisory Engine**: Automated agricultural suggestions utilizing predefined agronomic rules and environmental data parameters.
- **Automated Irrigation Scheduler**: Scheduled background tasks (`@Scheduled`) to trigger irrigation alerts or mechanisms based on temporal or environmental conditions.
- **Extensible Weather API Integration** *(Optional)*: Capable of incorporating live meteorological data for dynamic advisory formulation.

## 🏗️ System Architecture

The application is designed following a rigorous **N-Tier Architecture**, ensuring clear separation of concerns, high maintainability, and scalability.

- **Controller Layer (API Interface)**: Intercepts incoming HTTP requests, orchestrates validation, and routes to appropriate business logic services.
- **Service Layer (Business Logic)**: Encapsulates core business rules, transaction management (`@Transactional`), and cross-service communication.
- **Repository Layer (Data Access)**: Utilizes Spring Data JPA interfaces for abstracted, boilerplate-free database operations.
- **Security Filter Chain**: Intercepts requests to validate JWT tokens and enforce authorization policies prior to controller execution.

```mermaid
graph TD
    Client[Client App / Postman] --> API[REST Controllers]
    API --> Security[Spring Security / JWT Filter]
    Security --> Services[Business Services]
    Services --> Repositories[JPA Repositories]
    Repositories --> DB[(MySQL Database)]
```

## 🗄️ Database Schema Overview

The relational database schema is normalized to ensure data integrity and query efficiency.

- **Users**: Manages authentication credentials and role assignments (`Farmer`, `Admin`).
- **Crops**: Stores crop metadata (type, planting date, expected harvest). Relates to `Users`.
- **Expenses**: Financial transaction logs associated with specific `Crops`.
- **Advisories**: System-generated or Admin-created agricultural guidelines.
- **IrrigationSchedules**: Configured scheduling constraints for crop hydration.

*(Detailed ERD diagrams can be generated dynamically via JPA tooling).*

## 🔌 API Documentation

Comprehensive API documentation and interactive testing are provided via **Swagger UI (OpenAPI 3.0)**.

Once the application is running, access the documentation interface at:
`http://localhost:8080/swagger-ui.html`

### Sample Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/login` | Authenticate user and receive JWT | Public |
| `GET` | `/api/crops` | Retrieve all crops for the authenticated farmer | Farmer/Admin |
| `POST` | `/api/crops` | Register a new crop profile | Farmer/Admin |
| `POST` | `/api/expenses` | Log a new expense against a specific crop | Farmer |
| `GET` | `/api/advisories/active` | Retrieve current rule-based advisories | Farmer |

## 🛠️ Installation & Setup Instructions

### Prerequisites
- **Java Development Kit (JDK) 17** or higher
- **Maven 3.8+**
- **MySQL Server 8.0+**
- **Git**

### Step-by-Step Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/Smart-Agriculture-Management-System.git
   cd Smart-Agriculture-Management-System
   ```

2. **Database Configuration:**
   Create a new MySQL database named `smart_agriculture`.
   ```sql
   CREATE DATABASE smart_agriculture;
   ```

3. **Configure Environment Variables:**
   Create an `application-dev.yml` (or update `application.properties`) in the `src/main/resources` directory.

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/smart_agriculture?useSSL=false&serverTimezone=UTC
       username: ${DB_USERNAME}
       password: ${DB_PASSWORD}
     jpa:
       hibernate:
         ddl-auto: update
       show-sql: true

   jwt:
     secret: ${JWT_SECRET_KEY}
     expiration: 86400000 # 24 hours in ms
   ```

## 🚀 Running Locally

Ensure your MySQL server is running and your environment variables are set.

Using Maven, execute the Spring Boot application:

```bash
mvn clean install
mvn spring-boot:run
```

Alternatively, package the application and run the JAR:
```bash
mvn clean package
java -jar target/smart-agriculture-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`.

## ⚙️ Environment Variables

For security best practices, avoid hardcoding sensitive data. Provide the following environment variables when running the application:

- `DB_USERNAME`: Database user (e.g., `root`)
- `DB_PASSWORD`: Database password
- `JWT_SECRET_KEY`: Base64 encoded secret key for signing JWT tokens (Minimum 256-bit).

## 🔮 Future Enhancements

The architectural foundation supports rapid integration of advanced capabilities:

- [ ] **AI/ML Crop Yield Prediction**: Integrate Python-based ML models via microservices to forecast yields based on historical data.
- [ ] **Mobile Application Interface**: Develop a React Native or Flutter client for field-ready access.
- [ ] **IoT Sensor Integration**: Ingest real-time soil moisture and NPK sensor data directly into the advisory engine.
- [ ] **Advanced Spatial Data**: Integrate GIS mappings for farm plot visualization.

## 🤝 Contribution Guidelines

We welcome contributions from the community!

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes utilizing conventional commits (`git commit -m 'feat: Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request detailing the proposed changes and ensuring all tests pass.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.