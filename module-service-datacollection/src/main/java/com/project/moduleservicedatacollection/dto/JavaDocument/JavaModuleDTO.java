package com.project.moduleservicedatacollection.dto.JavaDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JavaModuleDTO {
    private String uid;
    private String name;
    private String description;
    // 상위 버전 정보 (Contents 테이블의 버전 데이터의 id)
    private String versionId;
    List<JavaPackageDTO> packages;
}
