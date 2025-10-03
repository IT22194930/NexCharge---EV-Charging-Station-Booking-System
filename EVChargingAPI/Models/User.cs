// Author: Peiris M. H. C. (IT22194930)
// Purpose: User model representing Backoffice, Operator, EVOwner
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
