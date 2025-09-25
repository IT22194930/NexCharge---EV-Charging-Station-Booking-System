// Author: Peiris M. H. C. (IT22194930)
// Purpose: DTOs for Auth endpoints
namespace EVChargingAPI.DTOs
{
   public record LoginRequest(string Nic, string Password);
   public record LoginResponse(string Token);
   public record RegisterRequest(string Nic, string FullName, string Password);
}
