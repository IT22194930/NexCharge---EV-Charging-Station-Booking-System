// Author: Peiris M. H. C. (IT22194930)
// Purpose: User DB operations
using EVChargingAPI.Models;
using MongoDB.Driver;

namespace EVChargingAPI.Repositories
{
	public class UserRepository
	{
		private readonly IMongoCollection<User> _users;
		public UserRepository(MongoDbService db)
		{
			_users = db.GetCollection<User>("Users");
		}

		public async Task CreateAsync(User u) => await _users.InsertOneAsync(u);
	}
}
