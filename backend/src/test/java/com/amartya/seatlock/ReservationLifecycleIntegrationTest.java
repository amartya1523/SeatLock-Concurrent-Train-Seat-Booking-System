package com.amartya.seatlock;

import com.amartya.seatlock.api.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationLifecycleIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void shortHoldForExpiryTest(DynamicPropertyRegistry registry) {
        registry.add("seatlock.lock-duration", () -> "PT1S");
    }

    @Autowired TestRestTemplate client;

    @Test
    void cancelledReservationImmediatelyReleasesItsSeat() {
        ReservationResponse hold = createHold("1A");

        ResponseEntity<ReservationResponse> cancellation = client.postForEntity(
            "/api/v1/reservations/" + hold.reservationId() + "/cancel", null, ReservationResponse.class);

        assertThat(cancellation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusOf("1A")).isEqualTo("AVAILABLE");
    }

    @Test
    void expiredReservationCannotBeConfirmedAndReleasesItsSeat() throws InterruptedException {
        ReservationResponse hold = createHold("1B");
        Thread.sleep(1_200);

        ResponseEntity<ApiError> confirmation = client.postForEntity(
            "/api/v1/reservations/" + hold.reservationId() + "/confirm", null, ApiError.class);

        assertThat(confirmation.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(confirmation.getBody()).extracting(ApiError::code).isEqualTo("RESERVATION_EXPIRED");
        assertThat(statusOf("1B")).isEqualTo("AVAILABLE");
    }

    private ReservationResponse createHold(String seatNumber) {
        var request = new CreateReservationRequest("Lifecycle Test", "lifecycle@example.com", List.of(seatNumber));
        ResponseEntity<ReservationResponse> response = client.postForEntity("/api/v1/reservations", request, ReservationResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private String statusOf(String seatNumber) {
        SeatResponse[] seats = client.getForObject("/api/v1/seats", SeatResponse[].class);
        return List.of(seats).stream().filter(seat -> seat.seatNumber().equals(seatNumber)).findFirst().orElseThrow().status();
    }
}
