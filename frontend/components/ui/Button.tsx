import React from "react";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "accent" | "destructive" | "ghost";
  size?: "sm" | "md" | "lg";
  isLoading?: boolean;
}

export function Button({
  children,
  className = "",
  variant = "primary",
  size = "md",
  isLoading = false,
  disabled,
  ...props
}: ButtonProps) {
  const baseStyles =
    "inline-flex items-center justify-center font-mono font-semibold transition-colors focus:outline-none focus:ring-1 focus:ring-[#D97706] disabled:opacity-50 disabled:cursor-not-allowed";

  const variantStyles = {
    primary: "bg-[#1C1917] text-[#FAFAF9] hover:bg-[#292524] border border-[#1C1917]",
    secondary: "bg-white text-[#1C1917] hover:bg-[#F5F5F4] border border-[#E7E5E4] hover:border-[#1C1917]",
    accent: "bg-[#D97706] text-white hover:bg-[#B45309] border border-[#D97706]",
    destructive: "bg-[#DC2626] text-white hover:bg-[#B91C1C] border border-[#DC2626]",
    ghost: "bg-transparent text-[#57534E] hover:text-[#1C1917] hover:bg-[#F5F5F4] border border-transparent",
  };

  const sizeStyles = {
    sm: "text-[11px] px-3 py-1.5 rounded-[4px] min-h-[32px]",
    md: "text-xs px-4 py-2 rounded-[4px] min-h-[40px]",
    lg: "text-sm px-6 py-2.5 rounded-[4px] min-h-[44px]",
  };

  return (
    <button
      className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <span className="inline-flex items-center gap-2">
          <span className="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin" />
          <span>{children}</span>
        </span>
      ) : (
        children
      )}
    </button>
  );
}
