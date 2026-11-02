package com.amartya.seatlock.repository;

import com.amartya.seatlock.domain.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findAllByOrderByRowNumberAscPositionAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.seatNumber in :numbers order by s.seatNumber")
    List<Seat> findAllBySeatNumberInForUpdate(@Param("numbers") List<String> numbers);
}
