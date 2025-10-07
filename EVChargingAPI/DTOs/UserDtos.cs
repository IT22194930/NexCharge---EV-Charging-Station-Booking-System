/*
 * File: UserDtos.cs
 * Author: Peiris M. H. C. (IT22194930)
 * Description: Data Transfer Objects (DTOs) for user management API endpoints.
 *              Defines request and response contracts for user CRUD operations, role management,
 *              and station assignments. Ensures type safety and clear API contracts for user services.
 * 
 * DTO Definitions:
 * - UserCreateDto: New user creation data (NIC, full name, password, role, optional station assignment)
 * - UserUpdateDto: User profile update data (optional full name and password changes)
 * - UpdateRoleDto: Role change requests with optional station assignment
 * - UpdateStationAssignmentDto: Station assignment updates for operators
 * - UserResponseDto: User information response (excludes sensitive data)
 * 
 * Security Considerations:
 * - Password fields are write-only and never returned in responses
 * - Role validation constraints ensure only valid roles are accepted
 * - Station assignment validation for operator role requirements
 * - Input sanitization and validation attributes for secure data handling
 * 
 * Validation Rules:
 * - NIC format validation for Sri Lankan context
 * - Password strength requirements enforcement
 * - Role enumeration validation (Backoffice, Operator, EVOwner)
 * - Station assignment consistency checks for operator roles
 * 
 * Usage: These DTOs are used by UsersController endpoints to ensure
 *        consistent request/response formats and proper data validation.
 */

namespace EVChargingAPI.DTOs
{
   public record UserCreateDto(string Nic, string FullName, string Password, string Role, string? AssignedStationId = null);
   public record UserUpdateDto(string? FullName, string? Password);
   public record UpdateRoleDto(string NewRole, string? AssignedStationId = null);
   public record UpdateStationAssignmentDto(string? AssignedStationId);
}
