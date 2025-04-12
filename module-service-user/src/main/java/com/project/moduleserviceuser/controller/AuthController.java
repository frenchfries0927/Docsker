package com.project.moduleserviceuser.controller;

import com.project.moduleserviceuser.common.ApiResponse;
import com.project.moduleserviceuser.dto.JoinDTO;
import com.project.moduleserviceuser.dto.LoginDTO;
import com.project.moduleserviceuser.dto.UserNameDTO;
import com.project.moduleserviceuser.service.AuthService;
import com.project.moduleserviceuser.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    /**
     * 회원가입 API
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<?>> joinProcess(@Valid @RequestBody JoinDTO joinDTO, BindingResult bindingResult) {
        // ✅ 유효성 검사 실패 시, FAIL 응답 반환
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(bindingResult));
        }
        // 닉네임 중복 검사
//        if (userService.checkNickname(joinDTO.getUsername())) {
//            return ResponseEntity.badRequest().body(ApiResponse.fail("이미 사용 중인 닉네임입니다."));
//        }do

        System.out.println("joinDTO = " + joinDTO.getEmail() + " " + joinDTO.getPassword() + " " + joinDTO.getUsername());
        authService.joinProcess(joinDTO);

        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    /**
     * Login API
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        System.out.println("🔹 로그인 요청 수신: " + loginDTO.getEmail());
        boolean success = authService.login(loginDTO, response);
        if (!success) {
            System.out.println("이거 에러");
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid email or password"));
        }
        return ResponseEntity.ok(ApiResponse.success("Login successful"));
    }

    /**
     * Logout API
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request, HttpServletResponse response) {
        boolean success = authService.logout(request, response);
        if (!success) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or missing refresh token"));
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    /**
     * Access Token 갱신 API (Refresh Token Rotation 적용)
     */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<?>> reissueToken(HttpServletRequest request, HttpServletResponse response) {
        boolean success = authService.reissueAccessToken(request, response);
        if (!success) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid or expired refresh token"));
        }
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully"));
    }
}
