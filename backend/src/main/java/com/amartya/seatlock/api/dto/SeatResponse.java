package com.amartya.seatlock.api.dto;

import java.time.Instant;

public record SeatResponse(Long id, String seatNumber, int rowNumber, String position, String status, Instant lockedUntil) {}
