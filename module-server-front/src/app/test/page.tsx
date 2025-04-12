"use client";

import { useState } from "react";
import { BASE_URL } from '@/lib/api';

export default function ChatPage() {
    const [chatInput, setChatInput] = useState("");
    const [responseText, setResponseText] = useState("");
    const [eventSource, setEventSource] = useState<EventSource | null>(null);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        // 기존 SSE 연결 종료
        if (eventSource) {
            eventSource.close();
        }

        // 응답 초기화
        setResponseText("");

        // chatdata를 쿼리 파라미터로 인코딩해서 전달합니다.
        const url = `http://127.0.0.1:5003/api/chat/stream?chatdata=${encodeURIComponent(chatInput)}`;

        // EventSource 연결 생성
        const es = new EventSource(url);

        es.onmessage = (event) => {
            console.log("Raw SSE data:", event.data);

            // "[DONE]" 메시지는 무시
            if (event.data.trim() === "[DONE]") {
                return;
            }

            try {
                const parsed = JSON.parse(event.data);
                const choice = parsed?.choices && parsed.choices[0];

                // finish_reason이 "stop"이고, delta.content가 없는 경우 마지막 데이터로 간주하여 무시
                if (choice && choice.finish_reason === "stop" && !choice.delta?.content) {
                    return;
                }

                // delta.content가 있을 경우 해당 텍스트만 추출
                const content = choice?.delta?.content;
                if (content) {
                    setResponseText((prev) => prev + content);
                } else {
                    // JSON 파싱은 성공했지만 원하는 필드가 없으면 원본 데이터 그대로 추가
                    setResponseText((prev) => prev + event.data);
                }
            } catch (error) {
                // JSON 파싱에 실패하면 원본 데이터 추가
                setResponseText((prev) => prev + event.data);
            }
        };

        es.onerror = (err) => {
            console.error("EventSource error:", err);
            es.close();
        };

        setEventSource(es);
    };

    return (
        <div style={{ padding: "2rem" }}>
            <h1>LLM 모델 Chat Test</h1>
            <form onSubmit={handleSubmit}>
                <input
                    type="text"
                    value={chatInput}
                    onChange={(e) => setChatInput(e.target.value)}
                    placeholder="메시지를 입력하세요..."
                    style={{ padding: "0.5rem", width: "300px" }}
                />
                <button type="submit" style={{ padding: "0.5rem", marginLeft: "1rem" }}>
                    전송
                </button>
            </form>
            <div style={{ marginTop: "2rem" }}>
                <h2>LLM 응답:</h2>
                <pre
                    style={{
                        background: "#f4f4f4",
                        padding: "1rem",
                        borderRadius: "4px",
                        whiteSpace: "pre-wrap",
                    }}
                >
          {responseText}
        </pre>
            </div>
        </div>
    );
}
