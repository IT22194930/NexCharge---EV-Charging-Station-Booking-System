/*
 * File: BookingRepository.cs
 * Author: Welikanna S. T. (IT22196910)
 * Description: Repository responsible for data access operations on bookings.
 *              Encapsulates database interactions for queries and mutations.
 * 
 * Operations:
 * - GetAllAsync, GetByIdAsync
 * - CreateAsync, UpdateAsync, DeleteAsync
 * - (Optional) Availability checks and uniqueness helpers
 * 
 * Persistence: Abstracts the underlying data store (e.g., MongoDB or EF Core).
 */

using EVChargingAPI.Models;
using MongoDB.Driver;

namespace EVChargingAPI.Repositories
{
    public class BookingRepository
    {
        private readonly IMongoCollection<Booking> _bookings;
        public BookingRepository(MongoDbService db)
        {
            _bookings = db.GetCollection<Booking>("Bookings");
        }

        // Inserts a new booking document into the collection.
        public async Task CreateAsync(Booking b) => await _bookings.InsertOneAsync(b);

        // Fetches a booking by its stringified ObjectId; returns null when absent.
        public async Task<Booking?> GetByIdAsync(string id) =>
            await _bookings.Find(x => x.Id == id).FirstOrDefaultAsync();

        // Lists bookings belonging to a specific owner NIC for profile/history views.
        public async Task<List<Booking>> GetByOwnerAsync(string ownerNic) =>
            await _bookings.Find(x => x.OwnerNIC == ownerNic).ToListAsync();

        // For station dashboards: upcoming (future-dated) non-cancelled bookings for a station.
        public async Task<List<Booking>> GetUpcomingForStationAsync(string stationId) =>
            await _bookings.Find(x => x.StationId == stationId && 
                                     x.Status != "Cancelled" && 
                                     x.ReservationDate > DateTime.UtcNow).ToListAsync();

        // Replaces the entire booking document by Id (idempotent overwrite).
        public async Task UpdateAsync(string id, Booking b) =>
            await _bookings.ReplaceOneAsync(x => x.Id == id, b);

        // Deletes a booking document by Id (callers enforce business guards).
        public async Task DeleteAsync(string id) =>
            await _bookings.DeleteOneAsync(x => x.Id == id);

        // Returns all bookings (unfiltered). Callers should handle role scoping and sorting.
        public async Task<List<Booking>> GetAllAsync() => await _bookings.Find(_ => true).ToListAsync();

        // Counts bookings for station/date/hour (excluding Cancelled) to evaluate capacity.
        public async Task<int> GetBookedCountForHourAsync(string stationId, DateTime date, int hour)
        {
            var startOfDay = date.Date;
            var endOfDay = date.Date.AddDays(1);

            var count = await _bookings.CountDocumentsAsync(x =>
                x.StationId == stationId &&
                x.ReservationDate >= startOfDay &&
                x.ReservationDate < endOfDay &&
                x.ReservationHour == hour &&
                x.Status != "Cancelled");

            return (int)count;
        }

        // Retrieves all bookings for a station on a given date (excluding Cancelled).
        public async Task<List<Booking>> GetBookingsForDateAsync(string stationId, DateTime date)
        {
            var startOfDay = date.Date;
            var endOfDay = date.Date.AddDays(1);

            return await _bookings.Find(x =>
                x.StationId == stationId &&
                x.ReservationDate >= startOfDay &&
                x.ReservationDate < endOfDay &&
                x.Status != "Cancelled").ToListAsync();
        }
    }
}

