package com.workafella.company;

import com.workafella.auth.AppUser;
import com.workafella.auth.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/companies")
@PreAuthorize("hasRole('ADMIN')")
public class CompanyController {
    private final CompanyRepository companies;
    private final UserRepository users;
    private final CompanyService service;

    public CompanyController(CompanyRepository companies, UserRepository users, CompanyService service) {
        this.companies = companies;
        this.users = users;
        this.service = service;
    }

    @GetMapping
    public List<CompanyDto> all() {
        return companies.findAll().stream().map(CompanyDto::from).toList();
    }

    @PostMapping
    public CompanyService.CreatedCompany create(@RequestBody CreateCompanyRequest request) {
        return service.create(request.name(), request.email(), request.seatCount(), request.ownerName());
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return this.users.findAll().stream().map(UserDto::from).toList();
    }

    public record CreateCompanyRequest(@NotBlank String name, @Email String email, @Min(1) int seatCount, @NotBlank String ownerName) {}
    public record CompanyDto(Long id, String name, String email, int seatCount, boolean active) {
        static CompanyDto from(Company c) { return new CompanyDto(c.getId(), c.getName(), c.getEmail(), c.getSeatCount(), c.isActive()); }
    }
    public record UserDto(Long id, String fullName, String email, String role, Long companyId) {
        static UserDto from(AppUser u) {
            return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.getCompany() == null ? null : u.getCompany().getId());
        }
    }
}
