# ReceptAI

ReceptAI is an AI-powered receipt processing and smart basket optimization platform built with Spring Boot. The application extracts structured purchase data from receipt images, stores the results in PostgreSQL, and uses semantic product matching to estimate the lowest possible shopping basket cost across multiple stores.

## Why This Project

Most expense tracking apps stop at storing totals. ReceptAI goes further:

- it digitizes receipts into structured product-level data
- it keeps historical price information per store
- it compares semantically similar products across receipts
- it estimates the cheapest basket either in a single store or across multiple stores

This makes the project useful both as an expense tracker and as a price intelligence tool.

## Core Features

- Receipt upload and AI-based extraction of store name, date, total, and purchased items
- Persistent storage for receipts, expenses, users, and historical pricing data
- Secure login and registration flow with Spring Security
- Web interface for scanning receipts, browsing history, and calculating optimized baskets
- Smart basket engine with single-store and mixed-store strategies
- Semantic search pipeline backed by external embedding and vector search services
- Scheduled master index rebuild for keeping the semantic search dataset in sync

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- Thymeleaf
- PostgreSQL
- Tess4J / Tesseract OCR
- Google Gemini API
- Docker Compose
- H2 for test isolation

## High-Level Flow

```mermaid
flowchart LR
    A["Receipt Image Upload"] --> B["Spring Boot Backend"]
    B --> C["Gemini Vision Parsing"]
    C --> D["Structured Receipt Data"]
    D --> E["PostgreSQL Storage"]
    D --> F["Embedding Service"]
    F --> G["C++ Vector Search Engine"]
    E --> H["Smart Basket Logic"]
    G --> H
    H --> I["Cheapest Basket Recommendation"]
```

## Architecture

### Main Components

- `ReceiptService`
  Handles receipt upload, persistence, and semantic indexing of extracted products.

- `GeminiParser`
  Sends the uploaded receipt image to Gemini Vision and maps the response into Java DTOs.

- `VectorSearchService`
  Integrates with the external Python embedding service and the C++ vector index service.

- `SmartBasketService`
  Runs the price comparison logic and combines semantic similarity with text-based reranking.

- `DataSyncScheduler`
  Rebuilds the semantic master index from recent database data on startup and on schedule.

### User-Facing Pages

- `/register`
- `/login`
- `/scan`
- `/history`
- `/smart-basket`

## Project Structure

```text
src/main/java/com/receiptai
|- config
|- controller
|- dto
|- exception
|- model
|- repository
|- scheduler
|- security
`- service
```

## Local Setup

### Prerequisites

- Java 17
- Docker
- Maven Wrapper support
- Optional: Gemini API key
- Optional: local Python embedding service
- Optional: local C++ vector search engine

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Configure Environment Variables

The application reads runtime configuration from environment variables. Example values are documented in `src/main/resources/application-example.properties`.

Minimum setup:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/receiptai
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export GEMINI_API_KEY=your_gemini_api_key
```

Optional service endpoints:

```bash
export EMBED_SINGLE_URL=http://127.0.0.1:8000/embed
export EMBED_BATCH_URL=http://127.0.0.1:8000/embed_batch
export VECTOR_SEARCH_URL=http://127.0.0.1:8081/search
export VECTOR_ADD_URL=http://127.0.0.1:8081/add_vector
export VECTOR_REBUILD_URL=http://127.0.0.1:8081/admin/rebuild_master
export APP_SYNC_ENABLED=true
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The app will start on `http://localhost:8080`.

## Testing

Tests are isolated from local infrastructure:

- in-memory H2 database
- mocked AI parsing
- mocked vector indexing dependencies

Run the test suite with:

```bash
./mvnw test
```

## Notes on External Services

Some features depend on services that are not part of this repository:

- Gemini API is required for real receipt parsing at runtime
- the Python embedding service is required for semantic vector generation
- the C++ vector engine is required for semantic product search and smart basket matching

Without these integrations, the core Spring Boot app still runs, but semantic recommendation features will be limited.

## What Makes This Project CV-Relevant

- It solves a concrete problem, not just a demo CRUD flow
- It combines backend engineering, AI integration, OCR, search, and business logic
- It includes authentication, persistence, scheduling, external service orchestration, and automated tests
- It demonstrates product thinking through the smart basket optimization use case

## Future Improvements

- Replace hardcoded UI styling with a dedicated frontend design system
- Add API documentation with Swagger / OpenAPI
- Introduce Testcontainers for closer PostgreSQL parity in integration tests
- Add retry / timeout policies for external service calls
- Add metrics and structured observability for AI and vector search latency
