import { AdminGuard } from "@/features/auth/AuthGuard";
import { AdminHeader } from "@/components/layout/AdminHeader";
import { AdminFooter } from "@/components/layout/AdminFooter";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AdminGuard>
      <div className="min-h-screen flex flex-col bg-[#FAFAF9]">
        <AdminHeader />
        <main className="flex-1 w-full max-w-[1440px] mx-auto px-6 py-8">
          {children}
        </main>
        <AdminFooter />
      </div>
    </AdminGuard>
  );
}
