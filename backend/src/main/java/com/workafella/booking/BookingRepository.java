package com.workafella.booking;

import com.workafella.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByRoomIdAndBookingDateAndStartTimeAndStatus(Long roomId, LocalDate date, LocalTime startTime, BookingStatus status);
    long countByCompanyAndRoomAndBookingDateBetweenAndStatus(Company company, Room room, LocalDate start, LocalDate end, BookingStatus status);
    List<Booking> findByBookingDateAndStatus(LocalDate date, BookingStatus status);
    List<Booking> findByCompanyIdOrderByBookingDateDescStartTimeDesc(Long companyId);
    List<Booking> findTop50ByOrderByBookingDateDescStartTimeDesc();
}
