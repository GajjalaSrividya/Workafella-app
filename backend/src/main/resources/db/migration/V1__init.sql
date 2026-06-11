CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE,
    email VARCHAR(180) NOT NULL UNIQUE,
    seat_count INTEGER NOT NULL CHECK (seat_count > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN','CLIENT')),
    company_id BIGINT REFERENCES companies(id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    capacity INTEGER NOT NULL CHECK (capacity IN (6,12)),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    booked_by BIGINT NOT NULL REFERENCES users(id),
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('BOOKED','CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_active_room_slot
ON bookings(room_id, booking_date, start_time)
WHERE status = 'BOOKED';

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    invoice_number VARCHAR(80) NOT NULL UNIQUE,
    billing_month DATE NOT NULL,
    seat_count INTEGER NOT NULL,
    amount_per_seat NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','SENT','PAID','OVERDUE')),
    sent_at TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    amount NUMERIC(12,2) NOT NULL,
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference VARCHAR(160)
);

CREATE TABLE gate_passes (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    created_by BIGINT NOT NULL REFERENCES users(id),

    visitor_name VARCHAR(160) NOT NULL,
    visitor_email VARCHAR(180) NOT NULL,

    host_name VARCHAR(160),
    purpose VARCHAR(500),

    visiting_date DATE NOT NULL,
    entry_time TIME NOT NULL,
    exit_time TIME NOT NULL,

    pass_code VARCHAR(80) NOT NULL UNIQUE,

    status VARCHAR(30) NOT NULL
        CHECK (status IN ('GENERATED','CANCELLED')),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_company_month ON bookings(company_id, booking_date);
CREATE INDEX idx_invoices_company_month ON invoices(company_id, billing_month);
CREATE INDEX idx_gate_passes_company_date ON gate_passes(company_id, visiting_date);

INSERT INTO rooms(name, capacity) VALUES ('6 Seater Conference Room', 6);
INSERT INTO rooms(name, capacity) VALUES ('12 Seater Conference Room', 12);
