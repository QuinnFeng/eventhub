package com.eventhub.userservice.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
	@GetMapping("/test")
    public String test() {
        return "User Service is running on Java 25!";
    }
}
