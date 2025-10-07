import React from "react";
import { formatBookingTimeRange } from "../../utils/dateUtils";

export default function BookingTable({
  bookings,
  allBookings,
  role,
  userNic,
  getStationName,
  approveBooking,
  completeBooking,
  openUpdateForm,
  canModifyBooking,
  cancelBooking,
  deleteBooking,
  startIndex = 0,
}) {
  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      {/* Scroll indicator for smaller screens */}
      <div className="px-4 py-2 bg-blue-50 border-b border-blue-100 xl:hidden">
        <p className="text-xs text-blue-700 flex items-center">
          <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 111.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
          </svg>
          Scroll horizontally to see all columns
        </p>
      </div>
      <div className="overflow-x-auto scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100">
        <table className="w-full min-w-[1000px]">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
              #
            </th>
            {role !== "EVOwner" && (
              <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-32">
                Owner NIC
              </th>
            )}
            <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              <div className="flex items-center space-x-3">
                <span>Station</span>
              </div>
            </th>
            <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-28">
              Time Slot
            </th>
            <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
              Status
            </th>
            <th className="px-3 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider w-56">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {bookings.map((booking, index) => (
            <tr key={booking.id}>
              <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-500 font-medium text-center">
                {startIndex + index + 1}
              </td>
              {role !== "EVOwner" && (
                <td className="px-3 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  <div className="truncate max-w-32" title={booking.ownerNIC}>
                    {booking.ownerNIC}
                  </div>
                </td>
              )}
              <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                <div className="truncate" title={getStationName(booking.stationId)}>
                  {getStationName(booking.stationId)}
                </div>
              </td>
              <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900 text-center">
                {formatBookingTimeRange(
                  booking.reservationDate,
                  booking.reservationHour || 0
                )}
              </td>
              <td className="px-3 py-4 whitespace-nowrap text-center">
                <span
                  className={`px-2 py-1 rounded-full text-xs font-medium ${
                    booking.status === "Approved"
                      ? "bg-green-100 text-green-800"
                      : booking.status === "Completed"
                      ? "bg-emerald-100 text-emerald-800"
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
              <td className="px-3 py-4 whitespace-nowrap text-sm font-medium">
                <div className="flex items-center justify-center gap-1">
                  {/* Column 1: Primary Action (Approve/Complete) */}
                  <div className="w-20">
                    {(role === "Backoffice" || role === "Operator") &&
                      booking.status === "Pending" && (
                        <button
                          onClick={() => approveBooking(booking.id)}
                          className="inline-flex items-center px-2 py-1.5 bg-green-600 text-white rounded-md text-xs font-medium hover:bg-green-700 transition-colors w-full justify-center"
                          title="Approve this booking"
                        >
                          <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                            <path
                              fillRule="evenodd"
                              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                              clipRule="evenodd"
                            />
                          </svg>
                          Approve
                        </button>
                      )}
                    {(role === "Backoffice" || role === "Operator") &&
                      booking.status === "Approved" && (
                        <button
                          onClick={() => completeBooking(booking.id)}
                          className="inline-flex items-center px-2 py-1.5 bg-blue-600 text-white rounded-md text-xs font-medium hover:bg-blue-700 transition-colors w-full justify-center"
                          title="Mark as completed"
                        >
                          <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                            <path
                              fillRule="evenodd"
                              d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 10-1.414 1.414L9 13.414l4.707-4.707z"
                              clipRule="evenodd"
                            />
                          </svg>
                          Complete
                        </button>
                      )}
                  </div>

                  {/* Column 2: Update Action */}
                  <div className="w-16">
                    {(role === "Backoffice" || role === "EVOwner") &&
                      booking.status === "Pending" &&
                      canModifyBooking(booking) && (
                        <button
                          onClick={() => openUpdateForm(booking)}
                          className="inline-flex items-center px-2 py-1.5 bg-indigo-600 text-white rounded-md text-xs font-medium hover:bg-indigo-700 transition-colors w-full justify-center"
                          title="Update this booking"
                        >
                          <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                          </svg>
                          Update
                        </button>
                      )}
                  </div>

                  {/* Column 3: Cancel Action */}
                  <div className="w-16">
                    {(role === "Backoffice" || role === "Operator") &&
                      booking.status === "Pending" && (
                        <button
                          onClick={() => cancelBooking(booking.id)}
                          className="inline-flex items-center px-2 py-1.5 bg-orange-500 text-white rounded-md text-xs font-medium hover:bg-orange-600 transition-colors w-full justify-center"
                          title="Cancel this booking"
                        >
                          <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                          </svg>
                          Cancel
                        </button>
                      )}
                    {booking.status !== "Cancelled" &&
                      booking.status !== "Completed" &&
                      booking.status !== "Pending" &&
                      canModifyBooking(booking) && (
                        <button
                          onClick={() => cancelBooking(booking.id)}
                          className="inline-flex items-center px-2 py-1.5 bg-orange-500 text-white rounded-md text-xs font-medium hover:bg-orange-600 transition-colors w-full justify-center"
                          title="Cancel this booking"
                        >
                          <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                          </svg>
                          Cancel
                        </button>
                      )}
                  </div>

                  {/* Column 4: QR Code */}
                  <div className="w-20">
                    {booking.qrBase64 && (
                      <a
                        href={`data:image/png;base64,${booking.qrBase64}`}
                        download={`booking-${booking.id}.png`}
                        className="inline-flex items-center px-2 py-1.5 bg-purple-600 text-white rounded-md text-xs font-medium hover:bg-purple-700 transition-colors w-full justify-center"
                        title="Download QR code"
                      >
                        <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                          <path
                            fillRule="evenodd"
                            d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z"
                            clipRule="evenodd"
                          />
                        </svg>
                        QR Code
                      </a>
                    )}
                  </div>

                  {/* Column 5: Delete Action */}
                  <div className="w-10">
                    {(role === "Backoffice" ||
                      (role === "EVOwner" && userNic === booking.ownerNIC)) && (
                      <button
                        onClick={() => deleteBooking(booking.id)}
                        className="p-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors w-full flex items-center justify-center"
                        title="Permanently delete this booking"
                      >
                        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                          <path
                            fillRule="evenodd"
                            d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
                            clipRule="evenodd"
                          />
                        </svg>
                      </button>
                    )}
                  </div>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
        </table>
      </div>

      {(allBookings?.length || bookings.length) === 0 && (
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
        </div>
      )}
    </div>
  );
}
