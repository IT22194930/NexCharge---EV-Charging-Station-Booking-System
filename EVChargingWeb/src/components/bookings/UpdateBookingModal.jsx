import React, { useState, useEffect } from "react";
import { formatBookingTimeRange } from "../../utils/dateUtils";
import { bookingAPI } from "../../api/bookingService";

export default function UpdateBookingModal({
  visible,
  closeModals,
  form,
  setForm,
  stations,
  updateBooking,
  currentBooking,
  getStationName,
  getMinDate,
  getMaxDate,
}) {
  const [availableHours, setAvailableHours] = useState([]);
  const [loadingHours, setLoadingHours] = useState(false);

  // Load available hours when station and date change
  useEffect(() => {
    const loadHours = async () => {
      if (
        form.stationId &&
        form.reservationDate &&
        form.stationId !== "" &&
        form.reservationDate !== ""
      ) {
        try {
          setLoadingHours(true);
          const hours = await bookingAPI.getAvailableHours(
            form.stationId,
            form.reservationDate
          );
          setAvailableHours(hours);

          // Reset selected hour if it's no longer available
          if (!hours.includes(form.reservationHour)) {
            setForm((prev) => ({
              ...prev,
              reservationHour: hours.length > 0 ? hours[0] : 0,
            }));
          }
        } catch (error) {
          console.error("Error loading available hours:", error);
          setAvailableHours([]);
        } finally {
          setLoadingHours(false);
        }
      } else {
        setAvailableHours([]);
      }
    };

    loadHours();
  }, [form.stationId, form.reservationDate, form.reservationHour, setForm]);

  const handleDateChange = (e) => {
    setForm((prev) => ({
      ...prev,
      reservationDate: e.target.value,
      reservationHour: 0,
    }));
  };

  if (!visible) return null;
  return (
    <div
      className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4"
      onClick={(e) => e.target === e.currentTarget && closeModals()}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl transform transition-all duration-300 max-h-[90vh] flex flex-col">
        {/* Modal Header */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 p-4 rounded-t-2xl flex-shrink-0">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-white/20 rounded-full flex items-center justify-center">
                <svg
                  className="w-4 h-4 text-white"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                </svg>
              </div>
              <div>
                <h2 className="text-lg font-bold text-white">Update Booking</h2>
                <p className="text-blue-100 text-xs">
                  Modify your reservation details
                </p>
              </div>
            </div>
            <button
              onClick={closeModals}
              className="text-white/80 hover:text-white hover:bg-white/10 p-1.5 rounded-full transition-all duration-200"
            >
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                  clipRule="evenodd"
                />
              </svg>
            </button>
          </div>
        </div>

        {/* Modal Body - Scrollable */}
        <div className="flex-1 overflow-y-auto">
          <div className="p-4">
            <form onSubmit={updateBooking} className="space-y-4">
              {/* Current Booking Info */}
              <div className="relative bg-gradient-to-br from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-3 shadow-sm">
                {/* Header with Icon */}
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center">
                    <div className="w-5 h-5 bg-blue-600 rounded-full flex items-center justify-center mr-2">
                      <svg
                        className="w-3 h-3 text-white"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                          clipRule="evenodd"
                        />
                      </svg>
                    </div>
                    <h3 className="text-sm font-semibold text-blue-900">
                      Current Booking Details
                    </h3>
                  </div>
                  <div className="flex items-center space-x-1">
                    <div className="w-2 h-2 bg-blue-400 rounded-full animate-pulse"></div>
                    <span className="text-xs text-blue-600 font-medium">
                      Active
                    </span>
                  </div>
                </div>

                {/* Booking Information - Compact Grid */}
                <div className="grid grid-cols-2 gap-2 text-xs">
                  {/* Owner */}
                  <div className="bg-white/70 rounded-lg p-2 border border-blue-100">
                    <div className="flex items-center mb-1">
                      <svg
                        className="w-3 h-3 text-blue-600 mr-1"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <span className="font-medium text-gray-600">Owner</span>
                    </div>
                    <p className="text-sm font-semibold text-gray-900 truncate">
                      {currentBooking?.ownerNIC}
                    </p>
                  </div>

                  {/* Station */}
                  <div className="bg-white/70 rounded-lg p-2 border border-blue-100">
                    <div className="flex items-center mb-1">
                      <svg
                        className="w-3 h-3 text-green-600 mr-1"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z" />
                      </svg>
                      <span className="font-medium text-gray-600">Station</span>
                    </div>
                    <p className="text-sm font-semibold text-gray-900 truncate">
                      {getStationName(currentBooking?.stationId)}
                    </p>
                  </div>

                  {/* Reservation */}
                  <div className="bg-white/70 rounded-lg p-2 border border-blue-100">
                    <div className="flex items-center mb-1">
                      <svg
                        className="w-3 h-3 text-purple-600 mr-1"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <span className="font-medium text-gray-600">
                        Time Slot
                      </span>
                    </div>
                    <div className="space-y-0">
                      <div className="flex items-baseline space-x-2.5">
                        <p className="text-sm font-semibold text-gray-900">
                          {formatBookingTimeRange(
                            currentBooking?.reservationDate,
                            currentBooking?.reservationHour || 0
                          )}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Status */}
                  <div className="bg-white/70 rounded-lg p-2 border border-blue-100">
                    <div className="flex items-center mb-1">
                      <svg
                        className="w-3 h-3 text-amber-600 mr-1"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                          clipRule="evenodd"
                        />
                      </svg>
                      <span className="font-medium text-gray-600">Status</span>
                    </div>
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                        currentBooking?.status === "Approved"
                          ? "bg-green-100 text-green-800"
                          : currentBooking?.status === "Pending"
                          ? "bg-yellow-100 text-yellow-800"
                          : "bg-gray-100 text-gray-800"
                      }`}
                    >
                      <div
                        className={`w-1 h-1 rounded-full mr-1 ${
                          currentBooking?.status === "Approved"
                            ? "bg-green-600"
                            : currentBooking?.status === "Pending"
                            ? "bg-yellow-600"
                            : "bg-gray-600"
                        }`}
                      ></div>
                      {currentBooking?.status}
                    </span>
                  </div>
                </div>
              </div>

              {/* Owner NIC Field (Read-only) */}
              <div>
                <label className="flex items-center text-xs font-medium text-gray-700 mb-1">
                  <svg
                    className="w-3 h-3 mr-1 text-gray-500"
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
                <div className="flex items-center px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg">
                  <svg
                    className="w-4 h-4 text-gray-400 mr-2"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-800">
                      {form.ownerNic}
                    </p>
                    <p className="text-xs text-gray-500">Cannot be modified</p>
                  </div>
                </div>
              </div>

              {/* Charging Station Selection */}
              <div>
                <label className="flex items-center text-xs font-medium text-gray-700 mb-1">
                  <svg
                    className="w-3 h-3 mr-1 text-gray-500"
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
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 appearance-none bg-white text-sm"
                    required
                  >
                    <option value="" disabled>
                      Choose a new charging station
                    </option>
                    {stations.map((station) => (
                      <option key={station.id} value={station.id}>
                        🔌 {station.name} - {station.location} ({station.type},{" "}
                        {station.availableSlots} slots)
                      </option>
                    ))}
                  </select>
                  <div className="absolute inset-y-0 right-0 flex items-center pr-2 pointer-events-none">
                    <svg
                      className="w-4 h-4 text-gray-400"
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
              </div>

              {/* Reservation Date & Time */}
              <div>
                <label className="flex items-center text-xs font-medium text-gray-700 mb-1">
                  <svg
                    className="w-3 h-3 mr-1 text-gray-500"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
                      clipRule="evenodd"
                    />
                  </svg>
                  New Reservation Date
                </label>
                <input
                  type="date"
                  value={form.reservationDate}
                  onChange={handleDateChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
                  min={getMinDate()}
                  max={getMaxDate()}
                  disabled={!form.stationId}
                  required
                />
                {!form.stationId && (
                  <p className="text-xs text-gray-500 mt-1">
                    Please select a charging station first
                  </p>
                )}
              </div>

              {/* New Time Slot */}
              <div>
                <label className="flex items-center text-xs font-medium text-gray-700 mb-1">
                  <svg
                    className="w-3 h-3 mr-1 text-gray-500"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
                      clipRule="evenodd"
                    />
                  </svg>
                  New Time Slot
                </label>
                <select
                  value={form.reservationHour}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      reservationHour: parseInt(e.target.value),
                    })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
                  required
                  disabled={
                    !form.stationId || !form.reservationDate || loadingHours
                  }
                >
                  {!form.stationId ? (
                    <option value="">Select a charging station first</option>
                  ) : !form.reservationDate ? (
                    <option value="">Select a date first</option>
                  ) : loadingHours ? (
                    <option value="">Loading available hours...</option>
                  ) : availableHours.length === 0 ? (
                    <option value="">No available slots for this date</option>
                  ) : (
                    <>
                      <option value="" disabled>
                        Choose an available time slot
                      </option>
                      {availableHours.map((hour) => (
                        <option key={hour} value={hour}>
                          {hour.toString().padStart(2, "0")}:00 -{" "}
                          {(hour + 1).toString().padStart(2, "0")}:00
                        </option>
                      ))}
                    </>
                  )}
                </select>
                <div className="mt-2 p-2 bg-amber-50 rounded-lg border border-amber-200">
                  <p className="text-xs text-amber-700 flex items-center">
                    <svg
                      className="w-3 h-3 mr-1 text-amber-500 flex-shrink-0"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                        clipRule="evenodd"
                      />
                    </svg>
                    <span>
                      <strong>Update Rules:</strong> Select station first, then
                      date (within 7 days), then time slot. Only available time
                      slots are shown. Changes must be made at least 12 hours
                      before the current reservation time.
                    </span>
                  </p>
                </div>
              </div>
            </form>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="p-4 border-t border-gray-200 flex-shrink-0">
          <div className="flex space-x-3">
            <button
              type="button"
              onClick={closeModals}
              className="flex-1 px-4 py-2.5 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-all duration-200 font-medium text-sm"
            >
              Cancel
            </button>
            <button
              type="submit"
              onClick={updateBooking}
              className="flex-1 px-4 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg hover:from-blue-700 hover:to-indigo-700 transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:scale-105 text-sm"
            >
              <span className="flex items-center justify-center">
                <svg
                  className="w-4 h-4 mr-1.5"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                </svg>
                Update Booking
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
