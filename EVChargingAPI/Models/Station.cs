/*
 * File: Station.cs
 * Author: Wickramasooriya W. A. A. L. (IT22126160)
 * Description: MongoDB model class for EV charging stations. Represents charging station
 *              entities with location data, operational details, and availability information.
 *              Used for persisting station data in MongoDB database.
 * 
 * User Roles: This model is accessed by:
 *   - Backoffice: Full CRUD operations for station management
 *   - Operators: Read access for operational monitoring
 *   - Customers: Read-only access for station discovery and booking
 *   - Public API: Anonymous read access for station information
 * 
 * Typical Properties:
 *   - Id: MongoDB ObjectId for unique identification
 *   - Name: Display name of the charging station
 *   - Location: Textual address or location description
 *   - Latitude/Longitude: GPS coordinates for mapping (optional)
 *   - Type: Charging type (AC/DC) indicating power delivery method
 *   - AvailableSlots: Number of charging ports available
 *   - IsActive: Operational status flag for enabling/disabling
 *   - OperatingHours: Flexible object for storing schedule information
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace EVChargingAPI.Models
{
    public class Station
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        [BsonElement("name")]
        public string Name { get; set; } = "";

        [BsonElement("location")]
        public string Location { get; set; } = ""; // textual address

        [BsonElement("lat")]
        public double? Latitude { get; set; }

        [BsonElement("lng")]
        public double? Longitude { get; set; }

        [BsonElement("type")]
        public string Type { get; set; } = "AC"; // AC or DC

        [BsonElement("availableSlots")]
        public int AvailableSlots { get; set; } = 1;

        [BsonElement("isActive")]
        public bool IsActive { get; set; } = true;

        [BsonElement("operatingHours")]
        public object? OperatingHours { get; set; }
    }
}
