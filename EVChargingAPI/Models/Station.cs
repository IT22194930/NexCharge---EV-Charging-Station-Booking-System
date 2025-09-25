// Author: Wickramasooriya W. A. A. L. (IT22126160)
// Purpose: Charging station model
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
