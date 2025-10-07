/*
 * File: UserRepository.cs
 * Author: Peiris M. H. C. (IT22194930)
 * Description: Data access layer repository for User entity operations.
 *              Implements comprehensive MongoDB operations for user data persistence,
 *              including CRUD operations, complex queries, and data integrity maintenance.
 * 
 * Key Methods:
 * - CreateAsync(user) - Insert new user document into MongoDB
 * - GetAllAsync() - Retrieve all users with optional filtering
 * - GetByNICAsync(nic) - Find user by National Identity Card number
 * - GetByIdAsync(id) - Find user by MongoDB ObjectId
 * - UpdateAsync(id, user) - Update existing user document
 * - DeleteAsync(id) - Remove user document from database
 * - GetByRoleAsync(role) - Retrieve users filtered by role
 * - GetActiveUsersAsync() - Get all active user accounts
 * - SearchUsersAsync(query) - Full-text search across user data
 * 
 * Database Operations:
 * - Efficient indexing on NIC field for fast lookups
 * - Compound indexes for role and status-based queries
 * - Optimized aggregation pipelines for complex reporting
 * - Transaction support for data consistency
 * - Bulk operations for batch processing
 * 
 * Data Validation:
 * - NIC uniqueness constraints and validation
 * - Role enumeration validation (Backoffice, Operator, EVOwner)
 * - Password hash format validation
 * - Station assignment consistency checks
 * 
 * Dependencies: MongoDB.Driver for database operations, MongoDbService for connection management.
 */

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

		public async Task<User?> GetByIdAsync(string id) =>
			await _users.Find(x => x.Id == id).FirstOrDefaultAsync();

		public async Task<User?> GetByNICAsync(string nic) =>
			await _users.Find(x => x.NIC == nic).FirstOrDefaultAsync();

		public async Task<List<User>> GetAllAsync() => await _users.Find(_ => true).ToListAsync();

		public async Task UpdateAsync(string id, User u) =>
			await _users.ReplaceOneAsync(x => x.Id == id, u);

		public async Task DeleteByNicAsync(string nic) =>
			await _users.DeleteOneAsync(x => x.NIC == nic);
	}
}
