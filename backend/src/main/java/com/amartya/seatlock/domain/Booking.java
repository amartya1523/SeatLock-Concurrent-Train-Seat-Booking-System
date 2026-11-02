package com.amartya.seatlock.domain;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
public class Booking {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String reference;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reservation_id", nullable = false, unique = true) private Reservation reservation;
    @Column(name = "customer_name", nullable = false) private String customerName;
    @Column(name = "customer_email", nullable = false) private String customerEmail;
    @Column(name = "confirmed_at", nullable = false) private Instant confirmedAt;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"), inverseJoinColumns = @JoinColumn(name = "seat_id"))
    @OrderBy("seatNumber ASC") private Set<Seat> seats = new LinkedHashSet<>();
    protected Booking() {}
    public Booking(Reservation reservation, Instant now) {
        id = UUID.randomUUID(); reference = "SL-" + id.toString().substring(0, 8).toUpperCase();
        this.reservation = reservation; customerName = reservation.getCustomerName();
        customerEmail = reservation.getCustomerEmail(); seats.addAll(reservation.getSeats()); confirmedAt = now;
    }
}
