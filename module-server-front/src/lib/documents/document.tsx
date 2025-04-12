import { BASE_URL } from '@/lib/api';
export async function increaseViewCount(blogId: number) {
    try {
        const response = await fetch(`${BASE_URL}/api/techblog/view/${blogId}`, {
            method: 'POST',
        });
        if (!response.ok) {
            throw new Error('조회수 증가 실패');
        }
    } catch (error) {
        console.error('조회수 증가 중 오류:', error);
    }
}

// 북마크
export async function toggleBookmark(classId: number, isBookmarked: boolean) {
    try {
        const method = isBookmarked ? 'DELETE' : 'POST';
        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
            alert('로그인 후 이용 가능합니다!');
            return;
        }
        const response = await fetch(`${BASE_URL}/api/bookmarks/content/${classId}`, {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `${accessToken}`
            },
            credentials: 'include',
        });

        if (!response.ok) {
            throw new Error(`북마크 ${isBookmarked ? '해제' : '추가'} 실패`);
        }
    } catch (error) {
        console.error(`북마크 ${isBookmarked ? '해제' : '추가'} 중 오류:`, error);
    }
}