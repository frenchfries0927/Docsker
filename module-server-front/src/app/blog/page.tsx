"use client";

import useAuth from "../../hook/useAuth";
import { useEffect, useState } from "react";
import {
  techBlogs,
  Content,
  increaseViewCount,
  toggleBookmark,
} from "../../lib/blog/techBlogs";
import { BASE_URL } from '@/lib/api';
import {fetchWithAuth} from "@/app/util/api";

interface TechBlogData {
  contentDetail: {
    provider: string;
  };
}

interface Company {
  id: string;
  name: string;
  displayName: string;
  logoPath: string;
}

const getDisplayName = (provider: string): string => {
  const nameMappings: { [key: string]: string } = {
    "Gmarket Tech Blog": "Gmarket",
    "Hyperconnect Blog": "Hyperconnect",
    "Naver Cloud Platform": "네이버 클라우드",
    "Spoqa Blog": "스포카",
    "Danawa Lab Blog": "다나와",
    "ElevenST Tech Blog": "11번가",
    "Devocean Blog": "데브오션",
    naver_d2: "네이버",
    kakao: "카카오",
    KakaoBank: "카카오뱅크",
    LGCNS: "LG",
    Woowahan: "우아한형제들",
    OliveYoung: "올리브영",
    Samsung: "삼성",
    Tada: "타다",
    Kurly: "컬리",
    Hancom: "한컴",
    KakaoPay: "카카오페이",
    당근: "당근마켓",
    Devsisters: "데브시스터즈",
    Socar: "소카",
    "CJ OnStyle": "CJ OnStyle",
    Toss: "Toss",
    "SK 플래닛": "SK 플래닛",
  };

  return nameMappings[provider] || provider;
};

const getCompanyLogo = (provider: string): string => {
  const logoMap: { [key: string]: string } = {
    "Gmarket Tech Blog": "/company-logos/gmarket.png",
    "Hyperconnect Blog": "/company-logos/hyperconnect.png",
    "Naver Cloud Platform": "/company-logos/naver-cloud.png",
    "Spoqa Blog": "/company-logos/spoqa.png",
    "Danawa Lab Blog": "/company-logos/danawa.png",
    "ElevenST Tech Blog": "/company-logos/11st.png",
    "Devocean Blog": "/company-logos/devocean.png",
    naver_d2: "/company-logos/naver.png",
    kakao: "/company-logos/kakao.png",
    KakaoBank: "/company-logos/kakao-bank.png",
    LGCNS: "/company-logos/lg.png",
    Woowahan: "/company-logos/woowahan.png",
    OliveYoung: "/company-logos/oliveyoung.png",
    Samsung: "/company-logos/samsung.png",
    Tada: "/company-logos/tada.png",
    Kurly: "/company-logos/kurly.png",
    Hancom: "/company-logos/hancom.png",
    KakaoPay: "/company-logos/kakao-pay.png",
    당근: "/company-logos/daangn.png",
    Devsisters: "/company-logos/devsisters.png",
    Socar: "/company-logos/socar.png",
    "CJ OnStyle": "/company-logos/cj-onstyle.png",
    Toss: "/company-logos/Toss.png",
    "SK 플래닛": "/company-logos/SK플래닛.png",
  };
  return logoMap[provider] || "/company-logos/default-logo.png";
};

