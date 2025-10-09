/*
 * File: StationDtos.cs
 * Author: Wickramasooriya W. A. A. L. (IT22126160)
 * Description: Data Transfer Objects for EV charging station management operations.
 *              Contains record types for API request/response data handling.
 * 
 * DTO Definitions:
 *   StationCreateDto - Record for creating new charging stations
 *     - Name: Station name/identifier (required)
 *     - Location: Physical address or location description (required)
 *     - Lat: Latitude coordinate (optional, nullable)
 *     - Lng: Longitude coordinate (optional, nullable)
 *     - Type: Station type classification (required)
 *     - AvailableSlots: Number of charging slots available (required)
 * 
 *   StationUpdateDto - Record for updating existing charging stations
 *     - Name: Updated station name/identifier (required)
 *     - Location: Updated physical address or location (required)
 *     - Lat: Updated latitude coordinate (optional, nullable)
 *     - Lng: Updated longitude coordinate (optional, nullable)
 *     - Type: Updated station type classification (required)
 *     - AvailableSlots: Updated number of charging slots (required)
 *     - IsActive: Station operational status flag (required)
 */

namespace EVChargingAPI.DTOs
{
   public record StationCreateDto(string Name, string Location, double? Lat, double? Lng, string Type, int AvailableSlots);
   public record StationUpdateDto(string Name, string Location, double? Lat, double? Lng, string Type, int AvailableSlots, bool IsActive);
}
