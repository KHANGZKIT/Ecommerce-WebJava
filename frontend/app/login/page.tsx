"use client";

import React, { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/features/auth/AuthContext";
import { ApiError } from "@/lib/api/client";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function getValidRedirect(target: string | null): string | null {
  if (!target) return null;
  // Must start with single slash, not double slashes or protocol schemes
  if (target.startsWith("/") && !target.startsWith("//") && !target.startsWith("/\\")) {
    const pathPart = target.split("?")[0];
    if (!pathPart.includes(":")) {
      return target;
    }
  }
  return null;
}

function LoginLoadingSkeleton() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#FAFAF9]">
      <div className="font-mono text-xs uppercase tracking-wider text-[#78716C] animate-pulse">
        INITIALIZING AUTHENTICATION...
      </div>
    </div>
  );
}

function LoginContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login, isAuthenticated, isLoading: authLoading, user } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Read return URL / redirect param safely
  const rawRedirect = searchParams.get("redirect") || searchParams.get("returnUrl");
  const validRedirect = getValidRedirect(rawRedirect);

  // If already authenticated, redirect to destination
  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      const destination = validRedirect || (user?.role === "ADMIN" ? "/admin" : "/shop");
      router.replace(destination);
    }
  }, [authLoading, isAuthenticated, user, router, validRedirect]);

  // Client-side validation matching FE-12 and LoginRequest.java
  const validateForm = (): boolean => {
    let isValid = true;
    setEmailError(null);
    setPasswordError(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setEmailError("Vui lòng nhập địa chỉ email.");
      isValid = false;
    } else if (!EMAIL_REGEX.test(trimmedEmail)) {
      setEmailError("Địa chỉ email không đúng định dạng.");
      isValid = false;
    }

    if (!password) {
      setPasswordError("Vui lòng nhập mật khẩu.");
      isValid = false;
    } else if (password.length < 8) {
      setPasswordError("Mật khẩu phải có ít nhất 8 ký tự.");
      isValid = false;
    }

    return isValid;
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // Prevent duplicate submission
    if (isSubmitting) return;

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setServerError(null);

    try {
      await login({
        email: email.trim(),
        password,
      });

      // Determine post-login destination
      let destination = validRedirect || "/shop";
      try {
        const savedUserJson = localStorage.getItem("techgear_user_profile");
        if (savedUserJson) {
          const savedUser = JSON.parse(savedUserJson);
          if (savedUser?.role === "ADMIN" && !validRedirect) {
            destination = "/admin";
          }
        }
      } catch {
        // Fallback to /shop
      }

      router.push(destination);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          setServerError("Email hoặc mật khẩu không chính xác.");
        } else if (err.status === 403) {
          setServerError("Tài khoản này đã bị khóa hoặc vô hiệu hóa.");
        } else if (err.status === 400) {
          if (err.errors && typeof err.errors === "object" && !Array.isArray(err.errors)) {
            if (err.errors.email) setEmailError(err.errors.email);
            if (err.errors.password) setPasswordError(err.errors.password);
          }
          setServerError(err.message || "Vui lòng kiểm tra lại thông tin đăng nhập.");
        } else {
          setServerError("Không thể kết nối đến máy chủ xác thực. Vui lòng thử lại sau.");
        }
      } else if (err instanceof Error) {
        const lowerMsg = err.message.toLowerCase();
        if (lowerMsg.includes("fetch") || lowerMsg.includes("network") || lowerMsg.includes("failed")) {
          setServerError("Không thể kết nối đến máy chủ xác thực. Vui lòng kiểm tra đường truyền.");
        } else {
          setServerError(err.message);
        }
      } else {
        setServerError("Không thể kết nối đến máy chủ xác thực. Vui lòng thử lại sau.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex flex-col lg:flex-row bg-[#FAFAF9] font-sans antialiased">
      {/* ================================================================ */}
      {/* 1. BRAND IMAGE PANEL (50% on Desktop)                           */}
      {/* ================================================================ */}
      <section className="hidden lg:block lg:w-1/2 relative min-h-screen overflow-hidden bg-[#1C1917]">
        <img
          src="/image/z6354260733123_e40c2e59d363133e5885bc48b92034fe.jpg"
          alt="TECHGEAR Login"
          className="w-full h-full object-cover"
        />
      </section>

      {/* ================================================================ */}
      {/* 2. AUTHENTICATION FORM PANEL (50% on Desktop / Full on Mobile)  */}
      {/* ================================================================ */}
      <section className="w-full lg:w-1/2 min-h-screen flex flex-col justify-between p-6 sm:p-10 lg:p-12 xl:p-16 bg-[#FAFAF9]">
        {/* Top Utility Bar */}
        <header className="flex items-center justify-between pb-6 border-b border-[#E7E5E4]">
          <Link
            href="/"
            className="flex items-center gap-1.5 select-none"
          >
            <span className="text-xl font-black tracking-tight text-[#1C1917]">
              TECHGEAR
            </span>
            <span className="w-2 h-2 rounded-full bg-[#D97706]" />
          </Link>

          <Link
            href="/shop"
            className="text-xs font-semibold text-[#78716C] hover:text-[#1C1917] transition-colors flex items-center gap-1.5 ml-auto"
          >
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            <span>Quay lại cửa hàng</span>
          </Link>
        </header>

        {/* Center Form Container */}
        <main className="my-auto py-8 w-full max-w-[400px] mx-auto">
          {/* Typographic Heading */}
          <div className="mb-8">
            <span className="text-[11px] font-bold uppercase tracking-widest text-[#D97706] block mb-1.5">
              TÀI KHOẢN KHÁCH HÀNG
            </span>
            <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-[#1C1917] mb-2">
              Chào mừng trở lại
            </h1>
            <p className="text-sm text-[#78716C] leading-relaxed">
              Đăng nhập để theo dõi đơn hàng và quản lý tài khoản TECHGEAR của bạn.
            </p>
          </div>

          {/* Server-Side Alert Box */}
          {serverError && (
            <div
              role="alert"
              className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-lg flex items-start gap-3"
            >
              <svg className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p className="text-xs font-medium text-rose-700 leading-snug">
                {serverError}
              </p>
            </div>
          )}

          {/* Authentication Form */}
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Field: Email */}
            <div>
              <label
                htmlFor="login-email"
                className="block text-xs font-bold uppercase tracking-wider text-[#1C1917] mb-1.5"
              >
                ĐỊA CHỈ EMAIL
              </label>
              <input
                id="login-email"
                type="email"
                name="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (emailError) setEmailError(null);
                  if (serverError) setServerError(null);
                }}
                disabled={isSubmitting}
                autoComplete="email"
                placeholder="tenban@email.com"
                aria-invalid={!!emailError}
                aria-describedby={emailError ? "email-error" : undefined}
                className={`h-11 w-full px-3.5 bg-white border rounded-lg text-sm text-[#1C1917] placeholder:text-[#78716C] focus:outline-none focus:ring-1 transition-colors disabled:bg-stone-50 disabled:text-[#78716C] disabled:cursor-not-allowed ${
                  emailError
                    ? "border-rose-500 focus:border-rose-500 focus:ring-rose-500"
                    : "border-[#E7E5E4] focus:border-[#1C1917] focus:ring-[#1C1917]"
                }`}
              />
              {emailError && (
                <p
                  id="email-error"
                  className="mt-1.5 text-xs text-rose-600 flex items-center gap-1"
                >
                  <span>•</span>
                  <span>{emailError}</span>
                </p>
              )}
            </div>

            {/* Field: Password */}
            <div>
              <label
                htmlFor="login-password"
                className="block text-xs font-bold uppercase tracking-wider text-[#1C1917] mb-1.5"
              >
                MẬT KHẨU
              </label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  name="password"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (passwordError) setPasswordError(null);
                    if (serverError) setServerError(null);
                  }}
                  disabled={isSubmitting}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  aria-invalid={!!passwordError}
                  aria-describedby={passwordError ? "password-error" : undefined}
                  className={`h-11 w-full pl-3.5 pr-11 bg-white border rounded-lg text-sm text-[#1C1917] placeholder:text-[#78716C] focus:outline-none focus:ring-1 transition-colors disabled:bg-stone-50 disabled:text-[#78716C] disabled:cursor-not-allowed ${
                    passwordError
                      ? "border-rose-500 focus:border-rose-500 focus:ring-rose-500"
                      : "border-[#E7E5E4] focus:border-[#1C1917] focus:ring-[#1C1917]"
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#78716C] hover:text-[#1C1917] transition-colors cursor-pointer"
                  aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                >
                  {showPassword ? (
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18" />
                    </svg>
                  ) : (
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  )}
                </button>
              </div>
              {passwordError && (
                <p
                  id="password-error"
                  className="mt-1.5 text-xs text-rose-600 flex items-center gap-1"
                >
                  <span>•</span>
                  <span>{passwordError}</span>
                </p>
              )}
            </div>

            {/* Submit Action */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={isSubmitting}
                className="h-11 w-full bg-[#1C1917] hover:bg-[#292524] active:bg-black text-white text-xs font-bold tracking-wider uppercase rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2 shadow-xs"
              >
                {isSubmitting ? (
                  <>
                    <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    <span>Đang xác thực...</span>
                  </>
                ) : (
                  <span>Đăng nhập</span>
                )}
              </button>
            </div>
          </form>

          {/* Secondary Registration Pathway */}
          <div className="mt-8 pt-6 border-t border-[#E7E5E4] text-center">
            <p className="text-sm text-[#78716C]">
              Bạn chưa có tài khoản?{" "}
              <Link
                href="/register"
                className="font-bold text-[#1C1917] hover:text-[#D97706] underline underline-offset-4 transition-colors"
              >
                Đăng ký ngay
              </Link>
            </p>
          </div>
        </main>

        {/* Footer Utility */}
        <footer className="pt-6 border-t border-[#E7E5E4] flex flex-col sm:flex-row justify-between items-center text-xs text-[#78716C] gap-2">
          <span>© 2026 TECHGEAR VIỆT NAM.</span>
          <span className="font-mono text-[11px] text-[#A8A29E] uppercase">Swiss Precision × E-Commerce</span>
        </footer>
      </section>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<LoginLoadingSkeleton />}>
      <LoginContent />
    </Suspense>
  );
}
