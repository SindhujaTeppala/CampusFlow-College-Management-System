package com.college.management.controller;

import com.college.management.model.*;
import com.college.management.repository.UserRepository;
import com.college.management.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min=6) String password,
                                  @NotBlank String rollNumber, String department, String phone) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var user = users.findByEmail(request.email()).orElse(null);
        if (user == null || !encoder.matches(request.password(), user.getPassword()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password"));
        return ResponseEntity.ok(Map.of("token", jwt.generate(user), "user", safe(user)));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByEmail(request.email()))
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
        if (users.existsByRollNumber(request.rollNumber()))
            return ResponseEntity.badRequest().body(Map.of("message", "Roll number already exists"));
        AppUser user = new AppUser(); user.setName(request.name()); user.setEmail(request.email());
        user.setPassword(encoder.encode(request.password())); user.setRole(Role.STUDENT);
        user.setRollNumber(request.rollNumber()); user.setDepartment(request.department()); user.setPhone(request.phone());
        users.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Student account created"));
    }

    public static Map<String,Object> safe(AppUser u) {
        return Map.of("id", u.getId(), "name", u.getName(), "email", u.getEmail(),
            "role", u.getRole().name(), "rollNumber", u.getRollNumber() == null ? "" : u.getRollNumber(),
            "department", u.getDepartment() == null ? "" : u.getDepartment());
    }
}
