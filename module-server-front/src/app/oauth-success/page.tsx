// app/oauth-success/page.tsx

'use client';
import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';

export default function OAuthSuccessPage() {
    const router = useRouter();
    const searchParams = useSearchParams();

    useEffect(() => {
        const accessToken = searchParams.get('accessToken');
        if (accessToken) {
            localStorage.setItem('accessToken', accessToken); // 토큰 저장
            router.replace('/'); // 홈으로 이동 or 원하는 페이지
        }
    }, [searchParams, router]);

    return (
        <div>
            <h2>로그인 처리 중...</h2>
        </div>
    );
}
