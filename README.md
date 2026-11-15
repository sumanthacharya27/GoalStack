# GoalStack (Version 3, phase 1: effort-weighted progress)

A goal management web app with user accounts. Each user creates goals, splits them into subgoals, sets optional deadlines, and sees progress calculated automatically. Users only see their own goals.

- **Backend:** Spring Boot REST API (Java 21) + MySQL + Spring Security with JWT login tokens
- **Frontend:** React (Vite) single-page app in `frontend/`

## Problem statement
Large goals are hard to finish when they are not broken into small steps. GoalStack lets each person organise goals into subgoals, track the status of each step, and see overall progress at a glance.

## Features
**Version 1 (still included)**
- Create, view, edit and delete goals (title, optional description, optional deadline); deleting a goal deletes its subgoals
- Add, edit, delete subgoals and change their status (Pending, In progress, Completed)
- Progress is calculated on the server and never stored (see "How progress works" below)
- Complete only when there is at least one subgoal and all are completed; Overdue when the deadline has passed and the goal is not complete
- Dashboard with goal cards, progress bars, title search, status filter, loading and empty states
- Server-side validation with messages under each field; clean JSON errors; delete confirmations

**New in Version 2**
- Register, log in and log out (passwords stored as BCrypt hashes; login uses a JWT token)
- Every goal belongs to one user; other users' goals and subgoals return "not found"
- React login and register pages, pages that need login, user name and Log out in the navbar, automatic logout when the token expires
- Run everything from one jar (Spring Boot serves the React app) or with Docker Compose

**New in Version 3, phase 1**
- Effort-weighted progress: each subgoal of a new goal has estimated hours (0.25 to 1000, at most 50 subgoals per goal); its weight is its share of the total hours, and progress is the sum of the weights of completed subgoals
- Weights are calculated on the server, always total exactly 100.00%, and are recalculated when a subgoal is added, deleted or its hours change
- Goals made before Version 3 keep equal weights forever (no hours); the app tells you so on the goal page
- Database changes are now managed by Flyway migration files instead of Hibernate `update`
- Planned next (not built yet): AI planning chat with a local Ollama model

Not included: refresh tokens, password reset, email verification, roles, rate limiting, nested subgoals, notifications.

## Technology stack
Java 21, Spring Boot 3.5.x, Maven, Spring Web MVC (REST), Spring Data JPA (Hibernate), Spring Security (OAuth2 resource server for JWT), MySQL 8, Jakarta Bean Validation, JUnit 5 and Mockito, React 18, React Router 6, Vite, Bootstrap 5, Node.js 20+, Docker (optional).

## Architecture
```
React (browser) --JSON + Bearer token--> Security filter -> Controller -> Service -> Repository -> MySQL
```

| Package / folder | Purpose |
|---|---|
| `security` | `SecurityConfig` (who may call what), `JwtConfig` (signing key, token encoder/decoder), `CurrentUser` |
| `controller` | REST endpoints (`AuthController`, `GoalController`, `SubgoalController`) and `SpaController` |
| `service` | Business rules: `GoalService`, `SubgoalService`, `AuthService`, `JwtService` |
| `repository` | Spring Data JPA interfaces; goal and subgoal queries always include the user id |
| `entity` | `User`, `Goal`, `Subgoal`, `SubgoalStatus` |
| `dto` | Request and response JSON classes |
| `exception` | Custom exceptions and the global JSON error handler |
| `frontend/src/auth` | `AuthContext` (who is logged in) and `ProtectedRoute` |
| `frontend/src/pages` | One React component per screen |
| `frontend/src/api.js` | The only file that calls the backend; adds the token to each request |

## How login works
1. `POST /api/auth/register` or `/api/auth/login` returns a token (a signed JWT whose subject is the user id).
2. React stores the token in the browser's `localStorage` and sends `Authorization: Bearer <token>` with every request.
3. Spring Security checks the signature and expiry. Controllers read the user id from the token and pass it to services.
4. Every goal and subgoal query includes that user id, so a user can never reach someone else's data (a foreign id looks like a missing one).
5. When the token expires, the API answers 401 and React sends the user to the login page.

