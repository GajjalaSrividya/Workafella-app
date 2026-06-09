package com.workafella.invoice;

import com.workafella.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByCompanyAndBillingMonth(Company company, LocalDate billingMonth);
    List<Invoice> findByCompanyIdOrderByBillingMonthDesc(Long companyId);
    List<Invoice> findByStatusIn(List<InvoiceStatus> statuses);
    long countByStatus(InvoiceStatus status);
}
