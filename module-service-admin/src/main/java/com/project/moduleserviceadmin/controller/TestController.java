package com.project.moduleserviceadmin.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public String testGet() {
        return "GET 요청이 성공적으로 처리되었습니다!";
    }

    @PostMapping
    public String testPost(@RequestBody(required = false) Object data) {
        return "POST 요청이 성공적으로 처리되었습니다!";
    }
}