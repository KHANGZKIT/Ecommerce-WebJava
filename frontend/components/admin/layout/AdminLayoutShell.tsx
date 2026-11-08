"use client";

import React, { useState } from "react";
import { AdminGuard } from "@/features/auth/AuthGuard";
import { AdminSidebar } from "./AdminSidebar";
import { AdminHeader } from "./AdminHeader";
import { Toaster } from "@/components/ui/sonner";

interface AdminLayoutShellProps {
  children: React.ReactNode;
}

export function AdminLayoutShell({ children }: AdminLayoutShellProps) {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <AdminGuard>
      <div className="min-h-screen flex w-full bg-background font-sans antialiased">
        {/* Left Sidebar */}
        <AdminSidebar
          collapsed={collapsed}
          onToggleCollapse={() => setCollapsed(!collapsed)}
          mobileOpen={mobileOpen}
          onCloseMobile={() => setMobileOpen(false)}
        />

        {/* Main Content Area */}
        <div className="flex flex-1 flex-col min-w-0 overflow-hidden">
          {/* Top Navbar */}
          <AdminHeader onToggleMobile={() => setMobileOpen(!mobileOpen)} />

          {/* Page Content Viewport */}
          <main className="flex-1 overflow-y-auto p-4 md:p-6 lg:p-8">
            <div className="mx-auto max-w-7xl space-y-6">
              {children}
            </div>
          </main>
        </div>
      </div>

      {/* Global Toast Provider */}
      <Toaster position="top-right" richColors />
    </AdminGuard>
  );
}
