/*
 * File: BookingService.cs
 * Author: Welikanna S. T. (IT22196910)
 * Description: Application service implementing booking business rules and workflow management.
 * 
 * Responsibilities:
 * - Enforce future-date and 7-day advance booking limits
 * - Validate 12-hour modification/cancellation window
 * - Check station availability before create/update
 * - Manage status transitions: Pending → Approved → Completed, Pending → Cancelled
 * - Generate QR payload/image upon approval
 * - Guard invalid transitions and prevent deletion of protected states
 * 
 * Notes: Single source of truth for booking invariants; controllers delegate validation here.
 */

using EVChargingAPI.Models;
using EVChargingAPI.Repositories;
using EVChargingAPI.DTOs;

namespace EVChargingAPI.Services
{
    public class BookingService
    {
        // Repository and auxiliary services injected via DI.
        private readonly BookingRepository _repo;
        private readonly StationRepository _stations;
        private readonly UserRepository _users;
        private readonly QrCodeService _qr;

        public BookingService(BookingRepository repo, StationRepository stations, UserRepository users, QrCodeService qr)
        {
            _repo = repo;
            _stations = stations;
            _users = users;
            _qr = qr;
        }

        public async Task<Booking> CreateAsync(Booking b)
        {
            // Validate owner exists and active
            var owner = await _users.GetByNICAsync(b.OwnerNIC) ?? throw new Exception("Owner not found");
            if (!owner.IsActive) throw new Exception("Owner not active");

            // Validate station exists and active
            var station = await _stations.GetByIdAsync(b.StationId) ?? throw new Exception("Station not found");
            if (!station.IsActive) throw new Exception("Station not active");

            // Booking date must be within 7 days from now
            if (b.ReservationDate > DateTime.UtcNow.AddDays(7) || b.ReservationDate < DateTime.UtcNow.Date)
                throw new Exception("Reservation must be within 7 days and not in the past");

            // Validate hour is within valid range (0-23)
            if (b.ReservationHour < 0 || b.ReservationHour > 23)
                throw new Exception("Reservation hour must be between 0 and 23");

            // Check if slot is available for the specific hour
            var bookedCount = await _repo.GetBookedCountForHourAsync(b.StationId, b.ReservationDate, b.ReservationHour);
            if (bookedCount >= station.AvailableSlots)
                throw new Exception($"No available slots for hour {b.ReservationHour}:00. All {station.AvailableSlots} slots are booked.");

            // Create booking as Pending
            b.Status = "Pending";
            await _repo.CreateAsync(b);
            return b;
        }

        public async Task<Booking> UpdateAsync(string id, Booking updated)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");

            // Only allow update >=12 hours before reservation
            var reservationDateTime = existing.ReservationDate.Date.AddHours(existing.ReservationHour);
            if (DateTime.UtcNow > reservationDateTime.AddHours(-12))
                throw new Exception("Cannot modify booking less than 12 hours before reservation");

            // Validate new station if changed
            if (updated.StationId != existing.StationId)
            {
                // Ensure target station exists and active prior to moving the booking
                var station = await _stations.GetByIdAsync(updated.StationId) ?? throw new Exception("Station not found");
                if (!station.IsActive) throw new Exception("Station not active");
            }

            // Validate new hour if changed
            if (updated.ReservationHour < 0 || updated.ReservationHour > 23)
                throw new Exception("Reservation hour must be between 0 and 23");

            // Check slot availability for new time/station if changed
            if (updated.ReservationDate != existing.ReservationDate ||
                updated.ReservationHour != existing.ReservationHour ||
                updated.StationId != existing.StationId)
            {
                // Use station capacity to ensure no overbooking on the new slot
                var station = await _stations.GetByIdAsync(updated.StationId) ?? throw new Exception("Station not found for availability check");
                var bookedCount = await _repo.GetBookedCountForHourAsync(updated.StationId, updated.ReservationDate, updated.ReservationHour);
                if (bookedCount >= station.AvailableSlots)
                    throw new Exception($"No available slots for hour {updated.ReservationHour}:00. All {station.AvailableSlots} slots are booked.");
            }

