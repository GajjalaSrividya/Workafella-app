package com.workafella.dashboard;

import com.workafella.auth.UserRepository;
import com.workafella.booking.BookingRepository;
import com.workafella.company.CompanyRepository;
import com.workafella.invoice.InvoiceRepository;
import com.workafella.invoice.InvoiceStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {
    private final CompanyRepository companies;
    private final UserRepository users;
    private final InvoiceRepository invoices;
    private final BookingRepository bookings;

    public DashboardController(CompanyRepository companies, UserRepository users, InvoiceRepository invoices, BookingRepository bookings) {
        this.companies = companies;
        this.users = users;
        this.invoices = invoices;
        this.bookings = bookings;
    }

    @GetMapping
    public AdminStats stats() {
        return new AdminStats(companies.count(), users.count(), invoices.countByStatus(InvoiceStatus.PAID),
                invoices.countByStatus(InvoiceStatus.SENT) + invoices.countByStatus(InvoiceStatus.OVERDUE), bookings.count());
    }

    public record AdminStats(long companies, long users, long paidBills, long pendingBills, long totalBookings) {}
}
