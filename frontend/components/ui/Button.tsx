import React from "react";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "accent" | "destructive" | "ghost" | "outline";
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
    "inline-flex items-center justify-center font-medium transition-all duration-150 cursor-pointer select-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706] focus-visible:ring-offset-2 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100 disabled:pointer-events-none";

  const variantStyles = {
    primary:
      "bg-[#1C1917] text-[#FAFAF9] hover:bg-[#292524] active:bg-black border border-[#1C1917] shadow-sm",
    secondary:
      "bg-white text-[#1C1917] hover:bg-[#FAFAF9] hover:border-[#1C1917] active:bg-[#F5F5F4] border border-[#E7E5E4] shadow-sm",
    accent:
      "bg-[#D97706] text-white hover:bg-[#B45309] active:bg-[#92400E] border border-[#D97706] shadow-sm",
    destructive:
      "bg-[#DC2626] text-white hover:bg-[#B91C1C] active:bg-[#991B1B] border border-[#DC2626] shadow-sm",
    ghost:
      "bg-transparent text-[#78716C] hover:text-[#1C1917] hover:bg-[#F5F5F4] border border-transparent",
    outline:
      "bg-transparent text-[#1C1917] hover:bg-[#1C1917] hover:text-white border border-[#1C1917]",
  };

  const sizeStyles = {
    sm: "text-xs px-3 py-1.5 rounded-lg min-h-[32px] gap-1.5",
    md: "text-xs sm:text-sm px-4 py-2 rounded-lg min-h-[38px] gap-2",
    lg: "text-sm sm:text-base px-6 py-2.5 rounded-xl min-h-[44px] gap-2.5",
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
