package com.orderflow.controller;

import com.orderflow.dto.UserProfileResponse;
import com.orderflow.dto.UserRoleUpdateRequest;
import com.orderflow.dto.UserUpdateRequest;
import com.orderflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe() {
        UserProfileResponse profile = userService.getMe();
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(@Valid @RequestBody UserUpdateRequest request) {
        UserProfileResponse profile = userService.updateMe(request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfileResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        
        UserProfileResponse profile = userService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}
