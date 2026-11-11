package com.college.management.service;

import com.college.management.model.*;
import com.college.management.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users = users; }
    public AppUser get() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
    public boolean isAdmin() { return get().getRole() == Role.ADMIN; }
}
