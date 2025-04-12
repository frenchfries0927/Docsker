package com.project.moduleservicedatacollection.dto.TodayQuestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HFContentRequestDTO {
    private List<MessageDTO> messages;
    private String model;
    private double temperature = 0.7;
    private int max_tokens = 8096;
    private boolean stream = false;
}
