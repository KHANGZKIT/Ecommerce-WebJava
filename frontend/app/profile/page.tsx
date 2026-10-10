"use client";

import React, { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProtectedRoute } from "@/features/auth/AuthGuard";
import { useAuth } from "@/features/auth/AuthContext";
import { getProfileApi, updateProfileApi } from "@/lib/api/profile";
import { UserProfileDto, UpdateUserProfileRequest } from "@/types/user";
import { ApiError } from "@/lib/api/client";

const NAME_REGEX = /^(?=.*\p{L})[\p{L}\s\-']+$/u;
const PHONE_REGEX = /^\+?[0-9]{10,15}$/;

function formatMemberSince(isoDateString?: string): string {
  if (!isoDateString) return "—";
  try {
    const d = new Date(isoDateString);
    if (isNaN(d.getTime())) return isoDateString;
    return `Tháng ${d.getMonth() + 1}, ${d.getFullYear()}`;
  } catch {
    return isoDateString;
  }
}

function getInitials(first?: string, last?: string): string {
  const f = first?.trim()?.[0] || "";
  const l = last?.trim()?.[0] || "";
  return (f + l).toUpperCase() || "U";
}

function ProfileLoadingSkeleton() {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start animate-pulse">
      {/* Left Card Skeleton */}
      <div className="lg:col-span-4 bg-white border border-stone-200 rounded-xl p-6 shadow-sm">
        <div className="w-12 h-12 bg-stone-200 rounded-full mb-4" />
        <div className="h-6 w-36 bg-stone-200 rounded mb-2" />
        <div className="h-4 w-48 bg-stone-100 rounded mb-6" />
        <div className="border-t border-stone-100 pt-5">
          <div className="h-3 w-24 bg-stone-200 rounded mb-2" />
          <div className="h-4 w-28 bg-stone-100 rounded" />
        </div>
      </div>

      {/* Right Card Skeleton */}
      <div className="lg:col-span-8 bg-white border border-stone-200 rounded-xl p-6 sm:p-8 shadow-sm">
        <div className="h-5 w-48 bg-stone-200 rounded mb-2" />
        <div className="h-4 w-72 bg-stone-100 rounded mb-8" />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
          <div className="h-11 bg-stone-100 rounded-lg" />
          <div className="h-11 bg-stone-100 rounded-lg" />
        </div>
        <div className="h-11 bg-stone-100 rounded-lg mb-4" />
        <div className="h-11 bg-stone-100 rounded-lg mb-6" />

        <div className="flex gap-3 pt-4 border-t border-stone-100">
          <div className="h-11 w-32 bg-stone-200 rounded-lg" />
          <div className="h-11 w-24 bg-stone-100 rounded-lg" />
        </div>
      </div>
    </div>
  );
}

