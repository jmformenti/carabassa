# Carabassa — Agent Instructions

## Project
Application with a Spring Boot backend and CLI, and Vue 3 frontend.

## Build the project

```bash
mvn clean install
```

## Run application

### Run the backend
```bash
cd backend/server/boot
export CARABASSA_REPO_DIR=/tmp/carabassa && mvn spring-boot:run
```
Starts at http://localhost:8080 and supports hot reloading

### Run the frontend
```bash
cd frontend
yarn install
yarn dev
```
Starts at http://localhost:3000 and supports hot reloading

### Run the cli
```bash
cd cli
mvn spring-boot:run
```

## Run the tests

### Run the backend tests
```bash
cd backend/server/boot && mvn test
```

### Run the frontend tests
```bash
cd frontend && yarn test
```

### Run the cli tests
```bash
cd cli && mvn test
```

### Run all the tests
```bash
mvn test
```

## Prepare minimal environment for manual testing

```bash
./run-dev.sh
```

## Important notes
