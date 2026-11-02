package com.amartya.seatlock.api;

import com.amartya.seatlock.api.dto.*;
import com.amartya.seatlock.service.SeatLockService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final SeatLockService service;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Lock selected seats for two minutes")
    public ReservationResponse create(@Valid @RequestBody CreateReservationRequest request) { return service.createReservation(request); }
    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm payment and create a booking")
    public BookingResponse confirm(@PathVariable UUID id) { return service.confirm(id); }
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an active reservation and release its seats")
    public ReservationResponse cancel(@PathVariable UUID id) { return service.cancel(id); }
}
