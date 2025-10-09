import React, { useState, useEffect } from "react";
import api from "../api/axios";

/*
  StationDetailsModal: Shows detailed information about a single station
  Used by both BackOffice and EVOwner roles for viewing station details
*/

const StationDetailsModal = ({ visible, station, onClose }) => {
  const [operators, setOperators] = useState([]);
  const [loadingOperators, setLoadingOperators] = useState(false);

  // Fetch operators assigned to this station
  const fetchOperators = async (stationId) => {
    if (!stationId) return;
    
    setLoadingOperators(true);
    try {
      const response = await api.get(`/users/station/${stationId}/operators`);
      setOperators(response.data);
    } catch (error) {
      console.error("Error fetching operators:", error);
      setOperators([]);
    } finally {
      setLoadingOperators(false);
    }
  };

  // Fetch operators when station changes
  useEffect(() => {
    if (visible && station?.id) {
      fetchOperators(station.id);
    }
  }, [visible, station?.id]);

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
      <div className="bg-white rounded-2xl shadow-2xl max-w-6xl w-full max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-green-600 to-emerald-700 px-6 py-4 text-white">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <div className="w-10 h-10 rounded-xl bg-white bg-opacity-20 flex items-center justify-center mr-3">
                <span className="text-2xl">⚡</span>
              </div>
              <div>
                <h2 className="text-xl font-bold">Station Details</h2>
                <p className="text-green-100 text-sm">Comprehensive station information</p>
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
        <div className="p-6 max-h-[calc(90vh-200px)] overflow-y-auto">
          {/* Station Name and Status */}
          <div className="mb-6">
            <div className="flex items-start justify-between mb-1">
              <h3 className="text-2xl font-bold text-gray-900">{station.name}</h3>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(station.isActive)}`}>
                {station.isActive ? "Active" : "Inactive"}
              </span>
            </div>
            <p className="text-gray-600">{station.location}</p>
          </div>

          {/* Two Column Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left Side - Station Details (2/3 width) */}
            <div className="lg:col-span-2 space-y-6">

              {/* Key Information Grid */}
              <div className="grid grid-cols-2 gap-4">
                {/* Charging Type */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center mb-3">
                    <span className="text-xl mr-3">⚡</span>
                    <h4 className="font-medium text-gray-700">Charging Type</h4>
                  </div>
                  <div className="mb-2">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${getTypeColor(station.type)}`}>
                      {station.type} {station.type === "DC" ? "Fast" : "Standard"}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500">
                    {station.type === "DC" ? "50-350 kW" : "3-22 kW"}
                  </p>
                </div>

                {/* Available Slots */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center mb-3">
                    <span className="text-xl mr-3">🔌</span>
                    <h4 className="font-medium text-gray-700">Available Slots</h4>
                  </div>
                  <div className="flex items-center mb-2">
                    <span className="text-2xl font-bold text-green-600">{station.availableSlots}</span>
                    <span className="text-gray-500 ml-2">slots</span>
                  </div>
                  <p className="text-sm text-gray-500">
                    Max simultaneous charging
                  </p>
                </div>

                {/* Operating Hours */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center mb-3">
                    <span className="text-xl mr-3">🕒</span>
                    <h4 className="font-medium text-gray-700">Operating Hours</h4>
                  </div>
                  <div className="mb-2">
                    <span className="font-semibold text-gray-800">
                      {formatOperatingHours(station.operatingHours)}
                    </span>
                  </div>
                  {station.operatingHours?.isOpen24Hours && (
                    <span className="px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-700">
                      Always Open
                    </span>
                  )}
                </div>

                {/* Station ID */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center mb-3">
                    <span className="text-xl mr-3">🏷️</span>
                    <h4 className="font-medium text-gray-700">Station ID</h4>
                  </div>
                  <div className="font-mono text-sm text-gray-600 bg-white px-3 py-2 rounded border">
                    {station.id}
                  </div>
                </div>
              </div>

              {/* Location Details */}
              {(station.latitude && station.longitude) && (
                <div className="bg-gradient-to-r from-green-50 to-emerald-50 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center">
                      <span className="text-xl mr-3">📍</span>
                      <h4 className="font-medium text-green-800">Location Coordinates</h4>
                    </div>
                    <button 
                      onClick={() => {
                        const url = `https://www.google.com/maps?q=${station.latitude},${station.longitude}`;
                        window.open(url, '_blank');
                      }}
                      className="inline-flex items-center px-3 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg text-sm font-medium transition-colors"
                    >
                      <span className="mr-2">🗺️</span>
                      View on Google Maps
                    </button>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <span className="text-sm text-green-600 font-medium">Latitude</span>
                      <div className="font-mono text-sm text-green-800 bg-white px-3 py-2 rounded border border-green-200">
                        {station.latitude.toFixed(6)}
                      </div>
                    </div>
                    <div>
                      <span className="text-sm text-green-600 font-medium">Longitude</span>
                      <div className="font-mono text-sm text-green-800 bg-white px-3 py-2 rounded border border-green-200">
                        {station.longitude.toFixed(6)}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Additional Information */}
              <div className="bg-gray-50 rounded-lg p-4">
                <h4 className="font-medium text-gray-700 mb-3 flex items-center">
                  <span className="text-xl mr-3">ℹ️</span>
                  Additional Information
                </h4>
                <div className="space-y-2 text-sm text-gray-600">
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

            {/* Right Side - Assigned Operators (1/3 width) */}
            <div className="lg:col-span-1">
              {/* Assigned Operators */}
              <div className="bg-gradient-to-r from-green-50 to-emerald-50 rounded-lg p-4 h-fit sticky top-4">
                <h4 className="font-medium text-green-800 mb-4 flex items-center">
                  <span className="text-xl mr-3">👥</span>
                  Assigned EV Operators
                </h4>
                
                {loadingOperators ? (
                  <div className="flex flex-col items-center justify-center py-6">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600 mb-2"></div>
                    <span className="text-sm text-green-700">Loading operators...</span>
                  </div>
                ) : operators.length > 0 ? (
                  <div className="space-y-3 max-h-96 overflow-y-auto">
                    {operators.map((operator, index) => (
                      <div key={operator.nic} className="bg-white rounded-lg p-3 border border-green-200 shadow-sm">
                        <div className="flex items-start space-x-3">
                          <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0">
                            <span className="text-green-600 font-semibold">
                              {operator.fullName.charAt(0).toUpperCase()}
                            </span>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-1">
                              <p className="font-medium text-gray-900 text-sm truncate">{operator.fullName}</p>
                              <span className="text-xs text-gray-400 ml-1">#{index + 1}</span>
                            </div>
                            <p className="text-xs text-gray-500 mb-2">NIC: {operator.nic}</p>
                            <div className="flex items-center justify-between">
                              <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                                operator.isActive ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                              }`}>
                                {operator.isActive ? 'Active' : 'Inactive'}
                              </span>
                            </div>
                            {operator.assignedStationName && (
                              <div className="mt-2 text-xs text-green-600 bg-green-50 px-2 py-1 rounded">
                                Assigned to: {operator.assignedStationName}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-6">
                    <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-3">
                      <span className="text-3xl text-gray-400">👤</span>
                    </div>
                    <p className="text-sm text-gray-500 mb-1">No operators assigned</p>
                    <p className="text-xs text-gray-400">Operators can be assigned through User Management</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="bg-gray-50 px-6 py-4 rounded-b-2xl border-t border-gray-200">
          <div className="flex justify-end">
            <button
              onClick={onClose}
              className="px-6 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded-lg font-medium transition-colors"
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