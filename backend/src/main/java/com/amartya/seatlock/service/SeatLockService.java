package com.amartya.seatlock.service;

import com.amartya.seatlock.api.dto.*;
import com.amartya.seatlock.domain.*;
import com.amartya.seatlock.exception.BookingException;
import com.amartya.seatlock.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatLockService {
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final BookingRepository bookingRepository;
    @Value("${seatlock.lock-duration:PT2M}") private Duration lockDuration;

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats() {
        Instant now = Instant.now();
        Map<Long, Reservation> activeLocks = reservationRepository.findActiveWithSeats(ReservationStatus.ACTIVE, now).stream()
            .flatMap(r -> r.getSeats().stream().map(s -> Map.entry(s.getId(), r)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, ignored) -> first));
        Set<Long> booked = bookingRepository.findAllWithSeats().stream().flatMap(b -> b.getSeats().stream()).map(Seat::getId).collect(Collectors.toSet());
        return seatRepository.findAllByOrderByRowNumberAscPositionAsc().stream().map(seat -> {
            Reservation lock = activeLocks.get(seat.getId());
            String status = booked.contains(seat.getId()) ? "BOOKED" : lock != null ? "LOCKED" : "AVAILABLE";
            return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getRowNumber(), seat.getPosition(), status, lock == null ? null : lock.getExpiresAt());
        }).toList();
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        Instant now = Instant.now();
        List<String> numbers = request.seatNumbers().stream().distinct().sorted().toList();
        if (numbers.size() != request.seatNumbers().size()) throw error(HttpStatus.BAD_REQUEST, "DUPLICATE_SEAT", "The same seat cannot be selected more than once.");
        List<Seat> seats = seatRepository.findAllBySeatNumberInForUpdate(numbers);
        if (seats.size() != numbers.size()) throw error(HttpStatus.NOT_FOUND, "SEAT_NOT_FOUND", "One or more requested seats do not exist.");
        List<Long> ids = seats.stream().map(Seat::getId).toList();
        if (!bookingRepository.findForSeatIds(ids).isEmpty() || !reservationRepository.findActiveForSeats(ids, now).isEmpty())
            throw error(HttpStatus.CONFLICT, "SEAT_UNAVAILABLE", "One or more selected seats are no longer available.");
        Reservation reservation = new Reservation(request.customerName().trim(), request.customerEmail().trim().toLowerCase(), new LinkedHashSet<>(seats), now, now.plus(lockDuration));
        return toReservation(reservationRepository.save(reservation), now);
    }

    @Transactional
    public BookingResponse confirm(UUID reservationId) {
        Instant now = Instant.now();
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reservation not found."));
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) throw error(HttpStatus.CONFLICT, "ALREADY_CONFIRMED", "This reservation has already been confirmed.");
        if (reservation.getStatus() != ReservationStatus.ACTIVE || reservation.isExpiredAt(now)) {
            if (reservation.getStatus() == ReservationStatus.ACTIVE) reservation.expire(now);
            throw error(HttpStatus.GONE, "RESERVATION_EXPIRED", "This reservation has expired. Please select the seats again.");
        }
        seatRepository.findAllBySeatNumberInForUpdate(reservation.getSeats().stream().map(Seat::getSeatNumber).sorted().toList());
        if (!bookingRepository.findForSeatIds(reservation.getSeats().stream().map(Seat::getId).toList()).isEmpty())
            throw error(HttpStatus.CONFLICT, "SEAT_UNAVAILABLE", "A selected seat has already been booked.");
        reservation.confirm(now);
        return toBooking(bookingRepository.save(new Booking(reservation, now)));
    }

    @Transactional
    public ReservationResponse cancel(UUID id) {
        Instant now = Instant.now();
        Reservation reservation = reservationRepository.findByIdForUpdate(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reservation not found."));
        if (reservation.getStatus() != ReservationStatus.ACTIVE) throw error(HttpStatus.CONFLICT, "RESERVATION_NOT_ACTIVE", "Only an active reservation can be cancelled.");
        reservation.cancel(now);
        return toReservation(reservation, now);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings() { return bookingRepository.findAllWithSeats().stream().map(this::toBooking).toList(); }

    @Transactional
    @Scheduled(fixedDelay = 15000)
    public void expireReservations() { reservationRepository.expireDueReservations(Instant.now()); }

    private ReservationResponse toReservation(Reservation r, Instant now) {
        long remaining = r.getStatus() == ReservationStatus.ACTIVE ? Math.max(0, Duration.between(now, r.getExpiresAt()).toSeconds()) : 0;
        return new ReservationResponse(r.getId(), r.getStatus().name(), r.getSeats().stream().map(Seat::getSeatNumber).sorted().toList(), r.getExpiresAt(), remaining);
    }
    private BookingResponse toBooking(Booking b) { return new BookingResponse(b.getId(), b.getReference(), b.getCustomerName(), b.getCustomerEmail(), b.getSeats().stream().map(Seat::getSeatNumber).sorted().toList(), b.getConfirmedAt()); }
    private BookingException error(HttpStatus status, String code, String message) { return new BookingException(status, code, message); }
}
