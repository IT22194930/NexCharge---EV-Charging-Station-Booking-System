import { useEffect, useState } from "react";
import api from "../api/axios";
import toast from "react-hot-toast";

export default function Bookings() {
  const [bookings, setBookings] = useState([]);
  const [stations, setStations] = useState([]);
  const [error, setError] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [bookingToDelete, setBookingToDelete] = useState(null);
  const [form, setForm] = useState({
    ownerNic: "",
    stationId: "",
    reservationDate: "",
  });
  const role = localStorage.getItem("role");
  
  // Get current user NIC from token
  const getCurrentUserNic = () => {
    try {
      if (role === "EVOwner") {
        return JSON.parse(
          atob(localStorage.getItem("token").split(".")[1])
        )["http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"];
      }
    } catch (err) {
      console.error("Error extracting user NIC:", err);
    }
    return null;
  };
  
  const userNic = getCurrentUserNic();

  const loadBookings = async () => {
    try {
      if (role === "EVOwner") {
        const nic = JSON.parse(
          atob(localStorage.getItem("token").split(".")[1])
        )["http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"];
        const res = await api.get(`/bookings/owner/${nic}`);
        setBookings(res.data);
      } else {
        const res = await api.get("/bookings");
        setBookings(res.data);
      }
    } catch (err) {
      setError("Failed to load bookings");
      console.error(err);
    }
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
    
    // Format date properly for datetime-local input
    const bookingDate = new Date(booking.reservationDate);
    // Adjust for timezone to prevent shifting
    const timezoneOffset = bookingDate.getTimezoneOffset() * 60000;
    const localTime = new Date(bookingDate.getTime() - timezoneOffset);
    const formattedDate = localTime.toISOString().slice(0, 16);
    
    setForm({
      ownerNic: booking.ownerNIC,
      stationId: booking.stationId,
      reservationDate: formattedDate,
    });
    setShowUpdateForm(true);
  };

  const resetForm = () => {
    setForm({
      ownerNic: "",
      stationId: "",
      reservationDate: "",
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
    const now = new Date();
    now.setHours(now.getHours() + 1); // At least 1 hour from now
    return now.toISOString().slice(0, 16);
  };

  const getMaxDate = () => {
    const now = new Date();
    const sevenDaysFromNow = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    return sevenDaysFromNow.toISOString().slice(0, 16);
  };

  useEffect(() => {
    loadBookings();
    loadStations();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

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

      {/* Create Booking Modal */}
      {showCreateForm && (
        <div
          className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4"
          onClick={(e) => e.target === e.currentTarget && closeModals()}
        >
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md transform transition-all duration-300 max-h-[95vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-green-600 to-emerald-600 p-6 rounded-t-2xl">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                    <svg
                      className="w-5 h-5 text-white"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-white">
                      Create New Booking
                    </h2>
                    <p className="text-green-100 text-sm">
                      Reserve your charging slot
                    </p>
                  </div>
                </div>
                <button
                  onClick={closeModals}
                  className="text-white/80 hover:text-white hover:bg-white/10 p-2 rounded-full transition-all duration-200"
                >
                  <svg
                    className="w-5 h-5"
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
              <form onSubmit={createBooking} className="space-y-6">
                {/* Owner NIC Field - Show different UI based on user role */}
                {role === "EVOwner" ? (
                  <div>
                    <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
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
                      Booking For
                    </label>
                    <div className="flex items-center px-4 py-3 bg-green-50 border border-green-200 rounded-xl">
                      <svg
                        className="w-5 h-5 text-green-600 mr-3"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <div>
                        <p className="text-sm font-medium text-green-800">
                          Your Account
                        </p>
                        <p className="text-sm text-green-600">
                          NIC: {form.ownerNic}
                        </p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div>
                    <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
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
                      Owner NIC
                    </label>
                    <input
                      type="text"
                      placeholder="Enter NIC number (e.g. 123456789V)"
                      value={form.ownerNic}
                      onChange={(e) =>
                        setForm({ ...form, ownerNic: e.target.value })
                      }
                      className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all duration-200 placeholder-gray-400"
                      required
                    />
                  </div>
                )}

                {/* Charging Station Selection */}
                <div>
                  <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                    <svg
                      className="w-4 h-4 mr-2 text-gray-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z" />
                    </svg>
                    Charging Station
                  </label>
                  <div className="relative">
                    <select
                      value={form.stationId}
                      onChange={(e) =>
                        setForm({ ...form, stationId: e.target.value })
                      }
                      className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all duration-200 appearance-none bg-white"
                      required
                    >
                      <option value="" disabled>
                        Choose a charging station
                      </option>
                      {stations.map((station) => (
                        <option key={station.id} value={station.id}>
                          🔌 {station.name} - {station.location} ({station.type}
                          , {station.availableSlots} slots available)
                        </option>
                      ))}
                    </select>
                    <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
                      <svg
                        className="w-5 h-5 text-gray-400"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                          clipRule="evenodd"
                        />
                      </svg>
                    </div>
                  </div>
                  {stations.length === 0 && (
                    <p className="text-sm text-amber-600 mt-1 flex items-center">
                      <svg
                        className="w-4 h-4 mr-1"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                          clipRule="evenodd"
                        />
                      </svg>
                      No active stations available
                    </p>
                  )}
                </div>

                {/* Reservation Date & Time */}
                <div>
                  <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                    <svg
                      className="w-4 h-4 mr-2 text-gray-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
                        clipRule="evenodd"
                      />
                    </svg>
                    Reservation Date & Time
                  </label>
                  <input
                    type="datetime-local"
                    value={form.reservationDate}
                    onChange={(e) =>
                      setForm({ ...form, reservationDate: e.target.value })
                    }
                    className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all duration-200"
                    min={getMinDate()}
                    max={getMaxDate()}
                    required
                  />
                  <div className="mt-2 p-3 bg-blue-50 rounded-lg border border-blue-200">
                    <p className="text-sm text-blue-700 flex items-center">
                      <svg
                        className="w-4 h-4 mr-2 text-blue-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <span>
                        <strong>Booking Rules:</strong> Reservations must be
                        made at least 1 hour in advance and within 7 days from
                        now.
                      </span>
                    </p>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex space-x-4 pt-4">
                  <button
                    type="button"
                    onClick={closeModals}
                    className="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-xl hover:bg-gray-300 transition-all duration-200 font-medium"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="flex-1 px-6 py-3 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl hover:from-green-700 hover:to-emerald-700 transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:scale-105"
                  >
                    <span className="flex items-center justify-center">
                      <svg
                        className="w-4 h-4 mr-2"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
                          clipRule="evenodd"
                        />
                      </svg>
                      Create Booking
                    </span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

    

      {/* Bookings Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Owner NIC
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Station
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Date & Time
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {bookings.map((booking) => (
              <tr key={booking.id}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {booking.ownerNIC}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {getStationName(booking.stationId)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {new Date(booking.reservationDate).toLocaleString()}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <span
                    className={`px-2 py-1 rounded-full text-xs font-medium ${
                      booking.status === "Approved"
                        ? "bg-green-100 text-green-800"
                        : booking.status === "Pending"
                        ? "bg-yellow-100 text-yellow-800"
                        : booking.status === "Cancelled"
                        ? "bg-red-100 text-red-800"
                        : "bg-gray-100 text-gray-800"
                    }`}
                  >
                    {booking.status}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <div className="flex items-center gap-2 flex-wrap">
                    {/* Primary Actions - First Row */}
                    <div className="flex items-center gap-2">
                      {/* Approve button for Operators/Backoffice */}
                      {(role === "Backoffice" || role === "Operator") &&
                        booking.status === "Pending" && (
                          <button
                            onClick={() => approveBooking(booking.id)}
                            className="inline-flex items-center px-3 py-1.5 bg-green-600 text-white rounded-md text-xs font-medium hover:bg-green-700 transition-colors duration-200 shadow-sm hover:shadow-md"
                            title="Approve this booking"
                          >
                            <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                            </svg>
                            Approve
                          </button>
                        )}

                      {/* Update button for Backoffice/EVOwner */}
                      {(role === "Backoffice" || role === "EVOwner") &&
                        booking.status === "Pending" &&
                        canModifyBooking(booking) && (
                          <button
                            onClick={() => openUpdateForm(booking)}
                            className="inline-flex items-center px-3 py-1.5 bg-blue-600 text-white rounded-md text-xs font-medium hover:bg-blue-700 transition-colors duration-200 shadow-sm hover:shadow-md"
                            title="Update this booking"
                          >
                            <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                              <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                            </svg>
                            Update
                          </button>
                        )}

                      {/* Cancel button */}
                      {booking.status !== "Cancelled" &&
                        canModifyBooking(booking) && (
                          <button
                            onClick={() => cancelBooking(booking.id)}
                            className="inline-flex items-center px-3 py-1.5 bg-orange-500 text-white rounded-md text-xs font-medium hover:bg-orange-600 transition-colors duration-200 shadow-sm hover:shadow-md"
                            title="Cancel this booking"
                          >
                            <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                            </svg>
                            Cancel
                          </button>
                        )}

                    </div>

                    {/* Separator Line */}
                    {(role === "Backoffice" || 
                      (role === "EVOwner" && userNic === booking.ownerNIC)) && 
                      (booking.status === "Pending" || booking.status === "Cancelled" || booking.qrBase64) && (
                        <div className="h-6 w-px bg-gray-300 mx-1"></div>
                    )}

                    {/* Destructive Actions - Delete button at the end */}
                    {(role === "Backoffice" || 
                      (role === "EVOwner" && userNic === booking.ownerNIC)) && (
                        <button
                          onClick={() => deleteBooking(booking.id)}
                          className="inline-flex items-center px-3 py-1.5 bg-red-600 text-white rounded-md text-xs font-medium hover:bg-red-700 transition-colors duration-200 shadow-sm hover:shadow-md"
                          title="Permanently delete this booking"
                        >
                          <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd" />
                          </svg>
                          Delete
                        </button>
                      )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Empty State */}
        {bookings.length === 0 && (
          <div className="text-center py-12">
            <div className="w-20 h-20 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
              <svg
                className="w-8 h-8 text-gray-400"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
                  clipRule="evenodd"
                />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              No bookings found
            </h3>
            <p className="text-gray-500 mb-4">
              Get started by creating your first charging station booking.
            </p>
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
                className="inline-flex items-center px-4 py-2 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-lg hover:from-green-700 hover:to-emerald-700 transition-all duration-200 font-medium"
              >
                <svg
                  className="w-4 h-4 mr-2"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"
                    clipRule="evenodd"
                  />
                </svg>
                Create Your First Booking
              </button>
            )}
          </div>
        )}
      </div>
      
     
      
    </div>
  );
}
