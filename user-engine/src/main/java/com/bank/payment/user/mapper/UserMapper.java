package com.bank.payment.user.mapper;

import com.bank.payment.user.dto.UserDto;
import com.bank.payment.user.entity.User;

public class UserMapper {
    //make static to direct call in streams
    public static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setKycVerified(user.isKycVerified());
        dto.setAddress(user.getAddress());
        return dto;
    }
}
