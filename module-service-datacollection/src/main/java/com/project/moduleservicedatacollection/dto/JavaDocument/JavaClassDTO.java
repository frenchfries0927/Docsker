package com.project.moduleservicedatacollection.dto.JavaDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JavaClassDTO {
    private String uid;
    private String name;
    private String type; // class, interface, 등
    private String description;
    // 디테일 페이지에서 추후에 추가되는 정보
    private String descriptionFull;
    private List<JavaClassMethodDTO> methods;
    private List<JavaConstructorDetailDTO> constructors;
    // 상위 패키지 및 버전 정보
    private String packageId;

    public JavaClassDTO(String uid) {
        this.uid = uid;
    }

}
