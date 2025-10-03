// Author: Welikanna S. T. (IT22196910)
// Purpose: Booking DB operations
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

        public async Task CreateAsync(Booking b) => await _bookings.InsertOneAsync(b);

        public async Task<Booking?> GetByIdAsync(string id) =>
            await _bookings.Find(x => x.Id == id).FirstOrDefaultAsync();

        public async Task<List<Booking>> GetByOwnerAsync(string ownerNic) =>
            await _bookings.Find(x => x.OwnerNIC == ownerNic).ToListAsync();

        public async Task<List<Booking>> GetUpcomingForStationAsync(string stationId) =>
            await _bookings.Find(x => x.StationId == stationId && 
                                     x.Status != "Cancelled" && 
                                     x.ReservationDate > DateTime.UtcNow).ToListAsync();

        public async Task UpdateAsync(string id, Booking b) =>
            await _bookings.ReplaceOneAsync(x => x.Id == id, b);

        public async Task DeleteAsync(string id) =>
            await _bookings.DeleteOneAsync(x => x.Id == id);

        public async Task<List<Booking>> GetAllAsync() => await _bookings.Find(_ => true).ToListAsync();

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

