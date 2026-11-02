package com.amartya.seatlock.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingResponse(UUID bookingId, String reference, String customerName, String customerEmail, List<String> seatNumbers, Instant confirmedAt) {}
