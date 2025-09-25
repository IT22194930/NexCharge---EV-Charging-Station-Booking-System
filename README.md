# EV Charging System

A comprehensive Electric Vehicle (EV) charging station management system built with ASP.NET Core Web API and React.js.

## 🚀 Project Overview

This system provides a complete solution for managing EV charging stations, user accounts, and booking reservations with role-based access control.

### 🎯 Key Features

#### User Management
- **Backoffice Users**: Full system administration access
- **Station Operators**: Booking approval and management
- **EV Owners**: Personal booking management

#### EV Owner Management
- Create, update, and delete EV owner profiles
- NIC-based primary key system
- Account activation/deactivation controls

#### Charging Station Management
- Support for AC/DC charging stations
- Slot availability management
- Operating hours and scheduling
- Location-based station management

#### Booking Management
- 7-day advance booking limit
- 12-hour modification constraint
- QR code generation for approved bookings
- Approval workflow system

## 🏗️ Project Structure

```
EAD-Assignment/
├── EAD Assignment.sln          # Visual Studio solution file
├── EVChargingAPI/              # Backend (ASP.NET Core Web API)
│   ├── Controllers/            # API controllers
│   ├── Models/                 # Data models
│   ├── Services/               # Business logic
│   ├── Repositories/           # Data access layer
│   ├── DTOs/                   # Data transfer objects
│   └── Program.cs              # Application entry point
└── EVChargingWeb/              # Frontend (React.js)
    ├── src/
    │   ├── components/         # Reusable components
    │   ├── pages/              # Page components
    │   ├── api/                # API integration
    │   └── App.jsx             # Main application component
    ├── package.json
    └── vite.config.js          # Vite configuration
```

## 🛠️ Technology Stack

### Backend
- **Framework**: ASP.NET Core 9.0
- **Database**: MongoDB
- **Authentication**: JWT Bearer Tokens
- **Documentation**: Swagger/OpenAPI

### Frontend
- **Framework**: React.js with Vite
- **Routing**: React Router DOM
- **Styling**: Tailwind CSS
- **HTTP Client**: Axios

## ⚙️ Setup Instructions

### Prerequisites
- .NET 9.0 SDK
- Node.js (v18 or higher)
- MongoDB (local or cloud instance)

### Backend Setup

1. **Navigate to the API directory**
   ```bash
   cd EVChargingAPI
   ```

2. **Restore dependencies**
   ```bash
   dotnet restore
   ```

3. **Update database connection**
   - Edit `appsettings.json`
   - Update the MongoDB connection string

4. **Run the API**
   ```bash
   dotnet run
   ```

The API will be available at `http://localhost:5274`

### Frontend Setup

1. **Navigate to the web directory**
   ```bash
   cd EVChargingWeb
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start the development server**
   ```bash
   npm run dev
   ```

The web application will be available at `http://localhost:5173`

## 🔐 Default User Accounts

| Role | NIC | Password | Access Level |
|------|-----|----------|-------------|
| Backoffice | 200233002002 | password | Full system administration |
| Operator | 1234 | password | Booking management |
| EVOwner | 200233002034 | password | Personal bookings |

## 📚 API Documentation

Once the backend is running, visit `http://localhost:5274/swagger` for interactive API documentation.

## 🎯 Assignment Requirements Compliance

### ✅ User Management
- [x] Create Backoffice and Station Operator users
- [x] Role-based access control
- [x] Mobile access for operators

### ✅ EV Owner Management  
- [x] CRUD operations with NIC as primary key
- [x] Account activation/deactivation
- [x] Profile management

### ✅ Charging Station Management
- [x] AC/DC station types with location and slots
- [x] Schedule management (availability)  
- [x] Deactivation with booking validation

### ✅ Booking Management
- [x] 7-day advance booking limit
- [x] 12-hour modification constraint
- [x] Reservation creation and updates
- [x] Cancellation functionality

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **IT22194930** - Peiris M. H. C. - *Initial work*

## 🆘 Support

If you encounter any issues or have questions, please open an issue in the GitHub repository.
