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

## Important notes
