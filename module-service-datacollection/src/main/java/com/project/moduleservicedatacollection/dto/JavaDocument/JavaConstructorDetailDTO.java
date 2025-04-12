package com.project.moduleservicedatacollection.dto.JavaDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JavaConstructorDetailDTO {
    private String signature;
    private String description;
    private List<String> parameters;
    private List<String> throwsList;
}