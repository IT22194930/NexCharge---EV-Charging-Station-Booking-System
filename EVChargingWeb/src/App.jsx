import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from 'react-hot-toast';
import Layout from "./components/Layout";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Users from "./pages/Users";
import Owners from "./pages/Owners";
import Bookings from "./pages/Bookings";
import Stations from "./pages/Stations";
import Profile from "./pages/Profile";

function PrivateRoute({ children, roles }) {
  const token = localStorage.getItem("token");
  const role = localStorage.getItem("role");
  if (!token) return <Navigate to="/login" />;
  if (roles && !roles.includes(role)) return <Navigate to="/login" />;
  return <Layout>{children}</Layout>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        {/* Dashboard - accessible by all authenticated users */}
        <Route path="/dashboard" element={
          <PrivateRoute>
            <Dashboard />
          </PrivateRoute>
        } />
        
        {/* User Management - Backoffice only */}
        <Route path="/users" element={
          <PrivateRoute roles={["Backoffice"]}>
            <Users />
          </PrivateRoute>
        } />
        
        {/* EV Owner Management - Backoffice only */}
        <Route path="/owners" element={
          <PrivateRoute roles={["Backoffice"]}>
            <Owners />
          </PrivateRoute>
        } />

        {/* Station Management - Backoffice only */}
        <Route path="/stations" element={
          <PrivateRoute roles={["Backoffice"]}>
            <Stations />
          </PrivateRoute>
        } />
        
        {/* Booking Management - Backoffice, Operator, and EVOwner */}
        <Route path="/bookings" element={
          <PrivateRoute roles={["Backoffice", "Operator", "EVOwner"]}>
            <Bookings />
          </PrivateRoute>
        } />
        
        {/* Profile - accessible by all authenticated users */}
        <Route path="/profile" element={
          <PrivateRoute>
            <Profile />
          </PrivateRoute>
        } />
      </Routes>
      
      {/* Toast Container */}
      <Toaster
        position="top-right"
      />
    </BrowserRouter>
  );
}
