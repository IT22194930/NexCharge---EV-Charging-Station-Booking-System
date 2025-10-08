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
   public record BookingCreateDto(string OwnerNic, string StationId, DateTime ReservationDate, int ReservationHour);
   public record BookingUpdateDto(DateTime ReservationDate, int ReservationHour, string StationId);
   public record AvailableSlotDto(int Hour, int AvailableSlots, int TotalSlots);
   public record StationAvailabilityDto(string StationId, string StationName, DateTime Date, List<AvailableSlotDto> AvailableHours);
}
