"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import { BASE_URL } from '@/lib/api';
import { UserCircle } from "lucide-react";
import { Menu } from "@headlessui/react";

const Header = () => {
  const { isLoggedIn, logout } = useAuth();
  const router = useRouter();

  // 로그아웃 처리
  const handleLogout = async () => {
    try {
      await fetch(`${BASE_URL}/auth/logout`, {
        method: "POST",
        credentials: "include", // 쿠키 전송
      });

      logout(); // AuthContext의 logout 함수 호출
      router.push("/"); // 메인 페이지로 이동
    } catch (error) {
      console.error("Logout failed", error);
    }
  };

  return (
    <header className="bg-white shadow-md">
      <div className="container mx-auto px-4 py-4">
        <div className="flex justify-between items-center">
          {/* 로고 */}
          <Link href="/" className="flex items-center">
            <span className="text-2xl font-bold text-blue-600">docsker</span>
          </Link>

          {/* 네비게이션 */}
          <nav className="hidden md:flex items-center space-x-12">
            <Link
              href="/documents"
              className="text-gray-700 hover:text-blue-600 font-medium px-2 py-1 rounded-md transition-colors duration-200"
            >
              문서
            </Link>
            <Link
              href="/blog"
              className="text-gray-700 hover:text-blue-600 font-medium px-2 py-1 rounded-md transition-colors duration-200"
            >
              기술 블로그
            </Link>
            <Link
              href="/today-question"
              className="text-gray-700 hover:text-blue-600 font-medium px-2 py-1 rounded-md transition-colors duration-200"
            >
              오늘의 질문
            </Link>
          </nav>

          {/* 인증 버튼 */}
          <div className="flex items-center space-x-4">
            {isLoggedIn ? (
                <Menu as="div" className="relative inline-block text-left">
                  <Menu.Button className="flex items-center focus:outline-none">
                    <UserCircle className="w-8 h-8 text-gray-700 hover:text-blue-600" />
                  </Menu.Button>
                  <Menu.Items className="absolute right-0 mt-2 w-40 origin-top-right bg-white border border-gray-200 rounded-md shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none z-50">
                    <div className="py-1">
                      <Menu.Item>
                        {({ active }) => (
                            <button
                                onClick={() => router.push("/mypage")}
                                className={`${
                                    active ? "bg-gray-100" : ""
                                } block px-4 py-2 text-sm text-gray-700 w-full text-left`}
                            >
                              마이페이지
                            </button>
                        )}
                      </Menu.Item>
                      <Menu.Item>
                        {({ active }) => (
                            <button
                                onClick={handleLogout}
                                className={`${
                                    active ? "bg-gray-100" : ""
                                } block px-4 py-2 text-sm text-gray-700 w-full text-left`}
                            >
                              로그아웃
                            </button>
                        )}
                      </Menu.Item>
                    </div>
                  </Menu.Items>
                </Menu>
            ) : (
                <>
                  <Link
                      href="/login"
                      className="text-gray-700 hover:text-blue-600 font-medium"
                  >
                    로그인
                  </Link>
                  <Link
                      href="/register"
                      className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded"
                  >
                    회원가입
                  </Link>
                </>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
