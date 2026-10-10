import React from "react";

export function LoadingSkeleton({
  className = "",
  rows = 3,
}: {
  className?: string;
  rows?: number;
}) {
  return (
    <div className={`space-y-3 animate-pulse ${className}`}>
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className="h-8 bg-[#E7E5E4] rounded-[4px] w-full"
          style={{ opacity: 1 - i * 0.15 }}
        />
      ))}
    </div>
  );
}

export function EmptyState({
  title = "Không tìm thấy dữ liệu",
  message = "Không có mục nào phù hợp với yêu cầu của bạn.",
  actionLabel,
  onAction,
}: {
  title?: string;
  message?: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <div className="w-full py-16 px-6 text-center bg-white border border-[#E7E5E4] rounded-lg">
      <h3 className="text-base font-semibold text-[#1C1917] mb-2">
        {title}
      </h3>
      <p className="text-sm text-[#57534E] max-w-md mx-auto mb-6">{message}</p>
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="inline-flex items-center justify-center text-xs font-semibold px-4 py-2 rounded-md bg-[#1C1917] text-white hover:bg-[#292524] transition-colors"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}

export function ErrorState({
  title = "Không thể tải dữ liệu",
  message = "Đã xảy ra lỗi kết nối hoặc máy chủ. Vui lòng thử lại.",
  onRetry,
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="w-full py-16 px-6 text-center bg-white border border-rose-200 rounded-lg">
      <div className="inline-flex items-center justify-center w-10 h-10 rounded-full bg-rose-50 text-rose-600 font-bold text-lg mb-3">
        !
      </div>
      <h3 className="text-base font-semibold text-rose-700 mb-2">
        {title}
      </h3>
      <p className="text-sm text-[#57534E] max-w-md mx-auto mb-6">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="inline-flex items-center justify-center text-xs font-semibold px-5 py-2.5 rounded-md bg-[#1C1917] text-white hover:bg-[#292524] transition-colors"
        >
          Thử lại
        </button>
      )}
    </div>
  );
}
