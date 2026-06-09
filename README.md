# Workafella Conference Room, Billing, and Gate Pass System

Full-stack project generated from the requested requirements.

## Tech Stack

- Frontend: React + JavaScript + Vite
- Backend: Spring Boot 3, Spring Security, JWT, Java Mail Sender
- Database: PostgreSQL database `workafella_app`
- Migrations: Flyway auto-creates tables on backend startup

## Default Admin Login

- Email: `admin@workafella.com`
- Password: `Admin@12345`

Change these with `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables before production use.

## Run Locally

1. Start PostgreSQL:

```bash
docker compose up -d
```

2. Start backend:

```bash
cd backend
mvn spring-boot:run
```

Backend runs on `http://localhost:9092`.

3. Start frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5174`.

## Mail Setup

Set these environment variables for real email delivery:

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-app-password
```

If mail is not configured, the backend keeps working and logs that mail was skipped.

## Production Notes

- Replace `JWT_SECRET` with a strong secret of at least 32 characters.
- Replace default admin credentials.
- Use managed PostgreSQL with backups.
- Use HTTPS and restrict CORS origins to your deployed frontend.
- Move generated client passwords to a reset-password flow before public production.

## Main Features

- Admin creates client companies and client login users.
- Client passwords are BCrypt-hashed in the database.
- Admin generates invoices at INR 11,000 per seat.
- Clients see invoice progress and billing history.
- Scheduled reminder email runs at 09:00 on the 1st day of every month.
- Clients book 6-seater and 12-seater conference rooms.
- Each company can book each room up to its seat count per month.
- Slots are one hour from 06:00 to 21:00, based on availability.
- Clients generate visitor gate passes that are emailed to visitors.
