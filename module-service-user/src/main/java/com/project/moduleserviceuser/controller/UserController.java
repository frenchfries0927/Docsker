package com.project.moduleserviceuser.controller;

import com.project.moduleserviceuser.common.ApiResponse;
import com.project.moduleserviceuser.dto.JoinDTO;
import com.project.moduleserviceuser.dto.UserNameDTO;
import com.project.moduleserviceuser.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<?>> getProfile(@RequestHeader("X-User-ID") Long userId) {
        // 여기선 단순히 유저 정보 내려주는 예시로
        Map<String, String> userProfile = userService.getUserProfile(userId);

        return ResponseEntity.ok(ApiResponse.success(userProfile, "마이페이지"));
    }

//    @GetMapping("/check/nickname")
//    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
//        boolean checkedNickname = userService.checkNickname(nickname);
//        return ResponseEntity.ok(!checkedNickname);
//    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<?>> updateProfile(@RequestParam String nickname,
                                                        @RequestHeader("X-User-ID") Long userId) {
        System.out.println("요청 들어옴 닉네임 변경");
        boolean isUpdated = userService.updateNickname(userId, nickname);

        if (isUpdated) {
            Map<String, String> result = new HashMap<>();
            result.put("username", nickname);
            return ResponseEntity.ok(ApiResponse.success(result, "닉네임이 성공적으로 업데이트되었습니다."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("닉네임 업데이트에 실패했습니다."));
        }
    }
}
