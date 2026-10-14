import type { Metadata } from "next";
import { Be_Vietnam_Pro } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/features/auth/AuthContext";

const beVietnamPro = Be_Vietnam_Pro({
  weight: ["300", "400", "500", "600", "700", "800"],
  subsets: ["latin", "vietnamese"],
  variable: "--font-sans",
  display: "swap",
});

export const metadata: Metadata = {
  title: "TECHGEAR — Cửa hàng thiết bị công nghệ & phụ kiện cao cấp",
  description: "Cửa hàng bán lẻ thiết bị công nghệ, laptop, điện thoại, phụ kiện cao cấp chính hãng tại Việt Nam.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className={beVietnamPro.variable}>
      <body className="bg-[#FAFAF9] text-[#1C1917] font-sans min-h-screen antialiased selection:bg-[#1C1917] selection:text-[#FAFAF9] flex flex-col">
        <AuthProvider>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
