// Author: Wickramasooriya W. A. A. L. (IT22126160)
// Purpose: DTOs for Stations
namespace EVChargingAPI.DTOs
{
   public record StationCreateDto(string Name, string Location, double? Lat, double? Lng, string Type, int AvailableSlots);
   public record StationUpdateDto(string Name, string Location, double? Lat, double? Lng, string Type, int AvailableSlots, bool IsActive);
}