## REST API
| Method | URL | Login needed | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | no | Create an account (201). Body: `name`, `email`, `password` |
| POST | `/api/auth/login` | no | Log in. Body: `email`, `password` |
| GET | `/api/auth/me` | yes | The logged-in user |
| GET | `/api/goals` | yes | Your goals with progress (no subgoal list) |
| POST | `/api/goals` | yes | Create a goal (201) |
| GET | `/api/goals/{id}` | yes | One goal with its subgoals |
| PUT | `/api/goals/{id}` | yes | Update a goal |
| DELETE | `/api/goals/{id}` | yes | Delete a goal and its subgoals (204) |
| POST | `/api/goals/{goalId}/subgoals` | yes | Add a subgoal (always starts PENDING) |
| GET | `/api/subgoals/{id}` | yes | One subgoal |
| PUT | `/api/subgoals/{id}` | yes | Update title, description, status |
| PATCH | `/api/subgoals/{id}/status` | yes | Body `{"status":"COMPLETED"}` |
| DELETE | `/api/subgoals/{id}` | yes | Delete a subgoal (204) |

Dates use `yyyy-MM-dd`. Errors look like `{"status":400,"message":"...","fieldErrors":{"title":"Title is required."}}`. Without a valid token, `/api/**` (except register and login) answers 401. Duplicate email answers 409, wrong login answers 401.

## How progress works
- **New goals (EFFORT):** weight of a subgoal = its hours / total hours of the goal x 100. Example: hours 10, 25, 15, 35, 15 give weights 10, 25, 15, 35, 15. If the first two are completed, progress is 35%, not 40%.
- **Older goals (EQUAL):** every subgoal has the same weight (3 subgoals: 33.34, 33.33, 33.33).
- Weights use exact whole hundredths of a percent with the largest-remainder rule: everybody gets the rounded-down value, and the leftover hundredths go to the biggest remainders (ties: the earlier subgoal). So the weights always total exactly 100.00 and the same input always gives the same output.
- Progress = sum of the weights of COMPLETED subgoals. IN_PROGRESS does not count. All completed gives exactly 100.00. The screen shows it rounded down to one decimal.
- A goal is complete only with at least one subgoal and all of them completed. Overdue works as before.
- Edge case: a very small subgoal next to very large ones (for example 0.25 hours among 49 subgoals of 1000 hours) can get a weight of 0.00%.

## Database
Three tables. The structure is managed by **Flyway**: the files in `src/main/resources/db/migration` run in order on startup (`V1` creates the Version 2 tables for a brand new database, `V2` adds the weighted-progress columns). Hibernate no longer changes tables (`ddl-auto=none`). Set the environment variable `DDL_AUTO=validate` to make Hibernate double-check entities against tables while developing. `database/schema.sql` is a manual copy of the final schema. Spring's own SQL initialisation is off.

- `users(id, name, email UNIQUE, password_hash, created_at)`
- `goals(id, user_id, title, description, deadline, weighting_mode, created_at, updated_at)`: `weighting_mode` is EQUAL for goals made before Version 3, EFFORT for new goals
- `subgoals(id, goal_id, title, description, status, estimated_hours, weight, sort_order, created_at, updated_at)`: `estimated_hours` is empty for older goals; `weight` and `sort_order` are always set
- A user has many goals; a goal has many subgoals. Both foreign keys use `ON DELETE CASCADE`.
- `goals.user_id` is nullable in the database so Version 1 goals can still be stored; the app always sets it for new goals.
- Progress is never stored.

## Prerequisites (Windows)
- JDK 21 (`java -version`), Maven 3.9+ (`mvn -v`), Node.js 20+ (`node -v`), MySQL 8 running locally
- Docker Desktop (only for the Docker option)

