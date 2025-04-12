"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { fetchWithAuth } from "../util/api";
import { toggleBookmark } from "../../lib/question/question";
import { BASE_URL } from '@/lib/api';

// API 응답 타입 정의
type Question = {
  id: number;
  question: string;
  questionType: string;
  date: string;
  answers: {
    score: string;
    answer: string;
    user_id: string;
    llm_answer: string;
  } | null;
  today: boolean;
};

type QuestionHistoryItem = {
  id: number;
  date: string;
  question: string;
  userAnswer: string;
  llmAnswer: string;
  today: boolean;
  bookmarked: boolean;
};

// API 호출 함수
const fetchTodayQuestion = async (): Promise<Question> => {
  const response = await fetchWithAuth(
    `${BASE_URL}/api/questions/today`
  );
  if (!response.ok) throw new Error("Failed to fetch today question");
  return response.json();
};

const fetchQuestionHistory = async (): Promise<QuestionHistoryItem[]> => {
  const response = await fetchWithAuth(
    `${BASE_URL}/api/questions/history`
  );
  if (!response.ok) throw new Error("Failed to fetch question history");
  return response.json();
};

const fetchQuestionById = async (id: number): Promise<Question> => {
  const response = await fetchWithAuth(
    `${BASE_URL}/api/questions/${id}`
  );
  if (!response.ok) throw new Error("Failed to fetch question");
  return response.json();
};

const saveUserAnswer = async (questionId: number, userAnswer: string) => {
  const response = await fetchWithAuth(
    `${BASE_URL}/api/questions/${questionId}/answer`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ answer: userAnswer }),
    }
  );
  if (!response.ok) throw new Error("Failed to save user answer");
  return response.json();
};

const saveLlmAnswer = async (questionId: number, llmAnswer: string) => {
  const response = await fetchWithAuth(
    `${BASE_URL}/api/questions/${questionId}/llm-answer`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ answer: llmAnswer }),
    }
  );
  if (!response.ok) throw new Error("Failed to save LLM answer");
  return response.json();
};

