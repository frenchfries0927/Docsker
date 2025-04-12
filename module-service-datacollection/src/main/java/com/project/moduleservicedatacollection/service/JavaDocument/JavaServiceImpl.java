package com.project.moduleservicedatacollection.service.JavaDocument;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.moduleservicedatacollection.dto.JavaDocument.*;
import com.project.moduleservicedatacollection.entity.ContentsEntity;
import com.project.moduleservicedatacollection.repository.ContentsRepository;
import com.project.moduleservicedatacollection.service.Deepl.DeeplAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavaServiceImpl implements JavaService {
    private final DeeplAPIService deeplAPIService;
    private final ContentsRepository contentsRepository;
    private final ObjectMapper objectMapper;

    private List<String> packageUrls = new ArrayList<>();
    private List<String> classDetailUrls = new ArrayList<>();

    @Override
    public ContentsEntity saveVersion(String url) throws JsonProcessingException {
        // version == 17
        String version = url.split("/")[6];

        // create uuid
        String uuid = UUID.randomUUID().toString();

        JavaVersionDTO javaVersionDTO = new JavaVersionDTO();
        javaVersionDTO.setUid(uuid);
        javaVersionDTO.setVersion(version);

        ContentsEntity entity = new ContentsEntity();
        entity.setContentType("version");
        String json = objectMapper.writeValueAsString(javaVersionDTO);
        entity.setContentDetail(json);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setViews(0);
        return contentsRepository.save(entity);
    }

    @Async
    @Override
    public CompletableFuture<ContentsEntity> parseAndSaveModule(String url) throws IOException {
        // 기존 코드 내용
        ContentsEntity contentsEntity = new ContentsEntity();
        String version = url.split("/")[6];

        contentsEntity = contentsRepository.findByContentTypeAndVersion(version);
        JavaVersionDTO javaVersionDTO = objectMapper.readValue(contentsEntity.getContentDetail(), JavaVersionDTO.class);

        String versionUid = javaVersionDTO.getUid();

        Document doc = Jsoup.connect(url).get();

        Elements modules = doc.select("div#all-modules-table");
        Elements javaSE = modules.select("div.all-modules-table-tab1");
        Elements modulesName = javaSE.select("div.col-first:not(.table-header)");
        Elements modulesDesc = javaSE.select("div.col-last:not(.table-header)");

        List<JavaModuleDTO> javaModuleDTOList = new ArrayList<>();
        List<String> moduleIDs = new ArrayList<>();

        List<String> DescTrans = new ArrayList<>();
        for (Element module : modulesDesc) {
            DescTrans.add(module.text());
        }
        // DescTrans = deeplAPIService.translateText(DescTrans);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < modulesName.size(); i++) {
            int finalI = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String moduleUid = UUID.randomUUID().toString();
                JavaModuleDTO moduleDTO = new JavaModuleDTO();
                moduleDTO.setUid(moduleUid);
                moduleIDs.add(moduleUid);
                moduleDTO.setName(modulesName.get(finalI).text());
                moduleDTO.setDescription(DescTrans.get(finalI));
                moduleDTO.setVersionId(versionUid);
                javaModuleDTOList.add(moduleDTO);
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // JavaModuleDTO를 JSON으로 변환
        List<String> moduleContentJsons = javaModuleDTOList.stream().map(javaModuleDTO -> {
            try {
                return objectMapper.writeValueAsString(javaModuleDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        List<ContentsEntity> entities = new ArrayList<>();
        for (String moduleContentJson : moduleContentJsons) {
            ContentsEntity entity = new ContentsEntity();
            entity.setContentType("module");
            entity.setContentDetail(moduleContentJson);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setViews(0);
            entities.add(entity);
        }
        contentsRepository.saveAll(entities);

        // version 정보가 포함된 module entity 의 하위 package id list 저장
        Map<String, Object> contentDetailMap = objectMapper.readValue(
                contentsEntity.getContentDetail(),
                new TypeReference<>() {}
        );

        contentDetailMap.put("modules", moduleIDs);
        String updatedContentDetail = objectMapper.writeValueAsString(contentDetailMap);
        contentsEntity.setContentDetail(updatedContentDetail);
        contentsEntity.setUpdatedAt(LocalDateTime.now());

        // 결과를 CompletableFuture로 감싸서 반환
        return CompletableFuture.completedFuture(contentsRepository.save(contentsEntity));
    }

    @Async
    @Override
    public CompletableFuture<ContentsEntity> parseAndSavePackage(String url) throws Exception {
        String moduleName = url.split("/")[9];

        ContentsEntity entities = contentsRepository.findByContentTypeAndModuleName(moduleName);
        JavaModuleDTO javaModuleDTO = objectMapper.readValue(entities.getContentDetail(), JavaModuleDTO.class);

        String moduleUid = javaModuleDTO.getUid();

        Document doc = Jsoup.connect(url).get();

        Elements packageHTML = doc.select("div.summary-table.two-column-summary");
        Elements packageName = packageHTML.select("div.col-first:not(.table-header)");
        Elements packageDesc = packageHTML.select("div.col-last:not(.table-header)");

        List<JavaPackageDTO> javaPackageDTOList = new ArrayList<>();
        List<String> packageIDs = new ArrayList<>();

        List<String> DescTrans = new ArrayList<>();
        for (Element module : packageDesc) {
            DescTrans.add(module.text());
        }
        // DescTrans = deeplAPIService.translateText(DescTrans);

        // 비동기적으로 JavaPackageDTO 객체 생성
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < packageName.size(); i++) {
            int finalI = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String packageUid = UUID.randomUUID().toString();
                JavaPackageDTO javaPackageDTO = new JavaPackageDTO();
                javaPackageDTO.setUid(packageUid);
                packageIDs.add(packageUid);
                javaPackageDTO.setName(packageName.get(finalI).text());
                javaPackageDTO.setDescription(DescTrans.get(finalI));
                javaPackageDTO.setModuleId(moduleUid);
                javaPackageDTOList.add(javaPackageDTO);
            });
            futures.add(future);
        }

        // 모든 비동기 작업이 끝날 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // JavaPackageDTO를 JSON으로 변환
        List<String> packageContentJsons = javaPackageDTOList.stream().map(javaPackageDTO -> {
            try {
                return objectMapper.writeValueAsString(javaPackageDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        // 비동기적으로 contentsRepository에 저장 처리
        List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
        for (String packageContentJson : packageContentJsons) {
            CompletableFuture<Void> saveFuture = CompletableFuture.runAsync(() -> {
                ContentsEntity entity = new ContentsEntity();
                entity.setContentType("package");
                entity.setContentDetail(packageContentJson);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setViews(0);
                contentsRepository.save(entity);
            });
            saveFutures.add(saveFuture);
        }

        // 저장 작업이 모두 끝날 때까지 기다림
        CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0])).join();

        // version 정보가 포함된 module entity 의 하위 package id list 저장
        Map<String, Object> contentDetailMap = objectMapper.readValue(
                entities.getContentDetail(),
                new TypeReference<>() {}
        );

        contentDetailMap.put("packages", packageIDs);
        String updatedContentDetail = objectMapper.writeValueAsString(contentDetailMap);
        entities.setContentDetail(updatedContentDetail);
        entities.setUpdatedAt(LocalDateTime.now());

        // 비동기적으로 최종 업데이트 작업 수행
        return CompletableFuture.completedFuture(contentsRepository.save(entities));
    }

    @Async
    @Override
    public CompletableFuture<ContentsEntity> parseAndSaveClass(String url) throws Exception {

        String version = url.split("/")[6];

        Document doc = Jsoup.connect(url).get();

        Elements classHTML = doc.select("div#class-summary div.summary-table.two-column-summary");
        Elements className = classHTML.select("div.col-first:not(.table-header)");
        Elements classDesc = classHTML.select("div.col-last:not(.table-header)");
        className.select("sup").remove();

        String moduleName = doc.select("div.sub-title a").text();
        String packageName = doc.select("span.element-name").text();

        // 패키지 이름을 주소에서 split으로 가져올수 없어서 페이지에서 탐색 해야해서 아래로 내렸습니다.
        ContentsEntity entities = contentsRepository.findByContentTypeAndPackageName(packageName);
        JavaPackageDTO javaPackageDTO = objectMapper.readValue(entities.getContentDetail(), JavaPackageDTO.class);
        String packageUid = javaPackageDTO.getUid();

        List<JavaClassDTO> javaClassDTOList = new ArrayList<>();
        List<String> classIDs = new ArrayList<>();

        List<String> DescTrans = new ArrayList<>();
        for (Element module : classDesc) {
            DescTrans.add(module.text());
        }
        // DescTrans = deeplAPIService.translateText(DescTrans);

        // 비동기적으로 JavaClassDTO 객체 생성
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < className.size(); i++) {
            int finalI = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String classUrl = "https://docs.oracle.com/en/java/javase/"
                        + version
                        + "/docs/api/"
                        + moduleName + "/"                                   // java.base
                        + packageName.replace(".","/") + "/"   // java/io
                        + className.get(finalI).text().split("<")[0]           // BufferedInputStream
                        + ".html";
                classDetailUrls.add(classUrl);

                String type = "";
                if (className.get(finalI).hasClass("class-summary-tab1")){
                    type = "Interfaces";
                } else if (className.get(finalI).hasClass("class-summary-tab2")) {
                    type = "Classes";
                } else if (className.get(finalI).hasClass("class-summary-tab3")) {
                    type = "Enum Classes";
                } else if (className.get(finalI).hasClass("class-summary-tab5")) {
                    type = "Exceptions";
                } else if (className.get(finalI).hasClass("class-summary-tab6")) {
                    type = "Errors";
                } else if (className.get(finalI).hasClass("class-summary-tab7")) {
                    type = "Annotation Interfaces";
                }

                String classUid = UUID.randomUUID().toString();
                JavaClassDTO javaClassDTO = new JavaClassDTO();
                javaClassDTO.setUid(classUid);
                classIDs.add(classUid);
                javaClassDTO.setType(type);
                javaClassDTO.setName(className.get(finalI).text());
                javaClassDTO.setDescription(DescTrans.get(finalI));
                javaClassDTO.setPackageId(packageUid);
                javaClassDTOList.add(javaClassDTO);
            });
            futures.add(future);
        }

        // 모든 비동기 작업이 끝날 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // JavaClassDTO를 JSON으로 변환
        List<String> classContentJsons = javaClassDTOList.stream().map(javaClassDTO -> {
            try {
                return objectMapper.writeValueAsString(javaClassDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        // 비동기적으로 contentsRepository에 저장 처리
        List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
        for (String classContentJson : classContentJsons) {
            CompletableFuture<Void> saveFuture = CompletableFuture.runAsync(() -> {
                ContentsEntity entity = new ContentsEntity();
                entity.setContentType("class");
                entity.setContentDetail(classContentJson);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setViews(0);
                contentsRepository.save(entity);
            });
            saveFutures.add(saveFuture);
        }

        // 저장 작업이 모두 끝날 때까지 기다림
        CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0])).join();

        // version 정보가 포함된 package entity 의 하위 class id list 저장
        Map<String, Object> contentDetailMap = objectMapper.readValue(
                entities.getContentDetail(),
                new TypeReference<>() {}
        );

        contentDetailMap.put("classes", classIDs);
        String updatedContentDetail = objectMapper.writeValueAsString(contentDetailMap);
        entities.setContentDetail(updatedContentDetail);
        entities.setUpdatedAt(LocalDateTime.now());

        // 비동기적으로 최종 업데이트 작업 수행
        return CompletableFuture.completedFuture(contentsRepository.save(entities));
    }

//    public ContentsEntity parseAndSaveClassDetail(String url) throws Exception {
//
//        Document doc = Jsoup.connect(url).get();
//        Elements classDesc = doc.select("section.class-description div.block");
//
//        List<String> classDescTrans = new ArrayList<>();
//        for (Element module : classDesc) {
//            classDescTrans.add(module.text());
//        }
////        classDescTrans = deeplAPIService.translateText(classDescTrans);
//
//        // 클래스 이름
//        String className = doc.select("div.type-signature span.element-name.type-name-label:not(.annotations)").text();
//
//        System.out.println(className);
//
//
//        // 메소드
//        Elements methods = doc.select("div#method-summary-table");
//        Elements namesM = methods.select("div.col-first:not(.table-header)");
//        Elements methodsM = methods.select("div.col-second:not(.table-header)");
//        Elements describesM = methods.select("div.col-last:not(.table-header)");
//
//        List<String> methodDescTrans = new ArrayList<>();
//
//        for (Element module : describesM) {
//            methodDescTrans.add(module.text());
//        }
//
////        methodDescTrans = deeplAPIService.translateText(methodDescTrans);
//
//        List<JavaClassMethodDTO> methodDTOList = new ArrayList<>();
//        if (methodDescTrans.size()!=0){
//            for (int i = 0; i < namesM.size(); i++) {
//                JavaClassMethodDTO methodDTO = new JavaClassMethodDTO(namesM.get(i).text(),methodsM.get(i).text(),methodDescTrans.get(i), null);
//                methodDTOList.add(methodDTO);
//            }
//        }
//
//
//        // 생성자
//        Elements constructor = doc.select("section.constructor-details");
//        Elements constDetail = constructor.select("section.detail");
//        Elements constSignature = constDetail.select("div.member-signature");
//        Elements constDescT = constDetail.select("div.block, div.deprecation-block");
//
//        List<String> constDescTrans = new ArrayList<>();
//        for (Element module : constDescT) {
//            constDescTrans.add(module.text());
//        }
//
////        constDescTrans = deeplAPIService.translateText(constDescTrans);
//
//        // Constructor Detail을 저장할 리스트
//        List<JavaConstructorDetailDTO> constDTOList = new ArrayList<>();
//        if (constDescTrans.size()!=0){
//            int n = 0;
//            for (Element constD : constDetail) {
//                JavaConstructorDetailDTO detailDTO = new JavaConstructorDetailDTO();
//                if (constD.hasClass("div.member-signature")){
//                    detailDTO.setSignature(constSignature.next().text());
//                }
//                if (constD.hasClass("div.deprecation-block")){
//                    detailDTO.setDescription(constDescTrans.get(n));
//                    n++;
//                }
//
//
//                Elements dtTags = constD.select("dt");
//
//                if (dtTags.size()>0) {
//                    List<String> parameters = new ArrayList<>();
//                    List<String> throwsList = new ArrayList<>();
//
//                    String currentCategory = "";
//
//                    // dtTags를 순회하며 "Parameters"와 "Throws"를 분리하여 처리
//                    for (Element dt : dtTags) {
//                        String key = dt.text().trim();
//
//                        // "Parameters" 또는 "Throws"를 기준으로 구분
//                        if (key.startsWith("Parameters")) {
//                            currentCategory = "Parameters";
//                        } else if (key.startsWith("Throws")) {
//                            currentCategory = "Throws";
//                        } else {
//                            currentCategory = ""; // 우리가 원하는 키워드가 아닐 경우 초기화
//                        }
//
//                        Element nextDd = dt.nextElementSibling(); // dt 태그의 다음 형제 태그 가져오기
//                        while (nextDd != null && nextDd.tagName().equals("dd")) {
//                            String value = nextDd.text().trim();
//
//                            // "Parameters"와 "Throws"에 맞는 리스트에 값을 추가
//                            if (currentCategory.equals("Parameters")) {
//                                parameters.add(value);
//                            } else if (currentCategory.equals("Throws")) {
//                                throwsList.add(value);
//                            }
//
//                            nextDd = nextDd.nextElementSibling(); // 다음 형제 태그로 이동
//                        }
//                    }
//                    detailDTO.setParameters(parameters);
//                    detailDTO.setThrowsList(throwsList);
//                } else {
//                    detailDTO.setParameters(new ArrayList<>());
//                    detailDTO.setThrowsList(new ArrayList<>());
//                }
//
//                constDTOList.add(detailDTO);
//
//            }
//        }
//
//
////        // detail을 추가할 class
//        String packageName = doc.select("div.sub-title a[href='package-summary.html']").text();
//        System.out.println(packageName);
//        ContentsEntity packageEntity = contentsRepository.findByContentTypeAndPackageName(packageName);
//        // packageName을 기반으로 패키지 엔티티 조회
//        JavaPackageDTO contentDetail = objectMapper.readValue(packageEntity.getContentDetail(), JavaPackageDTO.class);
//        String packageId = contentDetail.getUid();
//        ContentsEntity entities = contentsRepository.findByContentTypeAndClassNameAndPackageId(className,packageId);
//
//        // version 정보가 포함된 module entity 의 하위 package id list 저장
//        Map<String, Object> contentDetailMap = objectMapper.readValue(
//                entities.getContentDetail(),
//                new TypeReference<>() {
//                }
//        );
//
//
//        contentDetailMap.put("methods", methodDTOList);
//        contentDetailMap.put("constructors", constDTOList);
//        if (classDescTrans.size()>0){
//            contentDetailMap.put("descriptionFull", classDescTrans.get(0));
//        } else {
//            contentDetailMap.put("descriptionFull", "");
//        }
//        String updatedContentDetail = objectMapper.writeValueAsString(contentDetailMap);
//        entities.setContentDetail(updatedContentDetail);
//        entities.setUpdatedAt(LocalDateTime.now());
//        System.out.println(entities);
//        return contentsRepository.save(entities);
//    }

    @Async
    @Override
    public CompletableFuture<ContentsEntity> parseAndSaveClassDetail(String url) throws Exception {

        Document doc = Jsoup.connect(url).get();
        Elements classDesc = doc.select("section.class-description div.block");

        List<String> classDescTrans = new ArrayList<>();
        for (Element module : classDesc) {
            classDescTrans.add(module.text());
        }
//    classDescTrans = deeplAPIService.translateText(classDescTrans);

        // 클래스 이름
        String className = doc.select("div.type-signature span.element-name.type-name-label:not(.annotations)").text();

        // 메소드
        Elements methods = doc.select("div#method-summary-table");
        Elements namesM = methods.select("div.col-first:not(.table-header)");
        Elements methodsM = methods.select("div.col-second:not(.table-header)");
        Elements describesM = methods.select("div.col-last:not(.table-header)");

        List<String> methodDescTrans = new ArrayList<>();

        for (Element module : describesM) {
            methodDescTrans.add(module.text());
        }

//    methodDescTrans = deeplAPIService.translateText(methodDescTrans);

        List<JavaClassMethodDTO> methodDTOList = new ArrayList<>();
        if (methodDescTrans.size() != 0) {
            // 비동기적으로 methodDTO 객체 생성
            List<CompletableFuture<Void>> methodFutures = new ArrayList<>();
            for (int i = 0; i < namesM.size(); i++) {
                int finalI = i;
                CompletableFuture<Void> methodFuture = CompletableFuture.runAsync(() -> {
                    JavaClassMethodDTO methodDTO = new JavaClassMethodDTO(namesM.get(finalI).text(), methodsM.get(finalI).text(), methodDescTrans.get(finalI), null);
                    methodDTOList.add(methodDTO);
                });
                methodFutures.add(methodFuture);
            }

            // 모든 비동기 작업이 완료될 때까지 기다림
            CompletableFuture.allOf(methodFutures.toArray(new CompletableFuture[0])).join();
        }

        // 생성자
        Elements constructor = doc.select("section.constructor-details");
        Elements constDetail = constructor.select("section.detail");  // size 2
        Elements constSignature = constDetail.select("div.member-signature"); // size 2
        Elements constDescT = constDetail.select("div.block"); // size 2

        List<String> constDescTrans = new ArrayList<>();
        for (Element module : constDescT) {
            constDescTrans.add(module.text());
        }

//    constDescTrans = deeplAPIService.translateText(constDescTrans);

        // Constructor Detail을 저장할 리스트
        List<JavaConstructorDetailDTO> constDTOList = new ArrayList<>();
        if (constDescTrans.size() != 0) {
            // 비동기적으로 Constructor DTO 객체 생성
            List<CompletableFuture<Void>> constFutures = new ArrayList<>();
            for (int i = 0; i < constDetail.size(); i++) {
                int finalI = i;
                CompletableFuture<Void> constFuture = CompletableFuture.runAsync(() -> {
                    JavaConstructorDetailDTO detailDTO = new JavaConstructorDetailDTO();
                    detailDTO.setSignature(constSignature.get(finalI).text());
                    detailDTO.setDescription(constDescTrans.get(finalI));

                    Elements dtTags = constDetail.get(finalI).select("dt");

                    List<String> parameters = new ArrayList<>();
                    List<String> throwsList = new ArrayList<>();

                    String currentCategory = "";

                    // dtTags를 순회하며 "Parameters"와 "Throws"를 분리하여 처리
                    for (Element dt : dtTags) {
                        String key = dt.text().trim();

                        // "Parameters" 또는 "Throws"를 기준으로 구분
                        if (key.startsWith("Parameters")) {
                            currentCategory = "Parameters";
                        } else if (key.startsWith("Throws")) {
                            currentCategory = "Throws";
                        } else {
                            currentCategory = ""; // 우리가 원하는 키워드가 아닐 경우 초기화
                        }

                        Element nextDd = dt.nextElementSibling(); // dt 태그의 다음 형제 태그 가져오기
                        while (nextDd != null && nextDd.tagName().equals("dd")) {
                            String value = nextDd.text().trim();

                            // "Parameters"와 "Throws"에 맞는 리스트에 값을 추가
                            if (currentCategory.equals("Parameters")) {
                                parameters.add(value);
                            } else if (currentCategory.equals("Throws")) {
                                throwsList.add(value);
                            }

                            nextDd = nextDd.nextElementSibling(); // 다음 형제 태그로 이동
                        }
                    }
                    detailDTO.setParameters(parameters);
                    detailDTO.setThrowsList(throwsList);
                    constDTOList.add(detailDTO);
                });
                constFutures.add(constFuture);
            }

            // 모든 비동기 작업이 완료될 때까지 기다림
            CompletableFuture.allOf(constFutures.toArray(new CompletableFuture[0])).join();
        }

        // detail을 추가할 class
        String packageName = doc.select("div.sub-title a[href='package-summary.html']").text();
        System.out.println(packageName);
        ContentsEntity packageEntity = contentsRepository.findByContentTypeAndPackageName(packageName);
        // packageName을 기반으로 패키지 엔티티 조회
        JavaPackageDTO contentDetail = objectMapper.readValue(packageEntity.getContentDetail(), JavaPackageDTO.class);
        String packageId = contentDetail.getUid();
        ContentsEntity entities = contentsRepository.findByContentTypeAndClassNameAndPackageId(className, packageId);

        // version 정보가 포함된 module entity 의 하위 package id list 저장
        Map<String, Object> contentDetailMap = objectMapper.readValue(
                entities.getContentDetail(),
                new TypeReference<>() {}
        );

        contentDetailMap.put("methods", methodDTOList);
        contentDetailMap.put("constructors", constDTOList);
        contentDetailMap.put("descriptionFull", classDescTrans.get(0));
        String updatedContentDetail = objectMapper.writeValueAsString(contentDetailMap);
        entities.setContentDetail(updatedContentDetail);
        entities.setUpdatedAt(LocalDateTime.now());

        // 비동기적으로 최종 업데이트 작업 수행
        return CompletableFuture.completedFuture(contentsRepository.save(entities));
    }

//    @Override
//    public void updateAllPackage() throws Exception {
//        // 1개만 번역하기 위해선 i를 1로 변경해야 함
//        for (int i = 0; i < packageUrls.size(); i++) {
//            parseAndSaveClass(packageUrls.get(i));
//        }
//    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> updateAllPackage() throws Exception {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < packageUrls.size(); i++) {
            int finalI = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    parseAndSaveClass(packageUrls.get(finalI));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 모든 비동기 작업이 완료될 때까지 기다리고 완료 메시지를 반환
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> ResponseEntity.ok().body("All packages updated successfully"));
    }

