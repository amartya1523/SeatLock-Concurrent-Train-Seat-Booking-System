package com.amartya.seatlock;

import com.amartya.seatlock.api.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeatConflictIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired TestRestTemplate client;

    @Test
    void lockedSeatIsRejectedForASecondCustomer() {
        assertThat(hold("First Customer", "3A").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ApiError> conflict = holdExpectingError("Second Customer", "3A");

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).extracting(ApiError::code).isEqualTo("SEAT_UNAVAILABLE");
        assertThat(statusOf("3A")).isEqualTo("LOCKED");
    }

    @Test
    void bookedSeatIsRejectedForASecondCustomer() {
        ResponseEntity<ReservationResponse> first = hold("First Customer", "4B");
        client.postForEntity("/api/v1/reservations/" + first.getBody().reservationId() + "/confirm", null, BookingResponse.class);

        ResponseEntity<ApiError> conflict = holdExpectingError("Second Customer", "4B");

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).extracting(ApiError::code).isEqualTo("SEAT_UNAVAILABLE");
        assertThat(statusOf("4B")).isEqualTo("BOOKED");
    }

    @Test
    void confirmedReservationCannotBeConfirmedTwice() {
        ResponseEntity<ReservationResponse> hold = hold("Repeat Customer", "5C");
        String confirmUrl = "/api/v1/reservations/" + hold.getBody().reservationId() + "/confirm";
        assertThat(client.postForEntity(confirmUrl, null, BookingResponse.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiError> repeat = client.postForEntity(confirmUrl, null, ApiError.class);

        assertThat(repeat.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repeat.getBody()).extracting(ApiError::code).isEqualTo("ALREADY_CONFIRMED");
    }

    private ResponseEntity<ReservationResponse> hold(String name, String seat) {
        return client.postForEntity("/api/v1/reservations", request(name, seat), ReservationResponse.class);
    }

    private ResponseEntity<ApiError> holdExpectingError(String name, String seat) {
        return client.postForEntity("/api/v1/reservations", request(name, seat), ApiError.class);
    }

    private CreateReservationRequest request(String name, String seat) {
        return new CreateReservationRequest(name, name.toLowerCase().replace(' ', '.') + "@example.com", List.of(seat));
    }

    private String statusOf(String seatNumber) {
        SeatResponse[] seats = client.getForObject("/api/v1/seats", SeatResponse[].class);
        return List.of(seats).stream().filter(seat -> seat.seatNumber().equals(seatNumber)).findFirst().orElseThrow().status();
    }
}