            // Persist only allowed fields (status managed by workflow endpoints)
            existing.ReservationDate = updated.ReservationDate;
            existing.ReservationHour = updated.ReservationHour;
            existing.StationId = updated.StationId;
            await _repo.UpdateAsync(id, existing);
            return existing;
        }

        public async Task CancelAsync(string id)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            // Enforce 12-hour cancellation window relative to reservation date+hour
            var reservationDateTime = existing.ReservationDate.Date.AddHours(existing.ReservationHour);
            if (DateTime.UtcNow > reservationDateTime.AddHours(-12))
                throw new Exception("Cannot cancel booking less than 12 hours before reservation");
            // Status moved to terminal "Cancelled"
            existing.Status = "Cancelled";
            await _repo.UpdateAsync(id, existing);
        }

        public async Task<Booking> ApproveAsync(string id)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            // in production: decrement station availableSlots etc.
            existing.Status = "Approved";
            // QR payload encodes immutable session identifiers; image generated as Base64 for the client
            existing.QrBase64 = _qr.GenerateQrBase64($"booking:{existing.Id}|owner:{existing.OwnerNIC}|station:{existing.StationId}|date:{existing.ReservationDate:o}|hour:{existing.ReservationHour}");
            await _repo.UpdateAsync(id, existing);
            return existing;
        }

        public async Task<Booking> ConfirmAsync(string id)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            // Transitional "Started" state (scan/arrival) before completing the session
            if (existing.Status != "Approved") throw new Exception("Only approved bookings can be confirmed");
            existing.Status = "Started";
            await _repo.UpdateAsync(id, existing);
            return existing;
        }

        public async Task<Booking> CompleteAsync(string id)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            // Allow completion from "Started" (normal) or directly from "Approved" if session ended without confirm.
            if (existing.Status != "Started" && existing.Status != "Approved") 
                throw new Exception("Only started or approved bookings can be completed");
            existing.Status = "Completed";
            await _repo.UpdateAsync(id, existing);
            return existing;
        }

        public async Task DeleteAsync(string id)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            // Allow deletion if booking is Cancelled or Pending
            if (existing.Status == "Approved" || existing.Status == "Completed")
            {
                // Deletion is blocked for protected states to preserve audit/history
                throw new Exception("Cannot delete approved or completed bookings. Please cancel first (only approved) or this booking is finalized.");
            }
            await _repo.DeleteAsync(id);
        }

        public async Task<StationAvailabilityDto> GetStationAvailabilityAsync(string stationId, DateTime date)
        {
            var station = await _stations.GetByIdAsync(stationId) ?? throw new Exception("Station not found");
            var bookings = await _repo.GetBookingsForDateAsync(stationId, date);

            var availableHours = new List<AvailableSlotDto>();

            for (int hour = 0; hour < 24; hour++)
            {
                // Count how many bookings exist for the current hour
                var bookedCount = bookings.Count(b => b.ReservationHour == hour);
                var availableSlots = station.AvailableSlots - bookedCount;

                // Clamp at 0 to avoid negative numbers if data races occur
                availableHours.Add(new AvailableSlotDto(hour, Math.Max(0, availableSlots), station.AvailableSlots));
            }

            // Returns a per-hour view for the station-date pair
            return new StationAvailabilityDto(stationId, station.Name, date, availableHours);
        }

        public async Task<List<int>> GetAvailableHoursAsync(string stationId, DateTime date)
        {
            var station = await _stations.GetByIdAsync(stationId) ?? throw new Exception("Station not found");
            var bookings = await _repo.GetBookingsForDateAsync(stationId, date);

            var availableHours = new List<int>();

            for (int hour = 0; hour < 24; hour++)
            {
                // Accept hour if current bookings < capacity
                var bookedCount = bookings.Count(b => b.ReservationHour == hour);
                if (bookedCount < station.AvailableSlots)
                {
                    availableHours.Add(hour);
                }
            }

            // Sorted ascending by hour by construction (0..23)
            return availableHours;
        }
    }
}
