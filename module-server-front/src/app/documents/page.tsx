"use client";

import { useState, useEffect } from "react";
import { toggleBookmark } from "../../lib/documents/document";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import * as JavaVersion from "../../lib/documents/javaLatestVersion";
import * as JavaMoulesByVersionUid from "../../lib/documents/useModulesByVersion";
import useAuth from "../../hook/useAuth";
import { BASE_URL } from '@/lib/api';
import {fetchWithAuth} from "@/app/util/api";

interface ModuleDetail {
  uid: string;
  name: string;
  packages: string[];
  versionId: string;
  description: string;
}

interface ModuleResponse {
  id: number;
  createdAt: string;
  updatedAt: string;
  contentType: string;
  contentDetail: ModuleDetail;
  views: number;
  bookmarkUsers: string;
  uid: string;
  bookmarkUsersList: string[];
}

interface PackageDetail {
  uid: string;
  name: string;
  classes: string[];
  moduleId: string;
  description: string;
}

interface PackageResponse {
  id: number;
  createdAt: string;
  updatedAt: string;
  contentType: string;
  contentDetail: PackageDetail;
  views: number;
  bookmarkUsers: string;
  uid: string;
  bookmarkUsersList: string[];
}

interface VersionDetail {
  uid: string;
  modules: string[];
  version: string;
}

interface VersionResponse {
  id: number;
  createdAt: string;
  updatedAt: string;
  contentType: string;
  contentDetail: VersionDetail;
  views: number;
  bookmarkUsers: string;
  uid: string;
  bookmarkUsersList: string[];
}

interface ClassMethod {
  name: string;
  method: string;
  description: string;
  methodExample: string;
}

interface ClassConstructor {
  signature: string;
  parameters: string[];
  throwsList: string[];
  description: string;
}

interface ClassDetail {
  uid: string;
  name: string;
  type: string;
  methods: ClassMethod[];
  packageId: string;
  description: string;
  constructors: ClassConstructor[];
  descriptionFull: string;
}

interface ClassResponse {
  id: number;
  createdAt: string;
  updatedAt: string;
  contentType: string;
  contentDetail: ClassDetail;
  views: number;
  bookmarkUsers: string;
  uid: string;
  bookmarkUsersList: string[];
  bookmarked: boolean;
}

