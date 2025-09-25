// Author: Peiris M. H. C. (IT22194930)
// Purpose: Configure and provide MongoDB database and collections
using Microsoft.Extensions.Configuration;
using MongoDB.Driver;

namespace EVChargingAPI.Repositories
{
    public class MongoDbService
    {
        private readonly IMongoDatabase _db;
        public MongoDbService(IConfiguration config)
        {
            var cs = config.GetSection("MongoDB")["ConnectionString"];
            var dbName = config.GetSection("MongoDB")["DatabaseName"];
            var client = new MongoClient(cs);
            _db = client.GetDatabase(dbName);
        }

        public IMongoCollection<T> GetCollection<T>(string name) =>
            _db.GetCollection<T>(name);
    }
}
