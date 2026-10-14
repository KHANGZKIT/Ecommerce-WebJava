import React from "react";

export function LoadingSkeleton({
  className = "",
  rows = 3,
}: {
  className?: string;
  rows?: number;
}) {
  return (
    <div className={`space-y-3 animate-pulse ${className}`} role="status" aria-label="Đang tải dữ liệu">
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className="h-8 bg-[#E7E5E4] rounded-lg w-full"
          style={{ opacity: 1 - i * 0.15 }}
        />
      ))}
      <span className="sr-only">Đang tải...</span>
    </div>
  );
}

export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div
      className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6"
      role="status"
      aria-label="Đang tải danh sách sản phẩm"
    >
      {Array.from({ length: count }).map((_, idx) => (
        <div
          key={idx}
          className="bg-white border border-[#E7E5E4] rounded-xl p-4 flex flex-col animate-pulse"
        >
          <div className="aspect-square bg-[#F5F5F4] rounded-lg mb-3" />
          <div className="h-3 w-20 bg-[#E7E5E4] rounded mb-2" />
          <div className="h-4 w-4/5 bg-[#E7E5E4] rounded mb-4" />
          <div className="mt-auto pt-3 border-t border-[#F5F5F4] flex justify-between items-center">
            <div className="h-4 w-24 bg-[#E7E5E4] rounded" />
            <div className="h-3 w-16 bg-[#E7E5E4] rounded" />
          </div>
        </div>
      ))}
      <span className="sr-only">Đang tải sản phẩm...</span>
    </div>
  );
}

export function EmptyState({
  title = "Không tìm thấy dữ liệu",
  message = "Không có mục nào phù hợp với yêu cầu của bạn.",
  actionLabel,
  onAction,
  secondaryActionLabel,
  onSecondaryAction,
}: {
  title?: string;
  message?: string;
  actionLabel?: string;
  onAction?: () => void;
  secondaryActionLabel?: string;
  onSecondaryAction?: () => void;
}) {
  return (
    <div className="w-full py-14 px-6 text-center bg-white border border-[#E7E5E4] rounded-2xl shadow-sm">
      <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-[#FAFAF9] border border-[#E7E5E4] flex items-center justify-center text-[#A8A29E]">
        <svg
          className="w-8 h-8 text-[#78716C]"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
          />
        </svg>
      </div>
      <h3 className="text-base sm:text-lg font-bold text-[#1C1917] mb-2 tracking-tight">
        {title}
      </h3>
      <p className="text-xs sm:text-sm text-[#78716C] max-w-md mx-auto mb-6 leading-relaxed">
        {message}
      </p>
      <div className="flex flex-wrap items-center justify-center gap-3">
        {actionLabel && onAction && (
          <button
            type="button"
            onClick={onAction}
            className="inline-flex items-center justify-center text-xs font-semibold px-5 py-2.5 rounded-lg bg-[#1C1917] text-white hover:bg-[#292524] active:scale-[0.98] transition-all cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            {actionLabel}
          </button>
        )}
        {secondaryActionLabel && onSecondaryAction && (
          <button
            type="button"
            onClick={onSecondaryAction}
            className="inline-flex items-center justify-center text-xs font-semibold px-4 py-2.5 rounded-lg bg-white border border-[#E7E5E4] text-[#1C1917] hover:bg-[#FAFAF9] active:scale-[0.98] transition-all cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            {secondaryActionLabel}
          </button>
        )}
      </div>
    </div>
  );
}

export function ErrorState({
  title = "Không thể tải dữ liệu",
  message = "Đã xảy ra sự cố trong quá trình kết nối với máy chủ. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.",
  onRetry,
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="w-full py-12 px-6 text-center bg-white border border-rose-200 rounded-2xl shadow-sm">
      <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 mb-4 border border-rose-100">
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
          />
        </svg>
      </div>
      <h3 className="text-base sm:text-lg font-bold text-rose-700 mb-2 tracking-tight">
        {title}
      </h3>
      <p className="text-xs sm:text-sm text-[#78716C] max-w-md mx-auto mb-6 leading-relaxed">
        {message}
      </p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex items-center gap-2 text-xs font-semibold px-5 py-2.5 rounded-lg bg-[#1C1917] text-white hover:bg-[#292524] active:scale-[0.98] transition-all cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
        >
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          <span>Thử lại</span>
        </button>
      )}
    </div>
  );
}
