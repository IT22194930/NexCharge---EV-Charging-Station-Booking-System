// Author: Welikanna S. T. (IT22196910)
// Purpose: Booking model
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace EVChargingAPI.Models
{
    public class Booking
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        [BsonElement("ownerNic")]
        public string OwnerNIC { get; set; } = "";

        [BsonElement("stationId")]
        public string StationId { get; set; } = "";

        [BsonElement("reservationDate")]
        public DateTime ReservationDate { get; set; }

        [BsonElement("reservationHour")]
        public int ReservationHour { get; set; } // Hour of the day (0-23)

        [BsonElement("status")]
    public string Status { get; set; } = "Pending"; // Pending, Approved, Cancelled, Completed

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("qrBase64")]
        public string? QrBase64 { get; set; } // set when approved
    }
}
