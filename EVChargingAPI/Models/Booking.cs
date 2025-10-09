/*
 * File: Booking.cs
 * Author: Welikanna S. T. (IT22196910)
 * Description: Domain model representing an EV charging booking and its lifecycle.
 * 
 * Typical Properties:
 * - Id, OwnerNIC, StationId
 * - ReservationDate (date) and ReservationHour (slot)
 * - Status: Pending, Approved, Completed, Cancelled
 * - QR data (e.g., QRPayload or QrBase64)
 * - CreatedAtUtc, UpdatedAtUtc
 * 
 * Notes: Persisted by the repository and mapped to DTOs for API exposure.
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace EVChargingAPI.Models
{
    public class Booking
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }  // MongoDB ObjectId serialized as string for API friendliness

        [BsonElement("ownerNic")]
        public string OwnerNIC { get; set; } = "";  // Owner identity (NIC) associated with this booking

        [BsonElement("stationId")]
        public string StationId { get; set; } = ""; // Target charging station identifier

        [BsonElement("reservationDate")]
        public DateTime ReservationDate { get; set; } // Date component used for grouping/availability

        [BsonElement("reservationHour")]
        public int ReservationHour { get; set; } // Hour of the day (0-23)

        [BsonElement("status")]
        public string Status { get; set; } = "Pending"; // Pending, Approved, Cancelled, Completed, Started (intermediate)

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow; // Server-side creation timestamp (UTC)

        [BsonElement("qrBase64")]
        public string? QrBase64 { get; set; } // Base64-encoded PNG (or similar) set when approved
    }
}
