package com.project.moduleservicecontent.controller;

import com.project.moduleservicecontent.dto.ClassResponseDTO;
import com.project.moduleservicecontent.entity.ContentsEntity;
import com.project.moduleservicecontent.repository.ContentRepository;
import com.project.moduleservicecontent.service.JavaDocService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/javadoc")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class JavaDocController {

    private final JavaDocService javaDocService;
    private final ContentRepository contentRepository;

    /**
     * 최신 Java 버전 정보 조회
     */
    @GetMapping("/version/latest")
    public ResponseEntity<ContentsEntity> getLatestVersion() {
        log.info("최신 Java 버전 정보 조회");
        ContentsEntity version = javaDocService.getLatestVersion();
        if (version != null) {
            return ResponseEntity.ok(version);
        }
        return ResponseEntity.notFound().build();
    }

    //모든 데이터 조회
    @GetMapping("/all/content/type")
    public ResponseEntity<List<ContentsEntity>> getAllContentsByType(@RequestParam String contentType) {
        log.info("컨텐츠 타입 조회: {}", contentType);
        List<ContentsEntity> contents = javaDocService.getAllContentType(contentType);
        return ResponseEntity.ok(contents);
    }


    /**
     * 특정 UID로 버전 조회
     */
    @GetMapping("/version/{uid}")
    public ResponseEntity<ContentsEntity> getVersionByUid(@PathVariable String uid) {
        log.info("버전 정보 조회: {}", uid);
        ContentsEntity version = javaDocService.getVersionByUid(uid);
        if (version != null) {
            return ResponseEntity.ok(version);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 특정 버전에 속한 모든 모듈 조회
     */
    @GetMapping("/modules")
    public ResponseEntity<List<ContentsEntity>> getAllModulesUnderCertainVersion(@RequestParam("versionUid") String versionUid) {
        log.info("모듈 목록 조회 - 버전: {}", versionUid);
        List<ContentsEntity> modules = javaDocService.getModulesByVersionUid(versionUid);
        return ResponseEntity.ok(modules);
    }

    /**
     * 특정 UID로 모듈 조회
     */
    @GetMapping("/module/{uid}")
    public ResponseEntity<ContentsEntity> getModuleByUid(@PathVariable String uid) {
        log.info("모듈 정보 조회: {}", uid);
        ContentsEntity module = javaDocService.getModuleByUid(uid);
        if (module != null) {
            return ResponseEntity.ok(module);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 특정 모듈에 속한 모든 패키지 조회
     */
    @GetMapping("/packages")
    public ResponseEntity<List<ContentsEntity>> getAllPackagesUnderCertainModule(@RequestParam String moduleUid) {
        log.info("패키지 목록 조회 - 모듈: {}", moduleUid);
        List<ContentsEntity> packages = javaDocService.getPackagesByModuleUid(moduleUid);
        return ResponseEntity.ok(packages);
    }

    /**
     * 특정 UID로 패키지 조회
     */
    @GetMapping("/package/{uid}")
    public ResponseEntity<ContentsEntity> getPackageByUid(@PathVariable String uid) {
        log.info("패키지 정보 조회: {}", uid);
        ContentsEntity pkg = javaDocService.getPackageByUid(uid);
        if (pkg != null) {
            return ResponseEntity.ok(pkg);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 특정 패키지에 속한 모든 클래스 조회
     */
    @GetMapping("/classes")
    public ResponseEntity<List<ContentsEntity>> getAllClassesUnderCertainPackage(@RequestParam String packageUid,
                                                                                 @RequestHeader(value = "X-User-ID", required = false) Long userId) {
        log.info("클래스 목록 조회 - 패키지: {}", packageUid);
        List<ContentsEntity> classes = javaDocService.getClassesByPackageUid(packageUid);
        return ResponseEntity.ok(classes);
    }

    /**
     * 특정 UID로 클래스 조회
     */
    @GetMapping("/class/{uid}")
    public ResponseEntity<ContentsEntity> getClassByUid(@PathVariable String uid) {
        log.info("클래스 정보 조회: {}", uid);
        ContentsEntity clazz = javaDocService.getClassByUid(uid);
        if (clazz != null) {
            return ResponseEntity.ok(clazz);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 이름으로 클래스 검색
     */
    @GetMapping("/search/classes")
    public ResponseEntity<List<ContentsEntity>> searchClassesByName(@RequestParam String name) {
        log.info("클래스 검색: {}", name);
        List<ContentsEntity> classes = javaDocService.searchClassesByName(name);
        return ResponseEntity.ok(classes);
    }

    /**
     * 정확한 이름으로 클래스 찾기
     */
    @GetMapping("/find/class")
    public ResponseEntity<ContentsEntity> findCertainClass(@RequestParam String name) {
        log.info("정확한 이름으로 클래스 검색: {}", name);
        ContentsEntity clazz = javaDocService.findClassByExactName(name);
        if (clazz != null) {
            return ResponseEntity.ok(clazz);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 이름으로 패키지 검색
     */
    @GetMapping("/search/packages")
    public ResponseEntity<List<ContentsEntity>> searchPackagesByName(@RequestParam String name) {
        log.info("패키지 검색: {}", name);
        List<ContentsEntity> packages = javaDocService.searchPackagesByName(name);
        return ResponseEntity.ok(packages);
    }

    /**
     * 타입으로 클래스 조회
     */
    @GetMapping("/classes/type/{type}")
    public ResponseEntity<List<ContentsEntity>> getClassesByType(@PathVariable String type) {
        log.info("타입별 클래스 목록 조회: {}", type);
        List<ContentsEntity> classes = javaDocService.getClassesByType(type);
        return ResponseEntity.ok(classes);
    }

    /**
     * 클래스의 계층 구조 조회
     */
    @GetMapping("/hierarchy")
    public ResponseEntity<Map<String, Object>> getClassHierarchy(@RequestParam String classUid) {
        log.info("클래스 계층 구조 조회: {}", classUid);
        Map<String, ContentsEntity> hierarchy = javaDocService.getClassHierarchy(classUid);

        if (hierarchy.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        hierarchy.forEach((key, value) -> {
            result.put(key, javaDocService.jsonToMap(value.getContentDetail()));
        });

        return ResponseEntity.ok(result);
    }

    /**
k     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> search(@RequestParam String query) {
        log.info("종합 검색 수행: {}", query);
        List<ContentsEntity> classes = javaDocService.searchClassesByName(query);
        List<ContentsEntity> packages = javaDocService.searchPackagesByName(query);

        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        result.put("classes", classes.stream()
                .map(c -> javaDocService.jsonToMap(c.getContentDetail()))
                .collect(Collectors.toList()));

        result.put("packages", packages.stream()
                .map(p -> javaDocService.jsonToMap(p.getContentDetail()))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(result);
    }

    /**
     * 조회수 증가 - class
     */
    @PostMapping("/view/{id}")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long id) {
        javaDocService.increaseViewCount(id);
        return ResponseEntity.ok().build(); // 성공만 내려줌
    }

    @GetMapping("/test/repository")
    public ResponseEntity<Map<String, Object>> testRepository() {
        log.info("Repository 테스트");

        // 테스트 1: 최신 버전 직접 조회
        ContentsEntity latestVersion = contentRepository.findLatestVersion();

        // 테스트 2: 특정 UID로 버전 조회
        Optional<ContentsEntity> versionByUid = contentRepository.findByContentTypeAndUid(
                "version", "13060ec5-4166-429c-ab88-83f0f1f88bd9");

        // 결과 수집
        Map<String, Object> results = new HashMap<>();
        results.put("latestVersionExists", latestVersion != null);
        if (latestVersion != null) {
            results.put("latestVersionDetail", latestVersion.getContentDetail());
        }

        results.put("versionByUidExists", versionByUid.isPresent());
        versionByUid.ifPresent(v -> results.put("versionByUidDetail", v.getContentDetail()));

        return ResponseEntity.ok(results);
    }

// 검색을 위해 전체 class 데이터 받아오는 api
    @GetMapping("/classes/test/class")
    public ResponseEntity<List<ClassResponseDTO>> getAllClassesUnderCertainPackageTest(@RequestHeader(value = "X-User-ID", required = false) Long userId) {
        List<ClassResponseDTO> classes = javaDocService.getClassesTest(userId);
        log.info("📌 [Controller] 조회된 클래스 수: {}", classes.size());
        return ResponseEntity.ok(classes);
    }

    /**
     * 특정 패키지에 속한 모든 클래스 조회
     */
//    @GetMapping("/classes/test/{classUid}")
//    public ResponseEntity<List<ClassResponseDTO>> getAllClassesUnderCertainPackageTest(@PathVariable String classUid,
//                                                                                 @RequestHeader(value = "X-User-ID", required = false) Long userId) {
//        userId= 11L;
//        log.info("클래스 목록 조회 테스트 - 패키지 : {}", classUid);
//        List<ClassResponseDTO> classes = javaDocService.getClassesByPackageUidTest(classUid, userId);
//        log.info("📌 [Controller] 조회된 클래스 수: {}", classes.size());
//        return ResponseEntity.ok(classes);
//    }
    /**
     * 특정 UID로 클래스 조회
     */
    @GetMapping("/class/test/{uid}")
    public ResponseEntity<ClassResponseDTO> getClassByUidTest(@PathVariable String uid,
                                                              @RequestHeader(value = "X-User-ID", required = false) Long userId) {
        log.info("클래스 정보 조회: {}", uid);
        ClassResponseDTO clazz = javaDocService.getClassByUidTest(uid, userId);
        if (clazz != null) {
            return ResponseEntity.ok(clazz);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/popular")
    public ResponseEntity<List<ContentsEntity>> getPopularClasses() {
        log.info("인기 클래스 목록 조회");
        List<ContentsEntity> popularClasses = javaDocService.getPopularClasses();
        return ResponseEntity.ok(popularClasses);
    }
}