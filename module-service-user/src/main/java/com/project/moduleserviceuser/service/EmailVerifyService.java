package com.project.moduleserviceuser.service;

import com.project.moduleserviceuser.dto.VerifyMailRequestDTO;
import com.project.moduleserviceuser.entity.EmailVerifyEntity;
import com.project.moduleserviceuser.repository.EmailVerifyRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EmailVerifyService {

    private final EmailService emailService;
    private final EmailVerifyRepository emailVerifyRepository;

    public String createVerifyCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        return code.toString();
    }

    public String createVerificationEmailContent(String verificationCode) {
        return "<div style='font-family: Arial, sans-serif; text-align: center;'>" +
                "<h2>📧 DevPulse 이메일 인증</h2>" +
                "<p>아래 인증 코드를 입력하여 이메일 인증을 완료하세요:</p>" +
                "<h1 style='color: #007bff;'>" + verificationCode + "</h1>" +
                "<p>이 코드는 5분 동안 유효합니다.</p>" +
                "<p>감사합니다!</p>" +
                "</div>";
    }

    public void sendEmail(String email) throws MessagingException {
        String verificationCode = createVerifyCode();
        String text = createVerificationEmailContent(verificationCode);
        EmailVerifyEntity emailVerifyEntity = new EmailVerifyEntity(email, verificationCode);
        emailVerifyRepository.save(emailVerifyEntity);
        emailService.sendEmail(email, "[DevPulse] 메일 인증 요청", text);
    }

    public boolean verifyEmail(VerifyMailRequestDTO verifyMailRequestDTO) {
        Optional<EmailVerifyEntity> email = emailVerifyRepository.findByEmail(verifyMailRequestDTO.getEmail());
        if(email.isEmpty()){
            return false;
        }

        EmailVerifyEntity emailVerifyEntity = email.get();

        return emailVerifyEntity.getCode().equals(verifyMailRequestDTO.getCode());
    }
}
