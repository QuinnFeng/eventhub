package com.eventhub.userservice.dto;


import com.eventhub.userservice.entity.UserRole;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

	private Long id;
    private String name;
    private String email;
    private UserRole role;
}