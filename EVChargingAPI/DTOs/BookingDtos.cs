// Author: Welikanna S. T. (IT22196910)
// Purpose: DTOs for Bookings
namespace EVChargingAPI.DTOs
{
   public record BookingCreateDto(string OwnerNic, string StationId, DateTime ReservationDate);
   
}
