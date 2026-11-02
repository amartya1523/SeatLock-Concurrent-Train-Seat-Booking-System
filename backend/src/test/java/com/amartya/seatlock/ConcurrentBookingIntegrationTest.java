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
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentBookingIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired TestRestTemplate client;

    @Test
    void oneHundredConcurrentCustomersCanNeverDoubleBookTwentySeats() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(40);
        try {
            CountDownLatch ready = new CountDownLatch(100);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<BookingResponse>> attempts = new ArrayList<>();
            for (int user = 0; user < 100; user++) {
                int number = user;
                attempts.add(pool.submit(() -> {
                    ready.countDown(); start.await();
                    String seat = (number % 5 + 1) + String.valueOf((char) ('A' + (number % 4)));
                    var request = new CreateReservationRequest("Customer " + number, "customer" + number + "@example.com", List.of(seat));
                    ResponseEntity<ReservationResponse> hold = client.postForEntity("/api/v1/reservations", request, ReservationResponse.class);
                    if (hold.getStatusCode() != HttpStatus.CREATED || hold.getBody() == null) return null;
                    return client.postForEntity("/api/v1/reservations/" + hold.getBody().reservationId() + "/confirm", null, BookingResponse.class).getBody();
                }));
            }
            ready.await(10, TimeUnit.SECONDS); start.countDown();
            List<BookingResponse> winners = new ArrayList<>();
            for (Future<BookingResponse> attempt : attempts) { BookingResponse booking = attempt.get(30, TimeUnit.SECONDS); if (booking != null) winners.add(booking); }
            Set<String> bookedSeats = new HashSet<>();
            winners.forEach(booking -> bookedSeats.addAll(booking.seatNumbers()));
            assertThat(winners).hasSizeLessThanOrEqualTo(20);
            assertThat(bookedSeats).hasSize(winners.size());
        } finally {
            pool.shutdownNow();
        }
    }
}
