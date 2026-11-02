package com.amartya.seatlock.repository;

import com.amartya.seatlock.domain.Booking;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query("select distinct b from Booking b join fetch b.seats order by b.confirmedAt desc")
    List<Booking> findAllWithSeats();
    @Query("select distinct b from Booking b join b.seats s where s.id in :seatIds")
    List<Booking> findForSeatIds(Collection<Long> seatIds);
}
