package com.project.moduleserviceuser.dto;

import lombok.Data;

@Data
public class VerifyMailRequestDTO {
    private String email;
    private String code;
}
