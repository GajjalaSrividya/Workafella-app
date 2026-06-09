package com.workafella.company;

import com.workafella.auth.AppUser;
import com.workafella.auth.Role;
import com.workafella.auth.UserRepository;
import com.workafella.mail.MailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CompanyService {
    private final CompanyRepository companies;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    public CompanyService(CompanyRepository companies, UserRepository users, PasswordEncoder encoder, MailService mailService) {
        this.companies = companies;
        this.users = users;
        this.encoder = encoder;
        this.mailService = mailService;
    }

    @Transactional
    public CreatedCompany create(String name, String email, int seatCount, String ownerName) {
        Company company = new Company();
        company.setName(name);
        company.setEmail(email);
        company.setSeatCount(seatCount);
        companies.save(company);

        String plainPassword = generatedPassword();
        AppUser client = new AppUser();
        client.setFullName(ownerName);
        client.setEmail(email);
        client.setPasswordHash(encoder.encode(plainPassword));
        client.setRole(Role.CLIENT);
        client.setCompany(company);
        users.save(client);

        mailService.send(email, "Your Workafella client login",
                "Login email: " + email + "\nTemporary password: " + plainPassword);
        return new CreatedCompany(company.getId(), name, email, seatCount, plainPassword);
    }

    private String generatedPassword() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return "Wf@" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record CreatedCompany(Long id, String name, String email, int seatCount, String temporaryPassword) {}
}
