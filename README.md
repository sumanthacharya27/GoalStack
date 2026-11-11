# GoalStack (Version 2)

A goal management web app with user accounts. Each user creates goals, splits them into subgoals, sets optional deadlines, and sees progress calculated automatically. Users only see their own goals.

- **Backend:** Spring Boot REST API (Java 21) + MySQL + Spring Security with JWT login tokens
- **Frontend:** React (Vite) single-page app in `frontend/`

## Problem statement
Large goals are hard to finish when they are not broken into small steps. GoalStack lets each person organise goals into subgoals, track the status of each step, and see overall progress at a glance.

## Features
**Version 1 (still included)**
- Create, view, edit and delete goals (title, optional description, optional deadline); deleting a goal deletes its subgoals
- Add, edit, delete subgoals and change their status (Pending, In progress, Completed)
- Progress = completed subgoals / total subgoals x 100 (rounded down), calculated on the server and never stored
- Complete only when there is at least one subgoal and all are completed; Overdue when the deadline has passed and the goal is not complete
- Dashboard with goal cards, progress bars, title search, status filter, loading and empty states
- Server-side validation with messages under each field; clean JSON errors; delete confirmations

**New in Version 2**
- Register, log in and log out (passwords stored as BCrypt hashes; login uses a JWT token)
- Every goal belongs to one user; other users' goals and subgoals return "not found"
- React login and register pages, pages that need login, user name and Log out in the navbar, automatic logout when the token expires
- Run everything from one jar (Spring Boot serves the React app) or with Docker Compose

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

## Database
Three tables, created or updated automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`). A manual script is in `database/schema.sql`. Spring's own SQL initialisation is off (`spring.sql.init.mode=never`).

- `users(id, name, email UNIQUE, password_hash, created_at)`
- `goals(id, user_id, title, description, deadline, created_at, updated_at)`
- `subgoals(id, goal_id, title, description, status, created_at, updated_at)`
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
java -jar target\goalstack-0.2.0-SNAPSHOT.jar
```
Open http://localhost:8080. If PowerShell blocks the script, run `Set-ExecutionPolicy -Scope Process Bypass` first.

## Run option 3: Docker Compose
```powershell
copy .env.example .env     # then edit DB_PASSWORD and JWT_SECRET in .env
docker compose up --build
```
Open http://localhost:8080. MySQL data is kept in a Docker volume.

## Tests
`mvn test` runs 42 tests (no MySQL needed; the repository test uses in-memory H2).

## Upgrading from Version 1
1. Start the new version. Hibernate adds the `users` table and the `goals.user_id` column.
2. Register an account in the app.
3. Your old goals have no owner and are invisible. Edit the email in `database\claim_v1_goals.sql`, then run `mysql -u root -p goalstack < database\claim_v1_goals.sql`.
4. If you do not need the old data, you can instead drop the `goalstack` database and let the app recreate it.

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
