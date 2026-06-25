package com.orderflow.dto;

import jakarta.validation.constraints.NotBlank;

public class UserRoleUpdateRequest {

    @NotBlank(message = "Role is required")
    private String role; // CUSTOMER, STAFF, ADMIN

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
