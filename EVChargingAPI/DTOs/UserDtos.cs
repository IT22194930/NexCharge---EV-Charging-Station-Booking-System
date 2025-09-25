// Author: Peiris M. H. C. (IT22194930)
// Purpose: DTOs for Users
namespace EVChargingAPI.DTOs
{
   public record UserCreateDto(string Nic, string FullName, string Password, string Role);
   public record UserUpdateDto(string? FullName, string? Password);
   public record UpdateRoleDto(string NewRole);
}
