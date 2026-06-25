package com.orderflow.service;

import com.orderflow.dto.UserProfileResponse;
import com.orderflow.dto.UserUpdateRequest;
import com.orderflow.exception.ResourceNotFoundException;
import com.orderflow.mapper.UserMapper;
import com.orderflow.model.User;
import com.orderflow.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public UserProfileResponse getMe() {
        User currentUser = authService.getCurrentUser();
        return UserMapper.toProfileResponse(currentUser);
    }

    @Transactional
    public UserProfileResponse updateMe(UserUpdateRequest request) {
        User currentUser = authService.getCurrentUser();
        currentUser.setUsername(request.getUsername());
        currentUser.setPhone(request.getPhone());
        User saved = userRepository.save(currentUser);
        return UserMapper.toProfileResponse(saved);
    }

    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserMapper::toProfileResponse);
    }

    @Transactional
    public UserProfileResponse updateUserRole(Long id, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        User.Role role;
        try {
            role = User.Role.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
        }
        
        user.setRole(role);
        User saved = userRepository.save(user);
        return UserMapper.toProfileResponse(saved);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setIsActive(false);
        userRepository.save(user);
    }
}
