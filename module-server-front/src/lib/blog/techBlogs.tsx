import { BASE_URL } from '@/lib/api';

export interface Content {
    id: number;
    title: string;
    link: string;
    summary: string;
    provider: string;
    publishDate: string;
    views: number;
    bookmarkCount: number;
    isBookmarked: boolean;
    tags: string[];
}

export interface PagedResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
    last: boolean;
}

// ✅ provider, sort, page, size 받아서 API 호출
export async function techBlogs({
                                    provider = '',
                                    sort = 'latest',
                                    page = 0,
                                    size = 10,
                                }: {
    provider?: string;
    sort?: string;
    page?: number;
    size?: number;
}): Promise<PagedResponse<Content>> {
    try {
        const queryParams = new URLSearchParams({
            sort,
            page: page.toString(),
            size: size.toString(),
        });

        if (provider) {
            queryParams.append('provider', provider);
        }

        const accessToken = localStorage.getItem('accessToken');
        const response = await fetch(`${BASE_URL}/api/techblog?${queryParams.toString()}`, {
            headers: {
                'Authorization': `${accessToken}`,
            },
        });
        if (!response.ok) {
            throw new Error('네트워크 오류가 발생했습니다.');
        }

        const result: PagedResponse<Content> = await response.json();

        console.log("🔍 전체 API 응답:", result);

        return result;
    } catch (error) {
        console.error('블로그 데이터를 불러오는 중 오류 발생:', error);
        return {
            content: [],
            page: 0,
            size: 10,
            totalPages: 1,
            totalElements: 0,
            last: true,
        };
    }
}

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
export async function toggleBookmark(blogId: number, isBookmarked: boolean) {
    try {
        const method = isBookmarked ? 'DELETE' : 'POST';
        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
            alert('로그인 후 이용 가능합니다!');
            return;
        }
        const response = await fetch(`${BASE_URL}/api/bookmarks/content/${blogId}`, {
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
