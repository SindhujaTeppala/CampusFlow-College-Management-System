# College Course & Student Management System

A separate full-stack project built with:
- React + Vite (frontend)
- Java 17 + Spring Boot 3 (backend)
- Spring Data JPA + MySQL (database)
- Spring Security + JWT (authentication and role-based authorization)

## Roles and permissions

### Admin
- Login to the admin dashboard
- View dashboard totals
- Add, edit and delete student and teacher accounts
- View all courses and enrollments
- Add, edit and delete courses
- View reports and enrollment counts

### Teacher
- Login to the teacher dashboard
- Add courses and manage only courses assigned to them
- Add, edit and delete student records
- View students enrolled in each of their courses
- View subject-wise enrollment counts

### Student
- Login to the student dashboard
- View available courses
- Enroll in a course
- Cancel their own enrollment
- View their enrolled courses

Security rules:
- Passwords are stored as BCrypt hashes, never as plain text.
- API endpoints are protected by JWT authentication and roles.
- A student cannot enroll in the same course twice.
- A teacher can manage only their own courses.
- Students can only view or cancel their own enrollments.
- Admin has system-wide access.

## Requirements
- JDK 17 or later
- Maven 3.8+ (or use the included Maven wrapper if you add one)
- Node.js 20+
- MySQL 8+

## 1. Create the database
Open MySQL Workbench and run:

```sql
CREATE DATABASE college_management;
```

## 2. Configure the backend
Open `backend/src/main/resources/application.properties`.
Set the MySQL username and password, or define environment variables:

PowerShell:
```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
$env:JWT_SECRET = "change-this-to-a-long-random-secret-key-at-least-32-characters"
```

## 3. Start the backend
Open a terminal in the `backend` folder:

```powershell
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

On first start, sample accounts are created automatically if they do not already exist:

| Role | Email | Password |
|---|---|---|
| Admin | admin@college.com | Admin@123 |
| Teacher | teacher@college.com | Teacher@123 |
| Student | student@college.com | Student@123 |

**Change these sample passwords before using this project beyond local practice.**

## 4. Start the frontend
Open another terminal in the `frontend` folder:

```powershell
npm install
npm run dev
```

Open the URL printed by Vite, usually `http://localhost:5173`.

## Main API groups
- `POST /api/auth/login`
- `POST /api/auth/register` (student self-registration)
- `/api/courses`
- `/api/students`
- `/api/teachers`
- `/api/enrollments`
- `/api/dashboard`

## Notes
- The project is a learning/demo foundation. Review and harden it before production use.
- Course capacity is enforced when a course has a positive capacity.
- Deleting a course removes its enrollments.
- The backend creates/updates tables automatically with Hibernate `ddl-auto=update`.
