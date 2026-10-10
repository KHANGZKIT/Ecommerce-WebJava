import type { Metadata } from "next";
import { Inter, Georama } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/features/auth/AuthContext";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-sans",
  display: "swap",
});

const georama = Georama({
  subsets: ["latin"],
  variable: "--font-heading",
  display: "swap",
});

export const metadata: Metadata = {
  title: "TECHGEAR — Cửa hàng thiết bị công nghệ & phụ kiện cao cấp",
  description: "Cửa hàng bán lẻ thiết bị công nghệ, bàn phím cơ, chuột không dây, màn hình và phụ kiện cao cấp chính hãng tại Việt Nam.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className={`${inter.variable} ${georama.variable}`}>
      <body className="bg-[#FAFAF9] text-[#1C1917] font-sans min-h-screen antialiased selection:bg-[#1C1917] selection:text-[#FAFAF9] flex flex-col">
        <AuthProvider>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
