"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { fetchWithAuth } from "@/app/util/api";
import { useRouter } from "next/navigation";
import dayjs from "dayjs";
import { BASE_URL } from '@/lib/api';
import { useAuth } from "@/context/AuthContext";

// 북마크 타입 정의
type BookmarkType = "class" | "TechBlog";

interface Bookmark {
  contentId: number;
  type: BookmarkType;
  title: string;
  module?: string;
  packageName?: string;
  url?: string;
  blogProvider?: string;
  summary?: string;
  link?: string;
  createAt: string;
  publishedDate?: string;
}

interface QuestionBookmark {
  questionId: number;
  question: string;
  answer: string;
  publishedAt: string;
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

export default function MyPage() {
  const router = useRouter();
  const { isLoggedIn, isLoading } = useAuth();
  const [activeTab, setActiveTab] = useState<"TechBlog" | "class" | "question">(
    "TechBlog"
  );
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [id, setId] = useState<number | null>(null);
  const [email, setEmail] = useState("user@example.com");
  const [username, setUsername] = useState("사용자"); // 카드에 표시되는 닉네임
  const [nicknameInput, setNicknameInput] = useState(""); // 모달 입력 필드
  const [nicknameError, setNicknameError] = useState<string | null>(null);
  const [bookmarks, setBookmarks] = useState<Bookmark[]>([]);
  const [questionBookmarks, setQuestionBookmarks] = useState<
    QuestionBookmark[]
  >([]);
  const [currentPage, setCurrentPage] = useState({
    techBlog: 1,
    class: 1,
    question: 1,
  });
  const [searchTerm, setSearchTerm] = useState("");
  const itemsPerPage = 5;


  useEffect(() => {
    if (!isLoading && !isLoggedIn) {
      router.push("/login");
    }
  }, [isLoggedIn, isLoading]);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await fetchWithAuth(`${BASE_URL}/api/profile`);
        if (res.ok) {
          const data = await res.json();
          setEmail(data.data.email);
          setUsername(data.data.username);
          setNicknameInput(data.data.username);
        } else {
          console.log("❌ 프로필 가져오기 실패");
        }
      } catch (err) {
        console.error("❌ 에러 발생:", err);
      }
    };

    const fetchBookmarks = async () => {
      try {
        const res = await fetchWithAuth(
          `${BASE_URL}/api/bookmarks/content`
        );
        if (res.ok) {
          const data = await res.json();
          console.log("✅ 북마크 데이터:", data.data);
          setBookmarks(data.data);
        } else {
          console.log("❌ 북마크 가져오기 실패");
        }
      } catch (err) {
        console.error("❌ 북마크 에러:", err);
      }
    };
    const fetchQuestionBookmarks = async () => {
      try {
        const res = await fetchWithAuth(
          `${BASE_URL}/api/bookmarks/question`
        );
        if (res.ok) {
          const data = await res.json();
          setQuestionBookmarks(data.data);
          console.log("✅ 북마크 질문 데이터:", data.data);
        } else {
          console.log("❌ 질문 북마크 가져오기 실패");
        }
      } catch (err) {
        console.error("❌ 질문 북마크 에러:", err);
      }
    };

    fetchProfile();
    fetchBookmarks();
    fetchQuestionBookmarks();
  }, []);

  const removeBookmark = async (id: number) => {
    try {
      const res = await fetchWithAuth(
        `${BASE_URL}/api/bookmarks/content/${id}`,
        {
          method: "DELETE",
        }
      );

      if (res.ok) {
        console.log(`✅ 북마크 ${id} 제거 성공`);
        setBookmarks((prev) => prev.filter((b) => b.contentId !== id)); // 화면에서도 즉시 제거
      } else {
        console.log(`❌ 북마크 제거 실패`);
      }
    } catch (err) {
      console.error("❌ 북마크 제거 에러:", err);
    }
  };

  const removeBookmarkQuestion = async (id: number) => {
    try {
      const res = await fetchWithAuth(
        `${BASE_URL}/api/bookmarks/question/${id}`,
        {
          method: "DELETE",
        }
      );

      if (res.ok) {
        console.log(`✅ 북마크 ${id} 제거 성공`);
        setBookmarks((prev) => prev.filter((b) => b.contentId !== id)); // 화면에서도 즉시 제거
      } else {
        console.log(`❌ 북마크 제거 실패`);
      }
    } catch (err) {
      console.error("❌ 북마크 제거 에러:", err);
    }
  };

  const updateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setNicknameError(null); // 이전 에러 초기화

    try {

      const res = await fetchWithAuth(`${BASE_URL}/api/profile?nickname=${nicknameInput}`, {

        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      });

      const data = await res.json();
      if (res.ok) {
        console.log("프로필 수정 성공");
        setUsername(nicknameInput); // 성공 시 카드에 반영
        setIsProfileModalOpen(false);
      } else {
        setNicknameError(data.message || "닉네임 변경 실패");
      }
    } catch (err) {
      console.error("프로필 수정 에러:", err);
      setNicknameError("서버 오류가 발생했습니다.");
    }
  };


  // 페이지네이션 관련 함수들
  const getPaginatedItems = (items: any[], page: number) => {
    const startIndex = (page - 1) * itemsPerPage;
    return items.slice(startIndex, startIndex + itemsPerPage);
  };

  const getTotalPages = (items: any[]) => {
    return Math.ceil(items.length / itemsPerPage);
  };

  // 필터링된 북마크
  const filteredTechBlogs = bookmarks
    .filter((b) => b.type === "TechBlog")
    .filter((b) =>
      b.blogProvider?.toLowerCase().includes(searchTerm.toLowerCase())
    )
    .sort(
      (a, b) =>
        new Date(b.publishedDate || "").getTime() -
        new Date(a.publishedDate || "").getTime()
    );

  const filteredClasses = bookmarks
    .filter((b) => b.type === "class")
    .sort(
      (a, b) => new Date(b.createAt).getTime() - new Date(a.createAt).getTime()
    );

  const sortedQuestions = [...questionBookmarks].sort(
    (a, b) =>
      new Date(b.publishedAt).getTime() - new Date(a.publishedAt).getTime()
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-blue-50">
      <div className="container mx-auto px-4 py-8">
        <div className="flex flex-col lg:flex-row gap-8">
          {/* 왼쪽 프로필 섹션 */}
          <div className="lg:w-1/6">
            <div className="bg-white rounded-2xl shadow-lg overflow-hidden sticky top-8">
              <div className="p-6 bg-gradient-to-b from-blue-600 to-indigo-700 text-white">
                <div className="flex flex-col items-center text-center">
                  <div className="w-16 h-16 rounded-full bg-white text-blue-600 flex items-center justify-center text-2xl font-bold shadow-lg mb-4">
                    {username.charAt(0)}
                  </div>
                  <h1 className="text-xl font-bold mb-1">{username}</h1>
                  <p className="text-blue-100 text-sm mb-4">{email}</p>
                  <button
                    onClick={() => setIsProfileModalOpen(true)}
                    className="w-full px-4 py-2 bg-white text-blue-600 rounded-xl shadow-lg hover:bg-gray-50 transition-all duration-200 text-sm font-medium"
                  >
                    프로필 수정
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* 오른쪽 북마크 섹션 */}
          <div className="lg:w-5/6 space-y-8">
            {/* 기술 블로그 북마크 */}
            <div className="bg-white rounded-2xl shadow-lg p-6">
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center">
                  <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                    <svg
                      className="w-6 h-6 text-green-600"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth="2"
                        d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9.5a2.5 2.5 0 00-2.5-2.5H15"
                      />
                    </svg>
                  </div>
                  <h2 className="text-xl font-bold ml-3">기술 블로그</h2>
                </div>
                <div className="relative">
                  <input
                    type="text"
                    placeholder="회사명으로 검색..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent w-64"
                  />
                </div>
              </div>
              <div className="space-y-4">
                {filteredTechBlogs.length === 0 ? (
                  <div className="text-center py-6 text-gray-500 bg-gray-50 rounded-xl">
                    북마크된 기술 블로그가 없습니다
                  </div>
                ) : (
                  <>
                    {getPaginatedItems(
                      filteredTechBlogs,
                      currentPage.techBlog
                    ).map((bookmark) => (
                      <div
                        key={bookmark.contentId}
                        className="border border-gray-100 rounded-xl p-4 hover:shadow-md transition-all duration-200 relative"
                      >
                        <a
                          href={bookmark.link}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="block"
                        >
                          <div className="text-xs font-medium text-green-600 mb-1">
                            {getDisplayName(bookmark.blogProvider || "")}
                          </div>
                          <h3 className="text-base font-medium mb-2">
                            {bookmark.title}
                          </h3>
                          <div className="text-gray-600 text-sm mb-2 line-clamp-1">
                            {bookmark.summary || "요약 없음"}
                          </div>
                          <div className="text-xs text-gray-400">
                            {dayjs(bookmark.publishedDate).format(
                              "YYYY년 MM월 DD일"
                            )}
                          </div>
                        </a>
                        <button
                          onClick={() => removeBookmark(bookmark.contentId)}
                          className="absolute top-2 right-2 text-gray-400 hover:text-red-500 transition-colors"
                        >
                          <svg
                            className="w-5 h-5"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth="2"
                              d="M6 18L18 6M6 6l12 12"
                            />
                          </svg>
                        </button>
                      </div>
                    ))}
                    <div className="flex justify-center mt-6 space-x-2">
                      {Array.from(
                        { length: getTotalPages(filteredTechBlogs) },
                        (_, i) => i + 1
                      ).map((page) => (
                        <button
                          key={page}
                          onClick={() =>
                            setCurrentPage((prev) => ({
                              ...prev,
                              techBlog: page,
                            }))
                          }
                          className={`px-3 py-1 rounded-lg ${
                            currentPage.techBlog === page
                              ? "bg-green-600 text-white"
                              : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                          }`}
                        >
                          {page}
                        </button>
                      ))}
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* 공식 문서 북마크 */}
            <div className="bg-white rounded-2xl shadow-lg p-6">
              <div className="flex items-center mb-6">
                <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                  <svg
                    className="w-6 h-6 text-blue-600"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                    />
                  </svg>
                </div>
                <h2 className="text-xl font-bold ml-3">공식 문서</h2>
              </div>
              <div className="space-y-4">
                {filteredClasses.length === 0 ? (
                  <div className="text-center py-6 text-gray-500 bg-gray-50 rounded-xl">
                    북마크된 공식 문서가 없습니다
                  </div>
                ) : (
                  <>
                    {getPaginatedItems(filteredClasses, currentPage.class).map(
                      (bookmark) => (
                        <div
                          key={bookmark.contentId}
                          className="border border-gray-100 rounded-xl p-4 hover:shadow-md transition-all duration-200 relative"
                        >
                          <Link
                            href={`/documents?classUid=${bookmark.contentId}&module=${bookmark.module}&package=${bookmark.packageName}&class=${bookmark.title}`}
                            className="block"
                          >
                            <div className="text-xs font-medium text-blue-600 mb-1">
                              공식 문서
                            </div>
                            <h3 className="text-base font-medium mb-2">
                              {bookmark.title}
                            </h3>
                            <div className="text-sm text-gray-500 space-y-1">
                              <div>모듈: {bookmark.module}</div>
                              <div>패키지: {bookmark.packageName}</div>
                            </div>
                            <div className="text-xs text-gray-400 mt-2">
                              {dayjs(bookmark.createAt).format(
                                "YYYY년 MM월 DD일"
                              )}
                            </div>
                          </Link>
                          <button
                            onClick={() => removeBookmark(bookmark.contentId)}
                            className="absolute top-2 right-2 text-gray-400 hover:text-red-500 transition-colors"
                          >
                            <svg
                              className="w-5 h-5"
                              fill="none"
                              stroke="currentColor"
                              viewBox="0 0 24 24"
                            >
                              <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth="2"
                                d="M6 18L18 6M6 6l12 12"
                              />
                            </svg>
                          </button>
                        </div>
                      )
                    )}
                    <div className="flex justify-center mt-6 space-x-2">
                      {Array.from(
                        { length: getTotalPages(filteredClasses) },
                        (_, i) => i + 1
                      ).map((page) => (
                        <button
                          key={page}
                          onClick={() =>
                            setCurrentPage((prev) => ({ ...prev, class: page }))
                          }
                          className={`px-3 py-1 rounded-lg ${
                            currentPage.class === page
                              ? "bg-blue-600 text-white"
                              : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                          }`}
                        >
                          {page}
                        </button>
                      ))}
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* 오늘의 질문 북마크 */}
            <div className="bg-white rounded-2xl shadow-lg p-6">
              <div className="flex items-center mb-6">
                <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
                  <svg
                    className="w-6 h-6 text-red-600"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                </div>
                <h2 className="text-xl font-bold ml-3">오늘의 질문</h2>
              </div>
              <div className="space-y-4">
                {sortedQuestions.length === 0 ? (
                  <div className="text-center py-6 text-gray-500 bg-gray-50 rounded-xl">
                    북마크된 질문이 없습니다
                  </div>
                ) : (
                  <>
                    {getPaginatedItems(
                      sortedQuestions,
                      currentPage.question
                    ).map((qa) => (
                      <div
                        key={qa.questionId}
                        className="border border-gray-100 rounded-xl p-4 hover:shadow-md transition-all duration-200 cursor-pointer relative"
                        onClick={() =>
                          router.push(
                            `/today-question?history=${qa.questionId}`
                          )
                        }
                      >
                        <div className="text-xs font-medium text-red-600 mb-1">
                          오늘의 질문
                        </div>
                        <h3 className="text-base font-medium mb-2">
                          {qa.question}
                        </h3>
                        <p className="text-gray-600 text-sm mb-2 line-clamp-1">
                          {qa.answer || "답변 없음"}
                        </p>
                        <div className="text-xs text-gray-400">
                          {dayjs(qa.publishedAt).format("YYYY년 MM월 DD일")}
                        </div>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            removeBookmarkQuestion(qa.questionId);
                          }}
                          className="absolute top-2 right-2 text-gray-400 hover:text-red-500 transition-colors"
                        >
                          <svg
                            className="w-5 h-5"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth="2"
                              d="M6 18L18 6M6 6l12 12"
                            />
                          </svg>
                        </button>
                      </div>
                    ))}
                    <div className="flex justify-center mt-6 space-x-2">
                      {Array.from(
                        { length: getTotalPages(sortedQuestions) },
                        (_, i) => i + 1
                      ).map((page) => (
                        <button
                          key={page}
                          onClick={() =>
                            setCurrentPage((prev) => ({
                              ...prev,
                              question: page,
                            }))
                          }
                          className={`px-3 py-1 rounded-lg ${
                            currentPage.question === page
                              ? "bg-red-600 text-white"
                              : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                          }`}
                        >
                          {page}
                        </button>
                      ))}
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 프로필 수정 모달 */}
      {isProfileModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full">
            <div className="p-8">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold">프로필 수정</h2>
                <button
                  onClick={() => setIsProfileModalOpen(false)}
                  className="text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <svg
                    className="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>

              <form onSubmit={updateProfile} className="space-y-6">
                <div>
                  <label
                      htmlFor="username"
                      className="block text-sm font-medium text-gray-700 mb-2"
                  >
                    사용자 이름
                  </label>
                  <input
                      id="username"
                      type="text"
                      className={`w-full px-4 py-3 border rounded-xl focus:ring-2 transition-all ${
                          nicknameError
                              ? "border-red-500 focus:ring-red-500"
                              : "border-gray-300 focus:ring-blue-500 focus:border-transparent"
                      }`}
                      value={nicknameInput}
                      onChange={(e) => setNicknameInput(e.target.value)}
                      required
                  />
                  {nicknameError && (
                      <p className="text-sm text-red-500 mt-2">{nicknameError}</p>
                  )}
                </div>

                <div>
                  <label
                      htmlFor="profile-email"
                      className="block text-sm font-medium text-gray-700 mb-2"
                  >
                    이메일
                  </label>
                  <input
                      id="profile-email"
                      type="email"
                      className="w-full px-4 py-3 border border-gray-300 rounded-xl bg-gray-50 cursor-not-allowed"
                      value={email}
                      disabled
                  />
                  <p className="text-xs text-gray-500 mt-2">
                    이메일은 변경할 수 없습니다.
                  </p>
                </div>

                <div className="flex justify-end space-x-4 pt-4">
                  <button
                    type="button"
                    className="px-6 py-3 border border-gray-300 rounded-xl text-gray-700 bg-white hover:bg-gray-50 transition-all duration-200"
                    onClick={() => setIsProfileModalOpen(false)}
                  >
                    취소
                  </button>
                  <button
                    type="submit"
                    className="px-6 py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-all duration-200"
                  >
                    저장
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