export default function BlogPage() {
  const [activeCompany, setActiveCompany] = useState<string | null>(null);
  const [sortOption, setSortOption] = useState<string>("latest");
  const [blogs, setBlogs] = useState<Content[]>([]);
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [companies, setCompanies] = useState<Company[]>([]);
  const { isLoggedIn } = useAuth();

  useEffect(() => {
    const fetchCompanies = async () => {
      try {
        const response = await fetchWithAuth(`${BASE_URL}/api/techblog/all`);
        const data: TechBlogData[] = await response.json();

        // provider 중복 제거 및 정렬
        const uniqueProviders = Array.from(
          new Set(data.map((blog) => blog.contentDetail.provider))
        ).sort((a, b) => a.localeCompare(b));

        // companies 배열 생성
        const companiesList: Company[] = [
          {
            id: "all",
            name: "전체",
            displayName: "전체",
            logoPath: "/company-logos/default-logo.svg",
          },
          ...uniqueProviders.map((provider) => ({
            id: provider,
            name: provider,
            displayName: getDisplayName(provider),
            logoPath: getCompanyLogo(provider),
          })),
        ];

        setCompanies(companiesList);
      } catch (error) {
        console.error("회사 목록 로드 실패:", error);
      }
    };

    fetchCompanies();
  }, []);

  const handleBookmarkToggle = async (blog: Content) => {
    if (!isLoggedIn) {
      alert("로그인 후 이용해 주세요!");
      return;
    }

    try {
      await toggleBookmark(blog.id, blog.isBookmarked);
      setBlogs((prevBlogs) =>
        prevBlogs.map((b) =>
          b.id === blog.id
            ? {
                ...b,
                isBookmarked: !b.isBookmarked,
                bookmarkCount: b.isBookmarked
                  ? b.bookmarkCount - 1
                  : b.bookmarkCount + 1,
              }
            : b
        )
      );
    } catch (err) {
      console.error("북마크 실패:", err);
    }
  };

  const fetchBlogs = async (targetPage = 0) => {
    try {
      const response = await techBlogs({
        provider: activeCompany ?? "",
        sort: sortOption,
        page: targetPage,
        size: 10,
      });

      setBlogs(response.content);
      setTotalPages(response.totalPages);
      setPage(targetPage);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      console.error("데이터 로드 실패:", error);
    }
  };

  useEffect(() => {
    fetchBlogs(0);
  }, [activeCompany, sortOption]);

  const sortOptions = [
    { id: "latest", name: "최신순" },
    { id: "views", name: "조회수순" },
    { id: "bookmarks", name: "북마크순" },
  ];

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="max-w-7xl mx-auto px-6">
        <h1 className="text-3xl font-bold text-gray-800 mb-10">기술 블로그</h1>

        <div className="flex gap-8">
          {/* 왼쪽 사이드바 - 회사 목록 */}
          <div className="w-64 flex-shrink-0">
            <div className="sticky top-4">
              <h2 className="text-lg font-semibold text-gray-700 mb-4">
                회사별 보기
              </h2>
              <div className="space-y-2">
                {companies.map((company) => (
                  <button
                    key={company.id}
                    className={`w-full text-left px-4 py-2 rounded-lg text-sm font-medium transition-colors duration-200 flex items-center gap-3 ${
                      activeCompany === company.id ||
                      (company.id === "all" && activeCompany === null)
                        ? "bg-blue-600 text-white"
                        : "bg-white text-gray-800 hover:bg-gray-100"
                    } shadow-sm`}
                    onClick={() =>
                      setActiveCompany(company.id === "all" ? null : company.id)
                    }
                  >
                    <img
                      src={company.logoPath}
                      alt={`${company.displayName} 로고`}
                      className="w-6 h-6 object-contain"
                      onError={(e) => {
                        const target = e.target as HTMLImageElement;
                        target.src = "/company-logos/default-logo.svg";
                      }}
                    />
                    <span className="truncate">{company.displayName}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* 메인 컨텐츠 영역 */}
          <div className="flex-1">
            <div className="mb-8">
              <h2 className="text-lg font-semibold text-gray-700 mb-4">정렬</h2>
              <div className="flex gap-2">
                {sortOptions.map((option) => (
                  <button
                    key={option.id}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors duration-200 ${
                      sortOption === option.id
                        ? "bg-blue-600 text-white"
                        : "bg-white text-gray-800 hover:bg-gray-100"
                    } shadow-sm`}
                    onClick={() => setSortOption(option.id)}
                  >
                    {option.name}
                  </button>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-1 gap-8">
              {blogs.map((blog) => (
                <div
                  key={blog.id}
                  className="bg-white rounded-xl shadow-md hover:shadow-lg transition-shadow duration-300 overflow-hidden w-full"
                >
                  <div className="p-6 flex flex-col h-full">
                    <div className="flex items-center justify-between mb-2">
                      <span className="inline-block bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-semibold">
                        {getDisplayName(blog.provider)}
                      </span>
                      {isLoggedIn && (
                        <button
                          onClick={() => handleBookmarkToggle(blog)}
                          className={`flex items-center ${
                            blog.isBookmarked
                              ? "text-navy-400"
                              : "text-navy-600"
                          }`}
                        >
                          <svg
                            className="w-7 h-7"
                            fill={blog.isBookmarked ? "currentColor" : "none"}
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
                        </button>
                      )}
                    </div>

                    <h2 className="text-2xl font-bold mb-4 text-gray-800">
                      <a
                        href={blog.link}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="hover:underline"
                      >
                        {blog.title}
                      </a>
                    </h2>
                    <div className="flex flex-wrap gap-2 mb-4">
                      {blog.tags &&
                        blog.tags.length > 0 &&
                        blog.tags.map((tag) => (
                          <span
                            key={tag}
                            className="bg-gray-200 text-gray-700 px-2 py-1 rounded-full text-xs"
                          >
                            #{tag}
                          </span>
                        ))}
                    </div>
                    <p className="text-gray-600 mb-6 flex-grow text-sm line-clamp-1">
                      {blog.summary || "요약 없음"}
                    </p>

                    <div className="flex items-center text-gray-500 text-sm mb-4">
                      <span className="flex items-center mr-4">
                        <svg
                          className="w-4 h-4 mr-1"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M8 7V3m8 4V3M3 11h18M4 22h16a2 2 0 002-2V7a2 2 0 00-2-2H4a2 2 0 00-2 2v13a2 2 0 002 2z"
                          />
                        </svg>
                        {blog.publishDate}
                      </span>
                    </div>

                    <div className="mt-auto flex items-center justify-between">
                      <div className="flex items-center text-gray-500 text-sm">
                        <span className="flex items-center mr-4">
                          <svg
                            className="w-4 h-4 mr-1"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                            />
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                            />
                          </svg>
                          {blog.views.toLocaleString()}
                        </span>

                        <span className="flex items-center">
                          <svg
                            className="w-4 h-4 mr-1"
                            fill="none"
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
                          {blog.bookmarkCount.toLocaleString()}
                        </span>
                      </div>

                      <a
                        href="#"
                        onClick={async (e) => {
                          e.preventDefault();
                          await increaseViewCount(blog.id);
                          window.open(blog.link, "_blank");
                        }}
                        rel="noopener noreferrer"
                        className="inline-flex items-center text-blue-600 hover:text-blue-800"
                      >
                        <span className="mr-1">바로가기</span>
                        <span>→</span>
                      </a>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex justify-center mt-10 items-center gap-2">
                <button
                  className={`px-3 py-1 rounded ${
                    page === 0
                      ? "bg-gray-200 text-gray-500 cursor-not-allowed"
                      : "bg-white text-gray-800 hover:bg-gray-100"
                  }`}
                  onClick={() => fetchBlogs(page - 1)}
                  disabled={page === 0}
                >
                  이전
                </button>

                {Array.from({ length: totalPages }, (_, i) => i + 1)
                  .filter((pageNum) => {
                    const currentPage = page + 1;
                    let start, end;

                    if (currentPage <= 3) {
                      // 첫 페이지에서 3페이지 이하일 때
                      start = 1;
                      end = Math.min(5, totalPages);
                    } else if (currentPage >= totalPages - 2) {
                      // 마지막 페이지에서 2페이지 이하 남았을 때
                      start = Math.max(totalPages - 4, 1);
                      end = totalPages;
                    } else {
                      // 중간 페이지일 때
                      start = currentPage - 2;
                      end = currentPage + 2;
                    }

                    return pageNum >= start && pageNum <= end;
                  })
                  .map((pageNum) => (
                    <button
                      key={pageNum}
                      className={`px-3 py-1 rounded ${
                        pageNum === page + 1
                          ? "bg-blue-600 text-white"
                          : "bg-white text-gray-800 hover:bg-gray-100"
                      }`}
                      onClick={() => fetchBlogs(pageNum - 1)}
                    >
                      {pageNum}
                    </button>
                  ))}

                <button
                  className={`px-3 py-1 rounded ${
                    page === totalPages - 1
                      ? "bg-gray-200 text-gray-500 cursor-not-allowed"
                      : "bg-white text-gray-800 hover:bg-gray-100"
                  }`}
                  onClick={() => fetchBlogs(page + 1)}
                  disabled={page === totalPages - 1}
                >
                  다음
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
