// Author: Peiris M. H. C. (IT22194930)
// Purpose: Station DB operations
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