function ProfileContent() {
  const { refreshUser } = useAuth();

  const [profile, setProfile] = useState<UserProfileDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  // Form Fields
  const [firstName, setFirstName] = useState<string>("");
  const [lastName, setLastName] = useState<string>("");
  const [phone, setPhone] = useState<string>("");

  // Form State
  const [fieldErrors, setFieldErrors] = useState<{
    firstName?: string;
    lastName?: string;
    phone?: string;
  }>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Load Profile from Backend
  const loadProfile = useCallback(async () => {
    setIsLoading(true);
    setFetchError(null);
    try {
      const data = await getProfileApi();
      setProfile(data);
      setFirstName(data.firstName || "");
      setLastName(data.lastName || "");
      setPhone(data.phone || "");
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
          ? err.message
          : "Không thể lấy thông tin tài khoản.";
      setFetchError(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  // Client-Side Validation
  const validateForm = (): boolean => {
    const errors: { firstName?: string; lastName?: string; phone?: string } = {};

    const trimmedFirst = firstName.trim();
    if (trimmedFirst) {
      if (trimmedFirst.length < 2 || trimmedFirst.length > 100) {
        errors.firstName = "Tên phải từ 2 đến 100 ký tự.";
      } else if (!NAME_REGEX.test(trimmedFirst)) {
        errors.firstName = "Tên chỉ được chứa chữ cái, khoảng trắng và dấu gạch ngang.";
      }
    }

    const trimmedLast = lastName.trim();
    if (trimmedLast) {
      if (trimmedLast.length < 2 || trimmedLast.length > 100) {
        errors.lastName = "Họ và tên đệm phải từ 2 đến 100 ký tự.";
      } else if (!NAME_REGEX.test(trimmedLast)) {
        errors.lastName = "Họ và tên đệm chỉ được chứa chữ cái, khoảng trắng và dấu gạch ngang.";
      }
    }

    const trimmedPhone = phone.trim();
    if (trimmedPhone) {
      if (!PHONE_REGEX.test(trimmedPhone)) {
        errors.phone = "Số điện thoại không hợp lệ (ví dụ: 0901234567 hoặc +84901234567).";
      }
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Handle Form Submit
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (isSubmitting) return;

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);
    setSaveSuccess(false);

    try {
      const payload: UpdateUserProfileRequest = {
        firstName: firstName.trim() || undefined,
        lastName: lastName.trim() || undefined,
        phone: phone.trim() || null,
      };

      const updated = await updateProfileApi(payload);
      setProfile(updated);
      setFirstName(updated.firstName || "");
      setLastName(updated.lastName || "");
      setPhone(updated.phone || "");
      setSaveSuccess(true);

      // Synchronize with AuthContext
      try {
        localStorage.setItem("techgear_user_profile", JSON.stringify(updated));
      } catch {
        // ignore
      }
      await refreshUser();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setFieldErrors((prev) => ({
            ...prev,
            phone: "Số điện thoại này đã được sử dụng cho tài khoản khác.",
          }));
          setSubmitError(err.message || "Số điện thoại này đã được sử dụng cho tài khoản khác.");
        } else if (err.status === 400) {
          if (err.errors && typeof err.errors === "object" && !Array.isArray(err.errors)) {
            setFieldErrors((prev) => ({
              ...prev,
              ...(err.errors as Record<string, string>),
            }));
          }
          setSubmitError(err.message || "Vui lòng kiểm tra lại thông tin đã nhập.");
        } else {
          setSubmitError("Không thể lưu thông tin. Vui lòng thử lại sau.");
        }
      } else if (err instanceof Error) {
        setSubmitError(err.message);
      } else {
        setSubmitError("Không thể lưu thông tin. Vui lòng thử lại sau.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Revert Form to Current Profile State
  const handleCancel = () => {
    if (profile) {
      setFirstName(profile.firstName || "");
      setLastName(profile.lastName || "");
      setPhone(profile.phone || "");
    }
    setFieldErrors({});
    setSubmitError(null);
    setSaveSuccess(false);
  };

  return (
    <div className="max-w-[1280px] mx-auto py-0">
      {/* ================================================================ */}
      {/* 1. BREADCRUMBS & SECTION HEADER                                  */}
      {/* ================================================================ */}
      <section className="pb-6 border-b border-stone-200 mb-8">
        <div className="text-xs text-stone-500 mb-3 flex items-center gap-1.5">
          <Link href="/shop" className="hover:text-stone-900 transition-colors">
            Tài khoản
          </Link>
          <span>/</span>
          <span className="text-stone-900 font-semibold">Thông tin cá nhân</span>
        </div>

        <h1 className="text-3xl sm:text-4xl font-bold tracking-tight text-stone-900 mb-2">
          Thông tin cá nhân
        </h1>
        <p className="text-sm text-stone-500 leading-relaxed max-w-[60ch]">
          Quản lý thông tin liên hệ và chi tiết tài khoản của bạn tại TECHGEAR.
        </p>
      </section>

      {/* ================================================================ */}
      {/* 2. BODY: LOADING SKELETON / ERROR / POPULATED PROFILE            */}
      {/* ================================================================ */}
      {isLoading ? (
        <ProfileLoadingSkeleton />
      ) : fetchError ? (
        /* Fetch Error State with Retry */
        <div className="p-8 max-w-xl mx-auto my-12 bg-white border border-rose-200 rounded-xl shadow-sm text-center">
          <h2 className="text-lg font-bold text-stone-900 mb-2">
            Không thể tải thông tin tài khoản
          </h2>
          <p className="text-sm text-rose-700 mb-6 leading-relaxed">
            {fetchError}
          </p>
          <button
            type="button"
            onClick={loadProfile}
            className="inline-flex items-center justify-center bg-stone-900 text-white px-6 py-2.5 text-xs font-semibold rounded-lg hover:bg-stone-800 transition-colors cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* ============================================================ */}
          {/* LEFT PANEL: CUSTOMER IDENTITY CARD (4 columns on desktop)    */}
          {/* ============================================================ */}
          <aside className="lg:col-span-4 bg-white border border-stone-200 rounded-xl p-6 shadow-sm flex flex-col justify-between">
            <div>
              {/* Monogram Box */}
              <div className="w-14 h-14 bg-stone-100 border border-stone-200 rounded-full flex items-center justify-center font-bold text-stone-900 text-lg mb-4 select-none">
                {getInitials(profile?.firstName, profile?.lastName)}
              </div>

              {/* Full Name & Email */}
              <div className="mb-6">
                <h2 className="text-lg font-bold text-stone-900 leading-snug mb-1">
                  {profile?.firstName && profile?.lastName
                    ? `${profile.firstName} ${profile.lastName}`
                    : profile?.firstName || profile?.lastName || "Khách hàng"}
                </h2>
                <p className="text-xs text-stone-500 break-all">
                  {profile?.email}
                </p>
              </div>
            </div>

            {/* Membership Information */}
            <div className="border-t border-stone-100 pt-5">
              <div className="text-xs text-stone-400 uppercase tracking-wider mb-1">
                Thành viên từ
              </div>
              <div className="text-sm font-semibold text-stone-800">
                {formatMemberSince(profile?.createdAt)}
              </div>
            </div>
          </aside>

          {/* ============================================================ */}
          {/* RIGHT PANEL: PERSONAL INFORMATION & EDIT (8 columns desktop) */}
          {/* ============================================================ */}
          <main className="lg:col-span-8 bg-white border border-stone-200 rounded-xl p-6 sm:p-8 shadow-sm">
            {/* Card Header */}
            <div className="mb-6 pb-4 border-b border-stone-100">
              <h2 className="text-base font-bold text-stone-900 uppercase tracking-wider">
                Cập nhật thông tin
              </h2>
              <p className="text-xs text-stone-500 mt-1">
                Cập nhật họ tên và số điện thoại liên hệ. Địa chỉ email đăng nhập được cố định.
              </p>
            </div>

            {/* Server Error Alert */}
            {submitError && (
              <div
                role="alert"
                className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-lg"
              >
                <p className="text-xs text-rose-700 leading-snug">
                  {submitError}
                </p>
              </div>
            )}

            {/* Profile Edit Form */}
            <form onSubmit={handleSubmit} noValidate className="space-y-5">
              {/* Row 1: First Name & Last Name (2 Columns) */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label
                    htmlFor="profile-lastname"
                    className="block text-xs font-semibold text-stone-700 uppercase mb-1.5"
                  >
                    HỌ & TÊN ĐỆM
                  </label>
                  <input
                    id="profile-lastname"
                    type="text"
                    value={lastName}
                    onChange={(e) => {
                      setLastName(e.target.value);
                      if (fieldErrors.lastName) {
                        setFieldErrors((prev) => ({ ...prev, lastName: undefined }));
                      }
                      setSaveSuccess(false);
                      setSubmitError(null);
                    }}
                    disabled={isSubmitting}
                    placeholder="Nguyễn Văn"
                    className={`h-11 w-full bg-white border rounded-lg px-3.5 text-sm text-stone-900 placeholder:text-stone-400 focus:outline-none focus:ring-1 transition-colors disabled:bg-stone-50 disabled:text-stone-400 disabled:cursor-not-allowed ${
                      fieldErrors.lastName
                        ? "border-rose-500 focus:border-rose-500 focus:ring-rose-500"
                        : "border-stone-200 focus:border-stone-900 focus:ring-stone-900"
                    }`}
                  />
                  {fieldErrors.lastName && (
                    <p className="mt-1 text-xs text-rose-600">
                      {fieldErrors.lastName}
                    </p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="profile-firstname"
                    className="block text-xs font-semibold text-stone-700 uppercase mb-1.5"
                  >
                    TÊN
                  </label>
                  <input
                    id="profile-firstname"
                    type="text"
                    value={firstName}
                    onChange={(e) => {
                      setFirstName(e.target.value);
                      if (fieldErrors.firstName) {
                        setFieldErrors((prev) => ({ ...prev, firstName: undefined }));
                      }
                      setSaveSuccess(false);
                      setSubmitError(null);
                    }}
                    disabled={isSubmitting}
                    placeholder="An"
                    className={`h-11 w-full bg-white border rounded-lg px-3.5 text-sm text-stone-900 placeholder:text-stone-400 focus:outline-none focus:ring-1 transition-colors disabled:bg-stone-50 disabled:text-stone-400 disabled:cursor-not-allowed ${
                      fieldErrors.firstName
                        ? "border-rose-500 focus:border-rose-500 focus:ring-rose-500"
                        : "border-stone-200 focus:border-stone-900 focus:ring-stone-900"
                    }`}
                  />
                  {fieldErrors.firstName && (
                    <p className="mt-1 text-xs text-rose-600">
                      {fieldErrors.firstName}
                    </p>
                  )}
                </div>
              </div>

              {/* Row 2: Email Address (Read-Only) */}
              <div>
                <div className="flex items-center gap-2 mb-1.5">
                  <label
                    htmlFor="profile-email"
                    className="text-xs font-semibold text-stone-700 uppercase"
                  >
                    ĐỊA CHỈ EMAIL (CỐ ĐỊNH)
                  </label>
                  <span className="text-[10px] uppercase bg-stone-100 border border-stone-200 text-stone-500 px-1.5 py-0.5 rounded">
                    Bảo mật
                  </span>
                </div>
                <input
                  id="profile-email"
                  type="email"
                  disabled
                  value={profile?.email || ""}
                  className="h-11 w-full bg-stone-50 border border-stone-200 rounded-lg px-3.5 text-sm text-stone-500 cursor-not-allowed select-none"
                />
                <p className="text-xs text-stone-400 mt-1">
                  Email liên kết chặt chẽ với tài khoản và không thể tự thay đổi.
                </p>
              </div>

              {/* Row 3: Phone Number */}
              <div>
                <label
                  htmlFor="profile-phone"
                  className="block text-xs font-semibold text-stone-700 uppercase mb-1.5"
                >
                  SỐ ĐIỆN THOẠI
                </label>
                <input
                  id="profile-phone"
                  type="tel"
                  value={phone}
                  onChange={(e) => {
                    setPhone(e.target.value);
                    if (fieldErrors.phone) {
                      setFieldErrors((prev) => ({ ...prev, phone: undefined }));
                    }
                    setSaveSuccess(false);
                    setSubmitError(null);
                  }}
                  disabled={isSubmitting}
                  placeholder="0901234567"
                  className={`h-11 w-full bg-white border rounded-lg px-3.5 text-sm text-stone-900 placeholder:text-stone-400 focus:outline-none focus:ring-1 transition-colors disabled:bg-stone-50 disabled:text-stone-400 disabled:cursor-not-allowed ${
                    fieldErrors.phone
                      ? "border-rose-500 focus:border-rose-500 focus:ring-rose-500"
                      : "border-stone-200 focus:border-stone-900 focus:ring-stone-900"
                  }`}
                />
                {fieldErrors.phone && (
                  <p className="mt-1 text-xs text-rose-600">
                    {fieldErrors.phone}
                  </p>
                )}
              </div>

              {/* Actions & Feedback Row */}
              <div className="pt-4 border-t border-stone-100 flex flex-wrap items-center gap-3">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="h-11 px-6 bg-stone-900 hover:bg-stone-800 text-white font-semibold text-xs uppercase tracking-wider rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-stone-900 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2 shadow-sm"
                >
                  {isSubmitting ? (
                    <>
                      <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      <span>Đang lưu...</span>
                    </>
                  ) : (
                    <span>Lưu thay đổi</span>
                  )}
                </button>

                <button
                  type="button"
                  onClick={handleCancel}
                  disabled={isSubmitting}
                  className="h-11 px-6 bg-white border border-stone-200 hover:bg-stone-50 text-stone-700 font-semibold text-xs uppercase tracking-wider rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                >
                  Hủy bỏ
                </button>

                {/* Inline Status Message */}
                {saveSuccess && (
                  <span className="text-xs font-semibold text-emerald-600 flex items-center gap-1">
                    ✓ Đã cập nhật thành công!
                  </span>
                )}
              </div>
            </form>
          </main>
        </div>
      )}
    </div>
  );
}

export default function ProfilePage() {
  return (
    <ProtectedRoute>
      <CustomerShell>
        <ProfileContent />
      </CustomerShell>
    </ProtectedRoute>
  );
}
