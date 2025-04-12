"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useAuth } from "@/context/AuthContext";
import { BASE_URL } from '@/lib/api';

export default function Home() {
  const [answer, setAnswer] = useState("");
  const { isLoggedIn } = useAuth();
  const [popularBlogs, setPopularBlogs] = useState<any[]>([]);
  const [popularDocs, setPopularDocs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // 방문 API 호출
  useEffect(() => {
    fetch(`${BASE_URL}/api/visit`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
    }).catch((err) => console.error("방문자 수 API 호출 실패", err));
  }, []);

  // 인기 기술 블로그와 문서 게시물 데이터 가져오기
  useEffect(() => {
    const fetchPopularContent = async () => {
      try {
        setLoading(true);
        // 인기 기술 블로그 가져오기
        const blogResponse = await fetch(
          `${BASE_URL}/api/techblog/popular`
        );
        if (!blogResponse.ok) throw new Error("Failed to fetch popular blogs");
        const blogData = await blogResponse.json();
        setPopularBlogs(blogData.content);

        // 인기 문서 게시물 가져오기
        const docResponse = await fetch(
          `${BASE_URL}/api/javadoc/popular`
        );
        if (!docResponse.ok) throw new Error("Failed to fetch popular docs");
        const docData = await docResponse.json();
        setPopularDocs(docData);
      } catch (error) {
        console.error("데이터 로드 실패:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchPopularContent();
  }, []);

  const handleAnswerSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // 실제 구현에서는 여기서 로그인 체크 후 처리
    window.location.href = "/today-question";
  };

  // 북마크 토글 핸들러
  const toggleBookmark = async (
    e: React.MouseEvent,
    type: string,
    id: number
  ) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isLoggedIn) {
      alert("북마크 기능은 로그인 후 이용 가능합니다.");
      return;
    }

    try {
      const endpoint = `${BASE_URL}/api/bookmarks/content/${id}`;
      const accessToken = localStorage.getItem("accessToken");

      if (!accessToken) {
        alert("로그인 후 이용 가능합니다!");
        return;
      }

      // 현재 북마크 상태 확인
      const currentItem =
        type === "blog"
          ? popularBlogs.find((blog) => blog.id === id)
          : popularDocs.find((doc) => doc.id === id);

      if (!currentItem) return;

      const isCurrentlyBookmarked = currentItem.bookmarked;
      const method = isCurrentlyBookmarked ? "DELETE" : "POST";

      const response = await fetch(endpoint, {
        method,
        headers: {
          "Content-Type": "application/json",
          Authorization: accessToken,
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`북마크 ${method === "POST" ? "추가" : "해제"} 실패`);
      }

      // 상태 업데이트
      if (type === "blog") {
        setPopularBlogs((prevBlogs) =>
          prevBlogs.map((blog) =>
            blog.id === id
              ? {
                  ...blog,
                  bookmarked: !blog.bookmarked,
                  bookmarkCount: blog.bookmarked
                    ? blog.bookmarkCount - 1
                    : blog.bookmarkCount + 1,
                }
              : blog
          )
        );
      } else {
        setPopularDocs((prevDocs) =>
          prevDocs.map((doc) =>
            doc.id === id
              ? {
                  ...doc,
                  bookmarked: !doc.bookmarked,
                  bookmarkUsersList: doc.bookmarked
                    ? doc.bookmarkUsersList.filter(
                        (user: string) =>
                          user !== localStorage.getItem("userId")
                      )
                    : [
                        ...doc.bookmarkUsersList,
                        localStorage.getItem("userId"),
                      ],
                }
              : doc
          )
        );
      }
    } catch (error) {
      console.error("북마크 처리 실패:", error);
      alert("북마크 처리 중 오류가 발생했습니다.");
    }
  };

  return (
    <div className="bg-navy-50">
      {/* 첫 번째 섹션: 오늘의 질문 */}
      <section className="bg-gradient-to-r from-navy-800 to-navy-900 text-white py-20">
        <div className="container mx-auto px-4">
          <div className="flex flex-col items-center justify-center">
            <div className="w-full max-w-4xl">
              {/* 채팅 형태의 대화 */}
              <div className="space-y-20">
                {/* 왼쪽 - 오늘의 질문 (시스템) */}
                <div className="flex items-start">
                  <div className="flex-shrink-0 mr-3">
                    <div className="w-10 h-10 rounded-full bg-navy-600 flex items-center justify-center">
                      <span className="text-white font-bold">Q</span>
                    </div>
                  </div>
                  <div className="relative bg-white text-navy-800 p-5 rounded-lg rounded-tl-none shadow-md max-w-[80%]">
                    <div className="absolute top-1 -left-2 w-4 h-4 rotate-45 bg-white"></div>
                    <div className="font-semibold mb-2">오늘의 질문</div>
                    <p className="text-lg">
                      "자바에서 Stream API와 for 루프의 성능 차이는 어떤
                      상황에서 발생하나요?"
                    </p>
                  </div>
                </div>

                {/* 오른쪽 - 사용자 예시 답변 */}
                <div className="flex items-start justify-end">
                  <div className="relative bg-navy-200 text-navy-900 p-5 rounded-lg rounded-tr-none shadow-md max-w-[80%]">
                    <div className="absolute top-1 -right-2 w-4 h-4 rotate-45 bg-navy-200"></div>
                    <div className="font-semibold mb-2">사용자</div>
                    <p className="text-lg">
                      "데이터 크기가 작고 단순 반복만 필요한 경우 for 루프가
                      오버헤드 없이 빠르지만, 대용량 데이터나 병렬 처리가 필요한
                      경우 Stream API가 더 효율적입니다..."
                    </p>
                  </div>
                  <div className="flex-shrink-0 ml-3">
                    <div className="w-10 h-10 rounded-full bg-navy-400 flex items-center justify-center">
                      <span className="text-white font-bold">U</span>
                    </div>
                  </div>
                </div>

                {/* 왼쪽 - AI 응답 피드백 */}
                <div className="flex items-start">
                  <div className="flex-shrink-0 mr-3">
                    <div className="w-10 h-10 rounded-full bg-navy-500 flex items-center justify-center">
                      <span className="text-white font-bold">AI</span>
                    </div>
                  </div>
                  <div className="relative bg-white text-navy-900 p-5 rounded-lg rounded-tl-none shadow-md max-w-[80%]">
                    <div className="absolute top-1 -left-2 w-4 h-4 rotate-45 bg-white"></div>
                    <div className="font-semibold mb-2">AI 평가</div>
                    <p className="text-lg mb-2">
                      "훌륭한 답변입니다! 정확히 지적하셨습니다. 다음과 같은
                      점도 고려해볼 수 있습니다:"
                    </p>
                    <ul className="list-disc list-inside text-navy-800 space-y-2 mb-2">
                      <li>작은 데이터셋에서 Stream 초기화 오버헤드</li>
                      <li>복잡한 연산에서의 Stream 최적화 이점</li>
                      <li>가독성과 유지보수성 측면의 장단점</li>
                    </ul>
                    <div className="mt-3 pt-3 border-t border-navy-200">
                      <div className="flex items-center">
                        <span className="text-navy-600 font-medium">
                          타당성 점수:
                        </span>
                        <div className="ml-2 flex">
                          <span className="text-green-600 font-bold">
                            95/100
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="text-center mt-12">
                <Link
                  href="/today-question"
                  className="inline-block bg-white text-navy-800 font-medium py-3 px-8 rounded-full hover:bg-gray-100 shadow-lg transition-all transform hover:scale-105"
                >
                  답변 제출하러가기
                </Link>
                <p className="text-white mt-4 opacity-80">
                  매일 새로운 개발 질문에 답하고 AI의 즉각적인 피드백을
                  받아보세요.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 두 번째 섹션: 인기 기술 블로그 및 문서 게시글 */}
      <section className="py-16 bg-gray-50">
        <div className="container mx-auto px-4">
          {/* 인기 기술 블로그 */}
          <div className="mb-16">
            <h2 className="text-2xl font-bold mb-8 text-gray-800 flex items-center border-b border-gray-200 pb-4">
              <span className="mr-2">📚</span> 인기 기술 블로그
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
              {loading ? (
                // 로딩 상태 표시
                Array(3)
                  .fill(0)
                  .map((_, index) => (
                    <div
                      key={index}
                      className="bg-white rounded-xl shadow-md overflow-hidden h-full animate-pulse"
                    >
                      <div className="p-6">
                        <div className="h-4 bg-gray-200 rounded w-1/4 mb-4"></div>
                        <div className="h-6 bg-gray-200 rounded w-3/4 mb-3"></div>
                        <div className="h-4 bg-gray-200 rounded w-full mb-3"></div>
                        <div className="h-4 bg-gray-200 rounded w-5/6"></div>
                      </div>
                    </div>
                  ))
              ) : popularBlogs.length > 0 ? (
                popularBlogs.map((blog) => (
                  <a
                    key={blog.id}
                    href={blog.link}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-all duration-300 cursor-pointer h-full"
                  >
                    <div className="p-6 flex flex-col h-full">
                      <div className="flex items-center justify-between mb-4">
                        <span className="inline-block bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-semibold">
                          {blog.provider}
                        </span>
                        {isLoggedIn && (
                          <button
                            onClick={(e) => toggleBookmark(e, "blog", blog.id)}
                            className="focus:outline-none text-navy-400 hover:text-navy-600"
                            aria-label={
                              blog.bookmarked ? "북마크 제거" : "북마크 추가"
                            }
                          >
                            <svg
                              className="w-6 h-6"
                              fill={blog.bookmarked ? "currentColor" : "none"}
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

                      <h3 className="text-xl font-bold mb-3 text-gray-800 hover:text-blue-600 transition-colors duration-200">
                        {blog.title}
                      </h3>
                      <p className="text-gray-600 mb-6 flex-grow line-clamp-2">
                        {blog.summary}
                      </p>

                      <div className="mt-auto flex items-center justify-between pt-4 border-t border-gray-100">
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

                        <span className="text-blue-600 hover:text-blue-800 text-sm font-medium">
                          바로가기 →
                        </span>
                      </div>
                    </div>
                  </a>
                ))
              ) : (
                <div className="col-span-full text-center py-8 text-gray-500">
                  인기 기술 블로그가 없습니다.
                </div>
              )}
            </div>
            <div className="mt-8 text-center">
              <Link
                href="/blog"
                className="inline-block text-blue-600 font-medium hover:text-blue-800 border-b-2 border-blue-300 hover:border-blue-800 transition-colors"
              >
                더 많은 기술 블로그 보기 →
              </Link>
            </div>
          </div>

          {/* 인기 문서 게시물 */}
          <div>
            <h2 className="text-2xl font-bold mb-8 text-gray-800 flex items-center border-b border-gray-200 pb-4">
              <span className="mr-2">📝</span> 인기 문서 게시물
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {loading ? (
                // 로딩 상태 표시
                Array(3)
                  .fill(0)
                  .map((_, index) => (
                    <div
                      key={index}
                      className="bg-white rounded-xl shadow-md overflow-hidden h-full animate-pulse"
                    >
                      <div className="p-6">
                        <div className="h-4 bg-gray-200 rounded w-1/4 mb-4"></div>
                        <div className="h-6 bg-gray-200 rounded w-3/4 mb-3"></div>
                        <div className="h-4 bg-gray-200 rounded w-full mb-3"></div>
                        <div className="h-4 bg-gray-200 rounded w-5/6"></div>
                      </div>
                    </div>
                  ))
              ) : popularDocs && popularDocs.length > 0 ? (
                popularDocs.map((doc) => (
                  <Link
                    key={doc.id}
                    href={`/documents?classUid=${doc.uid}`}
                    className="block bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-all duration-300 cursor-pointer h-full"
                  >
                    <div className="p-6 flex flex-col h-full">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex gap-2">
                          <span className="inline-block bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-semibold">
                            {doc.contentDetail?.type || "Unknown Type"}
                          </span>
                        </div>
                        {isLoggedIn && (
                          <button
                            onClick={(e) => toggleBookmark(e, "doc", doc.id)}
                            className="focus:outline-none text-navy-400 hover:text-navy-600"
                            aria-label={
                              doc.bookmarked ? "북마크 제거" : "북마크 추가"
                            }
                          >
                            <svg
                              className="w-6 h-6"
                              fill={doc.bookmarked ? "currentColor" : "none"}
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

                      <h3 className="text-xl font-bold mb-3 text-gray-800 hover:text-blue-600 transition-colors duration-200">
                        {doc.contentDetail?.name || "제목 없음"}
                      </h3>
                      <p className="text-gray-600 mb-6 flex-grow line-clamp-2">
                        {doc.contentDetail?.description || "설명 없음"}
                      </p>

                      <div className="mt-auto flex items-center justify-between pt-4 border-t border-gray-100">
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
                            {doc.views?.toLocaleString() || "0"}
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
                            {doc.bookmarkUsersList?.length?.toLocaleString() ||
                              "0"}
                          </span>
                        </div>

                        <span className="text-blue-600 hover:text-blue-800 text-sm font-medium">
                          바로가기 →
                        </span>
                      </div>
                    </div>
                  </Link>
                ))
              ) : (
                <div className="col-span-full text-center py-8 text-gray-500">
                  인기 문서 게시물이 없습니다.
                </div>
              )}
            </div>
            <div className="mt-8 text-center">
              <Link
                href="/documents"
                className="inline-block text-blue-600 font-medium hover:text-blue-800 border-b-2 border-blue-300 hover:border-blue-800 transition-colors"
              >
                더 많은 문서 보기 →
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
