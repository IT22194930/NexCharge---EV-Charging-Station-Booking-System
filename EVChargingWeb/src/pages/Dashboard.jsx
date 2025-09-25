import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";
import StatsCardSkeleton from "../components/StatsCardSkeleton";

export default function Dashboard() {
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalStations: 0,
    totalBookings: 0,
    activeBookings: 0
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const role = localStorage.getItem("role");
  const navigate = useNavigate();

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await api.get("/dashboard/stats");
        setStats(response.data);
      } catch (err) {
        console.error("Failed to fetch dashboard stats:", err);
        setError("Failed to load dashboard statistics");
        // Fallback to default values on error
        setStats({
          totalUsers: 0,
          totalStations: 0,
          totalBookings: 0,
          activeBookings: 0
        });
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, [role]);

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Welcome to EV Charging System</h1>
        {error && (
          <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-red-800">{error}</p>
          </div>
        )}
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {loading ? (
          // Show skeleton cards while loading
          <>
            {role === "Backoffice" && (
              <>
                <StatsCardSkeleton />
                <StatsCardSkeleton />
              </>
            )}
            <StatsCardSkeleton />
            <StatsCardSkeleton />
          </>
        ) : (
          // Show actual stats cards
          <>
            {role === "Backoffice" && (
              <>
                <div className="bg-white p-6 rounded-lg shadow">
                  <div className="flex items-center">
                    <div className="p-3 rounded-full bg-blue-100">
                      <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                      </svg>
                    </div>
                    <div className="ml-4">
                      <p className="text-sm font-medium text-gray-600">Total Users</p>
                      <p className="text-2xl font-bold text-gray-900">{stats.totalUsers}</p>
                    </div>
                  </div>
                </div>

                <div className="bg-white p-6 rounded-lg shadow">
                  <div className="flex items-center">
                    <div className="p-3 rounded-full bg-green-100">
                      <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                      </svg>
                    </div>
                    <div className="ml-4">
                      <p className="text-sm font-medium text-gray-600">Charging Stations</p>
                      <p className="text-2xl font-bold text-gray-900">{stats.totalStations}</p>
                    </div>
                  </div>
                </div>
              </>
            )}

            <div className="bg-white p-6 rounded-lg shadow">
              <div className="flex items-center">
                <div className="p-3 rounded-full bg-yellow-100">
                  <svg className="w-8 h-8 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">
                    {role === "EVOwner" ? "My Bookings" : "Total Bookings"}
                  </p>
                  <p className="text-2xl font-bold text-gray-900">{stats.totalBookings}</p>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow">
              <div className="flex items-center">
                <div className="p-3 rounded-full bg-red-100">
                  <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">
                    {role === "EVOwner" ? "My Active Bookings" : "Active Bookings"}
                  </p>
                  <p className="text-2xl font-bold text-gray-900">{stats.activeBookings}</p>
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Quick Actions */}
      <div className="bg-white p-6 rounded-lg shadow">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {role === "Backoffice" && (
            <>
              <button
                onClick={() => navigate('/users')}
                className="text-center p-4 border rounded-lg hover:bg-blue-50 hover:border-blue-300 transition-all duration-200 cursor-pointer group"
              >
                <div className="p-3 rounded-full bg-blue-100 group-hover:bg-blue-200 transition-colors mx-auto w-fit mb-3">
                  <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <h3 className="font-medium text-gray-900">User Management</h3>
                <p className="text-sm text-gray-600 mt-1">Create and manage system users</p>
              </button>
              
              <button
                onClick={() => navigate('/owners')}
                className="text-center p-4 border rounded-lg hover:bg-green-50 hover:border-green-300 transition-all duration-200 cursor-pointer group"
              >
                <div className="p-3 rounded-full bg-green-100 group-hover:bg-green-200 transition-colors mx-auto w-fit mb-3">
                  <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </div>
                <h3 className="font-medium text-gray-900">EV Owner Management</h3>
                <p className="text-sm text-gray-600 mt-1">Manage EV owner profiles</p>
              </button>
              
              <button
                onClick={() => navigate('/stations')}
                className="text-center p-4 border rounded-lg hover:bg-yellow-50 hover:border-yellow-300 transition-all duration-200 cursor-pointer group"
              >
                <div className="p-3 rounded-full bg-yellow-100 group-hover:bg-yellow-200 transition-colors mx-auto w-fit mb-3">
                  <svg className="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                </div>
                <h3 className="font-medium text-gray-900">Station Management</h3>
                <p className="text-sm text-gray-600 mt-1">Configure charging stations</p>
              </button>
            </>
          )}
          
          <button
            onClick={() => navigate('/bookings')}
            className="text-center p-4 border rounded-lg hover:bg-purple-50 hover:border-purple-300 transition-all duration-200 cursor-pointer group"
          >
            <div className="p-3 rounded-full bg-purple-100 group-hover:bg-purple-200 transition-colors mx-auto w-fit mb-3">
              <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
            <h3 className="font-medium text-gray-900">
              {role === "EVOwner" ? "My Bookings" : "Booking Management"}
            </h3>
            <p className="text-sm text-gray-600 mt-1">
              {role === "Operator" ? "Approve and manage bookings" : 
               role === "EVOwner" ? "Create and manage your charging bookings" :
               "View and manage all bookings"}
            </p>
          </button>
        </div>
      </div>

      
    </div>
  );
}
