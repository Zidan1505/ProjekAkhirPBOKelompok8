package org.example.hotelsystem.model;

import java.time.LocalDate;

public class HousekeepingRecord {

    private int recordId;
    private int housekeepingId;
    private int roomNumber;
    private LocalDate recordDate;
    private String notes;

    public HousekeepingRecord(int recordId, int housekeepingId, int roomNumber, LocalDate recordDate, String notes) {
        this.recordId = recordId;
        this.housekeepingId = housekeepingId;
        this.roomNumber = roomNumber;
        this.recordDate = recordDate;
        this.notes = notes;
    }

    public int getRecordId() {
        return recordId;
    }

    public int getHousekeepingId() {
        return housekeepingId;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public void setHousekeepingId(int housekeepingId) {
        this.housekeepingId = housekeepingId;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
