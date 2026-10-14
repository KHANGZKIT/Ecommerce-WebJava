import React from "react";
import { Header } from "./Header";
import { Footer } from "./Footer";

export function CustomerShell({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className="min-h-screen flex flex-col bg-[#FAFAF9]">
      <Header />
      <main className={`flex-1 w-full px-3.5 sm:px-4 lg:px-5 py-6 ${className}`}>
        {children}
      </main>
      <Footer />
    </div>
  );
}
