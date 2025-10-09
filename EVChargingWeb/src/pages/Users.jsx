import { useEffect, useState, useCallback } from "react";
import api from "../api/axios";
import toast from "react-hot-toast";
import Pagination from "../components/Pagination";

export default function Users() {
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [stations, setStations] = useState([]);
  const [error, setError] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [userToDelete, setUserToDelete] = useState(null);
  const [showStatusConfirm, setShowStatusConfirm] = useState(false);
  const [userToToggle, setUserToToggle] = useState(null);
  const [showUpdateForm, setShowUpdateForm] = useState(false);
  const [userToUpdate, setUserToUpdate] = useState(null);
  const [roleFilter, setRoleFilter] = useState("All");
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10);
  const [form, setForm] = useState({
    nic: "",
    fullName: "",
    role: "Operator",
    password: "",
    assignedStationId: "",
  });

  const loadUsers = useCallback(async () => {
    try {
      const res = await api.get("/users");
      // Show all users in the system
      setUsers(res.data);
      filterUsers(res.data, roleFilter);
    } catch (err) {
      setError("Failed to load users");
      console.error(err);
    }
  }, [roleFilter]);

  const loadStations = useCallback(async () => {
    try {
      const res = await api.get("/stations");
      setStations(res.data);
    } catch (err) {
      console.error("Failed to load stations", err);
    }
  }, []);

  // Helper function to get operators count for a station
  const getOperatorsCountForStation = (stationId) => {
    return users.filter(user => user.role === "Operator" && user.assignedStationId === stationId).length;
  };

  const filterUsers = (allUsers, filter) => {
    if (filter === "All") {
      setFilteredUsers(allUsers);
    } else {
      const filtered = allUsers.filter(user => user.role === filter);
      setFilteredUsers(filtered);
    }
  };

  const handleRoleFilterChange = (filter) => {
    setRoleFilter(filter);
    setCurrentPage(1); // Reset to first page when filtering
    filterUsers(users, filter);
  };

  // Pagination logic
  const totalPages = Math.ceil(filteredUsers.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentUsers = filteredUsers.slice(startIndex, endIndex);

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  const createUser = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        nic: form.nic,
        fullName: form.fullName,
        role: form.role,
        password: form.password,
        ...(form.role === "Operator" && form.assignedStationId && {
          assignedStationId: form.assignedStationId
        })
      };
      await api.post("/users", payload);
      setForm({ nic: "", fullName: "", role: "Operator", password: "", assignedStationId: "" });
      setShowCreateForm(false);
      await loadUsers(); // This will apply the current filter
      toast.success("User created successfully!");
    } catch (err) {
      toast.error(
        "Error creating user: " + (err.response?.data?.message || err.message)
      );
    }
  };

  const deleteUser = async (nic) => {
    if (!showDeleteConfirm) {
      setUserToDelete({ nic });
      setShowDeleteConfirm(true);
      return;
    }
  };

  const confirmDelete = async () => {
    if (!userToDelete) return;

    try {
      await api.delete(`/users/${userToDelete.nic}`);
      await loadUsers(); // This will apply the current filter
      toast.success("User deleted successfully!");
      setShowDeleteConfirm(false);
      setUserToDelete(null);
    } catch (err) {
      toast.error(
        "Error deleting user: " + (err.response?.data?.message || err.message)
      );
      setShowDeleteConfirm(false);
      setUserToDelete(null);
    }
  };

  const cancelDelete = () => {
    setShowDeleteConfirm(false);
    setUserToDelete(null);
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      setShowDeleteConfirm(false);
      setUserToDelete(null);
    }
  };

  const toggleUserStatus = async (nic, isActive, fullName) => {
    if (!showStatusConfirm) {
      setUserToToggle({ nic, isActive, fullName });
      setShowStatusConfirm(true);
      return;
    }
  };

  const confirmStatusToggle = async () => {
    if (!userToToggle) return;

    try {
      const endpoint = userToToggle.isActive ? "deactivate" : "activate";
      await api.post(`/users/${endpoint}/${userToToggle.nic}`);
      await loadUsers(); // This will apply the current filter
      toast.success(`User ${userToToggle.isActive ? "deactivated" : "activated"} successfully!`);
      setShowStatusConfirm(false);
      setUserToToggle(null);
    } catch (err) {
      toast.error(
        `Error ${userToToggle.isActive ? "deactivating" : "activating"} user: ` +
          (err.response?.data?.message || err.message)
      );
      setShowStatusConfirm(false);
      setUserToToggle(null);
    }
  };

  const cancelStatusToggle = () => {
    setShowStatusConfirm(false);
    setUserToToggle(null);
  };

  const handleStatusBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      setShowStatusConfirm(false);
      setUserToToggle(null);
    }
  };

  const updateUserRole = (user) => {
    setUserToUpdate({
      nic: user.nic,
      fullName: user.fullName,
      currentRole: user.role,
      newRole: user.role,
      assignedStationId: user.assignedStationId || "",
      assignedStationName: user.assignedStationName || ""
    });
    setShowUpdateForm(true);
  };

  const confirmRoleUpdate = async () => {
    if (!userToUpdate) return;

    try {
      const payload = {
        newRole: userToUpdate.newRole,
        ...(userToUpdate.newRole === "Operator" && userToUpdate.assignedStationId && {
          assignedStationId: userToUpdate.assignedStationId
        })
      };
      
      // Using PATCH request to update the user role
      await api.patch(`/users/${userToUpdate.nic}/role`, payload);
      await loadUsers();
      
      toast.success(`User role updated to ${userToUpdate.newRole} successfully!`, {
        duration: 3000,
        position: 'top-right',
        style: {
          background: '#059669',
          color: '#fff',
          fontWeight: '500',
          borderRadius: '12px',
          padding: '12px 16px'
        },
        iconTheme: {
          primary: '#fff',
          secondary: '#059669'
        }
      });
      
      setShowUpdateForm(false);
      setUserToUpdate(null);
    } catch (err) {
      const errorMessage = err.response?.data || 'Failed to update user role';
      
      toast.error(errorMessage, {
        duration: 4000,
        position: 'top-right',
        style: {
          background: '#DC2626',
          color: '#fff',
          fontWeight: '500',
          borderRadius: '12px',
          padding: '12px 16px'
        }
      });
    }
  };

  const cancelRoleUpdate = () => {
    setShowUpdateForm(false);
    setUserToUpdate(null);
  };

  const handleUpdateBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      setShowUpdateForm(false);
      setUserToUpdate(null);
    }
  };

  useEffect(() => {
    loadUsers();
    loadStations();
  }, [loadUsers, loadStations]);

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">User Management</h1>
        <button
          onClick={() => setShowCreateForm(true)}
          className="bg-gradient-to-r from-green-600 to-emerald-600 text-white px-4 py-2 rounded hover:from-green-700 hover:to-emerald-700 transition-all duration-200"
        >
          Create New User
        </button>
      </div>

      {error && <p className="text-red-500 mb-4">{error}</p>}

      {/* Mobile Filter Section */}
      <div className="md:hidden mb-6">
        <div className="bg-white rounded-lg shadow p-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Filter by Role
          </label>
          <select
            value={roleFilter}
            onChange={(e) => handleRoleFilterChange(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
          >
            <option value="All">All Users ({users.length})</option>
            <option value="Backoffice">
              Backoffice ({users.filter(u => u.role === "Backoffice").length})
            </option>
            <option value="Operator">
              Operator ({users.filter(u => u.role === "Operator").length})
            </option>
            <option value="EVOwner">
              EV Owner ({users.filter(u => u.role === "EVOwner").length})
            </option>
          </select>
        </div>
      </div>

      {/* Create User Modal */}
      {showCreateForm && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg transform transition-all duration-300 max-h-[95vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-green-600 to-emerald-600 p-4 rounded-t-2xl">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-white/20 rounded-full flex items-center justify-center">
                    <svg
                      className="w-4 h-4 text-white"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z" />
                    </svg>
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-white">
                      Create New User
                    </h2>
                    <p className="text-green-100 text-xs">
                      Add a new user to the system
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setShowCreateForm(false)}
                  className="text-white/80 hover:text-white hover:bg-white/10 p-1.5 rounded-full transition-all duration-200"
                >
                  <svg
                    className="w-4 h-4"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                      clipRule="evenodd"
                    />
                  </svg>
                </button>
              </div>
            </div>

            {/* Modal Body */}
            <div className="p-5">
              <form onSubmit={createUser} className="space-y-4">
                {/* NIC Input */}
                <div>
                  <label className="flex items-center text-sm font-medium text-gray-700 mb-1.5">
                    <svg
                      className="w-4 h-4 mr-2 text-gray-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 2a1 1 0 00-1 1v1a1 1 0 002 0V3a1 1 0 00-1-1zM4 4h3a3 3 0 006 0h3a2 2 0 012 2v9a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm2.5 7a1.5 1.5 0 100-3 1.5 1.5 0 000 3zm2.45 4a2.5 2.5 0 10-4.9 0h4.9zM12 9a1 1 0 100 2h3a1 1 0 100-2h-3zm-1 4a1 1 0 011-1h2a1 1 0 110 2h-2a1 1 0 01-1-1z"
                        clipRule="evenodd"
                      />
                    </svg>
                    NIC (National Identity Card)
                  </label>
                  <input
                    type="text"
                    placeholder="Enter NIC number"
                    value={form.nic}
                    onChange={(e) => setForm({ ...form, nic: e.target.value })}
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                    required
                  />
                </div>

                {/* Full Name Input */}
                <div>
                  <label className="flex items-center text-sm font-medium text-gray-700 mb-1.5">
                    <svg
                      className="w-4 h-4 mr-2 text-gray-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
                        clipRule="evenodd"
                      />
                    </svg>
                    Full Name
                  </label>
                  <input
                    type="text"
                    placeholder="Enter full name"
                    value={form.fullName}
                    onChange={(e) =>
                      setForm({ ...form, fullName: e.target.value })
                    }
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                    required
                  />
                </div>

                {/* Role Selection */}
                <div>
                  <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                    <svg
                      className="w-4 h-4 mr-2 text-gray-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M6 6V5a3 3 0 013-3h2a3 3 0 013 3v1h2a2 2 0 012 2v3.57A22.952 22.952 0 0110 13a22.95 22.95 0 01-8-1.43V8a2 2 0 012-2h2zm2-1a1 1 0 011-1h2a1 1 0 011 1v1H8V5zm1 5a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1z"
                        clipRule="evenodd"
                      />
                    </svg>
                    User Role
                  </label>
                  <div className="grid grid-cols-3 gap-2">
                    <label className="flex flex-col items-center p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-all duration-200 hover:border-gray-300">
                      <input
                        type="radio"
                        name="role"
                        value="EVOwner"
                        checked={form.role === "EVOwner"}
                        onChange={(e) =>
                          setForm({ ...form, role: e.target.value, assignedStationId: "" })
                        }
                        className="w-3 h-3 text-green-600 focus:ring-green-500 mb-2"
                      />
                      <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center mb-1">
                        <svg
                          className="w-4 h-4 text-green-600"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path d="M8 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM15 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z" />
                          <path d="M3 4a1 1 0 00-1 1v10a1 1 0 001 1h1.05a2.5 2.5 0 014.9 0H10a1 1 0 001-1V5a1 1 0 00-1-1H3zM14 7a1 1 0 00-1 1v6.05A2.5 2.5 0 0115.95 16H17a1 1 0 001-1V8a1 1 0 00-1-1h-3z" />
                        </svg>
                      </div>
                      <div className="text-center">
                        <p className="font-medium text-gray-900 text-xs">
                          EV Owner
                        </p>
                        <p className="text-xs text-gray-500">Vehicle owner</p>
                      </div>
                    </label>

                    <label className="flex flex-col items-center p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-all duration-200 hover:border-gray-300">
                      <input
                        type="radio"
                        name="role"
                        value="Operator"
                        checked={form.role === "Operator"}
                        onChange={(e) =>
                          setForm({ ...form, role: e.target.value, assignedStationId: "" })
                        }
                        className="w-3 h-3 text-blue-600 focus:ring-blue-500 mb-2"
                      />
                      <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center mb-1">
                        <svg
                          className="w-4 h-4 text-blue-600"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path
                            fillRule="evenodd"
                            d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
                            clipRule="evenodd"
                          />
                        </svg>
                      </div>
                      <div className="text-center">
                        <p className="font-medium text-gray-900 text-xs">
                          Operator
                        </p>
                        <p className="text-xs text-gray-500">Station operator</p>
                      </div>
                    </label>

                    <label className="flex flex-col items-center p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-all duration-200 hover:border-gray-300">
                      <input
                        type="radio"
                        name="role"
                        value="Backoffice"
                        checked={form.role === "Backoffice"}
                        onChange={(e) =>
                          setForm({ ...form, role: e.target.value, assignedStationId: "" })
                        }
                        className="w-3 h-3 text-purple-600 focus:ring-purple-500 mb-2"
                      />
                      <div className="w-8 h-8 bg-purple-100 rounded-full flex items-center justify-center mb-1">
                        <svg
                          className="w-4 h-4 text-purple-600"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path
                            fillRule="evenodd"
                            d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z"
                            clipRule="evenodd"
                          />
                        </svg>
                      </div>
                      <div className="text-center">
                        <p className="font-medium text-gray-900 text-xs">
                          Backoffice
                        </p>
                        <p className="text-xs text-gray-500">Administrator</p>
                      </div>
                    </label>
                  </div>
                </div>

                {/* Station Assignment - Only for Operators */}
                {form.role === "Operator" && (
                  <div>
                    <label className="flex items-center text-sm font-medium text-gray-700 mb-1.5">
                      <svg
                        className="w-4 h-4 mr-2 text-gray-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z"
                          clipRule="evenodd"
                        />
                      </svg>
                      Assigned Station
                    </label>
                    <select
                      value={form.assignedStationId}
                      onChange={(e) =>
                        setForm({ ...form, assignedStationId: e.target.value })
                      }
                      className="w-full px-3 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                      required
                    >
                      <option value="">Select a station</option>
                      {stations.map((station) => {
                          const operatorCount = getOperatorsCountForStation(station.id);
                          return (
                            <option key={station.id} value={station.id}>
                              {station.name} - {station.location} ({operatorCount} operator{operatorCount !== 1 ? 's' : ''})
                            </option>
                          );
                        })}
                    </select>
                    {stations.length === 0 && (
                      <p className="text-xs text-red-500 mt-1">
                        No stations available in the system.
                      </p>
                    )}
                  </div>
                )}

                {/* Password Input */}
                <div>
                  <label className="flex items-center text-sm font-medium text-gray-700 mb-1.5">
                    <svg
                      className="w-4 h-4 mr-2 text-gray-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"
                        clipRule="evenodd"
                      />
                    </svg>
                    Password
                  </label>
                  <input
                    type="password"
                    placeholder="Enter secure password"
                    value={form.password}
                    onChange={(e) =>
                      setForm({ ...form, password: e.target.value })
                    }
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                    required
                  />
                </div>

                {/* Action Buttons */}
                <div className="flex space-x-3 pt-3">
                  <button
                    type="button"
                    onClick={() => setShowCreateForm(false)}
                    className="flex-1 px-4 py-2.5 bg-gray-200 text-gray-800 rounded-xl hover:bg-gray-300 transition-all duration-200 font-medium text-sm"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="flex-1 px-4 py-2.5 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl hover:from-green-700 hover:to-emerald-700 transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:scale-105 text-sm"
                  >
                    Create User
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div 
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 backdrop-blur-sm"
          onClick={handleBackdropClick}
        >
          <div className="bg-white rounded-2xl p-8 shadow-2xl transform transition-all duration-200 scale-100 max-w-md w-full mx-4">
            <div className="flex flex-col items-center space-y-6">
              <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center">
                <svg className="w-8 h-8 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z" clipRule="evenodd" />
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              </div>
              
              <div className="text-center">
                <h3 className="text-xl font-semibold text-gray-900 mb-2">Delete User</h3>
                <p className="text-gray-600">
                  Are you sure you want to delete user <span className="font-semibold text-gray-900">{userToDelete?.nic}</span>? 
                  This action cannot be undone.
                </p>
              </div>
              
              <div className="flex space-x-4 w-full">
                <button
                  onClick={cancelDelete}
                  className="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors duration-200 font-medium"
                >
                  Cancel
                </button>
                <button
                  onClick={confirmDelete}
                  className="flex-1 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors duration-200 font-medium"
                >
                  Yes, Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Status Change Confirmation Modal */}
      {showStatusConfirm && (
        <div 
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 backdrop-blur-sm"
          onClick={handleStatusBackdropClick}
        >
          <div className="bg-white rounded-2xl p-8 shadow-2xl transform transition-all duration-200 scale-100 max-w-md w-full mx-4">
            <div className="flex flex-col items-center space-y-6">
              <div className={`w-16 h-16 rounded-full flex items-center justify-center ${
                userToToggle?.isActive 
                  ? "bg-orange-100" 
                  : "bg-green-100"
              }`}>
                <svg className={`w-8 h-8 ${
                  userToToggle?.isActive 
                    ? "text-orange-600" 
                    : "text-green-600"
                }`} fill="currentColor" viewBox="0 0 20 20">
                  {userToToggle?.isActive ? (
                    // Pause/Deactivate icon
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM7 8a1 1 0 012 0v4a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v4a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd" />
                  ) : (
                    // Play/Activate icon
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clipRule="evenodd" />
                  )}
                </svg>
              </div>
              
              <div className="text-center">
                <h3 className="text-xl font-semibold text-gray-900 mb-2">
                  {userToToggle?.isActive ? "Deactivate User" : "Activate User"}
                </h3>
                <p className="text-gray-600">
                  Are you sure you want to {userToToggle?.isActive ? "deactivate" : "activate"} user{" "}
                  <span className="font-semibold text-gray-900">{userToToggle?.fullName} </span> 
                  ({userToToggle?.nic})?
                </p>
              </div>
              
              <div className="flex space-x-4 w-full">
                <button
                  onClick={cancelStatusToggle}
                  className="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors duration-200 font-medium"
                >
                  Cancel
                </button>
                <button
                  onClick={confirmStatusToggle}
                  className={`flex-1 px-6 py-3 text-white rounded-lg transition-colors duration-200 font-medium ${
                    userToToggle?.isActive
                      ? "bg-orange-600 hover:bg-orange-700"
                      : "bg-green-600 hover:bg-green-700"
                  }`}
                >
                  Yes, {userToToggle?.isActive ? "Deactivate" : "Activate"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Update Role Modal */}
      {showUpdateForm && (
        <div 
          className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4"
          onClick={handleUpdateBackdropClick}
        >
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md transform transition-all duration-300">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-green-600 to-emerald-600 p-4 rounded-t-2xl">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-white/20 rounded-full flex items-center justify-center">
                    <svg
                      className="w-4 h-4 text-white"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M6 6V5a3 3 0 013-3h2a3 3 0 013 3v1h2a2 2 0 012 2v3.57A22.952 22.952 0 0110 13a22.95 22.95 0 01-8-1.43V8a2 2 0 012-2h2zm2-1a1 1 0 011-1h2a1 1 0 011 1v1H8V5zm1 5a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-white">
                      Update User Role
                    </h2>
                    <p className="text-green-100 text-xs">
                      Change user's system role
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setShowUpdateForm(false)}
                  className="text-white/80 hover:text-white hover:bg-white/10 p-1.5 rounded-full transition-all duration-200"
                >
                  <svg
                    className="w-4 h-4"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                      clipRule="evenodd"
                    />
                  </svg>
                </button>
              </div>
            </div>

            {/* Modal Body */}
            <div className="p-6">
              <div className="space-y-4">
                {/* User Info */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-gray-700 mb-2">User Information</h3>
                  <p className="text-sm text-gray-900"><span className="font-medium">Name:</span> {userToUpdate?.fullName}</p>
                  <p className="text-sm text-gray-900"><span className="font-medium">NIC:</span> {userToUpdate?.nic}</p>
                  <p className="text-sm text-gray-900"><span className="font-medium">Current Role:</span> {userToUpdate?.currentRole}</p>
                  {userToUpdate?.currentRole === "Operator"  && (
                    <p className="text-sm text-gray-900"><span className="font-medium">Assigned Station:</span> {userToUpdate?.assignedStationName}</p>
                  )}
                </div>

                {/* Role Selection */}
                <div>
                  <label className="flex items-center text-sm font-medium text-gray-700 mb-3">
                    <svg
                      className="w-4 h-4 mr-2 text-gray-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M6 6V5a3 3 0 013-3h2a3 3 0 013 3v1h2a2 2 0 012 2v3.57A22.952 22.952 0 0110 13a22.95 22.95 0 01-8-1.43V8a2 2 0 012-2h2zm2-1a1 1 0 011-1h2a1 1 0 011 1v1H8V5zm1 5a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1z"
                        clipRule="evenodd"
                      />
                    </svg>
                    Select New Role
                  </label>
                  <div className="space-y-3">
                    <label className="flex items-center p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-all duration-200 hover:border-gray-300">
                      <input
                        type="radio"
                        name="newRole"
                        value="EVOwner"
                        checked={userToUpdate?.newRole === "EVOwner"}
                        onChange={(e) =>
                          setUserToUpdate({ ...userToUpdate, newRole: e.target.value, assignedStationId: "" })
                        }
                        className="w-4 h-4 text-green-600 focus:ring-green-500 mr-3"
                      />
                      <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center mr-3">
                        <svg
                          className="w-4 h-4 text-green-600"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path d="M8 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM15 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z" />
                          <path d="M3 4a1 1 0 00-1 1v10a1 1 0 001 1h1.05a2.5 2.5 0 014.9 0H10a1 1 0 001-1V5a1 1 0 00-1-1H3zM14 7a1 1 0 00-1 1v6.05A2.5 2.5 0 0115.95 16H17a1 1 0 001-1V8a1 1 0 00-1-1h-3z" />
                        </svg>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 text-sm">EV Owner</p>
                        <p className="text-xs text-gray-500">Vehicle owner with booking access</p>
                      </div>
                    </label>

                    <label className="flex items-center p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-all duration-200 hover:border-gray-300">
                      <input
                        type="radio"
                        name="newRole"
                        value="Operator"
                        checked={userToUpdate?.newRole === "Operator"}
                        onChange={(e) =>
                          setUserToUpdate({ ...userToUpdate, newRole: e.target.value, assignedStationId: "" })
                        }
                        className="w-4 h-4 text-blue-600 focus:ring-blue-500 mr-3"
                      />
                      <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center mr-3">
                        <svg
                          className="w-4 h-4 text-blue-600"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path
                            fillRule="evenodd"
                            d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
                            clipRule="evenodd"
                          />
                        </svg>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 text-sm">Station Operator</p>
                        <p className="text-xs text-gray-500">Manages charging stations</p>
                      </div>
                    </label>

                    <label className="flex items-center p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-all duration-200 hover:border-gray-300">
                      <input
                        type="radio"
                        name="newRole"
                        value="Backoffice"
                        checked={userToUpdate?.newRole === "Backoffice"}
                        onChange={(e) =>
                          setUserToUpdate({ ...userToUpdate, newRole: e.target.value, assignedStationId: "" })
                        }
                        className="w-4 h-4 text-purple-600 focus:ring-purple-500 mr-3"
                      />
                      <div className="w-8 h-8 bg-purple-100 rounded-full flex items-center justify-center mr-3">
                        <svg
                          className="w-4 h-4 text-purple-600"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path
                            fillRule="evenodd"
                            d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z"
                            clipRule="evenodd"
                          />
                        </svg>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 text-sm">Backoffice</p>
                        <p className="text-xs text-gray-500">Administrative access</p>
                      </div>
                    </label>
                  </div>
                </div>

                {/* Station Assignment - Only for new Operator role */}
                {userToUpdate?.newRole === "Operator" && (
                  <div>
                    <label className="flex items-center text-sm font-medium text-gray-700 mb-1.5">
                      <svg
                        className="w-4 h-4 mr-2 text-gray-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z"
                          clipRule="evenodd"
                        />
                      </svg>
                      Assigned Station
                    </label>
                    <select
                      value={userToUpdate?.assignedStationId || ""}
                      onChange={(e) =>
                        setUserToUpdate({ ...userToUpdate, assignedStationId: e.target.value })
                      }
                      className="w-full px-3 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                      required
                    >
                      <option value="">Select a station</option>
                      {stations.map((station) => {
                          const operatorCount = getOperatorsCountForStation(station.id);
                          return (
                            <option key={station.id} value={station.id}>
                              {station.name} - {station.location} ({operatorCount} operator{operatorCount !== 1 ? 's' : ''})
                            </option>
                          );
                        })}
                    </select>
                    {stations.length === 0 && (
                      <p className="text-xs text-red-500 mt-1">
                        No stations available in the system.
                      </p>
                    )}
                  </div>
                )}

                {/* Action Buttons */}
                <div className="flex space-x-3 pt-4">
                  <button
                    onClick={cancelRoleUpdate}
                    className="flex-1 px-4 py-2.5 bg-gray-200 text-gray-800 rounded-xl hover:bg-gray-300 transition-all duration-200 font-medium text-sm"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={confirmRoleUpdate}
                    disabled={
                      userToUpdate?.newRole === userToUpdate?.currentRole && 
                      (userToUpdate?.currentRole !== "Operator" || 
                       userToUpdate?.assignedStationId === (users.find(u => u.nic === userToUpdate?.nic)?.assignedStationId || ""))
                    }
                    className={`flex-1 px-4 py-2.5 rounded-xl transition-all duration-200 font-medium text-sm ${
                      userToUpdate?.newRole === userToUpdate?.currentRole && 
                      (userToUpdate?.currentRole !== "Operator" || 
                       userToUpdate?.assignedStationId === (users.find(u => u.nic === userToUpdate?.nic)?.assignedStationId || ""))
                        ? "bg-gray-300 text-gray-500 cursor-not-allowed"
                        : "bg-gradient-to-r from-green-600 to-emerald-600 text-white hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl transform hover:scale-105"
                    }`}
                  >
                    {userToUpdate?.newRole === userToUpdate?.currentRole && userToUpdate?.currentRole === "Operator" 
                      ? "Update Station" 
                      : "Update Role"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Users Table - Desktop View */}
      <div className="bg-white rounded-lg shadow overflow-hidden hidden md:block">
        <div className="px-4 py-2 bg-blue-50 border-b border-blue-100 lg:hidden">
          <p className="text-xs text-blue-700 flex items-center">
            <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 111.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
            </svg>
            Scroll horizontally to see all columns
          </p>
        </div>
        {/* Responsive table wrapper */}
        <div className="overflow-x-auto scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100">
          <table className="w-full min-w-[1000px]">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
                #
              </th>
              <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider min-w-[120px]">
                NIC
              </th>
              <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider min-w-[150px]">
                Full Name
              </th>
              <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider min-w-[200px]">
                <div className="flex flex-col space-y-2">
                  <span>Role</span>
                  <select
                    value={roleFilter}
                    onChange={(e) => handleRoleFilterChange(e.target.value)}
                    className="px-2 py-1 border border-gray-300 rounded text-xs bg-white focus:ring-2 focus:ring-green-500 focus:border-transparent w-full"
                  >
                    <option value="All">All ({users.length})</option>
                    <option value="Backoffice">
                      Backoffice ({users.filter(u => u.role === "Backoffice").length})
                    </option>
                    <option value="Operator">
                      Operator ({users.filter(u => u.role === "Operator").length})
                    </option>
                    <option value="EVOwner">
                      EV Owner ({users.filter(u => u.role === "EVOwner").length})
                    </option>
                  </select>
                </div>
              </th>
              <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider min-w-[180px]">
                Assigned Station
              </th>
              <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider min-w-[80px]">
                Status
              </th>
              <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider min-w-[250px]">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {currentUsers.map((user, index) => (
              <tr key={user.nic}>
                <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-500 font-medium">
                  {startIndex + index + 1}
                </td>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 break-all">
                  {user.nic}
                </td>
                <td className="px-3 py-4 text-sm text-gray-900">
                  <div className="max-w-[150px] truncate" title={user.fullName}>
                    {user.fullName}
                  </div>
                </td>
                <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                  <span
                    className={`px-2 py-1 rounded-full text-xs font-medium ${
                      user.role === "Backoffice"
                        ? "bg-purple-100 text-purple-800"
                        : user.role === "Operator"
                        ? "bg-blue-100 text-blue-800"
                        : user.role === "EVOwner"
                        ? "bg-green-100 text-green-800"
                        : "bg-gray-100 text-gray-800"
                    }`}
                  >
                    {user.role}
                  </span>
                </td>
                <td className="px-3 py-4 text-sm text-gray-900">
                  {user.role === "Operator" && user.assignedStationName ? (
                    <div className="flex items-center">
                      <svg
                        className="w-4 h-4 mr-1 text-blue-500 flex-shrink-0"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <span className="text-blue-600 font-medium truncate" title={user.assignedStationName}>
                        {user.assignedStationName}
                      </span>
                    </div>
                  ) : user.role === "Operator" ? (
                    <div className="flex items-center">
                      <svg
                        className="w-4 h-4 mr-1 text-red-500 flex-shrink-0"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <span className="text-red-600 font-medium text-xs">Not assigned</span>
                    </div>
                  ) : (
                    <span className="text-gray-400 text-xs">N/A</span>
                  )}
                </td>
                <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                  {user.isActive ? (
                    <span className="px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Active
                    </span>
                  ) : (
                    <span className="px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
                      Inactive
                    </span>
                  )}
                </td>
                <td className="px-3 py-4 text-sm font-medium">
                  <div className="flex flex-wrap gap-1">
                    <button
                      onClick={() => toggleUserStatus(user.nic, user.isActive, user.fullName)}
                      className={`px-2 py-1 rounded text-white text-xs ${
                        user.isActive
                          ? "bg-orange-600 hover:bg-orange-700"
                          : "bg-green-600 hover:bg-green-700"
                      }`}
                    >
                      {user.isActive ? "Deactivate" : "Activate"}
                    </button>
                    <button
                      onClick={() => updateUserRole(user)}
                      className="px-2 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-xs"
                    >
                      Update
                    </button>
                    <button
                      onClick={() => deleteUser(user.nic)}
                      className="px-2 py-1 bg-red-600 text-white rounded hover:bg-red-700 text-xs"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>

        {filteredUsers.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            {roleFilter === "All" 
              ? "No system users found. Create your first user above."
              : `No users found with role "${roleFilter}". Try selecting a different role filter.`
            }
          </div>
        )}
      </div>

      {/* Mobile Card View */}
      <div className="md:hidden space-y-4">
        {currentUsers.map((user, index) => (
          <div key={user.nic} className="bg-white rounded-lg shadow p-4">
            {/* User Number */}
            <div className="flex justify-between items-start mb-3">
              <span className="text-sm font-medium text-gray-500">#{startIndex + index + 1}</span>
              <div className="flex flex-wrap gap-1">
                <button
                  onClick={() => toggleUserStatus(user.nic, user.isActive, user.fullName)}
                  className={`px-2 py-1 rounded text-white text-xs ${
                    user.isActive
                      ? "bg-orange-600 hover:bg-orange-700"
                      : "bg-green-600 hover:bg-green-700"
                  }`}
                >
                  {user.isActive ? "Deactivate" : "Activate"}
                </button>
                <button
                  onClick={() => updateUserRole(user)}
                  className="px-2 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-xs"
                >
                  Update
                </button>
                <button
                  onClick={() => deleteUser(user.nic)}
                  className="px-2 py-1 bg-red-600 text-white rounded hover:bg-red-700 text-xs"
                >
                  Delete
                </button>
              </div>
            </div>

            {/* User Details */}
            <div className="space-y-2">
              <div>
                <p className="text-sm font-medium text-gray-900">{user.fullName}</p>
                <p className="text-xs text-gray-500">NIC: {user.nic}</p>
              </div>
              
              <div className="flex items-center justify-between">
                <span
                  className={`px-2 py-1 rounded-full text-xs font-medium ${
                    user.role === "Backoffice"
                      ? "bg-purple-100 text-purple-800"
                      : user.role === "Operator"
                      ? "bg-blue-100 text-blue-800"
                      : user.role === "EVOwner"
                      ? "bg-green-100 text-green-800"
                      : "bg-gray-100 text-gray-800"
                  }`}
                >
                  {user.role}
                </span>
                
                {user.isActive ? (
                  <span className="px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                    Active
                  </span>
                ) : (
                  <span className="px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
                    Inactive
                  </span>
                )}
              </div>

              {/* Station Assignment */}
              {user.role === "Operator" && (
                <div className="pt-2 border-t border-gray-100">
                  <p className="text-xs text-gray-500 mb-1">Assigned Station:</p>
                  {user.assignedStationName ? (
                    <div className="flex items-center">
                      <svg
                        className="w-4 h-4 mr-1 text-blue-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <span className="text-blue-600 font-medium text-sm">{user.assignedStationName}</span>
                    </div>
                  ) : (
                    <div className="flex items-center">
                      <svg
                        className="w-4 h-4 mr-1 text-red-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <span className="text-red-600 font-medium text-sm">Not assigned</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}

        {filteredUsers.length === 0 && (
          <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
            {roleFilter === "All" 
              ? "No system users found. Create your first user above."
              : `No users found with role "${roleFilter}". Try selecting a different role filter.`
            }
          </div>
        )}
      </div>
      
      {/* Pagination Component */}
      {filteredUsers.length > 0 && (
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={handlePageChange}
          itemsPerPage={itemsPerPage}
          totalItems={filteredUsers.length}
        />
      )}
    </div>
  );
}
