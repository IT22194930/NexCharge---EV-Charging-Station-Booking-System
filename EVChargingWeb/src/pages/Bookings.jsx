import { useEffect, useState, useCallback } from "react";
import api from "../api/axios";
import toast from "react-hot-toast";
import CreateBookingModal from "../components/bookings/CreateBookingModal";
import UpdateBookingModal from "../components/bookings/UpdateBookingModal";
import BookingTable from "../components/bookings/BookingTable";
import DeleteConfirmDialog from "../components/bookings/DeleteConfirmDialog";
import Pagination from "../components/Pagination";
import { extractDateOnly } from "../utils/dateUtils";

export default function Bookings() {
  const [bookings, setBookings] = useState([]);
  const [filteredBookings, setFilteredBookings] = useState([]);
  const [stations, setStations] = useState([]);
  const [error, setError] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showUpdateForm, setShowUpdateForm] = useState(false);
  const [currentBooking, setCurrentBooking] = useState(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [bookingToDelete, setBookingToDelete] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10);
  const [statusFilter, setStatusFilter] = useState("All");
  const [form, setForm] = useState({
    ownerNic: "",
    stationId: "",
    reservationDate: "",
    reservationHour: 0,
  });
  const role = localStorage.getItem("role");

  // Get current user NIC from token
  const getCurrentUserNic = () => {
    try {
      if (role === "EVOwner") {
        return JSON.parse(atob(localStorage.getItem("token").split(".")[1]))[
          "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"
        ];
      }
    } catch (err) {
      console.error("Error extracting user NIC:", err);
    }
    return null;
  };

  const userNic = getCurrentUserNic();

  const loadBookings = useCallback(async () => {
    try {
      if (role === "EVOwner") {
        const nic = JSON.parse(
          atob(localStorage.getItem("token").split(".")[1])
        )["http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"];
        const res = await api.get(`/bookings/owner/${nic}`);
        setBookings(res.data);
        filterBookings(res.data, statusFilter);
      } else {
        const res = await api.get("/bookings");
        setBookings(res.data);
        filterBookings(res.data, statusFilter);
      }
    } catch (err) {
      setError("Failed to load bookings");
      console.error(err);
    }
  }, [role, statusFilter]);

  const filterBookings = (allBookings, filter) => {
    if (filter === "All") {
      setFilteredBookings(allBookings);
    } else {
      const filtered = allBookings.filter(
        (booking) => booking.status === filter
      );
      setFilteredBookings(filtered);
    }
  };

  const handleStatusFilterChange = (filter) => {
    setStatusFilter(filter);
    setCurrentPage(1); // Reset to first page when filtering
    filterBookings(bookings, filter);
  };

  // Pagination logic
  const totalPages = Math.ceil(filteredBookings.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentBookings = filteredBookings.slice(startIndex, endIndex);

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  const getUserNic = () => {
    if (role === "EVOwner") {
      return JSON.parse(atob(localStorage.getItem("token").split(".")[1]))[
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"
      ];
    }
    return "";
  };

  const loadStations = async () => {
    try {
      const res = await api.get("/stations");
      setStations(res.data.filter((s) => s.isActive));
    } catch (err) {
      console.error("Failed to load stations", err);
    }
  };

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

  const approveBooking = async (id) => {
    try {
      await api.post(`/bookings/approve/${id}`);
      loadBookings();
      toast.success("Booking approved successfully!");
    } catch (err) {
      toast.error(
        "Error approving booking: " +
          (err.response?.data?.message || err.message)
      );
    }
  };

  const completeBooking = async (id) => {
    try {
      await api.post(`/bookings/complete/${id}`);
      loadBookings();
      toast.success("Booking marked as completed!");
    } catch (err) {
      toast.error(
        "Error completing booking: " +
          (err.response?.data?.message || err.message)
      );
    }
  };

  const cancelBooking = async (id) => {
    try {
      await api.post(`/bookings/cancel/${id}`);
      loadBookings();
      toast.success("Booking cancelled successfully!");
    } catch (err) {
      toast.error(
        "Error cancelling booking: " +
          (err.response?.data?.message || err.message)
      );
    }
  };

  const deleteBooking = async (id) => {
    setBookingToDelete(id);
    setShowDeleteConfirm(true);
  };

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

  const cancelDelete = () => {
    setShowDeleteConfirm(false);
    setBookingToDelete(null);
  };

  const handleDeleteBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      setShowDeleteConfirm(false);
      setBookingToDelete(null);
    }
  };

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

  const resetForm = () => {
    setForm({
      ownerNic: "",
      stationId: "",
      reservationDate: "",
      reservationHour: 0,
    });
  };

  const closeModals = () => {
    setShowCreateForm(false);
    setShowUpdateForm(false);
    setCurrentBooking(null);
    resetForm();
  };

  const canModifyBooking = (booking) => {
    const reservationDate = new Date(booking.reservationDate);
    const now = new Date();
    const timeDifference = reservationDate.getTime() - now.getTime();
    const hoursUntilReservation = timeDifference / (1000 * 60 * 60);
    return hoursUntilReservation >= 12;
  };

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

  useEffect(() => {
    loadBookings();
    loadStations();
  }, [loadBookings]);

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Booking Management
          </h1>
          <p className="text-gray-600 text-sm mt-1">
            Manage charging station reservations
          </p>
        </div>
        {(role === "Backoffice" || role === "EVOwner") && (
          <button
            onClick={() => {
              // Auto-populate Owner NIC for EVOwners
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

      {error && <p className="text-red-500 mb-4">{error}</p>}

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
        statusFilter={statusFilter}
        handleStatusFilterChange={handleStatusFilterChange}
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

      <DeleteConfirmDialog
        visible={showDeleteConfirm}
        cancelDelete={cancelDelete}
        confirmDelete={confirmDelete}
        handleDeleteBackdropClick={handleDeleteBackdropClick}
      />
    </div>
  );
}
