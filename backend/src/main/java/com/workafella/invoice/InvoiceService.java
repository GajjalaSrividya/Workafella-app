package com.workafella.invoice;

import com.workafella.company.Company;
import com.workafella.company.CompanyRepository;
import com.workafella.mail.MailService;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {
    public static final BigDecimal AMOUNT_PER_SEAT = BigDecimal.valueOf(11000);
    private final CompanyRepository companies;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final MailService mail;

    public InvoiceService(CompanyRepository companies, InvoiceRepository invoices, PaymentRepository payments, MailService mail) {
        this.companies = companies;
        this.invoices = invoices;
        this.payments = payments;
        this.mail = mail;
    }

    @Transactional
    public InvoiceDto generate(Long companyId, LocalDate billingMonth, LocalDate dueDate, boolean sendNow) {
        Company company = companies.findById(companyId).orElseThrow();
        LocalDate month = billingMonth.withDayOfMonth(1);
        invoices.findByCompanyAndBillingMonth(company, month)
                .ifPresent(i -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice already exists for this month"); });
        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setInvoiceNumber("WF-" + company.getId() + "-" + month.format(DateTimeFormatter.ofPattern("yyyyMM")));
        invoice.setBillingMonth(month);
        invoice.setSeatCount(company.getSeatCount());
        invoice.setAmountPerSeat(AMOUNT_PER_SEAT);
        invoice.setTotalAmount(AMOUNT_PER_SEAT.multiply(BigDecimal.valueOf(company.getSeatCount())));
        invoice.setDueDate(dueDate);
        invoices.save(invoice);
        if (sendNow) send(invoice);
        return InvoiceDto.from(invoice);
    }

    @Transactional
    public InvoiceDto sendInvoice(Long id) {
        Invoice invoice = invoices.findById(id).orElseThrow();
        send(invoice);
        return InvoiceDto.from(invoice);
    }

    @Transactional
    public InvoiceDto markPaid(Long id, BigDecimal amount, String reference) {
        Invoice invoice = invoices.findById(id).orElseThrow();
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setReference(reference);
        payments.save(payment);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        return InvoiceDto.from(invoice);
    }

    private void send(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(LocalDateTime.now());
        mail.send(invoice.getCompany().getEmail(), "Workafella invoice " + invoice.getInvoiceNumber(),
                "Amount due: INR " + invoice.getTotalAmount() + "\nDue date: " + invoice.getDueDate());
    }

    @Scheduled(cron = "0 0 9 1 * *")
    public void monthlyReminder() {
        invoices.findByStatusIn(List.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE)).forEach(invoice ->
                mail.send(invoice.getCompany().getEmail(), "Workafella payment reminder",
                        "Reminder: invoice " + invoice.getInvoiceNumber() + " is due on " + invoice.getDueDate()));
    }

    public record InvoiceDto(Long id, String company, String companyEmail, String invoiceNumber, LocalDate billingMonth, int seatCount,
                             BigDecimal amountPerSeat, BigDecimal totalAmount, LocalDate dueDate, String status, LocalDateTime paidAt) {
        static InvoiceDto from(Invoice i) {
            return new InvoiceDto(i.getId(), i.getCompany().getName(), i.getCompany().getEmail(), i.getInvoiceNumber(), i.getBillingMonth(),
                    i.getSeatCount(), i.getAmountPerSeat(), i.getTotalAmount(), i.getDueDate(), i.getStatus().name(), i.getPaidAt());
        }
    }
}
