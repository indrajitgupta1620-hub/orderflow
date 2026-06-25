package com.orderflow.dto;

public class AuthResponse {
    private String token;
    private String role;
    private String username;
    private Long userId;

    public AuthResponse() {}

    public AuthResponse(String token, String role, String username, Long userId) {
        this.token = token;
        this.role = role;
        this.username = username;
        this.userId = userId;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
