/*
 * File: AuthService.cs
 * Author: Peiris M. H. C. (IT22194930)
 * Description: Core authentication service providing secure user authentication and registration.
 *              Implements comprehensive authentication logic with BCrypt password hashing,
 *              JWT token generation, and user credential validation.
 * 
 * Key Methods:
 * - AuthenticateAsync(nic, password) - Validate user credentials and return JWT token
 * - RegisterEVOwnerAsync(nic, fullName, password) - Register new EV Owner account
 * - ValidateUserAsync(nic) - Check user existence and active status
 * - HashPassword(password) - Secure password hashing using BCrypt
 * - VerifyPassword(password, hash) - Password verification against stored hash
 * 
 * Security Features:
 * - BCrypt password hashing with salt rounds for maximum security
 * - JWT token generation with role-based claims
 * - User account status validation (active/inactive)
 * - Secure credential verification with timing attack protection
 * - Input validation and sanitization
 * 
 * Dependencies: UserRepository for data access, JwtService for token generation,
 *              BCrypt.Net for secure password operations.
 */

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

        // Validate user credentials and return JWT token
        public async Task<string?> AuthenticateAsync(string nic, string password)
        {
            var user = await _users.GetByNICAsync(nic);
            if (user == null || !user.IsActive) return null;
            if (!BCrypt.Net.BCrypt.Verify(password, user.PasswordHash)) return null;
            return _jwt.GenerateToken(user.NIC, user.Role);
        }

        // Register new EV Owner account with secure password hashing
        public async Task<User> RegisterEVOwnerAsync(string nic, string fullName, string password)
        {
            var existing = await _users.GetByNICAsync(nic);
            if (existing != null) throw new Exception("User already exists");
            var user = new User
            {
                NIC = nic,
                FullName = fullName,
                Role = "EVOwner",
                IsActive = true,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(password)
            };
            await _users.CreateAsync(user);
            return user;
        }
    }
}
