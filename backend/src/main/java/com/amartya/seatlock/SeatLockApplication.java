package com.amartya.seatlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SeatLockApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeatLockApplication.class, args);
    }
}
