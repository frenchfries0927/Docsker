package com.project.moduleserviceuser.repository;

import com.project.moduleserviceuser.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    boolean existsByRefreshToken(String refreshToken); // KEY가 refreshToken
}
