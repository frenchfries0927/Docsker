package com.project.moduleservicedatacollection.service.Deepl;


import com.project.moduleservicedatacollection.dto.Deepl.DeeplDTO;
import com.project.moduleservicedatacollection.dto.Deepl.DeeplResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "deeplAPI", url = "https://api-free.deepl.com/v2")
public interface DeeplAPI {

    @PostMapping(value = "/translate", consumes = MediaType.APPLICATION_JSON_VALUE)
    DeeplResponseDTO translate(@RequestBody DeeplDTO request,
                               @RequestHeader("Authorization") String authorization);
}
