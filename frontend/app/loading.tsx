import React from "react";
import { CustomerShell } from "@/components/layout/CustomerShell";

export default function RootLoading() {
  return (
    <CustomerShell className="!px-0 !py-0 !max-w-full bg-[#FAFAF9]">
      <div className="w-full px-3.5 sm:px-4 lg:px-5 py-4 sm:py-6 space-y-6 sm:space-y-8 animate-pulse">
        {/* Hero banner skeleton */}
        <div className="w-full h-[220px] sm:h-[320px] md:h-[400px] lg:h-[450px] bg-[#E7E5E4]/60 rounded-2xl border border-[#E7E5E4]" />

        {/* Category strip skeleton */}
        <div className="bg-white rounded-2xl p-4 sm:p-6 border border-[#E7E5E4] space-y-4">
          <div className="flex justify-between items-center pb-3 border-b border-[#F5F5F4]">
            <div className="h-5 w-48 bg-[#E7E5E4] rounded-md" />
            <div className="h-4 w-28 bg-[#F5F5F4] rounded-md" />
          </div>
          <div className="grid grid-cols-4 sm:grid-cols-5 md:grid-cols-7 lg:grid-cols-10 gap-3">
            {Array.from({ length: 10 }).map((_, i) => (
              <div key={i} className="flex flex-col items-center gap-2">
                <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-2xl bg-[#F5F5F4]" />
                <div className="h-3 w-12 bg-[#E7E5E4] rounded" />
              </div>
            ))}
          </div>
        </div>

        {/* Product carousel skeleton */}
        <div className="bg-white rounded-2xl p-4 sm:p-6 border border-[#E7E5E4] space-y-4">
          <div className="flex justify-between items-center pb-4 border-b border-[#F5F5F4]">
            <div className="h-6 w-56 bg-[#E7E5E4] rounded-md" />
            <div className="h-5 w-24 bg-[#F5F5F4] rounded-md" />
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3 sm:gap-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="aspect-square bg-[#F5F5F4] rounded-xl" />
            ))}
          </div>
        </div>
      </div>
    </CustomerShell>
  );
}
