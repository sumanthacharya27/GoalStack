# GoalStack

GoalStack is a goal-management application that helps users break larger
goals into smaller, manageable subgoals, schedule those subgoals, and
track progress based on the effort assigned to each task.

The project uses a Spring Boot REST API for the backend and a React
application for the frontend.

## Features

### Goal and subgoal management

-   Create, view, update, and delete goals.
-   Break goals into smaller subgoals.
-   Track subgoal status (for example, Pending, In Progress, and
    Completed).
-   Assign estimated effort to subgoals.
-   Calculate goal progress using effort-based weighting.
-   Assign an optional target date to each subgoal.

### Authentication and ownership

-   User registration and login.
-   JWT-based authentication for protected API requests.
-   Users access their own goals and associated subgoals.

### Calendar

-   View scheduled subgoals in day, week, and month views.
-   Navigate between dates and return to Today.
-   Select a date to view its scheduled subgoals.
-   Complete or reopen a subgoal from the calendar.
-   Calendar entries are based on each subgoal's `targetDate`.

Subgoals without a target date are not displayed in the calendar.

### AI Goal Planner

The project includes an AI planning foundation and a planner interface.
The AI provider can be configured to use either the scripted `fake`
provider or the local Ollama provider.

The intended AI planning workflow is: 1. Describe a goal. 2. Answer
relevant follow-up questions. 3. Generate a structured goal and subgoal
draft. 4. Review and edit the draft. 5. Save the goal and subgoals to
GoalStack.

**AI implementation status:** The fake provider is suitable for
demonstrating the workflow but does not generate genuinely intelligent
responses. To use a real local language model, configure and verify
Ollama as described below. Conversational plan revision and any
additional AI capabilities should be considered complete only after they
have been implemented and tested in the current branch.

## Technology stack

### Backend

-   Java 21
-   Spring Boot
-   Spring MVC / REST APIs
-   Spring Security
-   JWT authentication
-   Spring Data JPA / Hibernate
-   MySQL
-   Maven
-   Flyway database migrations
-   JUnit 5 and Mockito for tests

### Frontend

-   React
-   Vite
-   React Router
-   JavaScript
-   CSS / Bootstrap (as used by the project)

### Local AI

-   Ollama
-   Configurable AI provider (`fake` or `ollama`)
-   Structured AI response parsing and validation

## Prerequisites

Install the following before running the application:

-   JDK 21
-   Maven
-   Node.js and npm
-   MySQL 8 (or a compatible MySQL version)
-   Git

For real local AI generation, also install: - Ollama - The model
configured for the application (the example below uses `llama3.1:8b`)

## Project structure

The repository is organized approximately as follows:

``` text
GoalStack/
├── frontend/
│   └── src/
│       ├── components/
│       ├── pages/
│       ├── api.js
│       └── utils.js
├── src/
│   ├── main/
│   │   ├── java/com/goalstack/
│   │   │   ├── ai/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── db/migration/
│   │       └── application.properties
│   └── test/
├── scripts/
├── docs/
├── pom.xml
└── README.md
```

Exact packages and files may vary slightly by branch.

## Database setup

1.  Start MySQL.
2.  Create a database for GoalStack:

``` sql
CREATE DATABASE goalstack;
```

3.  Configure the database connection in
    `src/main/resources/application.properties` or through the
    environment variables supported by your configuration.

Example connection properties:

``` properties
spring.datasource.url=jdbc:mysql://localhost:3306/goalstack
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

Replace the placeholders with your local MySQL credentials. Do not
commit real passwords or secrets to Git.

### Database migrations

The project uses Flyway for schema migrations. The calendar change adds
an optional `target_date` column to the `subgoals` table.

Allow Flyway to apply pending migrations when the application starts. Do
not manually add a column that an unapplied migration is already
responsible for creating.

To verify the column in MySQL:

``` sql
SHOW COLUMNS FROM subgoals LIKE 'target_date';
```

## Running the backend

Open a terminal in the repository root.

Run the tests:

``` powershell
mvn clean test
```

Start Spring Boot:

``` powershell
mvn spring-boot:run
```

The backend uses the port configured in the Spring Boot application
properties (commonly `8080`).

## Running the frontend

Open a second terminal:

``` powershell
cd frontend
npm install
npm run dev
```

Vite prints the local frontend URL in the terminal (commonly
`http://localhost:5173`).

