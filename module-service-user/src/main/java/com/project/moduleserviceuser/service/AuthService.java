package com.project.moduleserviceuser.service;

import com.project.moduleserviceuser.dto.CustomUserDetails;
import com.project.moduleserviceuser.dto.JoinDTO;
import com.project.moduleserviceuser.dto.LoginDTO;
import com.project.moduleserviceuser.entity.RefreshToken;
import com.project.moduleserviceuser.entity.User;
import com.project.moduleserviceuser.enums.UserRole;
import com.project.moduleserviceuser.exception.UserAlreadyExistsException;
import com.project.moduleserviceuser.repository.RefreshTokenRepository;
import com.project.moduleserviceuser.repository.UserRepository;
import com.project.moduleserviceuser.security.jwt.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    /*
    * 회원가입 처리
    * */
    public void joinProcess(JoinDTO joinDTO) {
        String username = joinDTO.getUsername();
        String password = joinDTO.getPassword();
        String email = joinDTO.getEmail();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("이미 가입된 이메일입니다.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("이미 사용중인 닉네입입니다.");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(bCryptPasswordEncoder.encode(password));

        userRepository.save(user);
    }

    /**
     * 로그인 처리 (Access Token & Refresh Token 발급)
     */
    public boolean login(LoginDTO loginDTO, HttpServletResponse response) {
        System.out.println("로그인 요청 서비스");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
            );
            System.out.println("에러2");
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();
            String email = userDetails.getEmail();
            String roleString = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""); // ✅ "ROLE_USER" -> "USER" 변환
            UserRole role = UserRole.valueOf(roleString);
            System.out.println("에러3");
            // access & refresh 토큰 만들기
            String accessToken = jwtUtil.createAccessToken(userId, email, role);
            String refreshToken = jwtUtil.createRefreshToken(userId, email, role);
            System.out.println("에러4");
            // 기존 refresh 삭제하기
            refreshTokenRepository.findById(refreshToken).ifPresent(refreshTokenRepository::delete);
            System.out.println("에러5");
            // refresh 토큰 새로 제작후 저장
            try {
                refreshTokenRepository.save(RefreshToken.create(refreshToken, 86400000L));
                System.out.println("✅ [User Service] Refresh Token 저장 완료!"); // 🔥 이 로그가 찍히는지 확인
            } catch (Exception e) {
                System.out.println("❌ [User Service] Refresh Token 저장 중 오류 발생: " + e.getMessage());
                e.printStackTrace(); // 자세한 오류 출력
            }

            // ✅ 응답에 JWT 토큰 추가
            response.setHeader("Authorization", "Bearer " + accessToken);
            response.addCookie(createCookie("refresh", refreshToken));
            System.out.println("✅ [User Service] 응답 헤더 설정 완료!");

            return true;
        } catch (AuthenticationException e) {
            System.out.println("에러발생");
            return false;
        }
    }

    /**
     * 로그아웃 처리 (Refresh Token 삭제)
     */
    public boolean logout(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("로그아웃 요청 들어옴");
        String refreshToken = getRefreshTokenFromCookie(request);
        System.out.println("refreshToken = " + refreshToken);
        if (refreshToken == null || !refreshTokenRepository.existsById(refreshToken)) {
            System.out.println("이거 실행되나?");
            return false;
        }

        refreshTokenRepository.deleteById(refreshToken);
        System.out.println("삭제 완료");
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return true;
    }

    /**
     * Access Token 갱신 (Refresh Token Rotation 방식)
     */
    public boolean reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        System.out.println("리이슈 요청 들어옴, refreshToken: " + refreshToken);
        if (refreshToken == null) return false;

        try {
            if (jwtUtil.isTokenExpired(refreshToken)) {
                System.out.println("❌ 리프레시 토큰 만료됨");
                return false;
            }
            System.out.println("리프레시 토큰 유효");
            System.out.println("DB에 리프레시 토큰 존재 여부 체크");
            if (!refreshTokenRepository.existsById(refreshToken)) {
                System.out.println("❌ DB에 리프레시 토큰 없음");
                return false;
            }
            System.out.println("DB에 리프레시 토큰 존재");

            Long userId = jwtUtil.getUserId(refreshToken);
            String email = jwtUtil.getEmail(refreshToken);
            UserRole role = jwtUtil.getRole(refreshToken);

            String newAccessToken = jwtUtil.createAccessToken(userId, email, role);
            String newRefreshToken = jwtUtil.createRefreshToken(userId, email, role);

            refreshTokenRepository.deleteById(refreshToken);
            refreshTokenRepository.save(RefreshToken.create(newRefreshToken, 86400000L));

            response.setHeader("Authorization", "Bearer " + newAccessToken);
            response.addCookie(createCookie("refresh", newRefreshToken));
            System.out.println("리이슈 새롭게 만듬");
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    /**
     * 쿠키에서 Refresh Token 추출
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * HttpOnly Refresh Token 쿠키 생성
     */
    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}
