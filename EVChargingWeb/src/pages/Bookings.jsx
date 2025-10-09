import { useEffect, useState, useCallback } from "react";
import api from "../api/axios";
import toast from "react-hot-toast";
import CreateBookingModal from "../components/bookings/CreateBookingModal";
import UpdateBookingModal from "../components/bookings/UpdateBookingModal";
import BookingTable from "../components/bookings/BookingTable";
import DeleteConfirmDialog from "../components/bookings/DeleteConfirmDialog";
import Pagination from "../components/Pagination";
import ConfirmActionDialog from "../components/bookings/ConfirmActionDialog";
import { extractDateOnly } from "../utils/dateUtils";

export default function Bookings() {
  // Core datasets and filtered view
  const [bookings, setBookings] = useState([]);
  const [filteredBookings, setFilteredBookings] = useState([]);
  const [stations, setStations] = useState([]);

  // UI and error states
  const [error, setError] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showUpdateForm, setShowUpdateForm] = useState(false);
  const [currentBooking, setCurrentBooking] = useState(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [bookingToDelete, setBookingToDelete] = useState(null);

  // Pagination and filters
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10);
  const [statusFilter, setStatusFilter] = useState("All");
  const [stationFilter, setStationFilter] = useState("All"); // backoffice station filter

  // Action confirmation dialog state (approve/complete/cancel)
  const [actionConfirm, setActionConfirm] = useState({ visible: false });

  // Operator-only NIC search query
  const [searchOwnerNic, setSearchOwnerNic] = useState(""); // operator NIC search

  // Form model used by create/update modals
  const [form, setForm] = useState({
    ownerNic: "",
    stationId: "",
    reservationDate: "",
    reservationHour: 0,
  });

  // Logged-in user context (used to derive operator's assigned station)
  const [currentUser, setCurrentUser] = useState(null);

  // Role read from localStorage (EVOwner | Operator | Backoffice)
  const role = localStorage.getItem("role");

  // Get current user NIC from token (supports EVOwner and Operator tokens)
  const getCurrentUserNic = () => {
    try {
      if (role === "EVOwner" || role === "Operator") {
        return JSON.parse(atob(localStorage.getItem("token").split(".")[1]))[
          "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"
        ];
      }
    } catch (err) {
      console.error("Error extracting user NIC:", err);
    }
    return null;
  };

  // Cached NIC (used for EVOwner scoping and operators where needed)
  const userNic = getCurrentUserNic();

  // Load current operator profile to access assignedStationId
  const loadCurrentUser = useCallback(async () => {
    if (role === "Operator" && userNic) {
      try {
        const res = await api.get("/auth/profile");
        setCurrentUser(res.data);
      } catch (err) {
        console.error("Failed to load current user data:", err);
      }
    }
  }, [role, userNic]);

  // Fetch bookings list depending on role and context; sort newest-first
  const loadBookings = useCallback(async () => {
    try {
      if (role === "EVOwner") {
        const nic = JSON.parse(
          atob(localStorage.getItem("token").split(".")[1])
        )["http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"];
        const res = await api.get(`/bookings/owner/${nic}`);
        // Newest-first: prefer createdAt, fallback to reservationDate
        const sorted = [...res.data].sort((a,b) => new Date(b.createdAt || b.reservationDate) - new Date(a.createdAt || a.reservationDate));
        setBookings(sorted);
      } else if (role === "Operator" && currentUser?.assignedStationId) {
        // Operator: filter bookings to assigned station only
        const res = await api.get("/bookings");
        const stationBookings = res.data.filter(booking => booking.stationId === currentUser.assignedStationId);
        const sorted = [...stationBookings].sort((a,b) => new Date(b.createdAt || b.reservationDate) - new Date(a.createdAt || a.reservationDate));
        setBookings(sorted);
      } else if (role === "Backoffice") {
        // Backoffice: view all bookings
        const res = await api.get("/bookings");
        const sorted = [...res.data].sort((a,b) => new Date(b.createdAt || b.reservationDate) - new Date(a.createdAt || a.reservationDate));
        setBookings(sorted);
      } else {
        // Fallback: no accessible bookings for current context
        setBookings([]);
      }
    } catch (err) {
      setError("Failed to load bookings");
      console.error(err);
    }
  }, [role, currentUser?.assignedStationId]);

  // Apply status filter, operator NIC search, and (for backoffice) station filter
  const filterBookings = useCallback((allBookings, statusFilter, stationFilter) => {
    let working = allBookings;
    
    // Apply operator NIC search if applicable
    if (role === "Operator" && searchOwnerNic.trim()) {
      const q = searchOwnerNic.trim().toLowerCase();
      working = working.filter(b => (b.ownerNIC || "").toLowerCase().includes(q));
    }
    
    // Apply status filter
    if (statusFilter !== "All") {
      working = working.filter(b => b.status === statusFilter);
    }
    
    // Apply station filter for Backoffice users
    if (role === "Backoffice" && stationFilter !== "All") {
      working = working.filter(b => b.stationId === stationFilter);
    }
    
    setFilteredBookings(working);
  }, [role, searchOwnerNic]);

  // Handle status filter change (reset to first page for UX)
  const handleStatusFilterChange = (filter) => {
    setStatusFilter(filter);
    setCurrentPage(1); // Reset to first page when filtering
    filterBookings(bookings, filter, stationFilter);
  };

  // Handle station filter change for Backoffice (reset page 1)
  const handleStationFilterChange = (stationId) => {
    setStationFilter(stationId);
    setCurrentPage(1); // Reset to first page when filtering
    filterBookings(bookings, statusFilter, stationId);
  };

  // Pagination logic
  const totalPages = Math.ceil(filteredBookings.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentBookings = filteredBookings.slice(startIndex, endIndex);

  // Invoked by Pagination component
  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  // Helper to pull NIC from token for EVOwner when initializing create form
  const getUserNic = () => {
    if (role === "EVOwner") {
      return JSON.parse(atob(localStorage.getItem("token").split(".")[1]))[
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"
      ];
    }
    return "";
  };

  // Load and cache active stations for select lists and name resolution
  const loadStations = async () => {
    try {
      const res = await api.get("/stations");
      setStations(res.data.filter((s) => s.isActive));
    } catch (err) {
      console.error("Failed to load stations", err);
    }
  };

  // Create booking with client-side validation (future & within 7 days)
  const createBooking = async (e) => {
    e.preventDefault();

    // Validate date is within 7 days and in the future
    const reservationDate = new Date(form.reservationDate);
    const now = new Date();
    const sevenDaysFromNow = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);

    if (reservationDate <= now) {
      toast.error("Reservation date must be in the future");
      return;
    }

    if (reservationDate > sevenDaysFromNow) {
      toast.error("Reservation date must be within 7 days from now");
      return;
    }

    try {
      await api.post("/bookings", form);
      resetForm();
      setShowCreateForm(false);
      loadBookings();
      toast.success("Booking created successfully!");
    } catch (err) {
      toast.error(
        "Error creating booking: " +
          (err.response?.data?.message || err.message)
      );
    }
  };

  // Update booking with 12-hour window and 7-day bounds enforced client-side
  const updateBooking = async (e) => {
    e.preventDefault();

    // Validate 12-hour constraint
    const reservationDate = new Date(form.reservationDate);
    const now = new Date();
    const timeDifference = reservationDate.getTime() - now.getTime();
    const hoursUntilReservation = timeDifference / (1000 * 60 * 60);

    if (hoursUntilReservation < 12) {
      toast.error(
        "Cannot modify booking less than 12 hours before reservation"
      );
      return;
    }

    // Validate date is within 7 days and in the future
    const sevenDaysFromNow = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);

    if (reservationDate <= now) {
      toast.error("Reservation date must be in the future");
      return;
    }

    if (reservationDate > sevenDaysFromNow) {
      toast.error("Reservation date must be within 7 days from now");
      return;
    }

    try {
      await api.put(`/bookings/${currentBooking.id}`, {
        reservationDate: form.reservationDate,
        reservationHour: form.reservationHour,
        stationId: form.stationId,
      });
      resetForm();
      setShowUpdateForm(false);
      setCurrentBooking(null);
      loadBookings();
      toast.success("Booking updated successfully!");
    } catch (err) {
      toast.error(
        "Error updating booking: " +
          (err.response?.data?.message || err.message)
      );
    }
  };

  // Approve booking: confirms via modal, then calls API and reloads
  const approveBooking = async (id) => {
    setActionConfirm({
      visible: true,
      tone: "green",
      title: "Approve Booking",
      message: "Are you sure you want to approve this booking?",
      confirmLabel: "Approve",
      onConfirm: async () => {
        try {
          await api.post(`/bookings/approve/${id}`);
          toast.success("Booking approved successfully!");
          loadBookings();
        } catch (err) {
          toast.error(
            "Error approving booking: " +
              (err.response?.data?.message || err.message)
          );
        } finally {
          setActionConfirm({ visible: false });
        }
      },
      onCancel: () => setActionConfirm({ visible: false })
    });
  };

  // Complete booking: confirms via modal, then calls API and reloads
  const completeBooking = async (id) => {
    setActionConfirm({
      visible: true,
      tone: "emerald",
      title: "Complete Charging Session",
      message: "Mark this approved booking as completed session?",
      confirmLabel: "Complete",
      onConfirm: async () => {
        try {
          await api.post(`/bookings/complete/${id}`);
          toast.success("Booking marked as completed!");
          loadBookings();
        } catch (err) {
          toast.error(
            "Error completing booking: " +
              (err.response?.data?.message || err.message)
          );
        } finally {
          setActionConfirm({ visible: false });
        }
      },
      onCancel: () => setActionConfirm({ visible: false })
    });
  };

  // Cancel booking: confirms via modal, then calls API and reloads
  const cancelBooking = async (id) => {
    setActionConfirm({
      visible: true,
      tone: "orange",
      title: "Cancel Booking",
      message: "Are you sure you want to cancel this booking?",
      confirmLabel: "Cancel Booking",
      onConfirm: async () => {
        try {
          await api.post(`/bookings/cancel/${id}`);
          toast.success("Booking cancelled successfully!");
          loadBookings();
        } catch (err) {
          toast.error(
            "Error cancelling booking: " +
              (err.response?.data?.message || err.message)
          );
        } finally {
          setActionConfirm({ visible: false });
        }
      },
      onCancel: () => setActionConfirm({ visible: false })
    });
  };

  // Open delete confirm dialog (actual deletion in confirmDelete)
  const deleteBooking = async (id) => {
    setBookingToDelete(id);
    setShowDeleteConfirm(true);
  };

  // Execute deletion after confirmation
  const confirmDelete = async () => {
    try {
      await api.delete(`/bookings/${bookingToDelete}`);
      loadBookings();
      toast.success("Booking deleted successfully!");
    } catch (err) {
      toast.error(
        "Error deleting booking: " +
          (err.response?.data?.message || err.message)
      );
    }
    setShowDeleteConfirm(false);
    setBookingToDelete(null);
  };

  // Close delete dialog without action
  const cancelDelete = () => {
    setShowDeleteConfirm(false);
    setBookingToDelete(null);
  };

  // Allow backdrop click to close delete dialog
  const handleDeleteBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      setShowDeleteConfirm(false);
      setBookingToDelete(null);
    }
  };

  // Prefill and open update modal for the selected booking
  const openUpdateForm = (booking) => {
    setCurrentBooking(booking);

    // Extract just the date part for the date input
    const dateOnly = extractDateOnly(booking.reservationDate);

    setForm({
      ownerNic: booking.ownerNIC,
      stationId: booking.stationId,
      reservationDate: dateOnly,
      reservationHour: booking.reservationHour || 0,
    });
    setShowUpdateForm(true);
  };

  // Reset form model to defaults
  const resetForm = () => {
    setForm({
      ownerNic: "",
      stationId: "",
      reservationDate: "",
      reservationHour: 0,
    });
  };

  // Close any open modals and clear current booking/form
  const closeModals = () => {
    setShowCreateForm(false);
    setShowUpdateForm(false);
    setCurrentBooking(null);
    resetForm();
  };

  // Determine whether update/cancel is allowed based on 12-hour threshold
  const canModifyBooking = (booking) => {
    const reservationDate = new Date(booking.reservationDate);
    const now = new Date();
    const timeDifference = reservationDate.getTime() - now.getTime();
    const hoursUntilReservation = timeDifference / (1000 * 60 * 60);
    return hoursUntilReservation >= 12;
  };

  // Helper to map stationId to a friendly label with location
  const getStationName = (stationId) => {
    const station = stations.find((s) => s.id === stationId);
    return station ? `${station.name} (${station.location})` : stationId;
  };

  // Set minimum and maximum dates for date input
  const getMinDate = () => {
    const today = new Date();
    return today.toISOString().split("T")[0]; // Today's date
  };

  const getMaxDate = () => {
    const today = new Date();
    const sevenDaysFromNow = new Date(
      today.getTime() + 7 * 24 * 60 * 60 * 1000
    );
    return sevenDaysFromNow.toISOString().split("T")[0]; // 7 days from today
  };

  // Initial load: for operators, ensure profile is loaded then fetch data; otherwise load immediately
  useEffect(() => {
    if (role === "Operator") {
      loadCurrentUser().then(() => {
        loadBookings();
        loadStations();
      });
    } else {
      loadBookings();
      loadStations();
    }
  }, [loadBookings, loadCurrentUser, role]);

  // Centralized filtering effect so search persists after state-changing reloads
  useEffect(() => {
    filterBookings(bookings, statusFilter, stationFilter);
   
  }, [bookings, statusFilter, stationFilter, searchOwnerNic, role, filterBookings]);

  return (
    <div>
      {/* Header and action toolbar */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Booking Management
          </h1>
          <p className="text-gray-600 text-sm mt-1">
            Manage charging station reservations
          </p>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-end w-full lg:w-auto">
          {/* Status Filter */}
          <div className="relative">
            <select
              value={statusFilter}
              onChange={(e) => handleStatusFilterChange(e.target.value)}
              className="pl-3 pr-8 py-2 w-full sm:w-48 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
            >
              <option value="All">
                All Status ({bookings?.length || 0})
              </option>
              <option value="Pending">
                Pending (
                {bookings.filter((b) => b.status === "Pending").length})
              </option>
              <option value="Approved">
                Approved (
                {bookings.filter((b) => b.status === "Approved").length})
              </option>
              <option value="Completed">
                Completed (
                {bookings.filter((b) => b.status === "Completed").length})
              </option>
              <option value="Cancelled">
                Cancelled (
                {bookings.filter((b) => b.status === "Cancelled").length})
              </option>
            </select>
          </div>

          {role === "Backoffice" && (
            <div className="relative">
              {/* Station filter (Backoffice only) */}
              <select
                value={stationFilter}
                onChange={(e) => handleStationFilterChange(e.target.value)}
                className="pl-3 pr-8 py-2 w-full sm:w-64 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
              >
                <option value="All">All Stations</option>
                {stations.map((station) => (
                  <option key={station.id} value={station.id}>
                    {station.name}
                  </option>
                ))}
              </select>
            </div>
          )}
          {role === "Operator" && (
            <div className="relative">
              {/* Operator NIC search input */}
              <input
                type="text"
                value={searchOwnerNic}
                onChange={(e) => setSearchOwnerNic(e.target.value)}
                placeholder="Search by Owner NIC"
                className="pl-8 pr-3 py-2 w-full sm:w-64 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <svg className="w-4 h-4 text-gray-400 absolute left-2.5 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-4.35-4.35M9.5 17a7.5 7.5 0 100-15 7.5 7.5 0 000 15z" />
              </svg>
            </div>
          )}
          {(role === "Backoffice" || role === "EVOwner") && (
            <button
              onClick={() => {
                if (role === "EVOwner") {
                  const userNic = getUserNic();
                  setForm({
                    ownerNic: userNic,
                    stationId: "",
                    reservationDate: "",
                  });
                } else {
                  resetForm();
                }
                setShowCreateForm(true);
              }}
              className="bg-gradient-to-r from-green-600 to-emerald-600 text-white px-6 py-3 rounded-xl hover:from-green-700 hover:to-emerald-700 transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:scale-105 flex items-center space-x-2"
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"
                  clipRule="evenodd"
                />
              </svg>
              <span>Create New Booking</span>
            </button>
          )}
        </div>
      </div>

      {error && <p className="text-red-500 mb-4">{error}</p>}

      {/* Create booking modal (reuses shared form model) */}
      <CreateBookingModal
        visible={showCreateForm}
        closeModals={closeModals}
        role={role}
        form={form}
        setForm={setForm}
        stations={stations}
        createBooking={createBooking}
        getMinDate={getMinDate}
        getMaxDate={getMaxDate}
      />

      {/* Update booking modal for selected item */}
      <UpdateBookingModal
        visible={showUpdateForm}
        closeModals={closeModals}
        form={form}
        setForm={setForm}
        stations={stations}
        updateBooking={updateBooking}
        currentBooking={currentBooking}
        getStationName={getStationName}
        getMinDate={getMinDate}
        getMaxDate={getMaxDate}
      />

      {/* Main bookings table; receives current page slice and handlers */}
      <BookingTable
        bookings={currentBookings}
        allBookings={bookings}
        role={role}
        userNic={userNic}
        getStationName={getStationName}
        approveBooking={approveBooking}
        completeBooking={completeBooking}
        openUpdateForm={openUpdateForm}
        canModifyBooking={canModifyBooking}
        cancelBooking={cancelBooking}
        deleteBooking={deleteBooking}
        startIndex={startIndex}
        stationFilter={stationFilter}
        handleStationFilterChange={handleStationFilterChange}
        stations={stations}
      />

      {/* Pagination Component */}
      {filteredBookings.length > 0 && (
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={handlePageChange}
          itemsPerPage={itemsPerPage}
          totalItems={filteredBookings.length}
        />
      )}

      {/* Delete confirmation dialog */}
      <DeleteConfirmDialog
        visible={showDeleteConfirm}
        cancelDelete={cancelDelete}
        confirmDelete={confirmDelete}
        handleDeleteBackdropClick={handleDeleteBackdropClick}
      />

      {/* Generic action confirmation dialog for approve/complete/cancel */}
      <ConfirmActionDialog
        visible={actionConfirm.visible}
        tone={actionConfirm.tone}
        title={actionConfirm.title}
        message={actionConfirm.message}
        confirmLabel={actionConfirm.confirmLabel}
        onConfirm={actionConfirm.onConfirm}
        onCancel={actionConfirm.onCancel}
      />
    </div>
  );
}
