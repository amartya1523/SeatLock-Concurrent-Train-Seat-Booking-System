package com.amartya.seatlock.api;

import com.amartya.seatlock.api.dto.SeatResponse;
import com.amartya.seatlock.service.SeatLockService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatLockService service;
    @GetMapping
    @Operation(summary = "Get the live status of all 20 seats")
    public List<SeatResponse> getSeats() { return service.getSeats(); }
}
