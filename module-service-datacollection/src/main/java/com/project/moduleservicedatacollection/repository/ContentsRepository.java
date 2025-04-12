package com.project.moduleservicedatacollection.repository;

import com.project.moduleservicedatacollection.entity.ContentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentsRepository extends JpaRepository<ContentsEntity, Long> {
    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'version' AND c.content_detail->>'version' = :version", nativeQuery = true)
    ContentsEntity findByContentTypeAndVersion(@Param("version") String version);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'version' AND c.content_detail->>'uid' = :uid", nativeQuery = true)
    ContentsEntity findByContentTypeAndUid(@Param("uid") String uid);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'module' AND c.content_detail->>'name' = :moduleName", nativeQuery = true)
    ContentsEntity findByContentTypeAndModuleName(@Param("moduleName") String moduleName);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'package' AND c.content_detail->>'name' = :packageName", nativeQuery = true)
    ContentsEntity findByContentTypeAndPackageName(@Param("packageName") String packageName);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'class' AND c.content_detail->>'name' = :className", nativeQuery = true)
    ContentsEntity findByContentTypeAndClassName(@Param("className") String className);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'class' AND c.content_detail->>'name' = :className AND c.content_detail->>'packageId' = :packageId", nativeQuery = true)
    ContentsEntity findByContentTypeAndClassNameAndPackageId(@Param("className") String className, @Param("packageId") String packageId);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'module' AND c.content_detail->>'name' = :moduleName", nativeQuery = true)
    ContentsEntity findModuleName(@Param("moduleName") String moduleName);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'package' AND c.content_detail->>'uid' = :packageUid", nativeQuery = true)
    ContentsEntity findPackageByUid(@Param("packageUid") String packageUid);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'class' AND c.content_detail->>'uid' = :classUid", nativeQuery = true)
    ContentsEntity findClassByUid(@Param("classUid") String classUid);

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'module'", nativeQuery = true)
    List<ContentsEntity> findAllModules();

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'package'", nativeQuery = true)
    List<ContentsEntity> findAllPackages();

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'class'", nativeQuery = true)
    List<ContentsEntity> findAllClasses();

    @Query(value = "SELECT * FROM contents c WHERE c.content_type = 'class' AND c.content_detail->>'packageId' = :packageId", nativeQuery = true)
    List<ContentsEntity> findClassesByPackageUid(@Param("packageId") String packageId);
}
