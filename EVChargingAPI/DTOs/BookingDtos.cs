// Author: Welikanna S. T. (IT22196910)
// Purpose: DTOs for Bookings
namespace EVChargingAPI.DTOs
{
   public record BookingCreateDto(string OwnerNic, string StationId, DateTime ReservationDate, int ReservationHour);
   public record BookingUpdateDto(DateTime ReservationDate, int ReservationHour, string StationId);
   public record AvailableSlotDto(int Hour, int AvailableSlots, int TotalSlots);
   public record StationAvailabilityDto(string StationId, string StationName, DateTime Date, List<AvailableSlotDto> AvailableHours);
}
