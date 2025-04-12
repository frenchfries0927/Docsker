"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { BASE_URL } from '@/lib/api';

export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [isEmailSent, setIsEmailSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);


  const router = useRouter();

  // ✅ 이메일 인증 코드 요청
  const handleSendVerification = async (e: React.MouseEvent) => {
    e.preventDefault();

    if (!email || !email.includes("@")) {
      setError("유효한 이메일을 입력해주세요.");
      return;
    }

    setIsLoading(true);

    try {
      const res = await fetch(`${BASE_URL}/api/mail/send`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });

      if (!res.ok) throw new Error("인증 코드 전송 실패");

      setIsEmailSent(true);
      setError("");
    } catch (err: any) {
      setError(err.message || "인증 코드 전송 중 오류 발생");
    } finally {
      setIsLoading(false);
    }
  };

  // ✅ 이메일 인증 확인
  const handleVerifyEmail = async (e: React.MouseEvent) => {
    e.preventDefault();

    if (!verificationCode) {
      setError("인증 코드를 입력해주세요.");
      return;
    }

    setIsLoading(true);

    try {
      const res = await fetch(`${BASE_URL}/api/mail/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          code: verificationCode,
        }),
      });

      if (!res.ok) throw new Error("인증 실패");

      setIsEmailVerified(true);
      setError("");
    } catch (err: any) {
      setError(err.message || "인증 확인 중 오류 발생");
    } finally {
      setIsLoading(false);
    }
  };

  // ✅ 회원가입 처리
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isEmailVerified) {
      setError("이메일 인증을 완료해주세요.");
      return;
    }

    if (!nickname.trim()) {
      setError("닉네임을 입력해주세요.");
      return;
    }

    if (password !== confirmPassword) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }

    setIsLoading(true);
    setError("");

    try {
      const res = await fetch(`${BASE_URL}/auth/join`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          password,
          username: nickname,
        }),
      });
      const result = await res.json();
      if (!res.ok) throw new Error(result.message);

      // 성공 시 로그인 페이지로 이동
      router.push("/login");
    } catch (err: any) {
      setError(err.message || "회원가입 중 오류 발생");
    } finally {
      setIsLoading(false);
    }
  };
  return (
      <div className="min-h-screen bg-gray-50 py-12 flex items-center justify-center">
        <div className="max-w-md w-full bg-white rounded-lg shadow-lg overflow-hidden">
          <div className="px-6 py-8">
            <h2 className="text-center text-3xl font-bold text-gray-800 mb-6">
              회원가입
            </h2>

            {error && (
                <div className="mb-4 bg-red-50 border-l-4 border-red-500 p-4 text-red-700">
                  <p>{error}</p>
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* 이메일 입력 */}
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                  이메일
                </label>
                <div className="flex">
                  <input
                      id="email"
                      type="email"
                      autoComplete="email"
                      required
                      className={`appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                          isEmailVerified ? "bg-gray-100" : ""
                      }`}
                      placeholder="이메일을 입력하세요"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      disabled={isEmailVerified}
                  />
                  <button
                      onClick={handleSendVerification}
                      disabled={isEmailVerified || isLoading}
                      className={`ml-2 whitespace-nowrap px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 ${
                          isEmailVerified || isLoading ? "opacity-75 cursor-not-allowed" : ""
                      }`}
                  >
                    {isEmailSent ? "재전송" : "인증 코드 전송"}
                  </button>
                </div>
              </div>

              {/* 인증 코드 입력 */}
              {isEmailSent && !isEmailVerified && (
                  <div>
                    <label htmlFor="verification-code" className="block text-sm font-medium text-gray-700 mb-1">
                      인증 코드
                    </label>
                    <div className="flex">
                      <input
                          id="verification-code"
                          type="text"
                          required
                          className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                          placeholder="이메일로 받은 인증 코드 입력"
                          value={verificationCode}
                          onChange={(e) => setVerificationCode(e.target.value)}
                      />
                      <button
                          onClick={handleVerifyEmail}
                          disabled={isLoading}
                          className={`ml-2 whitespace-nowrap px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 ${
                              isLoading ? "opacity-75 cursor-not-allowed" : ""
                          }`}
                      >
                        인증 확인
                      </button>
                    </div>
                  </div>
              )}

              {/* 인증 완료 메시지 */}
              {isEmailVerified && (
                  <div className="bg-green-50 border-l-4 border-green-500 p-4 text-green-700">
                    <p>이메일 인증이 완료되었습니다.</p>
                  </div>
              )}

              {/* 닉네임 */}
              <div>
                <label htmlFor="nickname" className="block text-sm font-medium text-gray-700 mb-1">
                  닉네임
                </label>
                <input
                    id="nickname"
                    type="text"
                    autoComplete="nickname"
                    required
                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder="닉네임을 입력하세요"
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                />
              </div>

              {/* 비밀번호 */}
              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                  비밀번호
                </label>
                <input
                    id="password"
                    type="password"
                    autoComplete="new-password"
                    required
                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder="비밀번호를 입력하세요"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                />
              </div>

              {/* 비밀번호 확인 */}
              <div>
                <label htmlFor="confirm-password" className="block text-sm font-medium text-gray-700 mb-1">
                  비밀번호 확인
                </label>
                <input
                    id="confirm-password"
                    type="password"
                    autoComplete="new-password"
                    required
                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder="비밀번호를 다시 입력하세요"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>

              {/* 회원가입 버튼 */}
              <div>
                <button
                    type="submit"
                    disabled={!isEmailVerified || isLoading}
                    className={`w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 ${
                        !isEmailVerified || isLoading ? "opacity-75 cursor-not-allowed" : ""
                    }`}
                >
                  {isLoading ? "처리 중..." : "회원가입"}
                </button>
              </div>
            </form>
          </div>

          <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 sm:px-10">
            <p className="text-sm text-center text-gray-600">
              이미 계정이 있으신가요?{" "}
              <Link href="/login" className="font-medium text-blue-600 hover:text-blue-500">
                로그인
              </Link>
            </p>
          </div>
        </div>
      </div>
  );
}
