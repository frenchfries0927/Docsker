package com.project.moduleservicedatacollection.dto.JavaDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JavaClassMethodDTO {
    private String name;
    private String method;
    private String description;
    private String methodExample;
}