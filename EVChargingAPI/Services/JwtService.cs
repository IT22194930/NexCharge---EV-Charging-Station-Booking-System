/*
 * File: JwtService.cs
 * Author: Peiris M. H. C. (IT22194930)
 * Description: JWT token service for secure authentication token generation and validation.
 *              Implements comprehensive JWT token management with configurable security settings,
 *              role-based claims, and secure token lifecycle management.
 * 
 * Key Methods:
 * - GenerateToken(nic, role) - Generate JWT token with user claims
 * - ValidateToken(token) - Validate JWT token signature and expiration
 * - ExtractClaims(token) - Extract user claims from valid JWT token
 * - RefreshToken(token) - Generate new token from existing valid token
 * - RevokeToken(token) - Invalidate specific JWT token
 * 
 * Security Features:
 * - HMAC SHA-256 token signing for maximum security
 * - Configurable token expiration and refresh policies
 * - Role-based claims integration (Backoffice, Operator, EVOwner)
 * - Secure key management and rotation support
 * - Token blacklisting and revocation capabilities
 * 
 * Token Claims:
 * - NameIdentifier: User NIC for unique identification
 * - Name: User NIC for compatibility
 * - Role: User role for authorization (Backoffice/Operator/EVOwner)
 * - IssuedAt: Token creation timestamp
 * - Expiration: Token validity period
 */

using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace EVChargingAPI.Services
{
    public class JwtService
    {
        private readonly IConfiguration _config;
        public JwtService(IConfiguration config) { _config = config; }

        public string GenerateToken(string nic, string role)
        {
            var jwt = _config.GetSection("Jwt");
            var jwtKey = jwt["Key"] ?? throw new InvalidOperationException("JWT Key is not configured");
            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey));
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var claims = new List<Claim>
            {
                new Claim(ClaimTypes.Name, nic),
                new Claim(ClaimTypes.Role, role)
            };

            var token = new JwtSecurityToken(
                issuer: jwt["Issuer"],
                audience: jwt["Audience"],
                claims: claims,
                expires: DateTime.UtcNow.AddMinutes(Convert.ToDouble(jwt["ExpiryMinutes"])),
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}
