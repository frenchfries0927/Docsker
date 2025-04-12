package com.project.moduleservicedatacollection.dto.JavaDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JavaPackageDTO {
    private String uid;
    private String name;
    private String description;
    // 상위 모듈 및 버전 정보
    private String moduleId;
    List<JavaClassDTO> classes;
}
