package com.workafella.booking;

import com.workafella.auth.AppUser;
import com.workafella.common.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final BookingService service;

    public BookingController(RoomRepository rooms, BookingRepository bookings, BookingService service) {
        this.rooms = rooms;
        this.bookings = bookings;
        this.service = service;
    }

    @GetMapping("/rooms")
    public List<RoomDto> rooms() {
        return rooms.findByActiveTrueOrderByCapacityAsc().stream().map(RoomDto::from).toList();
    }

    @GetMapping("/bookings/availability")
    public List<BookingService.SlotDto> availability(@RequestParam Long roomId, @RequestParam LocalDate date) {
        return service.availability(roomId, date);
    }

    @GetMapping("/client/bookings/usage")
    @PreAuthorize("hasRole('CLIENT')")
    public BookingService.UsageDto usage(@RequestParam Long roomId, @RequestParam LocalDate date) {
        System.out.println("USAGE API HIT");
        return service.usage(CurrentUser.get(), roomId, date);
    }

    @PostMapping("/client/bookings")
    @PreAuthorize("hasRole('CLIENT')")
    public BookingService.BookingDto book(@RequestBody BookRequest request) {
        return service.book(CurrentUser.get(), request.roomId(), request.date(), LocalTime.parse(request.startTime()));
    }

    @GetMapping("/client/bookings")
    @PreAuthorize("hasRole('CLIENT')")
    public List<BookingService.BookingDto> myBookings() {
        AppUser user = CurrentUser.get();
        return bookings.findByCompanyIdOrderByBookingDateDescStartTimeDesc(user.getCompany().getId()).stream().map(BookingService.BookingDto::from).toList();
    }

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingService.BookingDto> allBookings() {
        return bookings.findTop50ByOrderByBookingDateDescStartTimeDesc().stream().map(BookingService.BookingDto::from).toList();
    }
    @PatchMapping("/client/bookings/{id}/cancel")
@PreAuthorize("hasRole('CLIENT')")
public BookingService.BookingDto cancel(@PathVariable Long id) {
    return service.cancelBooking(CurrentUser.get(), id);
}

    public record BookRequest(Long roomId, LocalDate date, String startTime) {}
    public record RoomDto(Long id, String name, int capacity) {
        static RoomDto from(Room r) { return new RoomDto(r.getId(), r.getName(), r.getCapacity()); }
    }
}
