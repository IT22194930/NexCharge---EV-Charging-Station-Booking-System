import React from "react";

/*
  StationDetailsModal: Shows detailed information about a single station
  Used by both BackOffice and EVOwner roles for viewing station details
*/

const StationDetailsModal = ({ visible, station, onClose }) => {
  if (!visible || !station) return null;

  const formatOperatingHours = (operatingHours) => {
    if (!operatingHours) return "Not specified";
    
    if (operatingHours.isOpen24Hours) {
      return "24/7 - Open all day";
    }
    
    return `${operatingHours.openTime || "06:00"} - ${operatingHours.closeTime || "22:00"}`;
  };

  const getStatusColor = (isActive) => {
    return isActive ? "text-green-700 bg-green-100" : "text-red-700 bg-red-100";
  };

  const getTypeColor = (type) => {
    return type === "DC" 
      ? "text-yellow-700 bg-yellow-100" 
      : "text-blue-700 bg-blue-100";
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[85vh] overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-6 py-4 text-white">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <div className="w-10 h-10 rounded-xl bg-white bg-opacity-20 flex items-center justify-center mr-3">
                <span className="text-2xl">⚡</span>
              </div>
              <div>
                <h2 className="text-xl font-bold">Station Details</h2>
                <p className="text-blue-100 text-sm">Comprehensive station information</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="w-8 h-8 rounded-full bg-white bg-opacity-20 hover:bg-opacity-30 flex items-center justify-center transition-colors"
            >
              <span className="text-black text-lg font-bold">✕</span>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="p-4">
          {/* Station Name and Status */}
          <div className="mb-4">
            <div className="flex items-start justify-between mb-1">
              <h3 className="text-xl font-bold text-gray-900">{station.name}</h3>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(station.isActive)}`}>
                {station.isActive ? "Active" : "Inactive"}
              </span>
            </div>
            <p className="text-gray-600 text-sm">{station.location}</p>
          </div>

          {/* Key Information Grid */}
          <div className="grid grid-cols-2 gap-3 mb-4">
            {/* Charging Type */}
            <div className="bg-gray-50 rounded-lg p-3">
              <div className="flex items-center mb-2">
                <span className="text-lg mr-2">⚡</span>
                <h4 className="font-medium text-gray-700 text-sm">Charging Type</h4>
              </div>
              <div className="mb-1">
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getTypeColor(station.type)}`}>
                  {station.type} {station.type === "DC" ? "Fast" : "Standard"}
                </span>
              </div>
              <p className="text-xs text-gray-500">
                {station.type === "DC" ? "50-350 kW" : "3-22 kW"}
              </p>
            </div>

            {/* Available Slots */}
            <div className="bg-gray-50 rounded-lg p-3">
              <div className="flex items-center mb-2">
                <span className="text-lg mr-2">🔌</span>
                <h4 className="font-medium text-gray-700 text-sm">Available Slots</h4>
              </div>
              <div className="flex items-center mb-1">
                <span className="text-xl font-bold text-blue-600">{station.availableSlots}</span>
                <span className="text-gray-500 ml-2 text-sm">slots</span>
              </div>
              <p className="text-xs text-gray-500">
                Max simultaneous charging
              </p>
            </div>

            {/* Operating Hours */}
            <div className="bg-gray-50 rounded-lg p-3">
              <div className="flex items-center mb-2">
                <span className="text-lg mr-2">🕒</span>
                <h4 className="font-medium text-gray-700 text-sm">Operating Hours</h4>
              </div>
              <div className="mb-1">
                <span className="font-semibold text-gray-800 text-sm">
                  {formatOperatingHours(station.operatingHours)}
                </span>
              </div>
              {station.operatingHours?.isOpen24Hours && (
                <span className="px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">
                  Always Open
                </span>
              )}
            </div>

            {/* Station ID */}
            <div className="bg-gray-50 rounded-lg p-3">
              <div className="flex items-center mb-2">
                <span className="text-lg mr-2">🏷️</span>
                <h4 className="font-medium text-gray-700 text-sm">Station ID</h4>
              </div>
              <div className="font-mono text-xs text-gray-600 bg-white px-2 py-1 rounded border">
                {station.id}
              </div>
            </div>
          </div>

          {/* Location Details */}
          {(station.latitude && station.longitude) && (
            <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-3 mb-4">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center">
                  <span className="text-lg mr-2">📍</span>
                  <h4 className="font-medium text-blue-800 text-sm">Location Coordinates</h4>
                </div>
                <button 
                  onClick={() => {
                    const url = `https://www.google.com/maps?q=${station.latitude},${station.longitude}`;
                    window.open(url, '_blank');
                  }}
                  className="inline-flex items-center px-2 py-1 bg-blue-600 hover:bg-blue-700 text-white rounded text-xs font-medium transition-colors"
                >
                  <span className="mr-1">🗺️</span>
                  View on Google Maps
                </button>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <span className="text-xs text-blue-600 font-medium">Latitude</span>
                  <div className="font-mono text-xs text-blue-800 bg-white px-2 py-1 rounded border border-blue-200">
                    {station.latitude.toFixed(6)}
                  </div>
                </div>
                <div>
                  <span className="text-xs text-blue-600 font-medium">Longitude</span>
                  <div className="font-mono text-xs text-blue-800 bg-white px-2 py-1 rounded border border-blue-200">
                    {station.longitude.toFixed(6)}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Additional Information */}
          <div className="bg-gray-50 rounded-lg p-3">
            <h4 className="font-medium text-gray-700 mb-2 flex items-center text-sm">
              <span className="text-lg mr-2">ℹ️</span>
              Additional Information
            </h4>
            <div className="space-y-1 text-xs text-gray-600">
              <div className="flex justify-between">
                <span>Station Status:</span>
                <span className={`font-medium ${station.isActive ? 'text-green-600' : 'text-red-600'}`}>
                  {station.isActive ? 'Currently Active' : 'Currently Inactive'}
                </span>
              </div>
              <div className="flex justify-between">
                <span>Charging Standard:</span>
                <span className="font-medium">
                  {station.type === 'DC' ? 'CCS/CHAdeMO' : 'Type 2/CCS'}
                </span>
              </div>
              <div className="flex justify-between">
                <span>Power Rating:</span>
                <span className="font-medium">
                  {station.type === 'DC' ? '50-350 kW' : '3-22 kW'}
                </span>
              </div>
              <div className="flex justify-between">
                <span>Typical Charging Time:</span>
                <span className="font-medium">
                  {station.type === 'DC' ? '20-45 minutes' : '2-8 hours'}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="bg-gray-50 px-4 py-3 rounded-b-2xl">
          <div className="flex justify-end">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded-lg font-medium transition-colors text-sm"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StationDetailsModal;