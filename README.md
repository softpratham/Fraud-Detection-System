# Banking Fraud Detection System

A high-performance, rule-based fraud detection engine built with Core Java. This system processes financial transactions in batch or interactive modes, evaluates them against a configurable set of risk rules, and generates detailed fraud alerts and audit reports.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation and Setup](#installation-and-setup)
- [Usage](#usage)
- [Testing](#testing)
- [Author](#author)

## Overview

The Fraud Detection System is designed to identify suspicious financial activities by analyzing transaction data against a dynamic set of business rules. It supports data ingestion from CSV sources, persists results to a relational database, and provides a Command Line Interface (CLI) for analysts to generate reports in PDF, CSV, and JSON formats.

## Architecture

The application follows a modular Layered Architecture, leveraging standard software design patterns to ensure maintainability and scalability:

* **Strategy Pattern:** Implemented in the `com.fraud.rules` package. Each fraud check (e.g., `HighAmountRule`, `GeoLocationRule`) is encapsulated as an independent strategy, allowing the system to be extended with new rules without modifying the core logic.
* **Factory Pattern:** The `RuleFactory` class dynamically instantiates and configures rule objects based on external JSON configuration files.
* **DAO Pattern:** The `com.fraud.dao` package abstracts raw JDBC SQL operations, decoupling business logic from database implementation details.
* **Service Layer:** Orchestrates the flow of data between the Data Access Objects (DAOs) and the Rule Engine.

## Key Features

### Rule Engine
The core engine evaluates transactions using weighted rules defined in configuration:
* **High Amount Check:** Flags transactions that exceed a specific monetary threshold.
* **Geo-Location Risk:** Identifies transactions originating from high-risk countries.
* **Night-Time Activity:** Flags transactions occurring outside standard business hours (configured default: 00:00 - 05:00).
* **Risky Merchants:** Checks transaction merchants against a blacklist of suspicious categories.
* **Channel Risk:** Assigns risk weights based on the transaction medium (e.g., Online, ATM).
* **Velocity Checks:** Detects high-frequency transactions within a short time window.

### Reporting
* **Multi-Format Export:** Generates audit reports in PDF (iText), CSV (Apache Commons), and JSON (Jackson).
* **Analyst Console:** An interactive CLI tool for querying alerts, viewing monthly summaries, and filtering data by risk level.

## Technology Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Java 11+ (Core Java) |
| **Build Tool** | Apache Maven |
| **Database** | MySQL 8.0 |
| **Data Access** | JDBC, HikariCP (Connection Pooling) |
| **Reporting** | iText 7 (PDF), Apache Commons CSV, Jackson (JSON) |
| **Testing** | JUnit 5, Mockito |
| **Logging** | SLF4J, Logback |

## Project Structure

```text
src/main/java/com/fraud
├── app           # Application entry points (Main.java, FraudAnalystConsole.java)
├── config        # Configuration loaders for Properties and JSON rules
├── dao           # Data Access Objects for database interactions
├── engine        # Core fraud evaluation engine and RuleFactory
├── model         # Domain entities (Transaction, FraudAlert)
├── rules         # Business Logic (Rule interface and implementations)
├── service       # Service Layer (DetectionService, ReportService)
└── util          # Utilities (CSV Parsing, Database Connections)
```
## Prerequisites

- Java Development Kit (JDK) 11 or higher  
- Apache Maven 3.6 or higher  
- MySQL Server 8.0  

---

## Installation and Setup

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/fraud-detection-system.git
cd fraud-detection-system
```

### 2. Initialize the Database

Log in to your MySQL instance and run the initialization script located at `src/main/resources/schema.sql`.

```sql
CREATE DATABASE frauddb;
USE frauddb;
SOURCE src/main/resources/schema.sql;
```

### 3. Configure the Application

Navigate to `src/main/resources/` and create a file named `application.properties`. You can copy the example file or use the template below:

```properties

# Database Configuration
db.url=jdbc:mysql://localhost:3306/frauddb
db.user=YOUR_DB_USER
db.password=YOUR_DB_PASSWORD

# Application Settings
thread.pool.size=8
report.output=alerts_report.csv

# Fraud Detection Thresholds
high_amount_threshold=50000
risk.score.high=60
risk.score.medium=30
velocity.window.seconds=120
velocity.limit=3
```

### 4. Build the Project

```bash
mvn clean package
```

## Usage

The application can be run in two modes: **Interactive Console** or **Non-Interactive CLI**.

### Interactive Console

To launch the interactive analyst dashboard:

```bash
mvn exec:java -Dexec.mainClass="com.fraud.app.Main"
```

**Menu Options:**

- **Run Detection Pipeline**: Processes the `transactions.csv` file and saves alerts to the database.
- **Monthly Summary Reports**: Displays analytics of fraud trends.
- **Export Alerts**: Generates reports in the selected format (CSV, PDF, JSON).
- **System Status**: Checks database connectivity and configuration.

### Non-Interactive CLI

You can execute specific tasks directly from the command line using arguments.

**Run the detection pipeline:**

```bash
mvn exec:java -Dexec.mainClass="com.fraud.app.Main" -Dexec.args="run-detection"
```

**Export a PDF report for a specific account:**

```bash
mvn exec:java -Dexec.mainClass="com.fraud.app.Main" -Dexec.args="export-pdf acct123 report.pdf"
```

**Test Database Connection:**

```bash
mvn exec:java -Dexec.mainClass="com.fraud.app.Main" -Dexec.args="db-test"
```

## Testing

The project includes unit tests for the Rule Engine and Service layer. Tests are written using JUnit 5 and Mockito.

- **Rule Tests**: Verify individual rules (e.g., `HighAmountRuleTest`) ensuring thresholds trigger correctly.
- **Service Tests**: Verify the orchestration logic (`DetectionServiceTest`) using mocks to isolate business logic from the database.

To run the test suite:

```bash
mvn test
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/NewFeature`).
3. Commit your changes.
4. Push to the branch and open a Pull Request.


## Author
**Prathmesh Deokar**
* **GitHub:** [github.com/softpratham](https://github.com/softpratham)
* **LinkedIn:** [linkedin.com/in/prathmeshdeokar37](https://www.linkedin.com/in/prathmeshdeokar37)
