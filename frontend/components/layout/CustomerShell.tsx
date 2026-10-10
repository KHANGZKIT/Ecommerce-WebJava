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
      <main className={`flex-1 w-full max-w-[1280px] mx-auto px-6 py-8 ${className}`}>
        {children}
      </main>
      <Footer />
    </div>
  );
}
