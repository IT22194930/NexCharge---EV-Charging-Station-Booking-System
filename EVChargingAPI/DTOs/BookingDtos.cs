/*
 * File: BookingDtos.cs
 * Author: Welikanna S. T. (IT22196910)
 * Description: Data Transfer Objects (DTOs) and contracts for booking API requests and responses.
 *              Defines shapes used by controllers to communicate with clients.
 * 
 * Contracts:
 * - BookingCreateDto    - Payload for creating a booking
 * - BookingUpdateDto    - Payload for updating a booking
 * - BookingResponseDto  - Standard response object for bookings
 * 
 * Notes: DTOs decouple API contracts from domain models and may include basic validation attributes.
 */

namespace EVChargingAPI.DTOs
{
   // Carries owner identity (NIC), station reference, target date, and hour slot for a new booking.
   // ReservationDate: date component is used by service for availability checks; hour in 24h format [0..23].
   public record BookingCreateDto(string OwnerNic, string StationId, DateTime ReservationDate, int ReservationHour);

   // Allows updating reservation date/hour and optionally moving to another station (subject to checks).
   public record BookingUpdateDto(DateTime ReservationDate, int ReservationHour, string StationId);

   // Represents capacity view per hour: how many slots are free vs total configured at the station.
   public record AvailableSlotDto(int Hour, int AvailableSlots, int TotalSlots);

   // Aggregates availability for a station on a given date with hour-by-hour slot counts.
   public record StationAvailabilityDto(string StationId, string StationName, DateTime Date, List<AvailableSlotDto> AvailableHours);
}
