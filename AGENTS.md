# Carabassa — Agent Instructions

## Project
Application with a Spring Boot backend, CLI, and Vue 3 frontend.

## Build the project

```bash
mvn clean install
```

## Run application

### Run the main application

Runs the backend, frontend, and creates a `test` dataset for testing.

```bash
./run-dev.sh
```

To restart and reset the dataset:

```bash
./run-dev.sh --reset
```

To stop the services:

```bash
./run-dev.sh --stop
```

### Run the cli
```bash
cd cli
mvn spring-boot:run
```

## Run the tests

### Run the backend tests
```bash
cd backend && mvn test
```

### Run the frontend tests
```bash
cd frontend && yarn test
```

### Run the cli tests
```bash
cd cli && mvn test
```

## MCP Server

The `mcp-server/` directory contains an MCP (Model Context Protocol) server that exposes the Carabassa REST API as tools for AI agents.

- **Capabilities:** query datasets, list/search/get items and tags, upload files, manage tag metadata.
- **Interfaces:** stdio transport (MCP protocol); configured via `CARABASSA_BASE_URL` and `CARABASSA_WRITE_ENABLED` environment variables.
- **Responsibilities:** read-only by default (12 tools); write operations (create/update/delete) require `CARABASSA_WRITE_ENABLED=true`.

## Important notes

- When the REST API changes (new endpoints, modified request/response fields, removed operations), update both:
  - `mcp-server/carabassa_mcp/server.py` — MCP tools that wrap the API
  - `cli/` — the Carabassa CLI client
