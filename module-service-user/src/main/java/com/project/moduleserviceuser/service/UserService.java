package com.project.moduleserviceuser.service;

import com.project.moduleserviceuser.dto.JoinDTO;
import com.project.moduleserviceuser.entity.User;
import com.project.moduleserviceuser.exception.UserAlreadyExistsException;
import com.project.moduleserviceuser.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;


    public Map<String, String> getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        Map<String, String> userProfile = new HashMap<>();
        userProfile.put("username", user.getUsername());
        userProfile.put("email", user.getEmail());

        return userProfile;
    }

    public boolean updateNickname(Long id, String newNickname) {
        Optional<User> userOpt = userRepository.findById(id);
        boolean checkNickname = userRepository.findByUsername(newNickname).isPresent();

        String regex = "^[A-Za-z0-9가-힣]{3,8}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(newNickname);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("닉네임은 3~8글자 사이여야 하며 공백을 포함할 수 없습니다.");
        }
        if (checkNickname) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(newNickname);
            userRepository.save(user);
            return true;
        }
        return false;
    }

}
