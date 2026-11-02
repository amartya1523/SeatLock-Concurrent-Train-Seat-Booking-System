CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    seat_number VARCHAR(4) NOT NULL UNIQUE,
    row_number INTEGER NOT NULL,
    position VARCHAR(10) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE reservation_seats (
    reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    seat_id BIGINT NOT NULL REFERENCES seats(id),
    PRIMARY KEY (reservation_id, seat_id)
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    reference VARCHAR(20) NOT NULL UNIQUE,
    reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(id),
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE booking_seats (
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id BIGINT NOT NULL UNIQUE REFERENCES seats(id),
    PRIMARY KEY (booking_id, seat_id)
);

CREATE INDEX idx_reservations_active_expiry ON reservations(expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_reservation_seats_seat ON reservation_seats(seat_id);

INSERT INTO seats (seat_number, row_number, position)
SELECT CONCAT(row_no, position), row_no, position
FROM generate_series(1, 5) AS row_no
CROSS JOIN (VALUES ('A'), ('B'), ('C'), ('D')) AS positions(position)
ORDER BY row_no, position;
