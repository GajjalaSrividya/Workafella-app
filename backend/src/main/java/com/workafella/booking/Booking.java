package com.workafella.booking;

import com.workafella.auth.AppUser;
import com.workafella.company.Company;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
    private Room room;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by")
    private AppUser bookedBy;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    @Enumerated(EnumType.STRING) private BookingStatus status = BookingStatus.BOOKED;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public AppUser getBookedBy() { return bookedBy; }
    public void setBookedBy(AppUser bookedBy) { this.bookedBy = bookedBy; }
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
