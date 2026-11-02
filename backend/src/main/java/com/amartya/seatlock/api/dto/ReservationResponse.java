package com.amartya.seatlock.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(UUID reservationId, String status, List<String> seatNumbers, Instant expiresAt, long remainingSeconds) {}