For a production build:

``` powershell
npm run build
```

## Configuring Ollama

The application can use the fake provider by default. To use the local
Ollama provider, first make sure Ollama is installed and running.

Check installed models:

``` powershell
ollama list
```

Download the example model if it is not installed:

``` powershell
ollama pull llama3.1:8b
```

Check that Ollama is responding:

``` powershell
Invoke-RestMethod http://localhost:11434/api/tags
```

If Ollama is already running, do not start a second server. A
port-in-use error on `127.0.0.1:11434` usually means another Ollama
process is already listening.

Set the provider and model in the same PowerShell terminal used to start
Spring Boot:

``` powershell
$env:AI_PROVIDER = "ollama"
$env:OLLAMA_MODEL = "llama3.1:8b"
mvn spring-boot:run
```

To use the scripted provider instead:

``` powershell
$env:AI_PROVIDER = "fake"
mvn spring-boot:run
```

The project configuration may support additional Ollama settings. Check
`application.properties` and the AI configuration classes in the current
branch for the exact property names and defaults.

## AI planning API

The AI planning controller exposes these endpoints:

  -------------------------------------------------------------------------
  Method                  Endpoint                  Purpose
  ----------------------- ------------------------- -----------------------
  POST                    `/api/ai/chat`            Send conversation
                                                    messages to the planner
                                                    and receive an
                                                    assistant response.

  POST                    `/api/ai/generate-plan`   Generate a structured
                                                    goal and subgoal plan.
  -------------------------------------------------------------------------

These endpoints are protected according to the application's Spring
Security configuration. Include a valid JWT access token when calling
them.

Use the DTO definitions in `src/main/java/com/goalstack/dto/` as the
source of truth for request and response fields.

## Testing

Run all backend tests:

``` powershell
mvn clean test
```

Run the AI planning tests, if present in the current branch:

``` powershell
mvn test -Dtest=AIPlanningControllerTest,AIPlanningServiceTest
```

Run the frontend production build:

``` powershell
cd frontend
npm run build
```

If the repository contains an Ollama smoke-test script, run it from the
project root:

``` powershell
.\scripts\ollama-smoke-test.ps1
```

The smoke test requires the Ollama service and configured model to be
available.

## Suggested manual verification

-   Register a user and log in.
-   Create a goal and add subgoals.
-   Assign estimated hours and target dates.
-   Verify that goal progress changes when subgoal statuses change.
-   Open the calendar and check day, week, and month views.
-   Complete and reopen a subgoal from the calendar.
-   Verify that the AI provider status matches the selected provider.
-   Send a planning message and inspect the assistant response.
-   Generate a plan, review it, and save it.
-   Confirm that saved goals and subgoals appear in the normal goal
    views and calendar.
-   Verify that one user cannot access another user's goals.

## Git workflow

A suggested workflow for feature development:

``` powershell
git status
git switch -c feature/your-feature-name
```

After implementing and testing:

``` powershell
git add .
git commit -m "Describe the change"
```

Merge a feature branch only after its tests and frontend build pass.

## Security and configuration notes

-   Never commit database passwords, JWT secrets, or other credentials.
-   Use environment variables or a local, untracked configuration file
    for secrets.
-   Do not assume that a `.env` file is automatically loaded by Spring
    Boot. Configure environment variables or explicitly configure a
    dotenv integration.
-   Keep authenticated endpoints protected and retain server-side
    user-ownership checks.
-   Treat AI-generated content as an editable suggestion. Validate it on
    the backend before saving it.

## Current development focus

The current V3 development work focuses on: - Verifying the calendar and
target-date workflow. - Switching the AI planner from scripted responses
to real Ollama inference. - Improving conversational context and
structured plan generation. - Testing the complete workflow from goal
description through saving and calendar display.

See the `docs/` directory for additional implementation notes and the
changelog.
