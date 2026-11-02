package com.amartya.seatlock.domain;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
public class Reservation {
    @Id private UUID id;
    @Column(name = "customer_name", nullable = false) private String customerName;
    @Column(name = "customer_email", nullable = false) private String customerEmail;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "reservation_seats", joinColumns = @JoinColumn(name = "reservation_id"), inverseJoinColumns = @JoinColumn(name = "seat_id"))
    @OrderBy("seatNumber ASC")
    private Set<Seat> seats = new LinkedHashSet<>();

    protected Reservation() {}
    public Reservation(String name, String email, Set<Seat> seats, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID(); this.customerName = name; this.customerEmail = email;
        this.seats.addAll(seats); this.status = ReservationStatus.ACTIVE;
        this.createdAt = now; this.updatedAt = now; this.expiresAt = expiresAt;
    }
    public void confirm(Instant now) { status = ReservationStatus.CONFIRMED; updatedAt = now; }
    public void cancel(Instant now) { status = ReservationStatus.CANCELLED; updatedAt = now; }
    public void expire(Instant now) { status = ReservationStatus.EXPIRED; updatedAt = now; }
    public boolean isExpiredAt(Instant instant) { return !expiresAt.isAfter(instant); }
}
