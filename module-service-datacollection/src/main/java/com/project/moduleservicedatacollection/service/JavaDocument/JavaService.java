package com.project.moduleservicedatacollection.service.JavaDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.moduleservicedatacollection.entity.ContentsEntity;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface JavaService {
    public ContentsEntity saveVersion(String url) throws JsonProcessingException;
    public CompletableFuture<ContentsEntity> parseAndSaveModule(String url) throws IOException;
    public CompletableFuture<ContentsEntity> parseAndSavePackage(String url) throws Exception;
    public CompletableFuture<ContentsEntity> parseAndSaveClass(String url) throws Exception;
//    public CompletableFuture<ContentsEntity> parseAndSaveClassDetail(String url) throws Exception;
    public CompletableFuture<ContentsEntity> parseAndSaveClassDetail(String url) throws Exception;
    public CompletableFuture<ResponseEntity<Object>> updateAllPackage() throws Exception;
//    public void updateAllClassDetails() throws Exception;
    public CompletableFuture<Void> updateAllClassDetails() throws Exception;
    public void updateAllPackageDetails() throws Exception;
    public CompletableFuture<Void> packageUrls(String url) throws Exception;
    public void classDetailUrls(String url)throws IOException;
    public CompletableFuture<Void> moduleTranslateText();
    public CompletableFuture<Void> packageTranslateText();
    public CompletableFuture<Void> classTranslateText(String packageUid);
}