package org.example.hotelsystem.model;

import java.time.LocalDate;

public class Reservation {
    private int reservationId;
    private int receptionistId;
    private int roomNumber;
    private String guestName;
    private String guestContact;
    private LocalDate reservationDate;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String roomStatus;
    private String reservationStatus;

    public Reservation(int reservationId, int receptionistId, int roomNumber, String guestName, String guestContact,
                       LocalDate reservationDate, LocalDate checkInDate, LocalDate checkOutDate,
                       String roomStatus, String reservationStatus) {
        this.reservationId = reservationId;
        this.receptionistId = receptionistId;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestContact = guestContact;
        this.reservationDate = reservationDate;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomStatus = roomStatus;
        this.reservationStatus = reservationStatus;
    }

    public int getReservationId() { return reservationId; }
    public int getReceptionistId() { return receptionistId; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestContact() { return guestContact; }
    public LocalDate getReservationDate() { return reservationDate; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public String getRoomStatus() { return roomStatus; }
    public String getReservationStatus() { return reservationStatus; }
}
