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

    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("h:mm a");

    private final GatePassRepository passes;
    private final MailService mail;
    private final SecureRandom random = new SecureRandom();

    public GatePassService(GatePassRepository passes,
                           MailService mail) {
        this.passes = passes;
        this.mail = mail;
    }

    public GatePassDto generate(
            AppUser user,
            String visitorName,
            String visitorEmail,
            String hostName,
            String purpose,
            LocalDate visitingDate,
            LocalTime entryTime,
            LocalTime exitTime) {

        if (visitingDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Gate passes are allowed only for today or future dates");
        }

        if (!exitTime.isAfter(entryTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Exit time must be after entry time");
        }

        GatePass pass = new GatePass();

        pass.setCompany(user.getCompany());
        pass.setCreatedBy(user);

        pass.setVisitorName(visitorName);
        pass.setVisitorEmail(visitorEmail);

        pass.setHostName(hostName);
        pass.setPurpose(purpose);

        pass.setVisitingDate(visitingDate);
        pass.setEntryTime(entryTime);
        pass.setExitTime(exitTime);

        String visitorId =
                "VIS-" +
                visitingDate.toString().replace("-", "") +
                "-" +
                (100000 + random.nextInt(900000));

        pass.setPassCode(visitorId);

        passes.save(pass);

        String html = buildPassHtml(
                visitorId,
                visitorName,
                user.getCompany().getName(),
                hostName,
                purpose,
                visitingDate,
                entryTime,
                exitTime
        );

        mail.sendHtml(
                visitorEmail,
                "Workafella Visitor Gate Pass",
                html
        );

        return GatePassDto.from(pass);
    }

    private String buildPassHtml(
            String visitorId,
            String visitorName,
            String clientName,
            String hostName,
            String purpose,
            LocalDate date,
            LocalTime entry,
            LocalTime exit) {

        return """
                <html>
                <body style='font-family:Arial;padding:20px;background:#f4f4f4;'>

                <div style='max-width:650px;margin:auto;
                            background:white;
                            border-radius:12px;
                            padding:25px;
                            border:1px solid #ddd;'>
                 <div style='text-align:center;margin-bottom:20px;'>

    <img
        src='cid:workafellaLogo'
        alt='Workafella'
        style='width:160px;'>

</div>
                    <div style="
background:#FAC50F;
color:white;
font-size:20px;
font-weight:bold;
text-align:center;
padding:15px;
border-radius:18px;
margin-top:10px;
margin-bottom:20px;">
VISITOR PASS
</div>

                    <hr>

                    <p><b>Visitor Name:</b> %s</p>
<p><b>Client Name:</b> %s</p>
<p><b>Host Name:</b> %s</p>
<p><b>Purpose:</b> %s</p>
<p><b>Visit Date:</b> %s</p>
<p><b>Entry Time:</b> %s</p>
<p><b>Exit Time:</b> %s</p>

<hr>

<p><b>Visitor ID:</b> %s</p>

                    <br>

                    <div style='padding:15px;
                                background:#eef7ff;
                                border-radius:8px;'>

                        Please present this pass at reception
                        during your visit.

                    </div>

                </div>

                </body>
                </html>
                """
                .formatted(
                        visitorName,
                        clientName,
                        hostName,
                        purpose,
                        date,
                        entry.format(DISPLAY_TIME),
                        exit.format(DISPLAY_TIME),
                        visitorId
                );
    }

    public record GatePassDto(
            Long id,
            String company,
            String visitorName,
            String visitorEmail,
            String hostName,
            String purpose,
            LocalDate visitingDate,
            String entryTime,
            String exitTime,
            String passCode,
            String status) {

        static GatePassDto from(GatePass p) {
            return new GatePassDto(
                    p.getId(),
                    p.getCompany().getName(),
                    p.getVisitorName(),
                    p.getVisitorEmail(),
                    p.getHostName(),
                    p.getPurpose(),
                    p.getVisitingDate(),
                    p.getEntryTime().toString(),
                    p.getExitTime().toString(),
                    p.getPassCode(),
                    p.getStatus().name()
            );
        }
    }
}