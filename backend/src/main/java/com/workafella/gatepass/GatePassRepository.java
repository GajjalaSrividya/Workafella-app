package com.workafella.gatepass;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GatePassRepository extends JpaRepository<GatePass, Long> {
    List<GatePass> findByCompanyIdOrderByVisitingDateDescEntryTimeDesc(Long companyId);
}
