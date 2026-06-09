package com.workafella.gatepass;

import com.workafella.auth.AppUser;
import com.workafella.mail.MailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class GatePassService {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a");
    private final GatePassRepository passes;
    private final MailService mail;
    private final SecureRandom random = new SecureRandom();

    public GatePassService(GatePassRepository passes, MailService mail) {
        this.passes = passes;
        this.mail = mail;
    }

    public GatePassDto generate(AppUser user, String name, String email, LocalDate date, LocalTime entry, LocalTime exit) {
        if (date.isBefore(LocalDate.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gate passes are allowed only for today or future dates");
        if (!exit.isAfter(entry)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exit time must be after entry time");
        GatePass pass = new GatePass();
        pass.setCompany(user.getCompany());
        pass.setCreatedBy(user);
        pass.setVisitorName(name);
        pass.setVisitorEmail(email);
        pass.setVisitingDate(date);
        pass.setEntryTime(entry);
        pass.setExitTime(exit);
        pass.setPassCode("GP-" + date.toString().replace("-", "") + "-" + (100000 + random.nextInt(900000)));
        passes.save(pass);
        mail.send(email, "Your Workafella visitor gate pass",
                "Visitor: " + name + "\nCompany: " + user.getCompany().getName() + "\nDate: " + date +
                        "\nEntry: " + entry.format(DISPLAY_TIME) + "\nExit: " + exit.format(DISPLAY_TIME) + "\nPass code: " + pass.getPassCode());
        return GatePassDto.from(pass);
    }

    public record GatePassDto(Long id, String company, String visitorName, String visitorEmail, LocalDate visitingDate,
                              String entryTime, String exitTime, String passCode, String status) {
        static GatePassDto from(GatePass p) {
            return new GatePassDto(p.getId(), p.getCompany().getName(), p.getVisitorName(), p.getVisitorEmail(),
                    p.getVisitingDate(), p.getEntryTime().toString(), p.getExitTime().toString(), p.getPassCode(), p.getStatus().name());
        }
    }
}
