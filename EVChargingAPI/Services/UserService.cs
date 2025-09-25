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

        public async Task<List<User>> GetAllAsync() => await _repo.GetAllAsync();

        public async Task<User?> GetByNicAsync(string nic) => await _repo.GetByNICAsync(nic);

        public async Task UpdateAsync(string id, User u) => await _repo.UpdateAsync(id, u);

        public async Task DeactivateByNicAsync(string nic)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("Not found");
            u.IsActive = false;
            await _repo.UpdateAsync(u.Id!, u);
        }

        public async Task ActivateByNicAsync(string nic)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("Not found");
            u.IsActive = true;
            await _repo.UpdateAsync(u.Id!, u);
        }

        public async Task UpdateRoleByNicAsync(string nic, string newRole)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("User not found");
            u.Role = newRole;
            await _repo.UpdateAsync(u.Id!, u);
        }

        public async Task DeleteByNicAsync(string nic)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("User not found");
            await _repo.DeleteByNicAsync(nic);
        }
    }
}