//    @Override
//    public void updateAllClassDetails() throws Exception {
//        for (int i = 0; i < classDetailUrls.size(); i++) {
//            parseAndSaveClassDetail(classDetailUrls.get(i));
//        }
//        classDetailUrls.clear();
//    }


//    @Override
//    public void updateAllClassDetails() throws Exception {
//        for (int i = 0; i < classDetailUrls.size(); i++) {
//            parseAndSaveClassDetail(classDetailUrls.get(i));
//        }
//        classDetailUrls.clear();
//    }

    @Async
    @Override
    public CompletableFuture<Void> updateAllClassDetails() throws Exception {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < classDetailUrls.size(); i++) {
            // 비동기적으로 parseAndSaveClassDetail 호출
            int finalI = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    parseAndSaveClassDetail(classDetailUrls.get(finalI));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 모든 비동기 작업이 완료될 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        classDetailUrls.clear();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void updateAllPackageDetails() throws Exception {
        for (int i = 0; i < packageUrls.size(); i++) {
            parseAndSaveClass(packageUrls.get(i));
            updateAllClassDetails();
            classDetailUrls.clear();
        }

    }

    // package 하위의 클래스리스트를 확인할수 있는 주소
    @Override
    public CompletableFuture<Void> packageUrls(String url) {
        return CompletableFuture.runAsync(() -> {
            try {
                packageUrls.clear();
                Document doc = Jsoup.connect(url).get();

                String moduleName = doc.select("div.module-signature span.element-name").text();
                Elements packageHTML = doc.select("div.summary-table.two-column-summary");
                Elements packageNames = packageHTML.select("div.col-first:not(.table-header)");

                for (Element packageName : packageNames) {
                    String version = url.split("/")[6];

                    String classUrl = "https://docs.oracle.com/en/java/javase/"
                            + version
                            + "/docs/api/"
                            + moduleName + "/"                                   // java.base
                            + packageName.text().replace(".", "/") + "/"   // java/io
                            + "package-summary.html";
                    packageUrls.add(classUrl);
                }
                System.out.println(packageUrls);
            } catch (Exception e) {
                e.printStackTrace();  // 예외 로깅
            }
        });
    }


    // 클래스 디테일 주소 리스트를 만들기 위해
    @Override
    public void classDetailUrls(String url) throws IOException {
        classDetailUrls.clear();
        Document doc = Jsoup.connect(url).get();

        String version = url.split("/")[6];

        Elements classHTML = doc.select("div#class-summary div.summary-table.two-column-summary");
        Elements className = classHTML.select("div.col-first:not(.table-header)");
        className.select("sup").remove();

        String moduleName = doc.select("div.sub-title a").text();
        String packageName = doc.select("span.element-name").text();

        for (int i = 0; i < className.size(); i++) {

            String classDetailUrl = "https://docs.oracle.com/en/java/javase/"
                    + version
                    + "/docs/api/"
                    + moduleName + "/"                                       // java.base
                    + packageName.replace(".", "/") + "/"   // java/io
                    + className.get(i).text().split("<")[0]            // BufferedInputStream
                    + ".html";
            classDetailUrls.add(classDetailUrl);
        }
    }


    @Async
    @Override
    public CompletableFuture<Void> moduleTranslateText() {
        List<ContentsEntity> contentsEntities = contentsRepository.findAllModules();

        // uid -> 원본 description 매핑
        Map<String, String> descriptionMap = new LinkedHashMap<>();
        // uid -> Entity 매핑
        Map<String, ContentsEntity> entityMap = new HashMap<>();

        for (ContentsEntity entity : contentsEntities) {
            try {
                JsonNode jsonNode = objectMapper.readTree(entity.getContentDetail());
                String uid = jsonNode.get("uid").textValue();
                String description = jsonNode.get("description").asText();

                descriptionMap.put(uid, description);
                entityMap.put(uid, entity);
            } catch (Exception e) {
                log.error("JSON 파싱 오류: {}", e.getMessage(), e);
            }
        }

        return CompletableFuture.supplyAsync(() -> deeplAPIService.translateText(new ArrayList<>(descriptionMap.values())))
                .thenAcceptAsync(translated -> {
                    List<ContentsEntity> updatedEntities = new ArrayList<>();
                    List<String> uidList = new ArrayList<>(descriptionMap.keySet());

                    int translateSize = translated.size();

                    for (int i = 0; i < translateSize; i++) {
                        String uid = uidList.get(i);
                        String translatedDescription = translated.get(i);
                        ContentsEntity entity = entityMap.get(uid);

                        if (entity == null) {
                            log.warn("uid [{}]에 해당하는 엔티티를 찾을 수 없습니다.", uid);
                            continue;
                        }

                        try {
                            JsonNode jsonNode = objectMapper.readTree(entity.getContentDetail());
                            ((ObjectNode) jsonNode).put("description", translatedDescription);
                            entity.setContentDetail(objectMapper.writeValueAsString(jsonNode));
                            entity.setUpdatedAt(LocalDateTime.now());
                            updatedEntities.add(entity);
                        } catch (Exception e) {
                            log.error("번역 결과 업데이트 중 오류 발생 (uid: {}): {}", uid, e.getMessage(), e);
                        }
                    }
                    contentsRepository.saveAll(updatedEntities);
                });
    }


    @Async
    @Override
    public CompletableFuture<Void> packageTranslateText() {
        List<ContentsEntity> contentsEntities = contentsRepository.findAllPackages();

        // uid -> 원본 description 매핑
        Map<String, String> descriptionMap = new LinkedHashMap<>();
        // uid -> Entity 매핑
        Map<String, ContentsEntity> entityMap = new HashMap<>();

        for (ContentsEntity entity : contentsEntities) {
            try {
                JsonNode jsonNode = objectMapper.readTree(entity.getContentDetail());
                String uid = jsonNode.get("uid").textValue();
                String description = jsonNode.get("description").asText();

                descriptionMap.put(uid, description);
                entityMap.put(uid, entity);
            } catch (Exception e) {
                log.error("JSON 파싱 오류: {}", e.getMessage(), e);
            }
        }

        return CompletableFuture.supplyAsync(() -> deeplAPIService.translateText(new ArrayList<>(descriptionMap.values())))
                .thenAcceptAsync(translated -> {
                    List<ContentsEntity> updatedEntities = new ArrayList<>();
                    List<String> uidList = new ArrayList<>(descriptionMap.keySet());

                    int translateSize = translated.size();

                    for (int i = 0; i < translateSize; i++) {
                        String uid = uidList.get(i);
                        String translatedDescription = translated.get(i);
                        ContentsEntity entity = entityMap.get(uid);

                        if (entity == null) {
                            log.warn("uid [{}]에 해당하는 엔티티를 찾을 수 없습니다.", uid);
                            continue;
                        }

                        try {
                            JsonNode jsonNode = objectMapper.readTree(entity.getContentDetail());
                            ((ObjectNode) jsonNode).put("description", translatedDescription);
                            entity.setContentDetail(objectMapper.writeValueAsString(jsonNode));
                            entity.setUpdatedAt(LocalDateTime.now());
                            updatedEntities.add(entity);
                        } catch (Exception e) {
                            log.error("번역 결과 업데이트 중 오류 발생 (uid: {}): {}", uid, e.getMessage(), e);
                        }
                    }
                    contentsRepository.saveAll(updatedEntities);
                });
    }

//    @Async
//    @Override
//    public CompletableFuture<Void> classTranslateText(String packageUid) {
//        List<ContentsEntity> classEntities = contentsRepository.findClassesByPackageUid(packageUid);
//
//        Map<String, List<String>> descriptionMap = new LinkedHashMap<>();
//        Map<String, ContentsEntity> entityMap = new HashMap<>();
//        Map<String, List<Integer>> translationIndexMap = new HashMap<>();
//
//        int globalIndex = 0; // 번역 리스트 전체에서의 인덱스
//
//        for (ContentsEntity entity : classEntities) {
//            try {
//                JsonNode jsonNode = objectMapper.readTree(entity.getContentDetail());
//                String uid = jsonNode.get("uid").textValue();
//
//                List<String> descriptions = new ArrayList<>();
//                List<Integer> indexList = new ArrayList<>();
//
//                if (jsonNode.has("description")) {
//                    descriptions.add(jsonNode.get("description").asText());
//                    indexList.add(globalIndex++);
//                }
//                if (jsonNode.has("descriptionFull")) {
//                    descriptions.add(jsonNode.get("descriptionFull").asText());
//                    indexList.add(globalIndex++);
//                }
//
//                // methods 배열의 각 method의 description을 추가
//                if (jsonNode.has("methods")) {
//                    for (JsonNode methodNode : jsonNode.get("methods")) {
//                        if (methodNode.has("description")) {
//                            descriptions.add(methodNode.get("description").asText());
//                            indexList.add(globalIndex++);
//                        }
//                    }
//                }
//
//                if (!descriptions.isEmpty()) {
//                    descriptionMap.put(uid, descriptions);
//                    translationIndexMap.put(uid, indexList);
//                }
//                entityMap.put(uid, entity);
//            } catch (Exception e) {
//                log.error("JSON 파싱 오류: {}", e.getMessage(), e);
//            }
//        }
//
//        return CompletableFuture.supplyAsync(() ->
//                deeplAPIService.translateText(descriptionMap.values().stream().flatMap(List::stream).collect(Collectors.toList()))
//        ).thenAcceptAsync(translated -> {
//            List<ContentsEntity> updatedEntities = new ArrayList<>();
//
//            for (String uid : descriptionMap.keySet()) {
//                List<String> descriptions = descriptionMap.get(uid);
//                List<Integer> indexes = translationIndexMap.get(uid);
//                ContentsEntity entity = entityMap.get(uid);
//
//                if (entity == null) {
//                    log.warn("uid [{}]에 해당하는 엔티티를 찾을 수 없습니다.", uid);
//                    continue;
//                }
//
//                try {
//                    JsonNode jsonNode = objectMapper.readTree(entity.getContentDetail());
//
//                    if (descriptions.size() > 0) {
//                        ((ObjectNode) jsonNode).put("description", translated.get(indexes.get(0)));
//                    }
//                    if (descriptions.size() > 1) {
//                        ((ObjectNode) jsonNode).put("descriptionFull", translated.get(indexes.get(1)));
//                    }
//
//                    if (jsonNode.has("methods")) {
//                        int methodIndex = 2; // description, descriptionFull 다음부터 methods 인덱스 시작
//                        for (JsonNode methodNode : jsonNode.get("methods")) {
//                            if (methodNode.has("description") && methodIndex < indexes.size()) {
//                                ((ObjectNode) methodNode).put("description", translated.get(indexes.get(methodIndex++)));
//                            }
//                        }
//                    }
//
//                    entity.setContentDetail(objectMapper.writeValueAsString(jsonNode));
//                    entity.setUpdatedAt(LocalDateTime.now());
//                    updatedEntities.add(entity);
//                } catch (Exception e) {
//                    log.error("번역 결과 업데이트 중 오류 발생 (uid: {}): {}", uid, e.getMessage(), e);
//                }
//            }
//
//            contentsRepository.saveAll(updatedEntities);
//        });
//    }

    @Async
    @Override
    public CompletableFuture<Void> classTranslateText(String packageId) {
        List<ContentsEntity> classEntities = contentsRepository.findClassesByPackageUid(packageId);

        List<ContentsEntity> updatedEntities = new ArrayList<>();

        for (ContentsEntity entity : classEntities) {
            try {
                JavaClassDTO javaClassDTO = objectMapper.readValue(entity.getContentDetail(), JavaClassDTO.class);

                List<String> descriptions = new ArrayList<>();
                List<JsonPointer> jsonPointers = new ArrayList<>();

                descriptions.add(javaClassDTO.getDescription());
                jsonPointers.add(JsonPointer.compile("/description"));

                descriptions.add(javaClassDTO.getDescriptionFull());
                jsonPointers.add(JsonPointer.compile("/descriptionFull"));

                if (javaClassDTO.getMethods() != null) {
                    int index = 0;
                    for (JavaClassMethodDTO method : javaClassDTO.getMethods()) {
                        if (method.getDescription() != null) {
                            descriptions.add(method.getDescription());
                            jsonPointers.add(JsonPointer.compile("/methods/" + index + "/description"));
                        }
                        index++;
                    }
                }

                if (javaClassDTO.getConstructors() != null) {
                    int index = 0;
                    for (JavaConstructorDetailDTO constructor : javaClassDTO.getConstructors()) {
                        if (constructor.getDescription() != null) {
                            descriptions.add(constructor.getDescription());
                            jsonPointers.add(JsonPointer.compile("/constructors/" + index + "/description"));
                        }
                        index++;
                    }
                }

                List<String> translated = deeplAPIService.translateText(descriptions);
                if (translated == null) {
                    System.out.println("번역 API 호출 실패");
                    continue;
                }

                for (int i = 0; i < translated.size(); i++) {
                    JsonPointer pointer = jsonPointers.get(i);
                    String pointerString = pointer.toString();

                    if (pointerString.equals("/description")) {
                        javaClassDTO.setDescription(translated.get(i));
                    } else if (pointerString.equals("/descriptionFull")) {
                        javaClassDTO.setDescriptionFull(translated.get(i));
                    } else if (pointerString.startsWith("/methods/")) {
                        int index = Integer.parseInt(pointerString.split("/")[2]);
                        JavaClassMethodDTO method = javaClassDTO.getMethods().get(index);
                        method.setDescription(translated.get(i));
                    } else if (pointerString.startsWith("/constructors/")) {
                        int index = Integer.parseInt(pointerString.split("/")[2]);
                        JavaConstructorDetailDTO constructor = javaClassDTO.getConstructors().get(index);
                        constructor.setDescription(translated.get(i));
                    }
                }

                entity.setContentDetail(objectMapper.writeValueAsString(javaClassDTO));
                entity.setUpdatedAt(LocalDateTime.now());
                updatedEntities.add(entity);

            } catch (Exception e) {
                System.out.println("번역 중 오류 발생: " + e.getMessage());
            }
        }

        return CompletableFuture.runAsync(() -> {
            if (!updatedEntities.isEmpty()) {
                contentsRepository.saveAll(updatedEntities);
            }
        });
    }


//    @Async
//    @Override
//    public CompletableFuture<Void> classTranslateText(String packageUid) {
//        List<ContentsEntity> classEntities = contentsRepository.findClassesByPackageUid(packageUid);
//
//        Map<String, List<String>> descriptionMap = new LinkedHashMap<>();
//        Map<String, ContentsEntity> entityMap = new HashMap<>();
//
//        for (ContentsEntity entity : classEntities) {
//            try {
//                JsonNode jsonNode = objectMapper.readTree(entity.getContentDetail());
//                String uid = jsonNode.get("uid").textValue();
//
//                List<String> descriptions = new ArrayList<>();
//                if (jsonNode.has("description")) {
//                    descriptions.add(jsonNode.get("description").asText());
//                }
//                if (jsonNode.has("descriptionFull")) {
//                    descriptions.add(jsonNode.get("descriptionFull").asText());
//                }
//
//                // methods 배열의 각 method의 description을 추가
//                if (!jsonNode.get("methods").isEmpty()) {
//                    for (JsonNode methodNode : jsonNode.get("methods")) {
//                        if (methodNode.has("description")) {
//                            descriptions.add(methodNode.get("description").asText());
//                        }
//                    }
//                }
//
//                if (!descriptions.isEmpty()) {
//                    descriptionMap.put(uid, descriptions);
//                }
//                entityMap.put(uid, entity);
//            } catch (Exception e) {
//                log.error("JSON 파싱 오류: {}", e.getMessage(), e);
//            }
//        }
//
//        // 번역 요청
//        return CompletableFuture.supplyAsync(() -> deeplAPIService.translateText(descriptionMap.values().stream().flatMap(List::stream).collect(Collectors.toList())))
//                .thenAcceptAsync(translated -> {
//                    List<ContentsEntity> updatedEntities = new ArrayList<>();
//                    List<String> uidList = new ArrayList<>(descriptionMap.keySet());
//
//                    int translateSize = translated.size();
//                    int translatedIndex = 0;
//
//                    for (int i = 0; i < uidList.size(); i++) {
//                        String uid = uidList.get(i);
//                        List<String> descriptions = descriptionMap.get(uid);
//                        ContentsEntity entity = entityMap.get(uid);
//
//                        if (entity == null) {
//                            log.warn("uid [{}]에 해당하는 엔티티를 찾을 수 없습니다.", uid);
//                            continue;
//                        }
//
//                        try {
//                            JsonNode jsonNode = objectMapper.readTree(entity.getContentDetail());
//
//                            // description과 descriptionFull에 번역된 값 적용
//                            if (descriptions.size() > 0) {
//                                ((ObjectNode) jsonNode).put("description", translated.get(translatedIndex++));
//                            }
//                            if (descriptions.size() > 1) {
//                                ((ObjectNode) jsonNode).put("descriptionFull", translated.get(translatedIndex++));
//                            }
//
//                            // methods 배열에 대해서도 번역된 값을 적용
//                            if (jsonNode.has("methods")) {
//                                for (JsonNode methodNode : jsonNode.get("methods")) {
//                                    if (methodNode.has("description")) {
//                                        ((ObjectNode) methodNode).put("description", translated.get(translatedIndex++));
//                                    }
//                                }
//                            }
//
//                            entity.setContentDetail(objectMapper.writeValueAsString(jsonNode));
//                            entity.setUpdatedAt(LocalDateTime.now());
//                            updatedEntities.add(entity);
//                        } catch (Exception e) {
//                            log.error("번역 결과 업데이트 중 오류 발생 (uid: {}): {}", uid, e.getMessage(), e);
//                        }
//                    }
//
//                    contentsRepository.saveAll(updatedEntities);
//                });
//    }
}
