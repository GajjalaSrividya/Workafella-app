package com.workafella.booking;

import com.workafella.auth.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {
    private static final LocalTime OPEN = LocalTime.of(6, 0);
    private static final LocalTime CLOSE = LocalTime.of(21, 0);
    private final RoomRepository rooms;
    private final BookingRepository bookings;

    public BookingService(RoomRepository rooms, BookingRepository bookings) {
        this.rooms = rooms;
        this.bookings = bookings;
    }

    public List<SlotDto> availability(Long roomId, LocalDate date) {
        Room room = rooms.findById(roomId).orElseThrow();
        Set<LocalTime> booked = bookings.findByBookingDateAndStatus(date, BookingStatus.BOOKED).stream()
                .filter(b -> b.getRoom().getId().equals(room.getId()))
                .map(Booking::getStartTime)
                .collect(java.util.stream.Collectors.toSet());
        return java.util.stream.Stream.iterate(OPEN, t -> t.plusHours(1))
                .limit(15)
                .map(t -> new SlotDto(t.toString(), t.plusHours(1).toString(), !booked.contains(t)))
                .toList();
    }

    public UsageDto usage(AppUser user, Long roomId, LocalDate date) {
        if (user.getCompany() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client user has no company");
        Room room = rooms.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        YearMonth ym = YearMonth.from(date);
        long used = bookings.countByCompanyAndRoomAndBookingDateBetweenAndStatus(
                user.getCompany(), room, ym.atDay(1), ym.atEndOfMonth(), BookingStatus.BOOKED);
        int limit = user.getCompany().getSeatCount();
        return new UsageDto(room.getName(), room.getCapacity(), limit, used, Math.max(0, limit - used));
    }

    @Transactional
    public BookingDto book(AppUser user, Long roomId, LocalDate date, LocalTime startTime) {
        if (user.getCompany() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client user has no company");
        if (date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bookings are allowed only for today or future dates");
        }
        if (date.equals(LocalDate.now())
        && startTime.isBefore(
                LocalTime.now()
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0))) {

    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Cannot book a past time slot");
}
        if (startTime.isBefore(OPEN) || !startTime.isBefore(CLOSE) || startTime.getMinute() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bookings must start hourly between 06:00 and 20:00");
        }
        Room room = rooms.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        bookings.findByRoomIdAndBookingDateAndStartTimeAndStatus(roomId, date, startTime, BookingStatus.BOOKED)
                .ifPresent(existing -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already booked"); });

        YearMonth ym = YearMonth.from(date);
        long used = bookings.countByCompanyAndRoomAndBookingDateBetweenAndStatus(
                user.getCompany(), room, ym.atDay(1), ym.atEndOfMonth(), BookingStatus.BOOKED);
        if (used >= user.getCompany().getSeatCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Monthly room booking limit reached for this company");
        }

        Booking booking = new Booking();
        booking.setCompany(user.getCompany());
        booking.setRoom(room);
        booking.setBookedBy(user);
        booking.setBookingDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusHours(1));
        return BookingDto.from(bookings.save(booking));
    }
    @Transactional
public BookingDto cancelBooking(AppUser user, Long bookingId) {

    Booking booking = bookings.findById(bookingId)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Booking not found"));

    System.out.println("BOOKING COMPANY = " +
            booking.getCompany().getId());

    System.out.println("USER COMPANY = " +
            user.getCompany().getId());

    if (!booking.getCompany().getId().equals(user.getCompany().getId())) {

        System.out.println("COMPANY MISMATCH");

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Not your booking");
    }

    booking.setStatus(BookingStatus.CANCELLED);

    return BookingDto.from(bookings.save(booking));
}
    public record SlotDto(String startTime, String endTime, boolean free) {}
    public record UsageDto(String room, int capacity, int monthlyLimit, long usedThisMonth, long remainingThisMonth) {}
    public record BookingDto(Long id, String company, String room, LocalDate date, String startTime, String endTime, String status) {
        static BookingDto from(Booking b) {
            return new BookingDto(b.getId(), b.getCompany().getName(), b.getRoom().getName(), b.getBookingDate(),
                    b.getStartTime().toString(), b.getEndTime().toString(), b.getStatus().name());
        }
    }
}
