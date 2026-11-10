# GoalStack

A goal management web app. Create goals, split them into subgoals, set optional deadlines, and see progress calculated automatically.

- **Backend:** Spring Boot REST API (Java 21) + MySQL
- **Frontend:** React (Vite) single-page app in `frontend/`

## Problem statement
Large goals are hard to finish when they are not broken into small steps. GoalStack lets one user organise goals into subgoals, track the status of each step, and see overall progress at a glance.

## Features (implemented in Version 1)
- Create, view, edit and delete goals (title, optional description, optional deadline)
- Add, edit, delete subgoals and change their status (Pending, In progress, Completed)
- Progress calculated on the fly: `completed subgoals / total subgoals x 100` (rounded down)
- A goal is complete only when it has at least one subgoal and all are completed
- Overdue indicator (deadline before today and goal not complete)
- Dashboard with goal cards, progress bars, loading and empty states
- Server-side validation (required title, max 120 characters); messages shown under the field, entered values kept
- Clean JSON errors for missing goals/subgoals and unexpected errors (no stack traces)
- Confirmation prompt before deleting
- Deleting a goal deletes its subgoals

Not included in V1: login, nested subgoals, notifications, analytics, AI features.

## Technology stack
Java 21, Spring Boot 3.5.x, Maven, Spring Web MVC (REST), Spring Data JPA (Hibernate), MySQL 8, Jakarta Bean Validation, JUnit 5 and Mockito, React 18, React Router 6, Vite, Bootstrap 5 (npm package), Node.js 20+.

## Architecture
```
React (browser) --JSON over HTTP--> Controller -> Service -> Repository -> MySQL
```

| Package / folder | Purpose |
|---|---|
| `controller` | REST endpoints under `/api`; receive JSON, call services, return JSON |
| `service` | All business rules (progress, complete, overdue, parent-child checks) |
| `repository` | Spring Data JPA interfaces that talk to the database |
| `entity` | `Goal`, `Subgoal`, `SubgoalStatus` mapped to tables |
| `dto` | Request bodies (`GoalRequest`, `SubgoalRequest`, `StatusRequest`), responses (`GoalResponse`, `SubgoalResponse`) and `GoalView` |
| `exception` | `ResourceNotFoundException`, `ApiError`, and the global JSON error handler |
| `frontend/src/pages` | One React component per screen |
| `frontend/src/api.js` | The only file that calls the backend |

In development the React dev server (port 5173) forwards `/api` requests to Spring Boot (port 8080) using the proxy in `frontend/vite.config.js`, so no CORS setup is needed.

## REST API
| Method | URL | Purpose |
|---|---|---|
| GET | `/api/goals` | All goals with progress (no subgoal list) |
| POST | `/api/goals` | Create a goal (201) |
| GET | `/api/goals/{id}` | One goal with its subgoals |
| PUT | `/api/goals/{id}` | Update a goal |
| DELETE | `/api/goals/{id}` | Delete a goal and its subgoals (204) |
| POST | `/api/goals/{goalId}/subgoals` | Add a subgoal (always starts PENDING) |
| GET | `/api/subgoals/{id}` | One subgoal |
| PUT | `/api/subgoals/{id}` | Update title, description, status |
| PATCH | `/api/subgoals/{id}/status` | Body `{"status":"COMPLETED"}` |
| DELETE | `/api/subgoals/{id}` | Delete a subgoal (204) |

Dates use `yyyy-MM-dd`. Errors look like `{"status":400,"message":"...","fieldErrors":{"title":"Title is required."}}`.

