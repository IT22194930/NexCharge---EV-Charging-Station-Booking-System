import api from "./axios";

// Booking API endpoints
export const bookingAPI = {
  // Get available hours for a specific station and date
  getAvailableHours: async (stationId, date) => {
    const response = await api.get(
      `/bookings/available-hours/${stationId}?date=${date}`
    );
    return response.data;
  },

  // Get station availability for a specific date
  getStationAvailability: async (stationId, date) => {
    const response = await api.get(
      `/bookings/availability/${stationId}?date=${date}`
    );
    return response.data;
  },

  // Create a new booking
  createBooking: async (bookingData) => {
    const response = await api.post("/bookings", bookingData);
    return response.data;
  },

  // Update an existing booking
  updateBooking: async (bookingId, bookingData) => {
    const response = await api.put(`/bookings/${bookingId}`, bookingData);
    return response.data;
  },

  // Get bookings by owner
  getBookingsByOwner: async (ownerNic) => {
    const response = await api.get(`/bookings/owner/${ownerNic}`);
    return response.data;
  },

  // Get all bookings (admin/operator)
  getAllBookings: async () => {
    const response = await api.get("/bookings");
    return response.data;
  },

  // Cancel a booking
  cancelBooking: async (bookingId) => {
    const response = await api.post(`/bookings/cancel/${bookingId}`);
    return response.data;
  },

  // Approve a booking (operator/admin)
  approveBooking: async (bookingId) => {
    const response = await api.post(`/bookings/approve/${bookingId}`);
    return response.data;
  },

  // Delete a booking
  deleteBooking: async (bookingId) => {
    const response = await api.delete(`/bookings/${bookingId}`);
    return response.data;
  },
};

// Station API endpoints
export const stationAPI = {
  // Get all stations
  getAllStations: async () => {
    const response = await api.get("/stations");
    return response.data;
  },

  // Get station by ID
  getStationById: async (stationId) => {
    const response = await api.get(`/stations/${stationId}`);
    return response.data;
  },
};
