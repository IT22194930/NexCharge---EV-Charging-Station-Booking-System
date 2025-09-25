// Author: Peiris M. H. C. (IT22194930)
// Purpose: DTOs for Stations
namespace EVChargingAPI.DTOs
{
   public record StationCreateDto(string Name, string Location, double? Lat, double? Lng, string Type, int AvailableSlots);
   public record StationUpdateDto(string Name, string Location, double? Lat, double? Lng, string Type, int AvailableSlots, bool IsActive);
}
