package com.project.moduleservicedatacollection.dto.TodayQuestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroqContentRequestDTO {
    private List<MessageDTO> messages;
    private String model;
    private double temperature = 0.6;
    private int max_completion_tokens = 10000;
    private double top_p = 0.95;
    private boolean stream = false;
    private Object stop = null;
}
