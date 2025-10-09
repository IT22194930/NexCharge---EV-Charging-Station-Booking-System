/*
 * File: StationRepository.cs
 * Author: Wickramasooriya W. A. A. L. (IT22126160)
 * Description: Repository class for MongoDB operations on charging station data.
 *              Handles all database interactions for Station entities using MongoDB driver.
 *              Provides data access layer abstraction for station management operations.
 * 
 * Operations:
 *   - CreateAsync: Insert new station document into MongoDB collection
 *   - GetByIdAsync: Retrieve single station by ObjectId
 *   - GetAllAsync: Fetch all stations from the collection
 *   - UpdateAsync: Replace existing station document with updated data
 *   - DeleteAsync: Remove station document from collection by ID
 */

using EVChargingAPI.Models;
using MongoDB.Driver;

namespace EVChargingAPI.Repositories
{
    public class StationRepository
    {
        private readonly IMongoCollection<Station> _stations;
        public StationRepository(MongoDbService db)
        {
            _stations = db.GetCollection<Station>("Stations");
        }

        public async Task CreateAsync(Station s) => await _stations.InsertOneAsync(s);

        public async Task<Station?> GetByIdAsync(string id) =>
            await _stations.Find(x => x.Id == id).FirstOrDefaultAsync();

        public async Task<List<Station>> GetAllAsync() =>
            await _stations.Find(_ => true).ToListAsync();

        public async Task UpdateAsync(string id, Station s) =>
            await _stations.ReplaceOneAsync(x => x.Id == id, s);

        public async Task DeleteAsync(string id) =>
            await _stations.DeleteOneAsync(x => x.Id == id);
    }
}
