package com.project.moduleservicedatacollection.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.moduleservicedatacollection.entity.ContentsEntity;
import com.project.moduleservicedatacollection.service.JavaDocument.JavaAIService;
import com.project.moduleservicedatacollection.service.JavaDocument.JavaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/java")
@RequiredArgsConstructor
public class JavaDocumentController {
    private final JavaService javaService;
    private final JavaAIService javaAIService;

    @PostMapping("/scrapeVersion")
    public ResponseEntity<ContentsEntity> saveJavaVersion(@RequestParam String url) {
        try {
            ContentsEntity versions = javaService.saveVersion(url);
            return ResponseEntity.ok(versions);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }
    }

//    @PostMapping("/scrapeModule")
//    public ResponseEntity<ContentsEntity> scrapeModule(@RequestParam String url) {
//        try {
//            ContentsEntity saved = javaService.parseAndSaveModule(url);
//            return ResponseEntity.ok(saved);
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }

    // 비동기
    @PostMapping("/scrapeModule")
    public CompletableFuture<ResponseEntity<ContentsEntity>> scrapeModule(@RequestParam String url) throws IOException {
        return javaService.parseAndSaveModule(url)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.badRequest().build());
    }

//    @PostMapping("/scrapePackage")
//    public ResponseEntity<ContentsEntity> scrapePackage(@RequestParam String url) {
//        try {
//            ContentsEntity saved = javaService.parseAndSavePackage(url);
//            javaService.packageUrls(url);
//            return ResponseEntity.ok(saved);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }

    // 비동기
    @PostMapping("/scrapePackage")
    public CompletableFuture<ResponseEntity<ContentsEntity>> scrapePackage(@RequestParam String url) throws Exception {
        return javaService.parseAndSavePackage(url)
                .thenCompose(saved -> {
                            try {
                                return javaService.packageUrls(url)  // URL 처리 후 결과가 필요하다면 여기에 적합
                                        .thenApply(v -> ResponseEntity.ok(saved));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
                .exceptionally(ex -> {
                    ex.printStackTrace();  // 예외 로깅 추가
                    return ResponseEntity.badRequest().build();
                });
    }

    // url로 1개만 받아올때
//    @PostMapping("/scrapeClass/single")
//    public ResponseEntity<ContentsEntity> scrapeClass(@RequestParam String url) {
//        try {
//            ContentsEntity saved = javaService.parseAndSaveClass(url);
//            return ResponseEntity.ok(saved);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }

//    @PostMapping("/scrapeClass")
//    public ResponseEntity<ContentsEntity> scrapeClass() {
//        try {
//            javaService.updateAllPackage();
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }

    // 비동기
    @PostMapping("/scrapeClass")
    public CompletableFuture<ResponseEntity<Object>> scrapeClass() throws Exception {
        return javaService.updateAllPackage()
                .thenApply(v -> ResponseEntity.ok().build()) // 정상 처리 시 200 OK 반환
                .exceptionally(ex -> {
                    ex.printStackTrace();  // 예외 로깅 추가
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred while processing the request"); // 서버 오류 시 메시지 반환
                });
    }


//     url로 1개만 받아올때
//    @PostMapping("/scrapeClass/single/detail")
//    public ResponseEntity<ContentsEntity> scrapeClassDetail(@RequestParam String url) {
//        try {
//            ContentsEntity saved = javaService.parseAndSaveClassDetail(url);
//            return ResponseEntity.ok(saved);
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }

//    @PostMapping("/scrapeClass/detail")
//    public ResponseEntity<ContentsEntity> scrapeClassDetail() {
//        try {
//            javaService.updateAllClassDetails();
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }

    // 비동기
    @PostMapping("/scrapeClass/detail")
    public CompletableFuture<ResponseEntity<Object>> scrapeClassDetail() {
        try {
            return javaService.updateAllClassDetails()
                    .thenApply(v -> ResponseEntity.ok().build())
                    .exceptionally(ex -> {
                        System.out.println(ex.getMessage());
                        return ResponseEntity.badRequest().build();
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Package에 포함된 class 주소 수집
    @PostMapping("/scrapePackage/urls")
    public void scrapePackageurl(@RequestParam String url) throws Exception {
        javaService.packageUrls(url);
    }

    // classDetail으로 진입하기 위한 주소 수집
    @PostMapping("/scrapeClass/urls")
    public void scrapeClassDetailurl(@RequestParam String url) throws IOException {
        javaService.classDetailUrls(url);
    }

    // package부터 정보 추출
    @PostMapping("/scrapePackage/toClass")
    public void packageToClassDetail() throws Exception {
        javaService.updateAllPackageDetails();
    }

    @PostMapping("/createMethodExample/{moduleName}")
    public String createMethodExample(@PathVariable String moduleName) throws Exception {
        javaAIService.insertMethodExampleBatch(moduleName);
        return "ok";
    }

    @PostMapping("/update-method-examples")
    public ResponseEntity<String> updateMethodExamples(@RequestParam String batchOutputFilePath,
                                                       @RequestParam String moduleName) {
        try {
            javaAIService.updateClassMethodExamples(batchOutputFilePath, moduleName);
            return ResponseEntity.ok("클래스 methodExample 업데이트 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("업데이트 실패: " + e.getMessage());
        }
    }
//    @PostMapping("/createMethodExample/allModule")
//    public String createMethodExampleAllModule() throws JsonProcessingException, InterruptedException {
//        javaAIService.insertMethodExampleForAllModules();
//        return "ok";
//    }


    @PostMapping("/update/module")
    public CompletableFuture<ResponseEntity<Object>> moduleTranslateText() {
        javaService.moduleTranslateText();
        return CompletableFuture.completedFuture(ResponseEntity.ok().build());
    }


    @PostMapping("/scrapeClass/translate")
    public CompletableFuture<String> translateClasses(@RequestParam String packageId) {
        return javaService.classTranslateText(packageId)
                .thenApplyAsync(v -> "번역 작업이 완료되었습니다."); // 완료 메시지 반환
    }
}
