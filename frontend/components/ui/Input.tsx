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
          className="block font-mono text-[11px] uppercase tracking-wider text-[#57534E] font-medium"
        >
          {label}
        </label>
      )}
      <input
        id={inputId}
        disabled={disabled}
        className={`w-full px-3 py-2 bg-white border text-[#1C1917] placeholder-[#78716C] rounded-[4px] text-sm focus:outline-none focus:ring-1 transition-colors disabled:bg-[#F5F5F4] disabled:text-[#78716C] disabled:cursor-not-allowed ${
          error
            ? "border-[#DC2626] focus:border-[#DC2626] focus:ring-[#DC2626]"
            : "border-[#E7E5E4] focus:border-[#1C1917] focus:ring-[#D97706]"
        } ${className}`}
        {...props}
      />
      {error && <p className="font-mono text-[11px] text-[#DC2626]">{error}</p>}
      {!error && helperText && <p className="font-mono text-[11px] text-[#78716C]">{helperText}</p>}
    </div>
  );
}
