# OrderFlow - Order Management System

OrderFlow is a secure, high-performance Order Management System designed to streamline inventory tracking, order lifecycles, and customer transactions. 

## 🔗 Live Application
The project is deployed and live at:
👉 **[https://orderfloworderflow-app.onrender.com](https://orderfloworderflow-app.onrender.com)**

---

## 🌟 Key Features

- **Role-Based Access Control (RBAC):**
  - **Admin:** Full system control, role assignments, and analytics.
  - **Staff:** Inventory administration, stock adjustment, and order status management.
  - **Customer:** Product browsing, cart management, and order placement.
- **Order Lifecycle Management:** Strict state-transition validations (Pending → Processing → Shipped → Delivered / Cancelled) with transactional database operations.
- **Inventory Tracking:** Real-time stock adjustment with automatic check on product availability during checkout.
- **Analytics Dashboard:** Interactive dashboard presenting real-time business metrics and charts (e.g., top-selling products, order status distribution) powered by Chart.js.
- **Invoice Generation:** Automatic PDF invoice generation for completed orders using OpenPDF.
- **Secure JWT Authentication:** Stateless security filter utilizing secure signature signing.

---

## 🛠️ Technology Stack

- **Backend:** Java 21, Spring Boot 3.4.1, Spring Data JPA, Spring Security (JWT)
- **Frontend:** Semantic HTML5, Vanilla CSS, Vanilla JavaScript (Modern ES6), Chart.js, FontAwesome Icons
- **Database:** 
  - Production: MySQL (configured for high durability and optimized connection pools)
  - Development: H2 In-Memory Database (for quick local setups)
- **Containerization & DevOps:** Docker, Docker Compose, GitHub Actions (CI/CD pipeline with GitHub Container Registry)

---

## 🚀 Getting Started

### Prerequisites
* Java 21 (JDK)
* Maven 3.9+
* Docker & Docker Compose (optional, for containerized run)

### Running Locally (Development Mode)
By default, the application runs on port `8080` with an in-memory H2 database.
1. Clone the repository:
   ```bash
   git clone https://github.com/indrajitgupta1620-hub/orderflow.git
   cd orderflow
   ```
2. Build and run:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
3. Open `http://localhost:8080` in your browser.

### Running with Docker Compose (Production Mode)
1. Ensure your `.env` file is set up with standard credentials (e.g. `MYSQL_USER`, `MYSQL_PASSWORD`, `JWT_SECRET`).
2. Run docker compose:
   ```bash
   docker-compose up -d --build
   ```
3. The app service will wait for the MySQL service to be healthy, then start on `http://localhost:8080`.

---

## 🤖 CI/CD Pipeline
A GitHub Actions workflow is configured under `.github/workflows/ci-cd.yml` that:
1. Runs full Maven test suites on every pull request or branch push.
2. Builds and packages the production Docker image upon merging to `main`.
3. Pushes the containerized app image to GitHub Container Registry (GHCR).
