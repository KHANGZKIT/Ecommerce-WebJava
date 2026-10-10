export default function AdminCategoriesPage() {
  return (
    <div>
      <div className="pb-6 border-b border-[#E7E5E4] mb-8">
        <div className="font-mono text-xs uppercase tracking-wider text-[#78716C] mb-1.5">
          ADMIN-04 // TAXONOMY OPERATIONS
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-[#1C1917]">
          CATEGORY OPERATIONS
        </h1>
        <p className="text-sm text-[#57534E] mt-1">
          Catalog taxonomy, category creation, description updates, and active status control.
        </p>
      </div>

      <div className="p-6 bg-white border border-[#E7E5E4] rounded-[6px]">
        <div className="font-mono text-xs font-semibold text-[#1C1917] mb-2 uppercase">
          Foundation Skeleton
        </div>
        <p className="text-sm text-[#57534E]">
          Route: <code className="font-mono text-xs bg-[#F5F5F4] px-1.5 py-0.5 rounded">/admin/categories</code>. Category table, creation/edit modals, and delete protection will be implemented in Step 16.
        </p>
      </div>
    </div>
  );
}
