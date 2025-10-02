import React, { useEffect, useState, useMemo } from "react";
import {
  GoogleMap,
  Marker,
  InfoWindow,
  useLoadScript,
} from "@react-google-maps/api";
import api from "../api/axios";
import toast from "react-hot-toast";

/*
  BackOfficeStationsMap: Shows all stations for BackOffice users.
  Allows viewing station details and managing stations from the map.
*/

const libraries = []; // can add 'places' later if needed
const mapContainerStyle = {
  width: "100%",
  height: "500px",
  borderRadius: "0.75rem",
};

export default function BackOfficeStationsMap() {
  const role = localStorage.getItem("role");
  
  // Hooks must run before any early return
  const { isLoaded, loadError } = useLoadScript({
    googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
    libraries,
  });

  const [stations, setStations] = useState([]);
  const [selectedStation, setSelectedStation] = useState(null);
  const [filter, setFilter] = useState("all"); // "all", "active", "inactive"

  // Load all stations
  const loadStations = async () => {
    try {
      const res = await api.get("/stations");
      setStations(res.data || []);
    } catch (err) {
      toast.error("Failed to load stations");
      console.error("Error loading stations:", err);
    }
  };

  useEffect(() => {
    loadStations();
  }, []);

  // Filter stations based on selected filter
  const filteredStations = useMemo(() => {
    let filtered = stations.filter(s => s.latitude && s.longitude); // Only show stations with coordinates
    
    switch (filter) {
      case "active":
        filtered = filtered.filter(s => s.isActive);
        break;
      case "inactive":
        filtered = filtered.filter(s => !s.isActive);
        break;
      default:
        // Show all stations with coordinates
        break;
    }
    
    return filtered;
  }, [stations, filter]);

  // Calculate center point based on all stations
  const center = useMemo(() => {
    if (filteredStations.length === 0) {
      return { lat: 6.927079, lng: 79.861244 }; // Default Colombo
    }
    
    const avgLat = filteredStations.reduce((sum, s) => sum + s.latitude, 0) / filteredStations.length;
    const avgLng = filteredStations.reduce((sum, s) => sum + s.longitude, 0) / filteredStations.length;
    
    return { lat: avgLat, lng: avgLng };
  }, [filteredStations]);

  const toggleStationStatus = async (stationId, currentStatus) => {
    try {
      await api.put(`/stations/${stationId}/toggle-status`);
      await loadStations(); // Reload stations to get updated data
      toast.success(`Station ${currentStatus ? 'deactivated' : 'activated'} successfully!`);
      setSelectedStation(null); // Close info window
    } catch (err) {
      toast.error(`Error ${currentStatus ? 'deactivating' : 'activating'} station: ` + (err.response?.data?.message || err.message));
    }
  };

  const getStationIcon = (station) => {
    return {
      path: window.google?.maps?.SymbolPath?.CIRCLE || 0,
      scale: station.isActive ? 8 : 6,
      fillColor: station.isActive ? "#10b981" : "#ef4444", // Green for active, red for inactive
      fillOpacity: station.isActive ? 1 : 0.7,
      strokeWeight: 2,
      strokeColor: "white",
    };
  };

  if (role !== "Backoffice") return null; // Only show for back office users

  if (loadError) {
    return (
      <div className="mt-6 p-4 bg-red-50 text-red-700 rounded-lg">
        <div className="flex items-center">
          <svg className="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
          Failed to load Google Maps
        </div>
      </div>
    );
  }

  return (
    <div className="mt-10">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-md mr-3">
            <svg
              className="w-6 h-6 text-white"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
          </div>
          <div>
            <h2 className="text-2xl font-bold text-gray-900 leading-tight">
              Stations Map View
            </h2>
            <p className="text-sm text-gray-500">
              View and manage all charging stations on the map
            </p>
          </div>
        </div>

        {/* Filter Controls */}
        <div className="flex items-center space-x-2">
          <span className="text-sm font-medium text-gray-700">Filter:</span>
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="all">All Stations ({stations.filter(s => s.latitude && s.longitude).length})</option>
            <option value="active">Active ({stations.filter(s => s.isActive && s.latitude && s.longitude).length})</option>
            <option value="inactive">Inactive ({stations.filter(s => !s.isActive && s.latitude && s.longitude).length})</option>
          </select>
        </div>
      </div>

      <div className="grid lg:grid-cols-4 gap-6">
        {/* Map Section */}
        <div className="lg:col-span-3">
          <div className="bg-white rounded-2xl shadow relative overflow-hidden">
            <div className="absolute inset-0 bg-gradient-to-br from-blue-50 to-indigo-50 opacity-60 pointer-events-none" />
            <div className="p-4 relative z-10">
              {!isLoaded ? (
                <div className="h-[500px] flex items-center justify-center text-gray-500 animate-pulse">
                  <div className="text-center">
                    <svg className="w-8 h-8 mx-auto mb-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    Loading map...
                  </div>
                </div>
              ) : (
                <GoogleMap
                  mapContainerStyle={mapContainerStyle}
                  zoom={filteredStations.length > 1 ? 10 : 13}
                  center={center}
                  options={{ 
                    streetViewControl: false, 
                    mapTypeControl: false,
                    fullscreenControl: true,
                    zoomControl: true,
                  }}
                >
                  {filteredStations.map((station) => (
                    <Marker
                      key={station.id}
                      position={{ lat: station.latitude, lng: station.longitude }}
                      icon={getStationIcon(station)}
                      onClick={() => setSelectedStation(station)}
                      title={`${station.name} - ${station.isActive ? 'Active' : 'Inactive'}`}
                    />
                  ))}
                  
                  {selectedStation && (
                    <InfoWindow
                      position={{
                        lat: selectedStation.latitude,
                        lng: selectedStation.longitude,
                      }}
                      onCloseClick={() => setSelectedStation(null)}
                    >
                      <div className="max-w-[280px]">
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex-1">
                            <h3 className="font-semibold text-gray-800 leading-snug text-sm">
                              {selectedStation.name}
                            </h3>
                            <p className="text-xs text-gray-500 mt-1 line-clamp-2">
                              {selectedStation.location}
                            </p>
                          </div>
                          <span className={`ml-2 px-2 py-1 rounded-full text-xs font-medium ${
                            selectedStation.isActive 
                              ? 'bg-green-100 text-green-700' 
                              : 'bg-red-100 text-red-700'
                          }`}>
                            {selectedStation.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </div>
                        
                        <div className="grid grid-cols-2 gap-2 mb-3 text-xs">
                          <div className="bg-gray-50 rounded-md p-2">
                            <span className="text-gray-500">Type:</span>
                            <div className="font-medium text-gray-800">{selectedStation.type}</div>
                          </div>
                          <div className="bg-gray-50 rounded-md p-2">
                            <span className="text-gray-500">Slots:</span>
                            <div className="font-medium text-gray-800">{selectedStation.availableSlots}</div>
                          </div>
                        </div>
                        
                        <div className="bg-gray-50 rounded-md p-2 mb-3 text-xs">
                          <span className="text-gray-500">Coordinates:</span>
                          <div className="font-mono text-gray-800 text-xs">
                            {selectedStation.latitude.toFixed(6)}, {selectedStation.longitude.toFixed(6)}
                          </div>
                        </div>
                        
                        <div className="flex space-x-2">
                          <button
                            onClick={() => toggleStationStatus(selectedStation.id, selectedStation.isActive)}
                            className={`flex-1 px-3 py-2 rounded-lg text-xs font-medium transition-colors ${
                              selectedStation.isActive
                                ? 'bg-red-600 hover:bg-red-700 text-white'
                                : 'bg-green-600 hover:bg-green-700 text-white'
                            }`}
                          >
                            {selectedStation.isActive ? 'Deactivate' : 'Activate'}
                          </button>
                          <button
                            onClick={() => {
                              // You can add edit functionality here if needed
                              toast.info('Edit functionality can be added here');
                            }}
                            className="px-3 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-medium transition-colors"
                          >
                            View Details
                          </button>
                        </div>
                      </div>
                    </InfoWindow>
                  )}
                </GoogleMap>
              )}
            </div>
          </div>
        </div>

        {/* Stations List Sidebar */}
        <div className="space-y-4">
          <div className="bg-white rounded-2xl shadow p-5 h-full flex flex-col">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-gray-800 flex items-center">
                <svg
                  className="w-4 h-4 text-blue-600 mr-2"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                  />
                </svg>
                Station List
              </h3>
              <span className="text-xs px-2 py-1 rounded-full bg-blue-50 text-blue-700 font-medium">
                {filteredStations.length} stations
              </span>
            </div>
            
            <div
              className="space-y-3 overflow-y-auto pr-1 custom-scrollbar flex-1"
              style={{ maxHeight: "450px" }}
            >
              {filteredStations.map((station, idx) => (
                <div
                  key={station.id}
                  className={`group border rounded-xl p-3 transition-all cursor-pointer ${
                    selectedStation?.id === station.id
                      ? 'border-blue-300 bg-blue-50'
                      : 'border-gray-100 hover:border-blue-300 bg-white hover:shadow-sm'
                  }`}
                  onClick={() => setSelectedStation(station)}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center mb-2">
                        <span className={`inline-flex items-center justify-center w-5 h-5 text-xs font-semibold rounded-full mr-2 ${
                          station.isActive 
                            ? 'bg-green-100 text-green-700' 
                            : 'bg-red-100 text-red-700'
                        }`}>
                          {idx + 1}
                        </span>
                        <p className="font-medium text-sm text-gray-900 truncate">
                          {station.name}
                        </p>
                      </div>
                      
                      <p className="text-xs text-gray-500 mb-2 line-clamp-1">
                        {station.location}
                      </p>
                      
                      <div className="flex flex-wrap items-center gap-1 text-xs">
                        <span className={`inline-flex items-center px-2 py-0.5 rounded font-medium ${
                          station.type === 'DC' 
                            ? 'bg-yellow-100 text-yellow-700' 
                            : 'bg-blue-100 text-blue-700'
                        }`}>
                          {station.type}
                        </span>
                        <span className="inline-flex items-center px-2 py-0.5 rounded bg-gray-100 text-gray-700 font-medium">
                          {station.availableSlots} slots
                        </span>
                        <span className={`inline-flex items-center px-2 py-0.5 rounded font-medium ${
                          station.isActive 
                            ? 'bg-green-100 text-green-700' 
                            : 'bg-red-100 text-red-700'
                        }`}>
                          {station.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
              
              {filteredStations.length === 0 && (
                <div className="text-center py-8">
                  <svg className="w-12 h-12 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <p className="text-sm text-gray-500 mb-1">No stations found</p>
                  <p className="text-xs text-gray-400">
                    {filter === "all" 
                      ? "No stations with coordinates available" 
                      : `No ${filter} stations with coordinates available`
                    }
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}