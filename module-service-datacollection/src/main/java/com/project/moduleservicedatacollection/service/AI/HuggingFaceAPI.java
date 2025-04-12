package com.project.moduleservicedatacollection.service.AI;

import com.project.moduleservicedatacollection.dto.TodayQuestion.ContentResponseDTO;
import com.project.moduleservicedatacollection.dto.TodayQuestion.HFContentRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "HuggingFace", url = "https://current-presently-mammal.ngrok-free.app/v1/chat/completions")
public interface HuggingFaceAPI {
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ContentResponseDTO getHFChat(@RequestBody HFContentRequestDTO hfContentRequestDTO,
                                         @RequestHeader("Content-type") String contentType);
}