import { useState, useEffect } from 'react';

export default function useAuth() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    useEffect(() => {
        const token = localStorage.getItem('accessToken'); // or check cookies
        setIsLoggedIn(!!token);
    }, []);

    return { isLoggedIn };
}
