package com.workafella.invoice;

import com.workafella.common.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InvoiceController {
    private final InvoiceRepository invoices;
    private final InvoiceService service;

    public InvoiceController(InvoiceRepository invoices, InvoiceService service) {
        this.invoices = invoices;
        this.service = service;
    }

    @PostMapping("/admin/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceService.InvoiceDto generate(@RequestBody GenerateInvoiceRequest request) {
        return service.generate(request.companyId(), request.billingMonth(), request.dueDate(), request.sendNow());
    }

    @PostMapping("/admin/invoices/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceService.InvoiceDto send(@PathVariable Long id) {
        return service.sendInvoice(id);
    }

    @PostMapping("/admin/invoices/{id}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceService.InvoiceDto paid(@PathVariable Long id, @RequestBody MarkPaidRequest request) {
        return service.markPaid(id, request.amount(), request.reference());
    }

    @GetMapping("/admin/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceService.InvoiceDto> all() {
        return invoices.findAll().stream().map(InvoiceService.InvoiceDto::from).toList();
    }

    @GetMapping("/client/invoices")
    @PreAuthorize("hasRole('CLIENT')")
    public List<InvoiceService.InvoiceDto> mine() {
        return invoices.findByCompanyIdOrderByBillingMonthDesc(CurrentUser.get().getCompany().getId())
                .stream().map(InvoiceService.InvoiceDto::from).toList();
    }

    public record GenerateInvoiceRequest(Long companyId, LocalDate billingMonth, LocalDate dueDate, boolean sendNow) {}
    public record MarkPaidRequest(BigDecimal amount, String reference) {}
}
