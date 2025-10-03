// Author: Peiris M. H. C. (IT22194930)
// Purpose: User domain logic
using EVChargingAPI.Models;
using EVChargingAPI.Repositories;

namespace EVChargingAPI.Services
{
    public class UserService
    {
        private readonly UserRepository _repo;
        private readonly StationRepository _stationRepo;
        public UserService(UserRepository repo, StationRepository stationRepo) 
        { 
            _repo = repo; 
            _stationRepo = stationRepo;
        }

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

        public async Task UpdateRoleByNicAsync(string nic, string newRole, string? assignedStationId = null)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("User not found");
            u.Role = newRole;
            
            // Handle station assignment for Operators
            if (newRole == "Operator")
            {
                if (string.IsNullOrEmpty(assignedStationId))
                {
                    throw new Exception("Station assignment is required for Operators");
                }
                
                // Check if station exists
                var station = await _stationRepo.GetByIdAsync(assignedStationId);
                if (station == null)
                {
                    throw new Exception("Assigned station not found");
                }
                
                // Multiple operators can be assigned to the same station
                // No need to check for existing operators
                
                u.AssignedStationId = assignedStationId;
                u.AssignedStationName = station.Name;
            }
            else
            {
                // Clear station assignment for non-operators
                u.AssignedStationId = null;
                u.AssignedStationName = null;
            }
            
            await _repo.UpdateAsync(u.Id!, u);
        }

        public async Task DeleteByNicAsync(string nic)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("User not found");
            await _repo.DeleteByNicAsync(nic);
        }
        
        public async Task<User?> GetOperatorByStationIdAsync(string stationId)
        {
            var allUsers = await _repo.GetAllAsync();
            return allUsers.FirstOrDefault(u => u.Role == "Operator" && u.AssignedStationId == stationId);
        }
        
        public async Task<User> CreateWithStationAsync(User user, string? assignedStationId = null)
        {
            // Handle station assignment for Operators
            if (user.Role == "Operator")
            {
                if (string.IsNullOrEmpty(assignedStationId))
                {
                    throw new Exception("Station assignment is required for Operators");
                }
                
                // Check if station exists
                var station = await _stationRepo.GetByIdAsync(assignedStationId);
                if (station == null)
                {
                    throw new Exception("Assigned station not found");
                }
                
                // Multiple operators can be assigned to the same station
                // No need to check for existing operators
                
                user.AssignedStationId = assignedStationId;
                user.AssignedStationName = station.Name;
            }
            
            await _repo.CreateAsync(user);
            return user;
        }

        public async Task UpdateStationAssignmentByNicAsync(string nic, string? assignedStationId)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("User not found");
            
            if (u.Role != "Operator")
            {
                throw new Exception("Only operators can have station assignments");
            }

            if (string.IsNullOrEmpty(assignedStationId))
            {
                // Clear station assignment
                u.AssignedStationId = null;
                u.AssignedStationName = null;
            }
            else
            {
                // Check if station exists
                var station = await _stationRepo.GetByIdAsync(assignedStationId);
                if (station == null)
                {
                    throw new Exception("Assigned station not found");
                }
                
                // Multiple operators can be assigned to the same station
                // No need to check for existing operators
                
                u.AssignedStationId = assignedStationId;
                u.AssignedStationName = station.Name;
            }
            
            await _repo.UpdateAsync(u.Id!, u);
        }
    }
}
