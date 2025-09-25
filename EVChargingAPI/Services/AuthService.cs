// Author: Peiris M. H. C. (IT22194930)
// Purpose: Authentication helpers
using EVChargingAPI.Models;
using EVChargingAPI.Repositories;
using BCrypt.Net;

namespace EVChargingAPI.Services
{
    public class AuthService
    {
        private readonly UserRepository _users;
        private readonly JwtService _jwt;
        public AuthService(UserRepository users, JwtService jwt)
        {
            _users = users;
            _jwt = jwt;
        }

        public async Task<string?> AuthenticateAsync(string nic, string password)
        {
            var user = await _users.GetByNICAsync(nic);
            if (user == null || !user.IsActive) return null;
            if (!BCrypt.Net.BCrypt.Verify(password, user.PasswordHash)) return null;
            return _jwt.GenerateToken(user.NIC, user.Role);
        }
    }
}
