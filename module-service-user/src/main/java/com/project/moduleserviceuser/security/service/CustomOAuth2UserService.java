package com.project.moduleserviceuser.security.service;

import com.project.moduleserviceuser.dto.CustomOAuth2User;
import com.project.moduleserviceuser.dto.GoogleResponse;
import com.project.moduleserviceuser.dto.NaverResponse;
import com.project.moduleserviceuser.dto.OAuth2Response;
import com.project.moduleserviceuser.entity.User;
import com.project.moduleserviceuser.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response;

        if ("google".equals(registrationId)) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        } else if ("naver".equals(registrationId)) {
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider", "지원되지 않는 OAuth2 제공자", null));
        }

        String email = oAuth2Response.getEmail();
        String username = oAuth2Response.getProvider() + "-" + oAuth2Response.getProviderId();

        User userEntity = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    if (userRepository.existsByEmail(email)) {
                        throw new OAuth2AuthenticationException(new OAuth2Error("email_exists", "이미 가입된 이메일입니다.", null));
                    }
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUsername(username);
                    newUser.setProvider(oAuth2Response.getProvider());
                    newUser.setProviderId(oAuth2Response.getProviderId());
                    return userRepository.save(newUser); // 저장 후 반환
                });

        if (!userEntity.getProvider().equals(oAuth2Response.getProvider())) {
            userEntity.setProvider(oAuth2Response.getProvider());
            userEntity.setProviderId(oAuth2Response.getProviderId());
            userRepository.save(userEntity);
        }

        return new CustomOAuth2User(userEntity, oAuth2User.getAttributes());
    }
}
