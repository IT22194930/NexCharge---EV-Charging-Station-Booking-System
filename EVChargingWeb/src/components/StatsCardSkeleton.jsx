export default function StatsCardSkeleton() {
  return (
    <div className="bg-white p-6 rounded-lg shadow animate-pulse">
      <div className="flex items-center">
        <div className="p-3 rounded-full bg-gray-200">
          <div className="w-8 h-8 bg-gray-300 rounded"></div>
        </div>
        <div className="ml-4 flex-1">
          <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
          <div className="h-8 bg-gray-300 rounded w-1/2"></div>
        </div>
      </div>
    </div>
  );
}