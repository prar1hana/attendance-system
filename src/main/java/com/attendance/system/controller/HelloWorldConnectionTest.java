package com.attendance.system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldConnectionTest {

    @GetMapping("/test")
    public String hello(){
        return "Hello, World!";
    }
}
