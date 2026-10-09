package com.eventhub.userservice.exception;

public class UserNotFoundByEmailException extends RuntimeException {

    public UserNotFoundByEmailException(String email) {
        super("User not found with email: " + email);
    }
}