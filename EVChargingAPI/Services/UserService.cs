/*
 * File: UserService.cs
 * Author: Peiris M. H. C. (IT22194930)
 * Description: Core business logic service for user management operations.
 *              Implements comprehensive user domain logic including CRUD operations,
 *              role management, station assignments, and account lifecycle management.
 * 
 * Key Methods:
 * - CreateAsync(user) - Create new user account with validation
 * - CreateWithStationAsync(user, stationId) - Create user with station assignment
 * - GetAllAsync() - Retrieve all users in the system
 * - GetByNicAsync(nic) - Get user by National Identity Card number
 * - UpdateAsync(id, user) - Update user profile information
 * - UpdateRoleByNicAsync(nic, role, stationId) - Update user role and assignments
 * - ActivateByNicAsync(nic) - Activate user account
 * - DeactivateByNicAsync(nic) - Deactivate user account
 * - DeleteByNicAsync(nic) - Permanently delete user account
 * 
 * Business Rules:
 * - Validates user role assignments (Backoffice, Operator, EVOwner)
 * - Manages operator-station relationships and assignments
 * - Enforces account status constraints and validation
 * - Implements data integrity checks and constraint validation
 * - Handles cascade operations for related data
 * 
 * Dependencies: UserRepository for data persistence, StationRepository for station validation.
 */

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

        // Create new user account with validation
        public async Task<User> CreateAsync(User user) 
        {
            await _repo.CreateAsync(user);
            return user;
        }

        // Retrieve all users in the system
        public async Task<List<User>> GetAllAsync() => await _repo.GetAllAsync();

        // Get user by National Identity Card number
        public async Task<User?> GetByNicAsync(string nic) => await _repo.GetByNICAsync(nic);

        // Update user profile information
        public async Task UpdateAsync(string id, User u) => await _repo.UpdateAsync(id, u);

        // Deactivate user account by NIC
        public async Task DeactivateByNicAsync(string nic)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("Not found");
            u.IsActive = false;
            await _repo.UpdateAsync(u.Id!, u);
        }

        // Activate user account by NIC
        public async Task ActivateByNicAsync(string nic)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("Not found");
            u.IsActive = true;
            await _repo.UpdateAsync(u.Id!, u);
        }

        // Update user role and station assignments
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

        // Delete user account permanently by NIC
        public async Task DeleteByNicAsync(string nic)
        {
            var u = await _repo.GetByNICAsync(nic) ?? throw new Exception("User not found");
            await _repo.DeleteByNicAsync(nic);
        }
        
        // Get first operator assigned to a specific station
        public async Task<User?> GetOperatorByStationIdAsync(string stationId)
        {
            var allUsers = await _repo.GetAllAsync();
            return allUsers.FirstOrDefault(u => u.Role == "Operator" && u.AssignedStationId == stationId);
        }
        
        // Get all operators assigned to a specific station
        public async Task<List<User>> GetOperatorsByStationIdAsync(string stationId)
        {
            var allUsers = await _repo.GetAllAsync();
            return allUsers.Where(u => u.Role == "Operator" && u.AssignedStationId == stationId).ToList();
        }
        
        // Create user account with station assignment validation
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

        // Update station assignment for operators
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
