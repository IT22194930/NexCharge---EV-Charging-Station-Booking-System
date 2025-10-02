import React from "react";
import StationDetailsModal from "./StationDetailsModal";

/*
  StationCard: A compact card component for displaying station information
  with an option to view full details in a modal
*/

const StationCard = ({ station, showDetailsButton = true }) => {
  const [showModal, setShowModal] = React.useState(false);

  const getStatusColor = (isActive) => {
    return isActive ? "text-green-700 bg-green-100" : "text-red-700 bg-red-100";
  };

  const getTypeColor = (type) => {
    return type === "DC" 
      ? "text-yellow-700 bg-yellow-100" 
      : "text-blue-700 bg-blue-100";
  };

  const formatOperatingHours = (operatingHours) => {
    if (!operatingHours) return "Not specified";
    
    if (operatingHours.isOpen24Hours) {
      return "24/7";
    }
    
    return `${operatingHours.openTime || "06:00"} - ${operatingHours.closeTime || "22:00"}`;
  };

  return (
    <>
      <div className="bg-white rounded-2xl shadow hover:shadow-md transition-shadow border border-gray-100 overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 px-4 py-3 border-b border-gray-100">
          <div className="flex items-start justify-between">
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold text-gray-900 text-lg truncate">
                {station.name}
              </h3>
              <p className="text-sm text-gray-600 mt-1 line-clamp-2">
                {station.location}
              </p>
            </div>
            <span className={`ml-3 px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(station.isActive)}`}>
              {station.isActive ? "Active" : "Inactive"}
            </span>
          </div>
        </div>

        {/* Content */}
        <div className="p-4">
          <div className="grid grid-cols-2 gap-4 mb-4">
            {/* Charging Type */}
            <div className="text-center">
              <span className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${getTypeColor(station.type)}`}>
                {station.type}
              </span>
              <p className="text-xs text-gray-500 mt-1">
                {station.type === "DC" ? "Fast Charging" : "Standard"}
              </p>
            </div>

            {/* Available Slots */}
            <div className="text-center">
              <div className="text-xl font-bold text-blue-600">
                {station.availableSlots}
              </div>
              <p className="text-xs text-gray-500">Charging Slots</p>
            </div>
          </div>

          {/* Operating Hours */}
          <div className="bg-gray-50 rounded-lg p-3 mb-4">
            <div className="flex items-center justify-center">
              <svg className="w-4 h-4 text-gray-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-sm font-medium text-gray-700">
                {formatOperatingHours(station.operatingHours)}
              </span>
            </div>
          </div>

          {/* Coordinates (if available) */}
          {station.latitude && station.longitude && (
            <div className="text-center mb-4">
              <p className="text-xs text-gray-500 font-mono">
                {station.latitude.toFixed(4)}, {station.longitude.toFixed(4)}
              </p>
            </div>
          )}

          {/* Action Button */}
          {showDetailsButton && (
            <button
              onClick={() => setShowModal(true)}
              className="w-full py-2 px-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white rounded-lg font-medium transition-colors flex items-center justify-center"
            >
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              View Details
            </button>
          )}
        </div>
      </div>

      {/* Details Modal */}
      <StationDetailsModal
        visible={showModal}
        station={station}
        onClose={() => setShowModal(false)}
      />
    </>
  );
};

export default StationCard;