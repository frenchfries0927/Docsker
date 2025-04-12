import { BASE_URL } from '@/lib/api';
import { useState } from "react";

export interface ContentDetail {
    uid: string;
    modules: string[];
    version: string;
}

export interface Content {
    id: number;
    createdAt: string;
    updatedAt: string | null;
    contentType: string;
    contentDetail: ContentDetail;
    views: number;
    uid: string | null;
}

// API에서 버전 블로그 데이터를 가져오는 함수
export async function useLatestJavaVersion(): Promise<Content> {
    try {
        const response = await fetch(`${BASE_URL}/api/javadoc/version/latest`); // 실제 API 주소로 변경
        if (!response.ok) {
            throw new Error('네트워크 오류가 발생했습니다.');
        }
        const data: Content = await response.json(); // API 응답이 객체일 경우
        return data;
    } catch (error) {
        console.error('버전 블로그 데이터를 불러오는 중 오류 발생:', error); // 오류만 콘솔에 출력
        return {} as Content; // 빈 객체 반환 (타입 맞추기 위해)
    }
}
