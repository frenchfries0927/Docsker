package com.project.moduleservicedatacollection.service.AI;

import com.project.moduleservicedatacollection.dto.TodayQuestion.GroqContentRequestDTO;
import com.project.moduleservicedatacollection.dto.TodayQuestion.ContentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "Groq", url = "https://api.groq.com/openai/v1/chat/completions")
public interface GroqAPI {
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ContentResponseDTO getGroqChat(@RequestBody GroqContentRequestDTO groqContentRequestDTO,
                                         @RequestHeader("Authorization") String token,
                                         @RequestHeader("Content-type") String contentType);
}
