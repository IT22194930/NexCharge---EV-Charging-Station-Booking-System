# EVChargingAPI

A .NET 9 Web API for managing EV charging stations, bookings, and users, using MongoDB and JWT authentication.

## Prerequisites
- [.NET 9 SDK](https://dotnet.microsoft.com/download)
- [MongoDB](https://www.mongodb.com/try/download/community) (local or cloud instance)

## Setup
1. **Clone the repository:**
   ```sh
   git clone https://github.com/IT22194930/EVChargingAPI
   cd EVChargingAPI
   ```
2. **Restore dependencies:**
   ```sh
   dotnet restore
   ```

3. **Build and run the API:**
   ```sh
   cd EVChargingAPI
   dotnet run
   ```
   The API will start (by default on `https://localhost:5274` or `http://localhost:5000`).

4. **API Documentation:**
   - Open your browser at [https://localhost:5274/swagger](https://localhost:5000/swagger) to view and test the API endpoints using Swagger UI.