## Database
Two tables, created automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`). An optional manual script is in `database/schema.sql`. Spring's own SQL initialisation is off (`spring.sql.init.mode=never`) so the two approaches never clash.

- `goals(id, title, description, deadline, created_at, updated_at)`
- `subgoals(id, goal_id, title, description, status, created_at, updated_at)`
- One goal has many subgoals; each subgoal belongs to one goal.
- `subgoals.goal_id` is a foreign key with `ON DELETE CASCADE` and an index.
- Progress is never stored; it is calculated from subgoal statuses.
- Hibernate creates timestamp columns as `datetime(6)`; the manual `schema.sql` uses plain `DATETIME`.
- `status` is `VARCHAR(20) NOT NULL DEFAULT 'PENDING'`. Allowed values are enforced by the application (enum). The manual `schema.sql` also adds a `CHECK` constraint.

## Prerequisites (Windows)
- JDK 21 (`java -version`)
- Maven 3.9+ (`mvn -v`), or IntelliJ IDEA which bundles Maven
- Node.js 20+ and npm (`node -v`)
- MySQL 8 running locally

## Database setup
Nothing required: the default URL has `createDatabaseIfNotExist=true`, so the `goalstack` database is created on first start. Or create it yourself: `mysql -u root -p < database\schema.sql`

## Configuration
`src/main/resources/application.properties` reads:

| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/goalstack?...` | JDBC URL |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | *(empty)* | MySQL password |

The empty default password only works if your local MySQL user has none. Set `DB_PASSWORD` otherwise. Never commit real passwords. `.env.example` lists the variables, but Spring Boot does not read `.env` files.

## Run on Windows (two terminals)
**Terminal 1: backend**
```powershell
cd goalstack
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
mvn spring-boot:run
```
**Terminal 2: frontend**
```powershell
cd goalstack\frontend
npm install
npm run dev
```
Open http://localhost:5173. (The backend alone on http://localhost:8080 serves only the API; try http://localhost:8080/api/goals.)

Run backend tests: `mvn test` (no MySQL needed; the repository test uses in-memory H2).
Build backend jar: `mvn clean package`. Build frontend: `cd frontend && npm run build` (output in `frontend/dist`).

There is no Maven wrapper (`mvnw`). Install Maven, or open the folder in IntelliJ IDEA and run `GoalStackApplication`.

## Example usage
1. Click **Add Goal**, enter "Learn Java", save.
2. Click **Add Subgoal** five times: Data Types, Variables and Operators, Classes and Objects, Exception Handling, Collections Framework.
3. Set the first two to **Completed**: the goal shows 2 of 5 and 40%.
4. Set one back to **In progress**: progress drops to 20%.

## Screenshots
Add your own to `docs/screenshots/` after running the app, then link them here: `dashboard.png`, `goal-details.png`.

## Manual test checklist
1. Create a goal, see it on the dashboard, edit it.
2. Add subgoals, mark one Completed, check the percentage.
3. Change Completed to In progress: progress decreases.
4. Add and delete subgoals: progress changes.
5. No subgoals shows 0%; all completed shows 100% and a Completed badge.
6. Past deadline on an incomplete goal shows Overdue; completing all subgoals removes it.
7. Delete a goal, then in MySQL: `SELECT COUNT(*) FROM subgoals WHERE goal_id = <id>;` returns 0.
8. Restart the backend: data is still there.
9. Server-side validation (the browser's `required` check blocks an empty title, so call the API directly):
   `curl.exe -i -X POST -H "Content-Type: application/json" -d "{\"title\":\"\"}" http://localhost:8080/api/goals` should return 400 with `fieldErrors.title`.
10. Open `http://localhost:8080/api/goals/9999` and `http://localhost:8080/api/subgoals/9999`: 404 JSON. In the React app, `/goals/9999` shows an error message.

## Known limitations
- Single user, no login
- In production you must serve `frontend/dist` yourself; Spring Boot does not host it yet
- No pagination, search or sorting on the dashboard
- Subgoals cannot be reordered
- The "overdue" rule uses the server's date
- No frontend automated tests

## Future enhancements
Authentication and per-user goals, serving the React build from Spring Boot, subgoal ordering, search and filters, Flyway migrations, Docker setup, deployment.