export default function DocumentsPage() {
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [showSearchResults, setShowSearchResults] = useState(false);
  const [selectedItem, setSelectedItem] = useState<{
    type: "module" | "package" | "class" | null;
    module?: string;
    package?: string;
    class?: string;
  }>({ type: null });
  const [expandedModules, setExpandedModules] = useState<string[]>([]);
  const [expandedPackages, setExpandedPackages] = useState<string[]>([]);
  const { isLoggedIn } = useAuth();

  const [versionData, setVersionData] = useState<VersionResponse | null>(null);
  const [moduleDataMap, setModuleDataMap] = useState<
    Map<string, ModuleResponse>
  >(new Map());
  const [packageDataMap, setPackageDataMap] = useState<
    Map<string, PackageResponse>
  >(new Map());
  const [classDataMap, setClassDataMap] = useState<Map<string, ClassResponse>>(
    new Map()
  );
  const [classBookmarkMap, setClassBookmarkMap] = useState<
    Map<string, boolean>
  >(new Map());
  const [classBookmarked, setClassBookmarked] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const searchParams = useSearchParams();
  const classUid = searchParams.get("classUid");
  const moduleName = searchParams.get("module");
  const packageName = searchParams.get("package");
  const className = searchParams.get("class");

  const [initialClassHandled, setInitialClassHandled] = useState(false);

  // 버전 데이터를 가져오는 useEffect
  // useEffect(() => {
  //   const fetchVersionData = async () => {
  //     try {
  //       setLoading(true);
  //       const response = await fetchWithAuth(
  //         `${BASE_URL}/api/javadoc/version/6327a80e-b10e-4510-9a4e-54f9197464ce`
  //       );
  //       if (!response.ok) {
  //         throw new Error("Failed to fetch version data");
  //       }
  //       const data: VersionResponse = await response.json();
  //       setVersionData(data);
  //     } catch (err) {
  //       setError(err instanceof Error ? err.message : "Unknown error");
  //     } finally {
  //       setLoading(false);
  //     }
  //   };
  //
  //   fetchVersionData();
  // }, []);
  //
  // // 모듈 데이터를 가져오는 useEffect
  // useEffect(() => {
  //   if (versionData?.contentDetail.modules) {
  //     const fetchModuleData = async (moduleUid: string) => {
  //       try {
  //         const response = await fetchWithAuth(
  //           `${BASE_URL}/api/javadoc/module/${moduleUid}`
  //         );
  //         if (!response.ok) {
  //           throw new Error(`Failed to fetch module data for ${moduleUid}`);
  //         }
  //         const data: ModuleResponse = await response.json();
  //         setModuleDataMap((prev) => new Map(prev).set(moduleUid, data));
  //       } catch (err) {
  //         console.error(`Error fetching module ${moduleUid}:`, err);
  //       }
  //     };
  //
  //     // 모든 모듈 데이터를 병렬로 가져오기
  //     Promise.all(versionData.contentDetail.modules.map(fetchModuleData));
  //   }
  // }, [versionData]);
  //
  // const [isFullyLoaded, setIsFullyLoaded] = useState(false);
  //
  // // 모든 패키지와 클래스를 로드하는 메서드
  // const loadAllContentData = async () => {
  //   try {
  //     // 패키지 데이터 로드
  //     const packagesResponse = await fetchWithAuth(
  //         `${BASE_URL}/api/javadoc/all/content/type?contentType=package`
  //     );
  //     const packageData = await packagesResponse.json();
  //
  //     // 클래스 데이터 로드
  //     const classesResponse = await fetchWithAuth(
  //         `${BASE_URL}/api/javadoc/all/content/type?contentType=class`
  //     );
  //     const classData = await classesResponse.json();
  //
  //     // 패키지 데이터 맵 업데이트
  //     const newPackageDataMap = new Map(packageDataMap);
  //     packageData.forEach((pkg: PackageResponse) => {
  //       newPackageDataMap.set(pkg.uid, pkg);
  //     });
  //     setPackageDataMap(newPackageDataMap);
  //
  //     // 클래스 데이터 맵 업데이트
  //     const newClassDataMap = new Map(classDataMap);
  //     const newClassBookmarkMap = new Map(classBookmarkMap);
  //     classData.forEach((cls: ClassResponse) => {
  //       newClassDataMap.set(cls.uid, cls);
  //       newClassBookmarkMap.set(cls.contentDetail.name, cls.bookmarked);
  //     });
  //     setClassDataMap(newClassDataMap);
  //     setClassBookmarkMap(newClassBookmarkMap);
  //
  //     // 로딩 완료 상태 설정
  //     setIsFullyLoaded(true);
  //     setLoading(false);
  //   } catch (err) {
  //     console.error("전체 콘텐츠 로드 중 오류 발생:", err);
  //     setError("데이터를 로드하는 중 오류가 발생했습니다.");
  //     setLoading(false);
  //   }
  // };

  const [isFullyLoaded, setIsFullyLoaded] = useState(false);
  const loadInitialData = async () => {
    try {
      setLoading(true);

      // 버전 데이터 먼저 로드
      const versionResponse = await fetchWithAuth(
          `${BASE_URL}/api/javadoc/version/6327a80e-b10e-4510-9a4e-54f9197464ce`
      );
      const versionData: VersionResponse = await versionResponse.json();
      setVersionData(versionData);

      // 모듈 데이터 로드
      const modulesResponse = await fetchWithAuth(
          `${BASE_URL}/api/javadoc/all/content/type?contentType=module`
      );
      const moduleData: ModuleResponse[] = await modulesResponse.json();

      const moduleDataMap = new Map(
          moduleData.map((module) => [module.uid, module])
      );
      setModuleDataMap(moduleDataMap);

      // 초기 로딩 완료
      setLoading(false);
    } catch (err) {
      console.error("초기 데이터 로드 중 오류 발생:", err);
      setError("기본 데이터를 로드하는 중 오류가 발생했습니다.");
      setLoading(false);
    }
  };

// 나머지 데이터를 백그라운드에서 로드하는 함수
  const loadAdditionalData = async () => {
    try {
      const [packagesResponse, classesResponse] = await Promise.all([
        fetchWithAuth(`${BASE_URL}/api/javadoc/all/content/type?contentType=package`),
        fetchWithAuth(`${BASE_URL}/api/javadoc/classes/test/class`)
      ]);

      const packageData: PackageResponse[] = await packagesResponse.json();
      const classData: ClassResponse[] = await classesResponse.json();

      const packageDataMap = new Map(
          packageData.map((pkg) => [pkg.uid, pkg])
      );
      setPackageDataMap(packageDataMap);

      const newClassDataMap = new Map(classDataMap);
      const newClassBookmarkMap = new Map(classBookmarkMap);

      classData.forEach((cls: ClassResponse) => {
        newClassDataMap.set(cls.uid, cls);
        newClassBookmarkMap.set(cls.contentDetail.name, cls.bookmarked);
      });

      setClassDataMap(newClassDataMap);
      setClassBookmarkMap(newClassBookmarkMap);

      setIsFullyLoaded(true);
    } catch (err) {
      console.error("추가 데이터 로드 중 오류 발생:", err);
    }
  };

// 컴포넌트 초기화 시 useEffect
  useEffect(() => {
    // 초기 데이터 로드
    loadInitialData();

    // 백그라운드에서 나머지 데이터 로드
    const loadRemainingData = async () => {
      await loadAdditionalData();
    };

    loadRemainingData();
  }, []);

  useEffect(() => {
    const handleInitialClass = async () => {
      if (!classUid || initialClassHandled || moduleDataMap.size === 0) return;

      try {
        // First, try to handle if we already have module/package/class names from URL params
        if (moduleName && packageName && className) {
          // Find the module by name
          const moduleEntry = Array.from(moduleDataMap.entries()).find(
            ([_, moduleData]) => moduleData.contentDetail.name === moduleName
          );

          if (moduleEntry) {
            // 1) 모듈 펼치기
            if (!expandedModules.includes(moduleName)) {
              await toggleModuleExpand(moduleName);
            }

            // Wait a bit for packages to load
            setTimeout(async () => {
              // 2) 패키지 펼치기
              if (!expandedPackages.includes(packageName)) {
                await togglePackageExpand(packageName);
              }

              // 3) 클래스 선택
              setTimeout(() => {
                setSelectedItem({
                  type: "class",
                  class: className,
                  package: packageName,
                  module: moduleName,
                });
                setInitialClassHandled(true);
              }, 500);
            }, 500);

            return;
          }
        }

        // Fallback approach: search by classUid
        for (const moduleData of moduleDataMap.values()) {
          // Check if contentDetail and packages exist before iterating
          if (
            moduleData?.contentDetail?.packages &&
            Array.isArray(moduleData.contentDetail.packages)
          ) {
            // Expand the module to load its packages
            if (!expandedModules.includes(moduleData.contentDetail.name)) {
              await toggleModuleExpand(moduleData.contentDetail.name);
            }

            // Wait for packages to load
            setTimeout(async () => {
              // Now check each package
              for (const packageUid of moduleData.contentDetail.packages) {
                const packageData = packageDataMap.get(packageUid);

                if (
                  packageData?.contentDetail?.classes &&
                  Array.isArray(packageData.contentDetail.classes) &&
                  packageData.contentDetail.classes.includes(classUid)
                ) {
                  // Expand the package
                  if (
                    !expandedPackages.includes(packageData.contentDetail.name)
                  ) {
                    await togglePackageExpand(packageData.contentDetail.name);
                  }

                  // Wait for classes to load
                  setTimeout(() => {
                    const classData = classDataMap.get(classUid);
                    if (classData) {
                      setSelectedItem({
                        type: "class",
                        class: classData.contentDetail.name,
                        package: packageData.contentDetail.name,
                        module: moduleData.contentDetail.name,
                      });
                      setInitialClassHandled(true);
                    }
                  }, 500);

                  return;
                }
              }
            }, 500);
          }
        }
      } catch (error) {
        console.error("Error in handleInitialClass:", error);
      }
    };

    handleInitialClass();
  }, [
    classUid,
    moduleDataMap,
    packageDataMap,
    classDataMap,
    expandedModules,
    expandedPackages,
    moduleName,
    packageName,
    className,
  ]);

  // 예시 데이터 제거
  const modules = Array.from(moduleDataMap.values()).map(
    (module) => module.contentDetail.name
  );

  //북마크
  const toggleClassBookmark = async (
    className: string,
    e: React.MouseEvent
  ) => {
    e.stopPropagation();

    const classData = Array.from(classDataMap.values()).find(
      (c) => c.contentDetail.name === className
    );

    if (!classData) return;

    const currentBookmarked = classBookmarkMap.get(className) || false;

    try {
      await toggleBookmark(Number(classData.id), currentBookmarked);
      setClassBookmarkMap((prev) =>
        new Map(prev).set(className, !currentBookmarked)
      );
    } catch (err) {
      console.error("북마크 토글 실패", err);
    }
  };

  // 검색 기능
  // 검색 텍스트가 먼저 나오게 정렬
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();

    if (!searchQuery.trim()) {
      setShowSearchResults(false);
      return;
    }

    const results: any[] = [];
    const query = searchQuery.toLowerCase();

    // 클래스 검색만 수행
    Array.from(classDataMap.values()).forEach((classData) => {
      const className = classData.contentDetail.name.toLowerCase();

      // 정확한 일치 체크
      const isExactMatch = className === query;

      // 검색어 포함 여부 및 위치 체크
      const indexOfQuery = className.indexOf(query);

      if (indexOfQuery !== -1) {
        // packageDataMap과 moduleDataMap에서 해당 항목을 찾는 작업
        const packageData = packageDataMap.get(classData.contentDetail.packageId);
        const moduleData = packageData
            ? moduleDataMap.get(packageData.contentDetail.moduleId)
            : undefined;

        if (packageData && moduleData) {
          results.push({
            type: "class",
            name: classData.contentDetail.name,
            package: packageData.contentDetail.name,
            module: moduleData.contentDetail.name,
            isExactMatch, // 정확한 일치 여부
            indexOfQuery, // 검색어의 위치
          });
        }
      }
    });

    // 정확한 일치가 먼저 오고, 그 후 검색어가 앞에 있을수록 높은 우선순위 부여
    const sortedResults = results.sort((a, b) => {
      // 정확한 일치가 우선
      if (a.isExactMatch && !b.isExactMatch) return -1;
      if (!a.isExactMatch && b.isExactMatch) return 1;

      // 정확한 일치가 아니면, 검색어가 앞에 있을수록 우선
      return a.indexOfQuery - b.indexOfQuery;
    });

    setSearchResults(sortedResults);
    setShowSearchResults(true);
  };

  const toggleModuleExpand = async (module: string) => {
    if (expandedModules.includes(module)) {
      setExpandedModules(expandedModules.filter((m) => m !== module));
    } else {
      setExpandedModules([...expandedModules, module]);

      // 해당 모듈의 패키지 데이터 가져오기
      const moduleData = Array.from(moduleDataMap.values()).find(
        (m) => m.contentDetail.name === module
      );

      if (moduleData && moduleData.contentDetail.packages) {
        const fetchPackageData = async (packageUid: string) => {
          try {
            const response = await fetchWithAuth(
              `${BASE_URL}/api/javadoc/package/${packageUid}`
            );
            if (!response.ok) {
              throw new Error(`Failed to fetch package data for ${packageUid}`);
            }
            const data: PackageResponse = await response.json();
            setPackageDataMap((prev) => new Map(prev).set(packageUid, data));
          } catch (err) {
            console.error(`Error fetching package ${packageUid}:`, err);
          }
        };

        // 모든 패키지 데이터를 병렬로 가져오기
        Promise.all(moduleData.contentDetail.packages.map(fetchPackageData));
      }
    }
  };

  const togglePackageExpand = async (pkg: string) => {
    if (expandedPackages.includes(pkg)) {
      setExpandedPackages(expandedPackages.filter((p) => p !== pkg));
    } else {
      setExpandedPackages([...expandedPackages, pkg]);

      // 해당 패키지의 클래스 데이터 가져오기
      const packageData = Array.from(packageDataMap.values()).find(
        (p) => p.contentDetail.name === pkg
      );

      if (packageData && packageData.contentDetail.classes) {
        const fetchClassData = async (classUid: string) => {
          try {
            const response = await fetchWithAuth(
              `${BASE_URL}/api/javadoc/class/test/${classUid}`
            );
            console.log("Response status: ", response.status);
            if (!response.ok) {
              throw new Error(`Failed to fetch class data for ${classUid}`);
            }
            const data: ClassResponse = await response.json();
            console.log("Fetched class data: ", data);
            setClassDataMap((prev) => new Map(prev).set(classUid, data));
            setClassBookmarkMap((prev) =>
              new Map(prev).set(data.contentDetail.name, data.bookmarked)
            );
          } catch (err) {
            console.error(`Error fetching class ${classUid}:`, err);
          }
        };

        // 모든 클래스 데이터를 병렬로 가져오기
        Promise.all(packageData.contentDetail.classes.map(fetchClassData));
      }
    }
  };

  const selectItem = (
    type: "module" | "package" | "class",
    name: string,
    moduleOrPackage?: string,
    parentModule?: string
  ) => {
    setShowSearchResults(false);

    if (type === "module") {
      setSelectedItem({ type, module: name });
      if (!expandedModules.includes(name)) {
        toggleModuleExpand(name);
      }
    } else if (type === "package") {
      setSelectedItem({ type, package: name, module: moduleOrPackage });
      if (!expandedPackages.includes(name)) {
        togglePackageExpand(name);
      }
    } else if (type === "class") {
      setSelectedItem({
        type,
        class: name,
        package: moduleOrPackage,
        module: parentModule,
      });
    }
  };

  // 현재 문서 경로 표시
  const documentPath = () => {
    let path = "";
    if (selectedItem.module) {
      path += selectedItem.module;
      if (selectedItem.package) {
        path += " > " + selectedItem.package;
        if (selectedItem.class) {
          path += " > " + selectedItem.class;
        }
      }
    }
    return path || versionData?.contentDetail.version;
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        {/* 문서 헤더 */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-3xl font-bold text-gray-800">Java 17 문서</h1>
          </div>
          <div className="text-gray-600">{documentPath()}</div>
        </div>

        {/* 검색 영역 */}
        <div className="bg-white rounded-lg shadow-md p-4 mb-8">
          <form onSubmit={handleSearch} className="flex gap-2">
            <input
              type="text"
              placeholder="클래스 이름을 입력하세요."
              className="flex-grow p-2 border border-gray-300 rounded"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <button
              type="submit"
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
            >
              검색
            </button>
          </form>
        </div>

        {/* 문서 내용 영역 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          {/* 왼쪽 사이드바 (트리 구조) */}
          <div className="md:col-span-1">
            <div className="bg-white rounded-lg shadow-md overflow-hidden">
              <div className="p-4 border-b bg-gray-50">
                <h3 className="font-medium text-gray-700">
                  Java {versionData?.contentDetail.version} API 문서 트리
                </h3>
              </div>
              <div className="p-0 max-h-[calc(100vh-16rem)] overflow-y-auto">
                <ul className="divide-y divide-gray-100">
                  {loading ? (
                    <li className="p-4 text-center">
                      <div className="flex items-center justify-center">
                        <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
                        <span className="ml-2 text-gray-600">로딩 중...</span>
                      </div>
                    </li>
                  ) : error ? (
                    <li className="p-4 text-center text-red-600">{error}</li>
                  ) : (
                    Array.from(moduleDataMap.values()).map((moduleData) => (
                      <li
                        key={moduleData.uid}
                        className="relative hover:bg-gray-50"
                      >
                        <div className="flex items-center">
                          <button
                            className="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-gray-600"
                            onClick={() =>
                              toggleModuleExpand(moduleData.contentDetail.name)
                            }
                          >
                            <svg
                              className={`w-4 h-4 transform transition-transform ${
                                expandedModules.includes(
                                  moduleData.contentDetail.name
                                )
                                  ? "rotate-90"
                                  : ""
                              }`}
                              fill="none"
                              stroke="currentColor"
                              viewBox="0 0 24 24"
                            >
                              <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M9 5l7 7-7 7"
                              />
                            </svg>
                          </button>
                          <button
                            className={`flex-grow px-2 py-2 text-left text-sm ${
                              selectedItem.type === "module" &&
                              selectedItem.module ===
                                moduleData.contentDetail.name
                                ? "font-medium text-blue-600"
                                : "text-gray-700 hover:text-gray-900"
                            }`}
                            onClick={() =>
                              selectItem(
                                "module",
                                moduleData.contentDetail.name
                              )
                            }
                          >
                            {moduleData.contentDetail.name}
                          </button>
                        </div>

                        {expandedModules.includes(
                          moduleData.contentDetail.name
                        ) &&
                          moduleData.contentDetail.packages && (
                            <ul className="pl-4 bg-gray-50 border-t border-gray-100">
                              {moduleData.contentDetail.packages
                                .map((packageUid) =>
                                  packageDataMap.get(packageUid)
                                )
                                .filter(
                                  (pkg): pkg is PackageResponse =>
                                    pkg !== undefined
                                )
                                .map((pkg) => (
                                  <li
                                    key={pkg.uid}
                                    className="relative hover:bg-gray-100/50"
                                  >
                                    <div className="flex items-center">
                                      <button
                                        className="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-gray-600"
                                        onClick={() =>
                                          togglePackageExpand(
                                            pkg.contentDetail.name
                                          )
                                        }
                                      >
                                        <svg
                                          className={`w-3 h-3 transform transition-transform ${
                                            expandedPackages.includes(
                                              pkg.contentDetail.name
                                            )
                                              ? "rotate-90"
                                              : ""
                                          }`}
                                          fill="none"
                                          stroke="currentColor"
                                          viewBox="0 0 24 24"
                                        >
                                          <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M9 5l7 7-7 7"
                                          />
                                        </svg>
                                      </button>
                                      <button
                                        className={`flex-grow px-2 py-2 text-left text-sm ${
                                          selectedItem.type === "package" &&
                                          selectedItem.package ===
                                            pkg.contentDetail.name
                                            ? "font-medium text-blue-600"
                                            : "text-gray-600 hover:text-gray-900"
                                        }`}
                                        onClick={() =>
                                          selectItem(
                                            "package",
                                            pkg.contentDetail.name,
                                            moduleData.contentDetail.name
                                          )
                                        }
                                      >
                                        {pkg.contentDetail.name}
                                      </button>
                                    </div>

                                    {expandedPackages.includes(
                                      pkg.contentDetail.name
                                    ) && (
                                      <ul className="pl-8 py-1 bg-gray-100/50">
                                        {pkg.contentDetail.classes
                                          .map((classUid) =>
                                            classDataMap.get(classUid)
                                          )
                                          .filter(
                                            (cls): cls is ClassResponse =>
                                              cls !== undefined
                                          )
                                          .map((cls) => (
                                            <li
                                              key={cls.uid}
                                              className="relative group"
                                            >
                                              <button
                                                className={`w-full px-2 py-1.5 text-left text-sm rounded-sm ${
                                                  selectedItem.type ===
                                                    "class" &&
                                                  selectedItem.class ===
                                                    cls.contentDetail.name
                                                    ? "font-medium text-blue-600 bg-white shadow-sm"
                                                    : "text-gray-600 hover:text-gray-900 hover:bg-white/50"
                                                }`}
                                                onClick={() =>
                                                  selectItem(
                                                    "class",
                                                    cls.contentDetail.name,
                                                    pkg.contentDetail.name,
                                                    moduleData.contentDetail
                                                      .name
                                                  )
                                                }
                                              >
                                                {cls.contentDetail.name}
                                              </button>
                                            </li>
                                          ))}
                                      </ul>
                                    )}
                                  </li>
                                ))}
                            </ul>
                          )}
                      </li>
                    ))
                  )}
                </ul>
              </div>
            </div>
          </div>

          {/* 오른쪽 내용 영역 */}
          <div className="md:col-span-3">
            <div className="bg-white rounded-lg shadow-md p-6">
              {/* 검색 결과 표시 */}
              {showSearchResults && (
                <div className="mb-8">
                  <h2 className="text-xl font-semibold mb-4">
                    '{searchQuery}' 검색 결과
                  </h2>

                  {searchResults.length === 0 ? (
                    <p className="text-gray-600">검색 결과가 없습니다.</p>
                  ) : (
                    <div className="space-y-4">
                      {searchResults.map((result, index) => (
                        <div
                          key={index}
                          className="p-3 border border-gray-200 rounded hover:bg-gray-50 cursor-pointer"
                          onClick={() =>
                            selectItem(
                              result.type,
                              result.name,
                              result.type === "class"
                                ? result.package
                                : result.module,
                              result.type === "class"
                                ? result.module
                                : undefined
                            )
                          }
                        >
                          <div className="flex items-center">
                            <span
                              className="inline-block px-2 py-1 text-xs text-white rounded mr-3"
                              style={{
                                backgroundColor:
                                  result.type === "module"
                                    ? "#4A5568"
                                    : result.type === "package"
                                    ? "#3182CE"
                                    : "#38A169",
                              }}
                            >
                              {result.type}
                            </span>
                            <span className="font-medium">{result.name}</span>
                          </div>
                          <div className="mt-1 text-sm text-gray-600">
                            {result.type === "class"
                              ? `${result.module} > ${result.package} > ${result.name}`
                              : result.type === "package"
                              ? `${result.module} > ${result.name}`
                              : result.name}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* 문서 내용 */}
              {!showSearchResults && !selectedItem.type && (
                <div className="text-center py-8">
                  <h2 className="text-xl font-semibold mb-4">
                    Java 17 문서에 오신 것을 환영합니다
                  </h2>
                  <p className="text-gray-600 mb-6">
                    왼쪽 사이드바에서 모듈, 패키지, 클래스를 선택하거나 검색하여
                    문서를 탐색하세요.
                  </p>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-2xl mx-auto">
                    {modules.slice(0, 3).map((module) => (
                      <button
                        key={module}
                        className="bg-blue-50 hover:bg-blue-100 text-blue-600 p-4 rounded-lg"
                        onClick={() => selectItem("module", module)}
                      >
                        {module}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* 모듈 정보 */}
              {!showSearchResults && selectedItem.type === "module" && (
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-xl font-semibold">
                      {selectedItem.module} 모듈
                    </h2>
                  </div>
                  <div className="mb-6">
                    <h3 className="text-lg font-medium mb-2">패키지 목록</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                      {Array.from(moduleDataMap.values())
                        .find(
                          (m) => m.contentDetail.name === selectedItem.module
                        )
                        ?.contentDetail.packages.map((packageUid) =>
                          packageDataMap.get(packageUid)
                        )
                        .filter(
                          (pkg): pkg is PackageResponse => pkg !== undefined
                        )
                        .map((pkg) => (
                          <div key={pkg.uid} className="relative group">
                            <button
                              className="w-full text-left bg-gray-50 hover:bg-gray-100 p-2 rounded"
                              onClick={() =>
                                selectItem(
                                  "package",
                                  pkg.contentDetail.name,
                                  selectedItem.module
                                )
                              }
                            >
                              {pkg.contentDetail.name}
                            </button>
                          </div>
                        ))}
                    </div>
                  </div>
                </div>
              )}

              {/* 패키지 정보 */}
              {!showSearchResults && selectedItem.type === "package" && (
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-xl font-semibold">
                      {selectedItem.module} &gt; {selectedItem.package}
                    </h2>
                  </div>

                  <div className="mb-6">
                    <h3 className="text-lg font-medium mb-2">클래스 목록</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                      {Array.from(packageDataMap.values())
                        .find(
                          (p) => p.contentDetail.name === selectedItem.package
                        )
                        ?.contentDetail.classes.map((classUid) =>
                          classDataMap.get(classUid)
                        )
                        .filter(
                          (cls): cls is ClassResponse => cls !== undefined
                        )
                        .map((cls) => (
                          <div key={cls.uid} className="relative group">
                            <button
                              className="w-full text-left bg-gray-50 hover:bg-gray-100 p-3 rounded"
                              onClick={() =>
                                selectItem(
                                  "class",
                                  cls.contentDetail.name,
                                  selectedItem.package,
                                  selectedItem.module
                                )
                              }
                            >
                              {cls.contentDetail.name}
                            </button>
                            <button
                              onClick={(e) =>
                                toggleClassBookmark(cls.contentDetail.name, e)
                              }
                              className="absolute right-3 top-3 focus:outline-none text-navy-400 hover:text-navy-600"
                              aria-label={
                                classBookmarkMap.get(cls.contentDetail.name)
                                  ? "북마크 제거"
                                  : "북마크 추가"
                              }
                            >
                              {isLoggedIn && (
                                <svg
                                  className="w-5 h-5"
                                  fill={
                                    classBookmarkMap.get(cls.contentDetail.name)
                                      ? "currentColor"
                                      : "none"
                                  }
                                  stroke="currentColor"
                                  viewBox="0 0 24 24"
                                >
                                  <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"
                                  />
                                </svg>
                              )}
                            </button>
                          </div>
                        ))}
                    </div>
                  </div>
                </div>
              )}

              {/* 클래스 정보 */}
              {!showSearchResults && selectedItem.type === "class" && (
                <div>
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-semibold">
                      {selectedItem.class}
                    </h2>
                    <button
                      onClick={(e) =>
                        toggleClassBookmark(selectedItem.class!, e)
                      }
                      className="focus:outline-none text-navy-400 hover:text-navy-600"
                      aria-label={
                        classBookmarkMap.get(selectedItem.class!)
                          ? "북마크 제거"
                          : "북마크 추가"
                      }
                    >
                      {isLoggedIn && (
                        <svg
                          className="w-6 h-6"
                          fill={
                            classBookmarkMap.get(selectedItem.class!)
                              ? "currentColor"
                              : "none"
                          }
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"
                          />
                        </svg>
                      )}
                    </button>
                  </div>

                  {/* 좌측 내비게이션 */}
                  <div className="flex mb-8">
                    <div className="w-64 mr-8">
                      <div className="sticky top-4">
                        <h3 className="text-lg font-medium mb-2">목차</h3>
                        <ul className="space-y-2 text-sm">
                          <li>
                            <a
                              href="#class-info"
                              className="text-blue-600 hover:underline"
                            >
                              클래스 정보
                            </a>
                          </li>
                          <li>
                            <a
                              href="#constructor"
                              className="text-blue-600 hover:underline"
                            >
                              생성자 정보
                            </a>
                          </li>
                          <li>
                            <a
                              href="#methods"
                              className="text-blue-600 hover:underline"
                            >
                              메소드 정보
                            </a>
                          </li>
                          <li>
                            <a
                              href="#examples"
                              className="text-blue-600 hover:underline"
                            >
                              예제 코드
                            </a>
                          </li>
                        </ul>
                      </div>
                    </div>

                    {/* 클래스 문서 내용 */}
                    <div className="flex-1">
                      {(() => {
                        const classData = Array.from(
                          classDataMap.values()
                        ).find(
                          (c) => c.contentDetail.name === selectedItem.class
                        );

                        if (!classData) return null;

                        return (
                          <>
                            <section id="class-info" className="mb-10">
                              <h3 className="text-lg font-semibold mb-4 pb-2 border-b">
                                클래스 정보
                              </h3>
                              <p className="mb-4">
                                <strong>패키지:</strong> {selectedItem.package}
                              </p>
                              <p className="mb-4">
                                <strong>모듈:</strong> {selectedItem.module}
                              </p>
                              <p className="mb-4 text-gray-700">
                                {classData.contentDetail.description}
                              </p>
                              {classData.contentDetail.descriptionFull && (
                                <p className="text-gray-700">
                                  {classData.contentDetail.descriptionFull}
                                </p>
                              )}
                            </section>

                            <section id="constructor" className="mb-10">
                              <h3 className="text-lg font-semibold mb-4 pb-2 border-b">
                                생성자 정보
                              </h3>
                              {classData.contentDetail.constructors.map(
                                (constructor, index) => (
                                  <div
                                    key={index}
                                    className="bg-gray-50 p-4 rounded mb-4"
                                  >
                                    <code className="block">
                                      {constructor.signature}
                                    </code>
                                    <p className="mt-2 text-sm text-gray-700">
                                      {constructor.description}
                                    </p>
                                    {constructor.parameters.length > 0 && (
                                      <div className="mt-2">
                                        <strong className="text-sm">
                                          매개변수:
                                        </strong>
                                        <ul className="mt-1 text-sm text-gray-700">
                                          {constructor.parameters.map(
                                            (param, i) => (
                                              <li key={i}>{param}</li>
                                            )
                                          )}
                                        </ul>
                                      </div>
                                    )}
                                    {constructor.throwsList.length > 0 && (
                                      <div className="mt-2">
                                        <strong className="text-sm">
                                          예외:
                                        </strong>
                                        <ul className="mt-1 text-sm text-gray-700">
                                          {constructor.throwsList.map(
                                            (throwItem, i) => (
                                              <li key={i}>{throwItem}</li>
                                            )
                                          )}
                                        </ul>
                                      </div>
                                    )}
                                  </div>
                                )
                              )}
                            </section>

                            <section id="methods" className="mb-10">
                              <h3 className="text-lg font-semibold mb-4 pb-2 border-b">
                                메소드 정보
                              </h3>
                              <div className="space-y-4">
                                {classData.contentDetail.methods.map(
                                  (method, index) => (
                                    <div
                                      key={index}
                                      className="bg-gray-50 p-4 rounded"
                                    >
                                      <h4 className="font-medium">
                                        {method.name} {method.method}
                                      </h4>
                                      <p className="mt-2 text-sm text-gray-700">
                                        {method.description}
                                      </p>
                                    </div>
                                  )
                                )}
                              </div>
                            </section>

                            <section id="examples" className="mb-10">
                              <h3 className="text-lg font-semibold mb-4 pb-2 border-b">
                                예제 코드
                              </h3>
                              <div className="space-y-4">
                                {classData.contentDetail.methods.map(
                                  (method, index) => (
                                    <div
                                      key={index}
                                      className="bg-gray-800 rounded-lg overflow-hidden"
                                    >
                                      <div className="bg-gray-700 px-4 py-2 text-white">
                                        <code>
                                          {method.name} {method.method}
                                        </code>
                                      </div>
                                      <div className="p-4">
                                        <pre className="text-sm text-gray-200 whitespace-pre-wrap">
                                          <code>{method.methodExample}</code>
                                        </pre>
                                      </div>
                                    </div>
                                  )
                                )}
                              </div>
                            </section>
                          </>
                        );
                      })()}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
