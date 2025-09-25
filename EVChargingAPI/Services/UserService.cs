// Author: Peiris M. H. C. (IT22194930)
// Purpose: User domain logic
using EVChargingAPI.Models;
using EVChargingAPI.Repositories;

namespace EVChargingAPI.Services
{
    public class UserService
    {
        private readonly UserRepository _repo;
        public UserService(UserRepository repo) { _repo = repo; }

        public async Task<User> CreateAsync(User user) 
        {
            await _repo.CreateAsync(user);
            return user;
        }
    }
}
