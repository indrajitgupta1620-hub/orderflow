package com.orderflow.mapper;

import com.orderflow.dto.UserProfileResponse;
import com.orderflow.model.User;

public class UserMapper {

    public static UserProfileResponse toProfileResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhone(),
                user.getCreatedAt()
        );
    }
}
