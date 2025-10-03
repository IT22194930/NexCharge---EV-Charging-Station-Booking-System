// Author: Peiris M. H. C. (IT22194930)
// Purpose: DTOs for Users
namespace EVChargingAPI.DTOs
{
   public record UserCreateDto(string Nic, string FullName, string Password, string Role, string? AssignedStationId = null);
   public record UserUpdateDto(string? FullName, string? Password);
   public record UpdateRoleDto(string NewRole, string? AssignedStationId = null);
   public record UpdateStationAssignmentDto(string? AssignedStationId);
}
