package com.project.moduleserviceuser.controller;

import com.project.moduleserviceuser.dto.EmailRequestDTO;
import com.project.moduleserviceuser.dto.VerifyMailRequestDTO;
import com.project.moduleserviceuser.service.EmailVerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/*
 * 보완할 점
 * 1. 현재 인증 코드를 DB에 저장하기 때문에 유효시간을 5분으로 하여 발급중임
 *    이후 Redis 적용시 Redis 내부의 TTL 기능을 사용하면 자동으로 키 만료 체크하고
 *    만료된 키는 자동으로 삭제 해준다고 함. 따라서 해당 기능 적용 예정.
 *
 * 2. 만약 이미 발급된 코드가 있고 5분 내에 같은 이메일로 재요청이 들어온다면 어떻게 처리할지 고민해야함
 *    Case1. 새로 발급하고 기존 발급 코드 제거 => 악의적으로 계속 요청할 수 있다고 생각함
 *    Case2. 5분 뒤에 다시 요청하라고 Alert => 위와 같은 행위를 막을 수 있지만 만약 사용자가 실수로 회원가입 페이지를 나갔다 들어온 경우라면?
 *
 * 3. 비밀번호 찾기 시 해당 메일 사용 할 예정
 *    어떻게 하지 흠....
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
public class MailController {
    private final EmailVerifyService emailVerifyService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMail(@RequestBody EmailRequestDTO emailRequestDTO) {
        Map<String, Object> response = new HashMap<>();

        if (emailRequestDTO.getEmail() == null) {
            response.put("success", false);
            response.put("message", "이메일을 입력하세요.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            emailVerifyService.sendEmail(emailRequestDTO.getEmail());

            response.put("success", true);
            response.put("message", "email 전송 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이메일 전송 실패");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyMail(@RequestBody VerifyMailRequestDTO verifyMailRequestDTO) {
        Map<String, Object> response = new HashMap<>();
        boolean isVerify = emailVerifyService.verifyEmail(verifyMailRequestDTO);

        if (!isVerify) {
            response.put("success", false);
            response.put("message", "인증 실패");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("message", "인증 성공!");
        return ResponseEntity.ok(response);
    }
}
