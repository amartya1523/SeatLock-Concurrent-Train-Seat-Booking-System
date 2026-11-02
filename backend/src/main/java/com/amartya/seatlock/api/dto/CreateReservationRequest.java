package com.amartya.seatlock.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateReservationRequest(
    @NotBlank @Size(max = 100) String customerName,
    @NotBlank @Email @Size(max = 255) String customerEmail,
    @NotEmpty @Size(max = 6) List<@Pattern(regexp = "^[1-5][A-D]$") String> seatNumbers
) {}