## Configuration
| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/goalstack?...` | JDBC URL (database is created if missing) |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | *(empty)* | MySQL password |
| `JWT_SECRET` | *(empty)* | Signs login tokens, at least 32 characters. If empty, a random one is made at startup and everyone is logged out on each restart |
| `JWT_EXPIRY_MINUTES` | `120` | How long a login lasts |

Never commit real passwords or secrets. `.env.example` lists the variables; Spring Boot does not read `.env` files, but Docker Compose does.

## Run option 1: development (two terminals)
**Terminal 1: backend**
```powershell
cd goalstack
$env:DB_PASSWORD = "your_mysql_password"
$env:JWT_SECRET = "replace-this-with-a-long-random-text-32-chars-or-more"
mvn spring-boot:run
```
**Terminal 2: frontend**
```powershell
cd goalstack\frontend
npm install
npm run dev
```
Open http://localhost:5173, register an account, and start adding goals. (The dev server forwards `/api` to port 8080.)

## Run option 2: one jar
```powershell
cd goalstack
.\build-single-jar.ps1
$env:DB_PASSWORD = "your_mysql_password"
$env:JWT_SECRET = "replace-this-with-a-long-random-text-32-chars-or-more"
java -jar target\goalstack-0.3.0-SNAPSHOT.jar
```
Open http://localhost:8080. If PowerShell blocks the script, run `Set-ExecutionPolicy -Scope Process Bypass` first.

## Run option 3: Docker Compose
```powershell
copy .env.example .env     # then edit DB_PASSWORD and JWT_SECRET in .env
docker compose up --build
```
Open http://localhost:8080. MySQL data is kept in a Docker volume.

## Tests
`mvn test` runs 70 tests (no MySQL needed; the repository test uses in-memory H2 with Flyway switched off, because the migration files are written for MySQL).

## Upgrading an existing database
Back up first: `mysqldump -u root -p goalstack > backup.sql`

- **Your database was created by Version 1** (no `users` table): run `mysql -u root -p goalstack < database\upgrade_v1_to_v2.sql` once, start the app, register an account, then run `database\claim_v1_goals.sql` (edit the email inside) so your old goals belong to that account.
- **Your database was created by Version 2:** just start the app. Flyway marks it as version 1 and adds the new columns. Every existing goal becomes an EQUAL (equal-weight) goal and its subgoals get equal weights.
- **You do not need the old data:** drop the `goalstack` database and start the app; Flyway builds everything.
- If the first start stops with `Table '...users' doesn't exist`, your database is still at Version 1; run the upgrade script above. Nothing was changed by the failed migration.

## Try the API from PowerShell
```powershell
$body = @{ name = "Asha"; email = "asha@example.com"; password = "password123" } | ConvertTo-Json
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType "application/json" -Body $body
$headers = @{ Authorization = "Bearer $($r.token)" }
Invoke-RestMethod http://localhost:8080/api/goals -Headers $headers      # works: []
Invoke-WebRequest http://localhost:8080/api/goals                         # no token: 401
```

## Security notes and limitations
- Passwords are stored as BCrypt hashes and must be 8 to 72 characters. Login errors do not say whether the email exists.
- The token is kept in `localStorage`. That is simple, but any script injected into the page could read it. React escapes text by default, which helps. Cookies with refresh tokens are a stronger design.
- No refresh tokens: when the token expires the user logs in again.
- No password reset, email verification, account deletion, rate limiting or lockout.
- Registration says when an email is already taken, so emails can be checked for existence.
- Without `JWT_SECRET`, logins reset on every restart. Always set it outside quick local testing.
- The overdue rule uses the server's date. No pagination or sorting on the dashboard. No frontend automated tests.

## Future enhancements
Refresh tokens and password reset, rate limiting, Flyway migrations, subgoal deadlines or ordering, frontend tests, cloud deployment.
