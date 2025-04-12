package com.project.moduleservicedatacollection.dto.TodayQuestion;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.project.moduleservicedatacollection.component.ContentDTODeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonDeserialize(using = ContentDTODeserializer.class)
public class ContentResponseDTO {
    private String content;
}
