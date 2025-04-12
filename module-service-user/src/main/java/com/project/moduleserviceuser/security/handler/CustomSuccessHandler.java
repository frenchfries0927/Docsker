package com.project.moduleserviceuser.security.handler;

import com.project.moduleserviceuser.dto.CustomOAuth2User;
import com.project.moduleserviceuser.entity.RefreshToken;
import com.project.moduleserviceuser.enums.UserRole;
import com.project.moduleserviceuser.repository.RefreshTokenRepository;
import com.project.moduleserviceuser.security.jwt.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Component
@AllArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = customUserDetails.getUserId();
        String email = customUserDetails.getEmail();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority authority = iterator.next();

        String roleString = authority.getAuthority().replace("ROLE_", "");
        UserRole role = UserRole.valueOf(roleString);

        String access = jwtUtil.createAccessToken(userId, email, role);
        String refresh = jwtUtil.createRefreshToken(userId, email, role);
        System.out.println("'refresh' = " + refresh);
        // Refresh 토큰 저장
        refreshRepository.findById(refresh).ifPresent(refreshRepository::delete);
        refreshRepository.save(RefreshToken.create(refresh, 86400000L));
        // 응답 설정
        response.setHeader("Authorization", "Bearer " + access);
        response.addCookie(createCookie("refresh", refresh));
        response.setStatus(HttpStatus.OK.value());
//        response.addHeader(HttpHeaders.SET_COOKIE,
//                        createCookie("refresh",refresh).toString());
//        System.out.println("'refresh' = " + refresh);
        response.sendRedirect("http://127.0.0.1:3000/oauth-success?accessToken=Bearer " + access);
    }

    private Cookie createCookie(String key, String value){
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        cookie.setMaxAge(60*60*60);
        cookie.setHttpOnly(true);
        return cookie;
    }
}
