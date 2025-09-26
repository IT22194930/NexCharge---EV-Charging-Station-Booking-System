
/*
 * File: EVOwnerDtos.cs
 * Author: Liyanage N S
 * Date: September 26, 2025
 * Description: Data Transfer Objects (DTOs) for EV Owner operations.
 *              Contains record types for API request/response data transfer,
 *              ensuring type safety and data validation for EV Owner endpoints.
 * 
 * DTOs:
 * - EVOwnerRegisterDto - Registration request data
 * - EVOwnerUpdateDto - Profile update request data
 * - EVOwnerChangePasswordDto - Password change request data
 * - EVOwnerProfileDto - Profile response data
 * 
 */

namespace EVChargingAPI.DTOs
{
    public record EVOwnerRegisterDto(string Nic, string FullName, string Password);
    public record EVOwnerUpdateDto(string? FullName, string? Password);
    public record EVOwnerChangePasswordDto(string CurrentPassword, string NewPassword);
    public record EVOwnerProfileDto(string Nic, string FullName, string Role, bool IsActive, DateTime CreatedAt);
}