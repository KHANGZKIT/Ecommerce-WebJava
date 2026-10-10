export default function AdminOrdersPage() {
  return (
    <div>
      <div className="pb-6 border-b border-[#E7E5E4] mb-8">
        <div className="font-mono text-xs uppercase tracking-wider text-[#78716C] mb-1.5">
          ADMIN-06 // ORDER FULFILLMENT
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-[#1C1917]">
          ORDER OPERATIONS
        </h1>
        <p className="text-sm text-[#57534E] mt-1">
          Targeted order lookup by Order ID or User ID, status lifecycle updates, and cancellation.
        </p>
      </div>

      <div className="p-6 bg-white border border-[#E7E5E4] rounded-[6px]">
        <div className="font-mono text-xs font-semibold text-[#1C1917] mb-2 uppercase">
          Foundation Skeleton
        </div>
        <p className="text-sm text-[#57534E]">
          Route: <code className="font-mono text-xs bg-[#F5F5F4] px-1.5 py-0.5 rounded">/admin/orders</code>. Order lookup console, inspected order panel, and status transition modal will be implemented in Step 18.
        </p>
      </div>
    </div>
  );
}
