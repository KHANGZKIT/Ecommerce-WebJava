import React from "react";
import Link from "next/link";

export interface BreadcrumbItem {
  label: string;
  href?: string;
  isCurrent?: boolean;
}

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
  className?: string;
}

export function Breadcrumbs({ items, className = "" }: BreadcrumbsProps) {
  if (!items || items.length === 0) return null;

  return (
    <nav
      aria-label="Đường dẫn trang"
      className={`flex items-center text-xs text-[#78716C] font-medium py-3 overflow-x-auto hide-scrollbar ${className}`}
    >
      <ol className="flex items-center gap-1.5 min-w-max list-none p-0 m-0">
        {/* Home icon link */}
        <li className="flex items-center">
          <Link
            href="/"
            className="flex items-center gap-1 text-[#78716C] hover:text-[#1C1917] transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[#D97706] rounded"
            title="Trang chủ TECHGEAR"
          >
            <svg
              className="w-3.5 h-3.5 text-[#A8A29E] hover:text-[#1C1917] transition-colors"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
              />
            </svg>
            <span className="sr-only">Trang chủ</span>
          </Link>
        </li>

        {items.map((item, index) => {
          const isLast = index === items.length - 1 || item.isCurrent;

          return (
            <li key={index} className="flex items-center gap-1.5">
              <span className="text-[#A8A29E] select-none text-[10px]" aria-hidden="true">
                /
              </span>
              {isLast || !item.href ? (
                <span
                  className="text-[#1C1917] font-semibold truncate max-w-[200px] sm:max-w-[320px]"
                  aria-current={isLast ? "page" : undefined}
                >
                  {item.label}
                </span>
              ) : (
                <Link
                  href={item.href}
                  className="text-[#78716C] hover:text-[#1C1917] transition-colors truncate max-w-[150px] sm:max-w-[240px] focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[#D97706] rounded"
                >
                  {item.label}
                </Link>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
