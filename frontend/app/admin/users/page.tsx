export default function AdminUsersPage() {
  return (
    <div>
      <div className="pb-6 border-b border-[#E7E5E4] mb-8">
        <div className="font-mono text-xs uppercase tracking-wider text-[#78716C] mb-1.5">
          ADMIN-05 // USER DIRECTORY
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-[#1C1917]">
          USER MANAGEMENT
        </h1>
        <p className="text-sm text-[#57534E] mt-1">
          Registered customer accounts, role inspection, and account termination controls.
        </p>
      </div>

      <div className="p-6 bg-white border border-[#E7E5E4] rounded-[6px]">
        <div className="font-mono text-xs font-semibold text-[#1C1917] mb-2 uppercase">
          Foundation Skeleton
        </div>
        <p className="text-sm text-[#57534E]">
          Route: <code className="font-mono text-xs bg-[#F5F5F4] px-1.5 py-0.5 rounded">/admin/users</code>. User directory list, detail inspection drawer, and user delete flow will be implemented in Step 17.
        </p>
      </div>
    </div>
  );
}
