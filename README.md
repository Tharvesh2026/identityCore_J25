# i.Core — Cloud Identity & Access Management Portal

Welcome to **i.Core v2.0 (Spring Boot Edition)**. This project is a complete architectural modernization of the original servlet-based `i.Core` identity platform, rebuilt from the ground up on **Spring Boot 3.3.5** and **Java 21/25** with **Spring Data JPA**, **Spring Security**, **H2 Database**, and **Thymeleaf**.

---

## 🚀 Key Features

* **Spring Security & RBAC:** Secure authentication, cookie-based CSRF protection, and path/API authorization gated by dynamic, database-resolved user permissions.
* **Modern MVC Architecture:** Clean division of concerns using Spring Boot MVC Controllers, Service layers, and repositories instead of raw Servlet/JDBC DAO classes.
* **Thymeleaf Template Engine:** Elegant server-side rendering with reusable fragments, dynamic layouts, and built-in Spring Security integration.
* **Automatic Database Seeding:** Clean, in-memory H2 Database configured to automatically generate schemas and populate initial permissions, roles, and a default superadmin user on startup.
* **Session Expiry Countdown:** Real-time visual countdown timer matching servlet session timeout, resetting automatically on user activity via background AJAX calls.
* **Aesthetic Dashboard UI:** Curated Inter/JetBrains typography, rich shadows, and visual feedback cards displaying real-time session metadata and security status.

---

## 🛠️ Technology Stack

| Component | v1.2.0-beta (Legacy) | v2.0 (Spring Boot Edition) |
| :--- | :--- | :--- |
| **Framework** | Jakarta Servlet API | Spring Boot 3.3.5 |
| **View Engine** | JSP & JSTL | Thymeleaf & Security Extras |
| **ORM / Database** | Raw JDBC (Manual DAOs) | Spring Data JPA (Hibernate) |
| **Database** | MySQL | In-Memory H2 Database |
| **Security** | Custom Filter (`AuthFilter`) | Spring Security 6.x |
| **Logging** | Log4j2 | SLF4J + Logback (File output) |
| **Build Tool** | Maven | Maven Wrapper (`mvnw`) |

---

## ⚡ Quick Start

### Prerequisites
* Java 21 or Java 25 installed
* Maven (or use the provided Maven Wrapper `mvnw`)

### Running the Application Locally
1. Clone the repository and navigate to the project directory:
   ```bash
   cd d:/iCore-Projects/identityCore_J25
   ```
2. Start the application using the Maven Wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Open your browser and navigate to:
   * **Application:** [http://localhost:8080/login](http://localhost:8080/login)
   * **H2 Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:icore`, Username: `sa`, Password: *blank*)

### Default Credentials (Seeded Admin)
* A default administrator account is automatically created during startup for local development.

* Refer to **data.sql** for the seeded credentials.

---

## 📂 Project Structure

```
identityCore_J25
├── src/main/java/dev/tharbytes/identityCore/
│   ├── config/            # Spring Security and MVC Configurations
│   ├── controller/        # Web MVC Controllers (welcome, settings, profile, users, roles)
│   │   ├── advice/        # Controller advices for global layout properties
│   │   └── api/           # REST Controllers for /user/* and /auth/* API clients
│   ├── dto/               # Data Transfer Objects (requests, responses)
│   ├── entity/            # JPA Entities (UserEntity, RoleEntity, PermissionEntity)
│   ├── exception/         # Custom Exceptions & Global REST Exception Handler
│   ├── repository/        # Spring Data JPA Repositories
│   ├── security/          # Spring Security Core (UserDetails Service, Auth Helper)
│   ├── service/           # Service layer containing main business logic
│   └── util/              # Password hashing and input validation utilities
└── src/main/resources/
    ├── static/            # Static assets (icore.css)
    ├── templates/         # Thymeleaf view templates
    ├── data.sql           # Database seeding SQL script
    └── application.properties # Main application properties
```

---

## 🔒 Security Practices

1. **Output Encoding:** All dynamic values are rendered securely via Thymeleaf (`th:text`), preventing cross-site scripting (XSS) injection.
2. **CSRF Validation:** Cookie-based CSRF protection is active on all non-REST endpoints, with the Thymeleaf layout automatically injecting CSRF tokens into forms.
3. **Password Salting:** Passwords are hashed using BCrypt with a strength factor of 12.
