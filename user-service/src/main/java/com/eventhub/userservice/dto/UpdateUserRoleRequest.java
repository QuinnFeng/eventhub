package com.eventhub.userservice.dto;

import com.eventhub.userservice.entity.UserRole;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull
    private UserRole role;
}