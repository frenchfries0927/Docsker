package com.project.moduleserviceuser.common;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공통 API 응답 클래스
 */
@Getter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    public enum Status { SUCCESS, FAIL, ERROR }

    private final Status status;
    private final T data;
    private final String message;

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(Status.SUCCESS, data, message);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(Status.SUCCESS, null, message);
    }

    public static ApiResponse<Map<String, String>> fail(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();

        List<ObjectError> allErrors = bindingResult.getAllErrors();
        for (ObjectError error : allErrors) {
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        }
        return new ApiResponse<>(Status.FAIL, errors, "Validation failed");
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(Status.ERROR, null, message);
    }
}