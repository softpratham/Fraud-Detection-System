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
- [Configuration](#configuration)
- [Usage](#usage)
- [Testing](#testing)
- [License](#license)

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

* **Language:** Java 11+ (Core Java)
* **Build Tool:** Apache Maven
* **Database:** MySQL 8.0
* **Data Access:** JDBC with HikariCP (Connection Pooling)
* **JSON Processing:** Jackson
* **CSV Processing:** Apache Commons CSV
* **PDF Generation:** iText 7
* **Logging:** SLF4J with Logback
* **Testing:** JUnit 5, Mockito

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
