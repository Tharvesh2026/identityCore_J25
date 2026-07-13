# Changelog

All notable changes to the **i.Core** project will be documented in this file.

---

## [v2.0] — Spring Boot Architecture Modernization
**Release Date:** July 2026  
**Release Type:** Major Upgrade (Stable)

This release represents a complete architectural rewrite of the legacy servlet-based implementation, transitioning the codebase into a robust, secure, and modern **Spring Boot** application.

### Rebuilt & Modernized
* **Platform Framework:** Replaced legacy Jakarta Servlet API and custom filters with **Spring Boot 3.3.5** and **Spring MVC Controllers**.
* **Database & Persistence:** Migrated database interactions from raw JDBC (`UserDAO`, `RoleDAO`, `PermissionDAO`) to **Spring Data JPA** with Hibernate ORM, introducing `UserEntity`, `RoleEntity`, and `PermissionEntity` mapped mappings.
* **View Engine:** Migrated all template code from legacy `.jsp` and `.jspf` files (with custom taglibs and JSTL `<c:out>`) to modern, componentized **Thymeleaf HTML templates**.
* **Security Layer:** Ported custom filter-based authentication and manual session tracking to an industry-standard **Spring Security 6.x** implementation, featuring form-based login, cookie-based CSRF protection, secure session management, and `AppUserDetailsService` authority mapping.
* **In-Memory Database Migration:** Moved database target from standalone MySQL to an in-memory **H2 Database**, allowing the application to run instantly without external database dependencies.
* **Auto-Seeding & Init:** Configured Spring SQL initialization (`data.sql`) to automatically seed default roles, permissions, role-permission associations, and a default system administrator account.
* **Layout Attributes Injection:** Implemented `@ControllerAdvice` (`LayoutAdvice`) to expose common layout variables (such as `currentUri` for rendering active sidebar states) dynamically across all Thymeleaf templates.

### Fixed & Optimized
* **XSS Mitigation:** Upgraded user-input rendering from custom JSTL filters to native Thymeleaf HTML-encoding (`th:text`), completely preventing stored XSS exploits.
* **Lombok Compiler Dependency Removal:** Replaced Lombok annotation-driven models and request/response DTOs with standard POJOs containing explicit getters, setters, constructors, and builders, eliminating class compilation issues under Java 21/25.
* **Unified Application Properties:** Consolidating application settings into a single `application.properties` configuration file.

---

## [v1.2.0-beta] — Security Hardening & RBAC Completion
**Release Date:** June 2026  
**Release Type:** Beta Release

Focuses on security hardening, role management completion, profile management capabilities, and overall platform stabilization.

### Added
* **Profile Management:**
  * Enabled user-facing profile views, updates (name, username, email), and secure password changing.
  * Added session-based profile access to isolate user scopes.
* **Role Management:**
  * Added custom role creation, status management (ACTIVE/INACTIVE), and dynamic role-permission assignment.
  * Implemented permission-based sidebar navigation rendering.
* **Permission-Based Authorization:**
  * Dynamic validation of role-permissions on every request.
  * Route and API-level authorization guards.

### Fixed & Improved
* **Stored XSS Fixes:** Fixed vulnerability vectors in name and username input fields using `ValidationDAOUtil` for filtering HTML tags, and encoding all JSP outputs using JSTL `c:out`.
* **API Security:** Removed exposure of password hashes in JSON responses.
* **Session Security:** Hardened authentication and authorization flow handling, CSRF protections, and session invalidation rules.
* **Auditing & Logging:** Enhanced Client IP resolution logic, log tracing under reverse proxies, and structured authentication audit logging.

### Legacy Technology Stack
* Java 25, Jakarta Servlet API, JSP/JSTL, MySQL, Log4j2, Apache Tomcat 11.
