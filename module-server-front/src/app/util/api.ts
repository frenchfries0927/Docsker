import { BASE_URL } from '@/lib/api';
export const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
    console.log(`🔽 fetchWithAuth 요청 시작: ${url}`);
    try {
        const accessToken = localStorage.getItem('accessToken');
        console.log(`현재 AccessToken: ${accessToken}`);

        const res = await fetch(url, {
            ...options,
            headers: {
                ...(options.headers || {}),
                Authorization: `${accessToken}`,
                'Content-Type': 'application/json',
            },
            credentials: 'include',
        });

        console.log(`⚠️ fetch 결과: ${res.status}`);

        if (res.status === 401) {
            console.log('⏰ AccessToken 만료, reissue 요청 시도');

            const refreshRes = await fetch(`${BASE_URL}/auth/reissue`, {
                method: 'POST',
                credentials: 'include',
            });

            console.log(`🔁 reissue 결과: ${refreshRes.status}`);

            if (refreshRes.ok) {
                const newAccessToken = refreshRes.headers.get('Authorization');
                console.log(`✅ 새 AccessToken 발급: ${newAccessToken}`);
                if (newAccessToken) {
                    localStorage.setItem('accessToken', newAccessToken);

                    // 원래 요청 재시도
                    console.log(`🔄 원래 요청 재시도: ${url}`);
                    const retryRes = await fetch(url, {
                        ...options,
                        headers: {
                            ...(options.headers || {}),
                            Authorization: `${newAccessToken}`,
                            'Content-Type': 'application/json',
                        },
                        credentials: 'include',
                    });
                    return retryRes;
                }
            } else {
                console.log('❌ RefreshToken 만료, 로그인 필요');
                throw new Error('Unauthorized');
            }
        }

        return res;
    } catch (err) {
        console.error('API 요청 중 에러:', err);
        throw err;
    }
};
