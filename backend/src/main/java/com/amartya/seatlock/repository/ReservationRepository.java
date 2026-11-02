package com.amartya.seatlock.repository;

import com.amartya.seatlock.domain.Reservation;
import com.amartya.seatlock.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct r from Reservation r left join fetch r.seats where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    @Query("select distinct r from Reservation r join fetch r.seats where r.status = :status and r.expiresAt > :now")
    List<Reservation> findActiveWithSeats(@Param("status") ReservationStatus status, @Param("now") Instant now);

    @Query("select distinct r from Reservation r join r.seats s where s.id in :seatIds and r.status = 'ACTIVE' and r.expiresAt > :now")
    List<Reservation> findActiveForSeats(@Param("seatIds") Collection<Long> seatIds, @Param("now") Instant now);

    @Modifying
    @Query("update Reservation r set r.status = 'EXPIRED', r.updatedAt = :now where r.status = 'ACTIVE' and r.expiresAt <= :now")
    int expireDueReservations(@Param("now") Instant now);
}
