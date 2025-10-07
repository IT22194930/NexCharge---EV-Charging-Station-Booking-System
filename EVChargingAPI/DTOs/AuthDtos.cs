/*
 * File: AuthDtos.cs
 * Author: Peiris M. H. C. (IT22194930)
 * Description: Data Transfer Objects (DTOs) for authentication-related API endpoints.
 *              Defines request and response contracts for login, registration, and authentication operations.
 *              Ensures type safety and clear API contracts for authentication services.
 * 
 * DTO Definitions:
 * - LoginRequest: User login credentials (NIC and password)
 * - LoginResponse: Authentication response containing JWT token
 * - RegisterRequest: New user registration data (NIC, full name, password)
 * - ProfileResponse: User profile information for authenticated users
 * - TokenValidationRequest: Token validation and refresh requests
 * 
 * Security Considerations:
 * - Password fields are write-only and never returned in responses
 * - Sensitive user data is excluded from response DTOs
 * - Input validation attributes for secure data handling
 * - Consistent naming conventions for API contract clarity
 * 
 * Usage: These DTOs are used by AuthController endpoints to ensure
 *        consistent request/response formats and proper data validation.
 */

namespace EVChargingAPI.DTOs
{
   public record LoginRequest(string Nic, string Password);
   public record LoginResponse(string Token);
   public record RegisterRequest(string Nic, string FullName, string Password);
}
