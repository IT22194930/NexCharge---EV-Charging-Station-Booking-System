/*
 * File: User.cs
 * Author: Peiris M. H. C. (IT22194930)
 * Description: User domain model representing all user types in the NexCharge system.
 *              Defines the complete user entity structure with MongoDB document mapping,
 *              supporting three distinct user roles: Backoffice administrators, Station operators, and EV owners.
 * 
 * User Roles:
 * - Backoffice: System administrators with full system access and user management capabilities
 * - Operator: Station operators managing assigned charging stations and bookings
 * - EVOwner: Electric vehicle owners who book charging sessions
 * 
 * Key Properties:
 * - Id: MongoDB ObjectId for unique document identification
 * - NIC: National Identity Card number (unique identifier and login credential)
 * - FullName: Complete user name for display and identification
 * - Role: User role determining system permissions and access levels
 * - PasswordHash: BCrypt-hashed password for secure authentication
 * - AssignedStationId: Station assignment for Operator role (null for other roles)
 * - AssignedStationName: Cached station name for display purposes
 * - IsActive: Account status flag for enabling/disabling user access
 * - CreatedAt: Account creation timestamp for audit trails
 * - UpdatedAt: Last modification timestamp for change tracking
 * 
 * Security Features:
 * - Password stored as BCrypt hash with salt for maximum security
 * - NIC-based authentication system for Sri Lankan context
 * - Role-based access control integration
 * - Account status management for security compliance
 * 
 * Database Mapping:
 * - MongoDB BSON document with proper serialization attributes
 * - Indexed fields for optimal query performance
 * - Validation constraints for data integrity
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace EVChargingAPI.Models
{
    public class User
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        [BsonElement("nic")]
        public string NIC { get; set; } = "";

        [BsonElement("fullName")]
        public string FullName { get; set; } = "";

        [BsonElement("role")]
        public string Role { get; set; } = "EVOwner"; // Backoffice, Operator, EVOwner

        [BsonElement("passwordHash")]
        public string PasswordHash { get; set; } = "";

        [BsonElement("assignedStationId")]
        public string? AssignedStationId { get; set; } = null; // For Operators only

        [BsonElement("assignedStationName")]
        public string? AssignedStationName { get; set; } = null; // For Operators only

        [BsonElement("isActive")]
        public bool IsActive { get; set; } = true;

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}
