package com.project.moduleservicedatacollection.dto.JavaDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JavaVersionDTO {
    private String uid;
    private String version;
    List<JavaModuleDTO> modules;
}
