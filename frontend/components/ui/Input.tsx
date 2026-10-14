import React from "react";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

export function Input({
  label,
  error,
  helperText,
  id,
  className = "",
  disabled,
  ...props
}: InputProps) {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, "-") : undefined);

  return (
    <div className="w-full space-y-1.5">
      {label && (
        <label
          htmlFor={inputId}
          className="block text-xs font-semibold text-[#1C1917] tracking-tight"
        >
          {label}
        </label>
      )}
      <div className="relative">
        <input
          id={inputId}
          disabled={disabled}
          className={`w-full h-10 px-3.5 bg-white border text-sm text-[#1C1917] placeholder-[#A8A29E] rounded-lg transition-all focus:outline-none disabled:bg-[#F5F5F4] disabled:text-[#A8A29E] disabled:cursor-not-allowed ${
            error
              ? "border-[#DC2626] focus:border-[#DC2626] focus:ring-2 focus:ring-[#DC2626]/20"
              : "border-[#E7E5E4] hover:border-[#1C1917]/50 focus:border-[#1C1917] focus:ring-2 focus:ring-[#1C1917]/10"
          } ${className}`}
          {...props}
        />
      </div>
      {error && (
        <div className="flex items-center gap-1.5 text-xs text-[#DC2626] font-medium pt-0.5">
          <svg className="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{error}</span>
        </div>
      )}
      {!error && helperText && (
        <p className="text-xs text-[#78716C]">{helperText}</p>
      )}
    </div>
  );
}
