package com.bank.payment.user.mapper;

import com.bank.payment.user.dto.UserDto;
import com.bank.payment.user.entity.User;

public final class UserMapper {

    private UserMapper(){

    }

    //make static to direct call in streams
    public static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setKycVerified(user.isKycVerified());
        dto.setAddress(user.getAddress());
        dto.setCreatedOn(user.getCreatedOn());
        dto.setUpdatedOn(user.getUpdatedOn());
        dto.setUpdatedBy(user.getUpdatedBy());
        return dto;
    }
}
