"use client";

import Link from "next/link";

const Footer = () => {
  return (
    <footer className="bg-gray-800 text-white py-6">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          {/* 회사 정보 */}
          <div>
            <h3 className="text-base font-semibold mb-3">docsker</h3>
            <p className="text-gray-300 text-xs">
              다양하고 쉽게 docs를 제공하는 플랫폼
              <br />
              Docker의 오마주로 바다같이 넓은 docs 제공 플랫폼
            </p>
          </div>

          {/* 링크 */}
          <div>
            <h3 className="text-base font-semibold mb-3">바로가기</h3>
            <ul className="space-y-1.5 text-gray-300 text-xs">
              <li>
                <Link href="/documents" className="hover:text-blue-400">
                  문서
                </Link>
              </li>
              <li>
                <Link href="/blog" className="hover:text-blue-400">
                  기술 블로그
                </Link>
              </li>
              <li>
                <Link href="/today-question" className="hover:text-blue-400">
                  오늘의 질문
                </Link>
              </li>
            </ul>
          </div>

          {/* 법적 고지 */}
          <div>
            <h3 className="text-base font-semibold mb-3">법적 고지</h3>
            <ul className="space-y-1.5 text-gray-300 text-xs">
              <li>
                <Link href="/terms" className="hover:text-blue-400">
                  이용약관
                </Link>
              </li>
              <li>
                <Link href="/privacy" className="hover:text-blue-400">
                  개인정보처리방침
                </Link>
              </li>
              <li>
                <Link href="/copyright" className="hover:text-blue-400">
                  저작권 정책
                </Link>
              </li>
            </ul>
          </div>

          {/* 고객 지원 및 소셜 미디어 */}
          <div>
            <h3 className="text-base font-semibold mb-3">고객 지원</h3>
            <ul className="space-y-1.5 text-gray-300 text-xs">
              <li>
                <Link href="/support" className="hover:text-blue-400">
                  고객센터
                </Link>
              </li>
              <li>
                <Link href="/faq" className="hover:text-blue-400">
                  자주 묻는 질문
                </Link>
              </li>
            </ul>
            <div className="mt-3">
              <h3 className="text-base font-semibold mb-1.5">소셜 미디어</h3>
              <div className="flex space-x-3">
                <a
                  href="https://github.com"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-gray-300 hover:text-white"
                >
                  GitHub
                </a>
                <a
                  href="https://twitter.com"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-gray-300 hover:text-white"
                >
                  Twitter
                </a>
              </div>
            </div>
          </div>
        </div>

        <div className="border-t border-gray-700 mt-6 pt-4 text-center text-gray-400 text-xs">
          <p>© {new Date().getFullYear()} docsker. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
