import React from "react";
import { AdminLayoutShell } from "@/components/admin/layout/AdminLayoutShell";

export const metadata = {
  title: "TECHGEAR Admin Dashboard",
  description: "Trang quản trị vận hành sàn thương mại điện tử công nghệ TECHGEAR",
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AdminLayoutShell>{children}</AdminLayoutShell>;
}
