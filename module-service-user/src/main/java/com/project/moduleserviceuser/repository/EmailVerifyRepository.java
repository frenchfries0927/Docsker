package com.project.moduleserviceuser.repository;


import com.project.moduleserviceuser.entity.EmailVerifyEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface EmailVerifyRepository extends CrudRepository<EmailVerifyEntity, String> {
    Optional<EmailVerifyEntity> findByEmail(String email);
}
