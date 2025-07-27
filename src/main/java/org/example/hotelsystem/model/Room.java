package org.example.hotelsystem.model;

import java.time.LocalDate;

public class Room {
    private int roomNumber;
    private String status;
    private float price;
    private boolean ac;
    private boolean singleBed;
    private boolean twinBed;
    private boolean doubleBed;
    private String checkStatus;
    private int adminId;
    private LocalDate lastModifiedDate;

    public Room(int roomNumber, String status, float price, boolean ac, boolean singleBed, boolean twinBed, boolean doubleBed, String checkStatus, int adminId, LocalDate lastModifiedDate) {
        this.roomNumber = roomNumber;
        this.status = status;
        this.price = price;
        this.ac = ac;
        this.singleBed = singleBed;
        this.twinBed = twinBed;
        this.doubleBed = doubleBed;
        this.checkStatus = checkStatus;
        this.adminId = adminId;
        this.lastModifiedDate = lastModifiedDate;
    }

    public int getRoomNumber() { return roomNumber; }
    public String getStatus() { return status; }
    public float getPrice() { return price; }
    public boolean isAc() { return ac; }
    public boolean isSingleBed() { return singleBed; }
    public boolean isTwinBed() { return twinBed; }
    public boolean isDoubleBed() { return doubleBed; }
    public String getCheckStatus() { return checkStatus; }
    public int getAdminId() { return adminId; }
    public LocalDate getLastModifiedDate() { return lastModifiedDate; }
}
