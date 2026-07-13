# Changelog

All notable changes to the **i.Core** project will be documented in this file.

---

## [v2.0] — PostgreSQL Migration & Stability Patch
**Release Date:** July 2026
**Release Type:** Minor Release (Stable)

Follow-up stabilization release addressing template-engine regressions, entity lifecycle bugs, and a production-database migration from H2 to **PostgreSQL (Supabase)**.

### Changed
* **Database Backend:** Migrated primary datastore from in-memory H2 to persistent **PostgreSQL** via Supabase, introducing profile-based configuration (`application-postgres.properties`) alongside the existing H2 profile for local development.
* **SQL Initialization Scope:** Restricted `spring.sql.init.mode` to `embedded`, so `data.sql` seeding now runs only against H2; PostgreSQL environments rely exclusively on the idempotent `SeedService.seed()` logic, preventing duplicate-key failures on application restart.

### Fixed
* **Entity `equals()`/`hashCode()` Collection Bug:** Removed lazy-loaded collection and association fields from Lombok-generated `equals()`/`hashCode()` on `Roles` and `Permissions`, resolving a `ConcurrentModificationException` triggered by re-entrant Hibernate collection loading during authentication and seeding.
* **Immutable Collection Assignment:** Replaced `Set.of(...)` / `List.of(...)` assignments to managed JPA collections in `SeedService` with mutable `HashSet`/`ArrayList` wrappers, resolving an `UnsupportedOperationException` during entity merge.
* **REST Login Endpoint:** Replaced reliance on Spring's default form-login POST handling with a dedicated JSON-based `AuthController`, resolving a 403 Forbidden response when authenticating via JSON payloads (e.g. Swagger/API clients).
* **Thymeleaf Implicit Object Removal:** Replaced deprecated `#session`/`#request` expression-object references (removed in Thymeleaf 3.1+) with explicit model attributes (`sessionTimeout`) across `welcome.html`, resolving template parsing failures.
* **Missing Navbar Model Attribute:** Added the `user` model attribute to the `/users` and `/roles` controllers, resolving an `EL1007E` null-property error in the shared `navbar` fragment.
* **Session Countdown Script:** Fixed undeclared-variable and temporal-dead-zone (`ReferenceError`) bugs in the session-timeout countdown script; subsequently removed the active countdown/refresh-session script from `welcome.html` after identifying it as a cause of unintended session invalidation.
* **Restricted SpEL Type Resolution:** Replaced `new String[]{...}` array-construction expressions in `users.html` with `#strings.arraySplit(...)`, resolving an `EL1005E: Type cannot be found` error caused by Thymeleaf's sandboxed SpEL type locator.

---

## [v2.0-beta] — Spring Boot Architecture Modernization
**Release Date:** July 2026  
**Release Type:** Major Upgrade

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