export default function TodayQuestionPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // 답변 형식화 함수
  const formatLlmAnswer = (
    llmAnswer: string,
    score: string | number
  ): string => {
    if (!llmAnswer) return "AI 답변이 없습니다.";
    return `${llmAnswer}\n\n타당성 점수: ${score}/100`;
  };

  // 상태 관리
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [userAnswer, setUserAnswer] = useState("");
  const [aiAnswer, setAiAnswer] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [hasAnswered, setHasAnswered] = useState(false);
  const [selectedHistory, setSelectedHistory] = useState<number | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [messages, setMessages] = useState<
    { type: "user" | "ai"; content: string }[]
  >([]);
  const [questionHistory, setQuestionHistory] = useState<QuestionHistoryItem[]>(
    []
  );
  const [todayQuestion, setTodayQuestion] = useState<Question | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [bookmarks, setBookmarks] = useState<number[]>([1]);
  const [hasTodayAnswer, setHasTodayAnswer] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // Refs
  const chatEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const prevAiAnswerRef = useRef<string>("");

  // 인증 상태 체크
  useEffect(() => {
    const checkAuth = async () => {
      try {
        // 실제 API 요청을 통해 토큰 유효성 검증
        const [today, history] = await Promise.all([
          fetchTodayQuestion(),
          fetchQuestionHistory(),
        ]);

        setIsAuthenticated(true);
        setTodayQuestion(today);
        setQuestionHistory(history);

        // URL에서 history 파라미터 확인
        const historyId = searchParams.get('history');

        // 오늘 날짜의 답변이 있는지 확인
        const todayAnswerExists = history.some((q) => q.today && q.userAnswer);
        setHasTodayAnswer(todayAnswerExists);

        // 히스토리 ID가 URL에 있으면 해당 질문 선택
        if (historyId) {
          const selectedHistoryItem = history.find((q) => q.id === parseInt(historyId));

          if (selectedHistoryItem) {
            setSelectedHistory(selectedHistoryItem.id);

            // 해당 질문의 답변 불러오기
            try {
              const question = await fetchQuestionById(selectedHistoryItem.id);

              // answers JSON 파싱
              let answers = null;
              try {
                answers =
                    typeof question.answers === "string"
                        ? JSON.parse(question.answers)
                        : question.answers;
              } catch (e) {
                console.error("Failed to parse answers:", e);
              }

              // 사용자 답변과 LLM 답변 추출
              const userAnswer = answers?.[0]?.answer || "답변이 없습니다.";
              const llmAnswer = answers?.[0]?.llm_answer || "AI 답변이 없습니다.";
              const score = answers?.[0]?.score || 0;

              if (
                  userAnswer !== "답변이 없습니다." ||
                  llmAnswer !== "AI 답변이 없습니다."
              ) {
                setMessages([
                  {
                    type: "user",
                    content: userAnswer,
                  },
                  {
                    type: "ai",
                    content: formatLlmAnswer(llmAnswer, score),
                  },
                ]);
                setHasAnswered(true);
              }
            } catch (err) {
              console.error("Failed to fetch question details:", err);
            }
          }
        } else if (todayAnswerExists) {
          // 오늘 답변이 있으면 해당 답변 표시 (기존 로직)
          const todayQuestion = history.find((q) => q.today);
          if (todayQuestion) {
            setSelectedHistory(todayQuestion.id);
            let answers = null;
            try {
              answers =
                  typeof today.answers === "string"
                      ? JSON.parse(today.answers)
                      : today.answers;
            } catch (e) {
              console.error("Failed to parse answers:", e);
            }

            const userAnswer = answers?.[0]?.answer || "답변이 없습니다.";
            const llmAnswer = answers?.[0]?.llm_answer || "AI 답변이 없습니다.";
            const score = answers?.[0]?.score || 0;

            if (
                userAnswer !== "답변이 없습니다." ||
                llmAnswer !== "AI 답변이 없습니다."
            ) {
              setMessages([
                {
                  type: "user",
                  content: userAnswer,
                },
                {
                  type: "ai",
                  content: formatLlmAnswer(llmAnswer, score),
                },
              ]);
              setHasAnswered(true);
            }
          }
        }
      } catch (err) {
        console.error("Authentication check failed:", err);
        setIsAuthenticated(false);
        if (err instanceof Error && err.message === "Unauthorized") {
          router.push("/login");
        }
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  // EventSource 정리
  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  // SSE 스트리밍 효과 처리
  useEffect(() => {
    if (aiAnswer === prevAiAnswerRef.current) return;

    if (
      aiAnswer &&
      messages.length > 0 &&
      messages[messages.length - 1].type === "ai"
    ) {
      prevAiAnswerRef.current = aiAnswer;
      setMessages((prev) => {
        if (prev[prev.length - 1].content === aiAnswer) {
          return prev;
        }
        return [
          ...prev.slice(0, prev.length - 1),
          { type: "ai", content: aiAnswer },
        ];
      });
    }
  }, [aiAnswer, messages]);

  // AI 답변 저장
  useEffect(() => {
    if (aiAnswer && !isStreaming && isAuthenticated) {
      const currentQuestion = selectedHistory
        ? questionHistory.find((q) => q.id === selectedHistory)
        : todayQuestion;

      // SSE 서버가 정상적으로 응답한 경우에만 AI 답변 저장
      if (
        currentQuestion &&
        aiAnswer !== "서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요."
      ) {
        // 점수 추출 및 포맷팅된 답변 생성
        const scoreMatch = aiAnswer.match(/타당성 점수: (\d+)\/100/);
        const score = scoreMatch ? parseInt(scoreMatch[1]) : 0;
        const formattedAnswer = aiAnswer
          .replace(/타당성 점수: \d+\/100/, "")
          .trim();

        // LLM 답변 저장 API 호출
        fetchWithAuth(
          `${BASE_URL}/api/questions/${currentQuestion.id}/llm-answer`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: localStorage.getItem("accessToken") || "",
            },
            body: JSON.stringify({
              answer: formattedAnswer,
              score: score,
            }),
          }
        )
          .then(() => {
            // 메시지 업데이트
            setMessages((prev) => {
              const newMessages = [...prev];
              if (newMessages.length > 0) {
                newMessages[newMessages.length - 1].content = formatLlmAnswer(
                  formattedAnswer,
                  score
                );
              }
              return newMessages;
            });
            setIsEditing(false);
          })
          .catch(console.error);
      }
    }
  }, [aiAnswer, isStreaming, isAuthenticated]);

  // 로딩 중일 때 표시할 컴포넌트
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  // 북마크 토글 함수
  // Question 북마크 토글 함수
  const handleBookmarkToggle = async (
      questionId: number,
      isCurrentlyBookmarked: boolean
  ) => {
    if (!isAuthenticated) {
      alert("로그인 후 이용해 주세요!");
      return;
    }

    try {
      // API 호출 전에 먼저 낙관적 UI 업데이트
      setQuestionHistory((prev) =>
          prev.map((item) =>
              item.id === questionId
                  ? { ...item, bookmarked: !isCurrentlyBookmarked }
                  : item
          )
      );

      // 북마크 토글 API 호출
      await toggleBookmark(questionId, isCurrentlyBookmarked);
    } catch (err) {
      console.error("북마크 실패:", err);

      // API 호출 실패 시 원래 상태로 롤백
      setQuestionHistory((prev) =>
          prev.map((item) =>
              item.id === questionId
                  ? { ...item, bookmarked: isCurrentlyBookmarked }
                  : item
          )
      );
    }
  };

  // 자동 스크롤 함수 - 필요시 스크롤 위치를 맨 아래로 이동
  const scrollToBottom = () => {
    if (!messagesContainerRef.current) return;

    const container = messagesContainerRef.current;

    // 현재 스크롤 위치 확인
    const isNearBottom =
      container.scrollHeight - container.scrollTop - container.clientHeight <
      150;

    // 사용자가 이미 스크롤을 맨 아래 근처로 내려놓은 경우에만 자동 스크롤
    if (isNearBottom) {
      // 브라우저 렌더링 사이클이 완료된 후 스크롤
      setTimeout(() => {
        container.scrollTo({
          top: container.scrollHeight,
          behavior: "smooth",
        });
      }, 10);
    }
  };

  // 실제 SSE 스트리밍을 사용하는 함수
  const streamFromSSE = (chatInput: string) => {
    setIsStreaming(true);
    setAiAnswer("");
    prevAiAnswerRef.current = ""; // AI 답변 추적 ref 초기화

    // 이전 EventSource가 있으면 정리
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    const url = `${BASE_URL}/api/question/chat/stream?chatdata=${encodeURIComponent(
      chatInput
    )}`;
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;

    let fullResponse = "";
    let score: number | null = null;

    eventSource.onmessage = (event) => {
      console.log("Raw SSE data:", event.data);

      // "[DONE]" 메시지를 받으면 연결 종료
      if (event.data.trim() === "[DONE]") {
        console.log("Received [DONE] message, closing connection");
        eventSource.close();
        eventSourceRef.current = null;
        setIsStreaming(false);

        // 최종 응답에서 점수 추출
        const scoreMatch = fullResponse.match(/타당성 점수: (\d+)\/100/);
        if (scoreMatch) {
          score = parseInt(scoreMatch[1]);
          fullResponse = fullResponse
            .replace(/타당성 점수: \d+\/100/, "")
            .trim();
        }

        // 최종 응답 설정
        setAiAnswer(formatLlmAnswer(fullResponse, score || 0));
        return;
      }

      try {
        const parsed = JSON.parse(event.data);
        const choice = parsed?.choices && parsed.choices[0];

        if (
          choice &&
          choice.finish_reason === "stop" &&
          !choice.delta?.content
        ) {
          console.log("Received finish_reason=stop, closing connection");
          eventSource.close();
          eventSourceRef.current = null;
          setIsStreaming(false);
          return;
        }

        const content = choice?.delta?.content;
        if (content) {
          fullResponse += content;
          setAiAnswer((prev) => {
            const newContent = prev + content;
            setTimeout(scrollToBottom, 10);
            return newContent;
          });
        }
      } catch (error) {
        fullResponse += event.data;
        setAiAnswer((prev) => {
          const newContent = prev + event.data;
          setTimeout(scrollToBottom, 10);
          return newContent;
        });
      }
    };

    eventSource.onerror = (error) => {
      console.error("SSE 에러:", error);
      eventSource.close();
      eventSourceRef.current = null;
      setIsStreaming(false);

      setMessages((prev) => [
        ...prev.slice(0, -1),
        {
          type: "ai",
          content: "서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.",
        },
      ]);
      setHasAnswered(false);
      setUserAnswer((prev) => prev);
    };

    eventSource.addEventListener("end", () => {
      console.log("SSE 'end' 이벤트 수신");
      eventSource.close();
      eventSourceRef.current = null;
      setIsStreaming(false);
    });

    return () => {
      eventSource.close();
      eventSourceRef.current = null;
    };
  };

  // SSE 스트리밍을 시뮬레이션하는 함수 (실제 API 연결이 불가능할 경우를 위한 백업)
  const simulateStreaming = (fullAnswer: string) => {
    setIsStreaming(true);
    setAiAnswer("");
    prevAiAnswerRef.current = ""; // AI 답변 추적 ref 초기화

    const words = fullAnswer.trim().split(" ");
    let currentIndex = 0;
    let intervalId: NodeJS.Timeout | null = null;

    const cleanup = () => {
      if (intervalId) {
        clearInterval(intervalId);
        intervalId = null;
      }
      setIsStreaming(false);
    };

    intervalId = setInterval(() => {
      if (currentIndex < words.length) {
        setAiAnswer((prev) => {
          const newContent = prev + " " + words[currentIndex];
          // 스크롤 함수 호출
          setTimeout(scrollToBottom, 10);
          return newContent;
        });
        currentIndex++;
      } else {
        console.log("Simulated streaming complete");
        cleanup();
      }
    }, 100);

    // 컴포넌트 언마운트 혹은 함수 재호출 시 정리 함수 반환
    return cleanup;
  };

  // 메시지 초기화 함수 - 이력 선택 시 사용
  const resetMessages = () => {
    setMessages([]);
    setHasAnswered(false);
    setAiAnswer("");
    prevAiAnswerRef.current = ""; // AI 답변 추적 ref 초기화
    setUserAnswer("");
  };

  // 히스토리 선택 처리
  const handleHistorySelect = async (id: number, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (selectedHistory === id) return;

    const currentScrollY = window.scrollY;
    setSelectedHistory(id);

    // 선택된 히스토리 항목 찾기
    const selected = questionHistory.find((q) => q.id === id);
    if (!selected) return;

    // 답변이 있는 경우 먼저 표시
    if (selected.userAnswer || selected.llmAnswer) {
      setMessages([
        {
          type: "user",
          content: selected.userAnswer || "답변이 없습니다.",
        },
        {
          type: "ai",
          content: selected.llmAnswer || "AI 답변이 없습니다.",
        },
      ]);
      setHasAnswered(true);
    } else {
      resetMessages();
    }

    window.scrollTo({ top: currentScrollY });

    try {
      const question = await fetchQuestionById(id);

      // answers JSON 파싱
      let answers = null;
      try {
        answers =
          typeof question.answers === "string"
            ? JSON.parse(question.answers)
            : question.answers;
      } catch (e) {
        console.error("Failed to parse answers:", e);
      }

      // 사용자 답변과 LLM 답변 추출
      const userAnswer = answers?.[0]?.answer || "답변이 없습니다.";
      const llmAnswer = answers?.[0]?.llm_answer || "AI 답변이 없습니다.";
      const score = answers?.[0]?.score || 0;

      if (
        userAnswer !== "답변이 없습니다." ||
        llmAnswer !== "AI 답변이 없습니다."
      ) {
        setMessages([
          {
            type: "user",
            content: userAnswer,
          },
          {
            type: "ai",
            content: formatLlmAnswer(llmAnswer, score),
          },
        ]);
        setHasAnswered(true);
      }
    } catch (err) {
      console.error("Failed to fetch question details:", err);
      // 에러 발생 시 메시지 표시
      setMessages([
        {
          type: "ai",
          content: "답변을 불러오는데 실패했습니다. 다시 시도해주세요.",
        },
      ]);
    }
  };

  // 답변 수정 모드 활성화
  const handleEditMode = () => {
    const currentQuestion = selectedHistory
      ? questionHistory.find((q) => q.id === selectedHistory)
      : todayQuestion;

    if (!currentQuestion?.today) return;

    // 현재 사용자 답변을 입력창에 설정
    const currentUserAnswer =
      messages.find((m) => m.type === "user")?.content || "";
    setUserAnswer(currentUserAnswer);
    setIsEditing(true);
    setHasAnswered(false);
  };

  // textarea 자동 리사이즈 함수 추가
  const autoResize = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const textarea = e.target;
    textarea.style.height = "auto";
    textarea.style.height = `${textarea.scrollHeight}px`;
    setUserAnswer(e.target.value);
  };

  // 답변 제출 처리
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!userAnswer.trim()) return;

    const currentQuestion = selectedHistory
      ? questionHistory.find((q) => q.id === selectedHistory)
      : todayQuestion;

    if (!currentQuestion) return;

    // 오늘 날짜의 답변이 이미 있고 수정 모드가 아닌 경우 제출 막기
    if (hasTodayAnswer && !isEditing) {
      setMessages([
        {
          type: "ai",
          content: "오늘의 질문에 대한 답변을 다시 작성해보세요.",
        },
      ]);
      return;
    }

    setMessages((prev) => {
      if (isEditing) {
        return [{ type: "user", content: userAnswer }];
      }
      return [...prev, { type: "user", content: userAnswer }];
    });
    setIsSubmitting(true);

    try {
      // 사용자 답변 저장
      const endpoint = `${BASE_URL}/api/questions/${currentQuestion.id}/answer`;
      const method = isEditing ? "PUT" : "POST";

      await fetchWithAuth(endpoint, {
        method: method,
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ answer: userAnswer }),
      });

      // AI 답변 요청 및 스트리밍 시작
      const chatInput = `질문: ${currentQuestion.question}\n\n답변: ${userAnswer}`;
      setMessages((prev) => [...prev, { type: "ai", content: "" }]);
      streamFromSSE(chatInput);
      setHasAnswered(true);
      setIsEditing(false);
    } catch (err) {
      console.error("Failed to save answer:", err);
      setMessages((prev) => [
        ...prev,
        {
          type: "ai",
          content: "답변 저장에 실패했습니다. 다시 시도해 주세요.",
        },
      ]);
      setHasAnswered(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 선택된 질문이 오늘 질문인지 확인하고 답변 가능한지 확인
  const canAnswerToday = (): boolean => {
    if (hasTodayAnswer && !isEditing) return false;

    if (!selectedHistory) return true;
    const selected = questionHistory.find((q) => q.id === selectedHistory);
    return selected ? selected.today : true;
  };

  return (
    <div className="bg-gray-50 py-9 flex-1 h-screen overflow-y-auto">
      <div className="container mx-auto px-4 max-w-7xl h-[calc(100vh-9rem)]">
        <h1 className="text-3xl font-bold text-gray-800 mb-6">오늘의 질문</h1>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 h-[calc(100%-4rem)]">
          {/* 왼쪽 사이드바 (질문 이력) */}
          <div className="md:col-span-1 h-full flex flex-col">
            <div className="bg-white rounded-lg shadow-md overflow-hidden flex flex-col h-full">
              <div className="p-4 bg-gray-100 border-b flex justify-between items-center">
                <h2 className="font-semibold">질문 이력</h2>
              </div>
              <div className="divide-y divide-gray-200 overflow-y-auto overflow-x-hidden flex-1 select-none">
                {questionHistory
                  .filter((item) => item.userAnswer !== "" || item.today)
                  .map((item) => (
                    <div
                      key={item.id}
                      className={`relative w-full ${
                        selectedHistory === item.id
                          ? "bg-blue-50"
                          : item.today
                          ? "hover:bg-gray-50"
                          : "hover:bg-gray-50"
                      }`}
                    >
                      <div
                        className={`border-l-4 w-full ${
                          selectedHistory === item.id
                            ? "border-l-blue-500"
                            : item.today
                            ? "border-l-green-500"
                            : "border-l-transparent"
                        }`}
                      >
                        <div
                          className="w-full px-4 py-3 text-left flex items-start gap-3 cursor-pointer"
                          onClick={(e) => handleHistorySelect(item.id, e)}
                          onTouchEnd={(e) => {
                            e.preventDefault();
                            handleHistorySelect(
                              item.id,
                              e as unknown as React.MouseEvent
                            );
                          }}
                        >
                          <div className="flex-1 pr-8">
                            <div className="flex items-center gap-2">
                              <div className="text-sm font-medium">
                                {item.date}
                                {item.today && (
                                  <span className="ml-2 px-2 py-0.5 text-xs bg-green-100 text-green-800 rounded-full">
                                    오늘
                                  </span>
                                )}
                              </div>
                            </div>
                            <div className="text-gray-700 text-sm truncate mt-1">
                              {item.question}
                            </div>
                            {/* <div className="text-xs text-gray-500 mt-1 truncate">
                              답변:{" "}
                              {item.userAnswer
                                ? item.userAnswer.substring(0, 30) +
                                  (item.userAnswer.length > 30 ? "..." : "")
                                : "답변 없음"}
                            </div>
                            <div className="text-xs text-gray-500 mt-1 truncate">
                              AI 답변:{" "}
                              {item.llmAnswer
                                ? item.llmAnswer.substring(0, 30) +
                                  (item.llmAnswer.length > 30 ? "..." : "")
                                : "AI 답변 없음"}
                            </div> */}
                          </div>
                        </div>
                      </div>

                      {/* 북마크 버튼 - 메인 페이지와 동일한 스타일 */}
                      <div className="absolute top-3 right-3 z-10">
                        <button
                          onClick={() =>
                            handleBookmarkToggle(item.id, item.bookmarked)
                          }
                          className={`flex items-center ${
                            item.bookmarked ? "text-navy-400" : "text-navy-600"
                          } ${
                            !isAuthenticated
                              ? "opacity-50 cursor-not-allowed"
                              : ""
                          }`}
                        >
                          <svg
                            className="w-5 h-5"
                            fill={item.bookmarked ? "currentColor" : "none"}
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
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          </div>

          {/* 오른쪽 메인 콘텐츠 */}
          <div className="md:col-span-3 h-full flex flex-col">
            <div className="bg-white rounded-lg shadow-md flex flex-col h-full">
              {/* 질문 헤더 */}
              <div className="p-4 border-b bg-gray-50">
                <div className="text-sm text-gray-500 mb-1">
                  {selectedHistory
                    ? questionHistory.find((q) => q.id === selectedHistory)
                        ?.date
                    : todayQuestion?.date}
                </div>
                <h2 className="text-xl font-semibold line-clamp-2">
                  {selectedHistory
                    ? questionHistory.find((q) => q.id === selectedHistory)
                        ?.question
                    : todayQuestion?.question}
                </h2>
              </div>

              {/* 채팅 영역 */}
              <div className="flex flex-col flex-1 min-h-0">
                <div
                  className={`flex-1 ${
                    messages.length > 0 ? "overflow-y-auto" : "overflow-hidden"
                  } p-4 space-y-4 overflow-x-hidden`}
                  ref={messagesContainerRef}
                >
                  {messages.length === 0 && (
                    <div className="flex justify-center items-center h-full text-gray-400">
                      <div className="text-center">
                        <div className="text-5xl mb-4">
                          <span className="inline-block bg-blue-50 text-blue-600 p-3 rounded-full">
                            <svg
                              xmlns="http://www.w3.org/2000/svg"
                              className="h-8 w-8"
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                            >
                              <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
                              />
                            </svg>
                          </span>
                        </div>
                        <p className="text-gray-600 font-medium">
                          {selectedHistory && !canAnswerToday()
                            ? "답변 내용을 불러오는 중..."
                            : "질문에 대한 답변을 작성해 보세요"}
                        </p>
                      </div>
                    </div>
                  )}

                  {messages.map((message, idx) => (
                    <div
                      key={idx}
                      className={`flex ${
                        message.type === "user"
                          ? "justify-end"
                          : "justify-start"
                      }`}
                    >
                      {/* 수정 버튼 (사용자 메시지일 때만 표시) */}
                      {message.type === "user" && (
                        <button
                          onClick={handleEditMode}
                          className={`self-end mb-2 mr-2 p-1.5 rounded-full hover:bg-gray-100 transition-colors ${
                            !selectedHistory ||
                            !questionHistory.find(
                              (q) => q.id === selectedHistory
                            )?.today
                              ? "hidden"
                              : ""
                          }`}
                          disabled={
                            !selectedHistory ||
                            !questionHistory.find(
                              (q) => q.id === selectedHistory
                            )?.today
                          }
                        >
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            className="h-4 w-4 text-gray-500"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                            />
                          </svg>
                        </button>
                      )}
                      <div
                        className={`${
                          message.type === "user"
                            ? "max-w-[70%] bg-blue-500 text-white rounded-br-none shadow-sm"
                            : "max-w-[85%] bg-gray-100 text-gray-800 rounded-bl-none shadow-sm"
                        } rounded-lg p-4 overflow-hidden`}
                      >
                        <div
                          className={`whitespace-pre-line ${
                            message.type === "ai"
                              ? "prose prose-sm max-w-none overflow-x-auto break-words prose-pre:whitespace-pre-wrap prose-pre:break-words prose-pre:overflow-x-auto prose-pre:max-w-full"
                              : "break-words"
                          }`}
                          style={{
                            overflowWrap: "break-word",
                            wordWrap: "break-word",
                            wordBreak: "break-word",
                            maxWidth: "100%",
                          }}
                        >
                          {message.content}
                          {message.type === "ai" &&
                            isStreaming &&
                            idx === messages.length - 1 && (
                              <span className="inline-block w-2 h-4 ml-1 bg-gray-500 animate-pulse"></span>
                            )}
                        </div>
                      </div>
                    </div>
                  ))}
                  <div ref={chatEndRef} />
                </div>

                {/* 입력 영역 - 오늘의 질문일 때만 표시 */}
                {(!selectedHistory ||
                  questionHistory.find((q) => q.id === selectedHistory)
                    ?.today) && (
                  <div className="p-4 border-t bg-white">
                    <form
                      onSubmit={handleSubmit}
                      className="flex gap-2 items-end"
                    >
                      <textarea
                        rows={1}
                        className={`flex-1 p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none min-h-[44px] max-h-[200px] overflow-y-auto [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none] ${
                          isSubmitting ||
                          !canAnswerToday() ||
                          (hasAnswered && !selectedHistory) ||
                          (selectedHistory && !canAnswerToday())
                            ? "bg-gray-100"
                            : ""
                        }`}
                        placeholder={
                          !canAnswerToday()
                            ? "오늘의 질문에 대한 답변을 다시 작성해보세요."
                            : "답변을 입력하세요."
                        }
                        value={userAnswer}
                        onChange={autoResize}
                        disabled={Boolean(
                          isSubmitting ||
                            !canAnswerToday() ||
                            (hasAnswered && !selectedHistory) ||
                            (selectedHistory && !canAnswerToday())
                        )}
                        required
                        onKeyDown={(e) => {
                          if (e.key === "Enter" && !e.shiftKey) {
                            e.preventDefault();
                            if (
                              userAnswer.trim() &&
                              !isSubmitting &&
                              canAnswerToday() &&
                              (!hasAnswered ||
                                (selectedHistory && canAnswerToday()))
                            ) {
                              handleSubmit(e);
                            }
                          }
                        }}
                      ></textarea>

                      <button
                        type="submit"
                        className={`w-[44px] h-[44px] flex items-center justify-center bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 ${
                          isSubmitting ||
                          !userAnswer.trim() ||
                          !canAnswerToday() ||
                          (hasAnswered && !selectedHistory) ||
                          (selectedHistory && !canAnswerToday())
                            ? "opacity-50 cursor-not-allowed"
                            : ""
                        }`}
                        disabled={Boolean(
                          isSubmitting ||
                            !userAnswer.trim() ||
                            !canAnswerToday() ||
                            (hasAnswered && !selectedHistory) ||
                            (selectedHistory && !canAnswerToday())
                        )}
                      >
                        {isSubmitting ? (
                          <svg
                            className="animate-spin h-5 w-5 text-white"
                            xmlns="http://www.w3.org/2000/svg"
                            fill="none"
                            viewBox="0 0 24 24"
                          >
                            <circle
                              className="opacity-25"
                              cx="12"
                              cy="12"
                              r="10"
                              stroke="currentColor"
                              strokeWidth="4"
                            ></circle>
                            <path
                              className="opacity-75"
                              fill="currentColor"
                              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                            ></path>
                          </svg>
                        ) : (
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            className="h-5 w-5"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
                            />
                          </svg>
                        )}
                      </button>
                    </form>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
