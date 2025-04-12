package com.project.moduleserviceuser.dto;

import com.project.moduleserviceuser.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final User userDTO;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User userDTO, Map<String, Object> attributes) {
        this.userDTO = userDTO;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add((GrantedAuthority) () -> userDTO.getRole().getAuthority());
        return authorities;
    }

    public String getEmail() {
        return userDTO.getEmail();
    }

    @Override
    public String getName() {
        return userDTO.getUsername();
    }

    public Long getUserId() {
        return userDTO.getId();
    }

}
