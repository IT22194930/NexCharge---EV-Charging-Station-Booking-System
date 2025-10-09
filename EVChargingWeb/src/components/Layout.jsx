import { Link, useNavigate, useLocation } from "react-router-dom";
import { useState, useEffect } from "react";
import toast from "react-hot-toast";
import api from "../api/axios";

export default function Layout({ children }) {
  const role = localStorage.getItem("role");
  const token = localStorage.getItem("token");
  const navigate = useNavigate();
  const location = useLocation();
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [userName, setUserName] = useState("");

  // Helper function to check if current path matches the link
  const isActiveLink = (path) => {
    return location.pathname === path;
  };

  // Fetch user profile to get the full name
  useEffect(() => {
    const fetchUserProfile = async () => {
      if (!token) return;
      
      try {
        const response = await api.get("/auth/profile");
        if (response.data && response.data.fullName) {
          setUserName(response.data.fullName);
        }
      } catch (error) {
        console.error("Failed to fetch user profile:", error);
        // If profile fetch fails, we'll just show "User" as fallback
      }
    };

    fetchUserProfile();
  }, [token]);

  const handleLogout = () => {
    if (!showLogoutConfirm) {
      setShowLogoutConfirm(true);
    }
  };

  const confirmLogout = () => {
    localStorage.clear();
    navigate("/login");
    toast.success("Logged out successfully!");
  };

  const cancelLogout = () => {
    setShowLogoutConfirm(false);
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      setShowLogoutConfirm(false);
    }
  };

  if (!token) return children;

  return (
    <>
      <div className="flex h-screen bg-gradient-to-br from-slate-50 to-slate-100">
        {/* Sidebar Navigation */}
        <div className="w-72 bg-gradient-to-br from-emerald-900 via-emerald-800 to-teal-800 text-white shadow-2xl relative overflow-hidden">
          {/* Gradient Overlay */}
          <div className="absolute inset-0 bg-gradient-to-br from-emerald-900/20 via-transparent to-teal-700/30 pointer-events-none"></div>
        {/* Logo Section */}
        <div className="p-4 border-b border-emerald-700 relative z-10">
          <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-emerald-700 via-teal-600 to-emerald-700"></div>
          <Link to="/dashboard" className="block">
            <div className="flex items-center space-x-4 cursor-pointer hover:bg-emerald-800/30 rounded-lg p-4 transition-all duration-200 group">
              <div className="flex-shrink-0">
                <div className="w-16 h-16 bg-white rounded-xl flex items-center justify-center shadow-lg">
                  <img 
                    src="/NexCharge-logo.png" 
                    alt="NexCharge Logo" 
                    className="w-12 h-12 object-contain group-hover:scale-105 transition-transform duration-200"
                  />
                </div>
              </div>
              <div className="flex-1 min-w-0">
                <h1 className="text-lg font-bold text-white group-hover:text-emerald-300 transition-colors duration-200">NEXCHARGE</h1>
                <p className="text-xs text-emerald-300 uppercase tracking-wider group-hover:text-emerald-200 transition-colors duration-200">Stay Charged</p>
              </div>
            </div>
          </Link>
        </div>

        {/* Welcome Section */}
        <div className="px-4 mt-8 relative z-10">
          <div className="bg-gradient-to-r from-emerald-800/50 to-teal-700/50 backdrop-blur-sm rounded-xl p-4 border border-emerald-600/20 shadow-lg">
            <div className="flex items-center space-x-3 mb-3">
              <div className="w-10 h-10 bg-gradient-to-br from-emerald-400 to-teal-400 rounded-full flex items-center justify-center shadow-lg">
                <svg className="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-6-3a2 2 0 11-4 0 2 2 0 014 0zm-2 4a5 5 0 00-4.546 2.916A5.986 5.986 0 0010 16a5.986 5.986 0 004.546-2.084A5 5 0 0010 11z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="flex-1">
                <p className="text-xs text-slate-200 mb-1">Welcome back,</p>
                <p className="text-sm font-semibold text-white truncate">
                  {userName || "User"}
                </p>
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="bg-slate-800/40 rounded-lg p-2 text-center border border-slate-600/30">
                <p className="text-slate-300 mb-1">Role</p>
                <p className="text-white font-medium">{role}</p>
              </div>
              <div className="bg-slate-800/40 rounded-lg p-2 text-center border border-slate-600/30">
                <p className="text-slate-300 mb-1">Status</p>
                <div className="flex items-center justify-center space-x-1">
                  <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                  <p className="text-white font-medium">Online</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        {/* Navigation Menu */}
        <nav className="mt-8 px-4 space-y-2 relative z-10">
          <Link
            to="/dashboard"
            className={`flex items-center px-4 py-3 rounded-xl transition-all duration-200 group ${
              isActiveLink('/dashboard') 
                ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-lg' 
                : 'text-emerald-100 hover:bg-emerald-700/50 hover:text-white'
            }`}
          >
            <svg className="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
              <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z" />
            </svg>
            Dashboard
          </Link>
          
          {role === "Backoffice" && (
            <>
              <Link
                to="/users"
                className={`flex items-center px-4 py-3 rounded-xl transition-all duration-200 group ${
                  isActiveLink('/users') 
                    ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-lg' 
                    : 'text-emerald-100 hover:bg-gradient-to-r hover:from-emerald-700/60 hover:to-teal-600/60 hover:text-white hover:shadow-lg'
                }`}
              >
                <svg className="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                </svg>
                User Management
              </Link>
              <Link
                to="/owners"
                className={`flex items-center px-4 py-3 rounded-xl transition-all duration-200 group ${
                  isActiveLink('/owners') 
                    ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-lg' 
                    : 'text-emerald-100 hover:bg-gradient-to-r hover:from-emerald-700/60 hover:to-teal-600/60 hover:text-white hover:shadow-lg'
                }`}
              >
                <svg className="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-6-3a2 2 0 11-4 0 2 2 0 014 0zm-2 4a5 5 0 00-4.546 2.916A5.986 5.986 0 0010 16a5.986 5.986 0 004.546-2.084A5 5 0 0010 11z" clipRule="evenodd" />
                </svg>
                EV Owners
              </Link>
              <Link
                to="/stations"
                className={`flex items-center px-4 py-3 rounded-xl transition-all duration-200 group ${
                  isActiveLink('/stations') 
                    ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-lg' 
                    : 'text-emerald-100 hover:bg-gradient-to-r hover:from-emerald-700/60 hover:to-teal-600/60 hover:text-white hover:shadow-lg'
                }`}
              >
                <svg className="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd" />
                </svg>
                Charging Stations
              </Link>
              <Link
                to="/bookings"
                className={`flex items-center px-4 py-3 rounded-xl transition-all duration-200 group ${
                  isActiveLink('/bookings') 
                    ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-lg' 
                    : 'text-emerald-100 hover:bg-gradient-to-r hover:from-emerald-700/60 hover:to-teal-600/60 hover:text-white hover:shadow-lg'
                }`}
              >
                <svg className="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd" />
                </svg>
                Bookings
              </Link>
            </>
          )}
          
          {role === "Operator" && (
            <Link
              to="/bookings"
              className={`flex items-center px-4 py-3 rounded-xl transition-all duration-200 group ${
                isActiveLink('/bookings') 
                  ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-lg' 
                  : 'text-emerald-100 hover:bg-gradient-to-r hover:from-emerald-700/60 hover:to-teal-600/60 hover:text-white hover:shadow-lg'
              }`}
            >
              <svg className="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd" />
              </svg>
              Bookings
            </Link>
          )}
          
          {role === "EVOwner" && (
            <Link
              to="/bookings"
              className={`flex items-center px-4 py-3 rounded-xl transition-all duration-200 group ${
                isActiveLink('/bookings') 
                  ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-lg' 
                  : 'text-emerald-100 hover:bg-gradient-to-r hover:from-emerald-700/60 hover:to-teal-600/60 hover:text-white hover:shadow-lg'
              }`}
            >
              <svg className="w-5 h-5 mr-3 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd" />
              </svg>
              My Bookings
            </Link>
          )}
        </nav>
        
        {/* Profile Link - accessible by all authenticated users */}
        <div className="absolute bottom-20 left-4 right-4 z-10">
          <Link
            to="/profile"
            className={`w-full py-3 px-4 rounded-xl transition-all duration-200 shadow-lg hover:shadow-xl flex items-center justify-center group ${
              isActiveLink('/profile')
                ? 'bg-gradient-to-r from-emerald-500 via-emerald-600 to-teal-500 text-white'
                : 'bg-gradient-to-r from-emerald-600 via-emerald-700 to-teal-600 text-white hover:from-emerald-700 hover:via-teal-600 hover:to-emerald-800'
            }`}
          >
            <svg className="w-5 h-5 mr-2 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-6-3a2 2 0 11-4 0 2 2 0 014 0zm-2 4a5 5 0 00-4.546 2.916A5.986 5.986 0 0010 16a5.986 5.986 0 004.546-2.084A5 5 0 0010 11z" clipRule="evenodd" />
            </svg>
            My Profile
          </Link>
        </div>
        
        {/* Logout Button */}
        <div className="absolute bottom-6 left-4 right-4 z-10">
          <button
            onClick={handleLogout}
            className="w-full bg-gradient-to-r from-red-600 to-red-700 text-white py-3 px-4 rounded-xl hover:from-red-700 hover:to-red-800 transition-all duration-200 shadow-lg hover:shadow-xl flex items-center justify-center group"
          >
            <svg className="w-5 h-5 mr-2 group-hover:scale-110 transition-transform" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 01-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clipRule="evenodd" />
            </svg>
            Logout
          </button>
        </div>
      </div>
      
      {/* Main Content */}
      <div className="flex-1 overflow-auto">
        <div className="p-8 max-w-7xl mx-auto">
          {children}
        </div>
      </div>
      </div>
      
      {/* Logout Confirmation Modal */}
      {showLogoutConfirm && (
        <div 
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 backdrop-blur-sm"
          onClick={handleBackdropClick}
        >
          <div className="bg-white rounded-2xl p-8 shadow-2xl transform transition-all duration-200 scale-100 max-w-md w-full mx-4">
            <div className="flex flex-col items-center space-y-6">
              <div className="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center">
                <svg className="w-8 h-8 text-amber-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
              </div>
              
              <div className="text-center">
                <h3 className="text-xl font-semibold text-gray-900 mb-2">Confirm Logout</h3>
                <p className="text-gray-600">Are you sure you want to logout from your account?</p>
              </div>
              
              <div className="flex space-x-4 w-full">
                <button
                  onClick={cancelLogout}
                  className="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors duration-200 font-medium"
                >
                  Cancel
                </button>
                <button
                  onClick={confirmLogout}
                  className="flex-1 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors duration-200 font-medium"
                >
                  Yes, Logout
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
