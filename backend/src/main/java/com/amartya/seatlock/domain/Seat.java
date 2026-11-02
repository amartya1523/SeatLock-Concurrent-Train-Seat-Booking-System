package com.amartya.seatlock.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "seats")
@Getter
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "seat_number", nullable = false, unique = true)
    private String seatNumber;
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    @Column(nullable = false)
    private String position;
    @Version
    private long version;
    protected Seat() {}
}
