package com.amartya.seatlock.api;

import com.amartya.seatlock.api.dto.BookingResponse;
import com.amartya.seatlock.service.SeatLockService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final SeatLockService service;
    @GetMapping
    @Operation(summary = "List confirmed bookings for the dashboard")
    public List<BookingResponse> getBookings() { return service.getBookings(); }
}
