package com.workafella.gatepass;

import com.workafella.common.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class GatePassController {
    private final GatePassRepository passes;
    private final GatePassService service;

    public GatePassController(GatePassRepository passes, GatePassService service) {
        this.passes = passes;
        this.service = service;
    }

    @PostMapping("/client/gatepasses")
    @PreAuthorize("hasRole('CLIENT')")
    public GatePassService.GatePassDto generate(@RequestBody GatePassRequest request) {
        return service.generate(CurrentUser.get(), request.visitorName(), request.visitorEmail(),
                request.visitingDate(), LocalTime.parse(request.entryTime()), LocalTime.parse(request.exitTime()));
    }

   @GetMapping("/client/gatepasses")
@PreAuthorize("hasRole('CLIENT')")
public List<GatePassService.GatePassDto> mine() {

    System.out.println("GATEPASS API HIT");

    var data = passes.findByCompanyIdOrderByVisitingDateDescEntryTimeDesc(
            CurrentUser.get().getCompany().getId());

    System.out.println("ROWS FOUND = " + data.size());

    return data.stream()
            .map(GatePassService.GatePassDto::from)
            .toList();
}

    @GetMapping("/admin/gatepasses")
    @PreAuthorize("hasRole('ADMIN')")
    public List<GatePassService.GatePassDto> all() {
        return passes.findAll().stream().map(GatePassService.GatePassDto::from).toList();
    }

    public record GatePassRequest(String visitorName, String visitorEmail, LocalDate visitingDate, String entryTime, String exitTime) {}
}
