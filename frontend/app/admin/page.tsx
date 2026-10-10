export default function AdminDashboardPage() {
  return (
    <div>
      <div className="pb-6 border-b border-[#E7E5E4] mb-8">
        <div className="font-mono text-xs uppercase tracking-wider text-[#78716C] mb-1.5">
          ADMIN-02 // OPERATIONAL DASHBOARD
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-[#1C1917]">
          SYSTEM DASHBOARD
        </h1>
        <p className="text-sm text-[#57534E] mt-1">
          Administrative gateway into catalog, order processing, and customer operations.
        </p>
      </div>

      <div className="p-6 bg-white border border-[#E7E5E4] rounded-[6px]">
        <div className="font-mono text-xs font-semibold text-[#1C1917] mb-2 uppercase">
          Foundation Skeleton
        </div>
        <p className="text-sm text-[#57534E]">
          Route: <code className="font-mono text-xs bg-[#F5F5F4] px-1.5 py-0.5 rounded">/admin</code>. Module routing vitrine and direct operational links will be implemented in Step 14.
        </p>
      </div>
    </div>
  );
}
