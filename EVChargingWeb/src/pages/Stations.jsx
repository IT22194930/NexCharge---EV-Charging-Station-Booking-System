import { useEffect, useState } from "react";
import api from "../api/axios";
import toast from "react-hot-toast";

export default function Stations() {
  const [stations, setStations] = useState([]);
  const [error, setError] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showUpdateForm, setShowUpdateForm] = useState(false);
  const [showScheduleForm, setShowScheduleForm] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [currentStation, setCurrentStation] = useState(null);
  const [stationToDelete, setStationToDelete] = useState(null);
  const [form, setForm] = useState({
    name: "",
    location: "",
    type: "AC",
    availableSlots: 1,
    operatingHours: {
      openTime: "06:00",
      closeTime: "22:00",
      isOpen24Hours: false
    }
  });

  const loadStations = async () => {
    try {
      const res = await api.get("/stations");
      setStations(res.data);
    } catch (err) {
      setError("Failed to load stations");
      console.error(err);
    }
  };

  const createStation = async (e) => {
    e.preventDefault();
    try {
      await api.post("/stations", form);
      resetForm();
      setShowCreateForm(false);
      loadStations();
      toast.success("Station created successfully!");
    } catch (err) {
      toast.error("Error creating station: " + (err.response?.data?.message || err.message));
    }
  };

  const updateStation = async (e) => {
    e.preventDefault();
    try {
      await api.put(`/stations/${currentStation.id}`, form);
      resetForm();
      setShowUpdateForm(false);
      setCurrentStation(null);
      loadStations();
      toast.success("Station updated successfully!");
    } catch (err) {
      toast.error("Error updating station: " + (err.response?.data?.message || err.message));
    }
  };

  const updateSchedule = async (e) => {
    e.preventDefault();
    try {
      await api.put(`/stations/${currentStation.id}/schedule`, form.operatingHours);
      resetForm();
      setShowScheduleForm(false);
      setCurrentStation(null);
      loadStations();
      toast.success("Station schedule updated successfully!");
    } catch (err) {
      toast.error("Error updating schedule: " + (err.response?.data?.message || err.message));
    }
  };

  const deleteStation = async (id, stationName) => {
    setStationToDelete({ id, name: stationName });
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    if (!stationToDelete) return;
    
    try {
      await api.delete(`/stations/${stationToDelete.id}`);
      loadStations();
      toast.success("Station deleted successfully!");
      setShowDeleteConfirm(false);
      setStationToDelete(null);
    } catch (err) {
      toast.error("Error deleting station: " + (err.response?.data?.message || err.message));
    }
  };

  const cancelDelete = () => {
    setShowDeleteConfirm(false);
    setStationToDelete(null);
  };

  const handleDeleteBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      setShowDeleteConfirm(false);
      setStationToDelete(null);
    }
  };

  const toggleStationStatus = async (id, isActive) => {
    try {
      const endpoint = isActive ? "deactivate" : "activate";
      await api.post(`/stations/${endpoint}/${id}`);
      loadStations();
    } catch (err) {
      toast.error(`Error ${isActive ? "deactivating" : "activating"} station: ` + (err.response?.data?.message || err.message));
    }
  };

  const openUpdateForm = (station) => {
    setCurrentStation(station);
    setForm({
      name: station.name,
      location: station.location,
      type: station.type,
      availableSlots: station.availableSlots,
      operatingHours: station.operatingHours || {
        openTime: "06:00",
        closeTime: "22:00",
        isOpen24Hours: false
      }
    });
    setShowUpdateForm(true);
  };

  const openScheduleForm = (station) => {
    setCurrentStation(station);
    setForm({
      ...form,
      operatingHours: station.operatingHours || {
        openTime: "06:00",
        closeTime: "22:00",
        isOpen24Hours: false
      }
    });
    setShowScheduleForm(true);
  };

  const resetForm = () => {
    setForm({
      name: "",
      location: "",
      type: "AC",
      availableSlots: 1,
      operatingHours: {
        openTime: "06:00",
        closeTime: "22:00",
        isOpen24Hours: false
      }
    });
  };

  const closeModals = () => {
    setShowCreateForm(false);
    setShowUpdateForm(false);
    setShowScheduleForm(false);
    setCurrentStation(null);
    resetForm();
  };

  useEffect(() => {
    loadStations();
  }, []);

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Charging Station Management</h1>
        <button
          onClick={() => setShowCreateForm(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          Create New Station
        </button>
      </div>

      {error && <p className="text-red-500 mb-4">{error}</p>}

      {/* Create Station Modal */}
      {showCreateForm && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[95vh] flex flex-col transform transition-all">
            {/* Header */}
            <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4 rounded-t-2xl flex-shrink-0">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="p-2 bg-white/20 rounded-lg">
                    <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-white">Create New Station</h2>
                    <p className="text-blue-100 text-xs">Add a new charging station to your network</p>
                  </div>
                </div>
                <button
                  onClick={closeModals}
                  className="p-2 hover:bg-white/20 rounded-lg transition-colors"
                >
                  <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>

            {/* Form Content */}
            <div className="flex-1 overflow-y-auto">
              <form onSubmit={createStation} className="p-4 space-y-3">
              {/* Station Name */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                  Station Name
                </label>
                <input
                  type="text"
                  placeholder="Enter station name (e.g., Downtown Charging Hub)"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                  required
                />
              </div>

              {/* Location */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  Location
                </label>
                <input
                  type="text"
                  placeholder="Enter full address or location details"
                  value={form.location}
                  onChange={(e) => setForm({ ...form, location: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                  required
                />
              </div>

              {/* Charging Type */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                  Charging Type
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <label className={`relative flex items-center p-4 border-2 rounded-xl cursor-pointer transition-all duration-200 ${
                    form.type === 'AC' 
                      ? 'border-blue-500 bg-blue-50 text-blue-700' 
                      : 'border-gray-300 hover:border-gray-400 bg-white'
                  }`}>
                    <input
                      type="radio"
                      name="type"
                      value="AC"
                      checked={form.type === 'AC'}
                      onChange={(e) => setForm({ ...form, type: e.target.value })}
                      className="sr-only"
                    />
                    <div className="flex-1 text-center">
                      <div className="font-medium">AC Charging</div>
                      <div className="text-xs text-gray-500">Standard Speed</div>
                    </div>
                    {form.type === 'AC' && (
                      <svg className="w-5 h-5 text-blue-500 absolute top-2 right-2" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                      </svg>
                    )}
                  </label>
                  <label className={`relative flex items-center p-4 border-2 rounded-xl cursor-pointer transition-all duration-200 ${
                    form.type === 'DC' 
                      ? 'border-yellow-500 bg-yellow-50 text-yellow-700' 
                      : 'border-gray-300 hover:border-gray-400 bg-white'
                  }`}>
                    <input
                      type="radio"
                      name="type"
                      value="DC"
                      checked={form.type === 'DC'}
                      onChange={(e) => setForm({ ...form, type: e.target.value })}
                      className="sr-only"
                    />
                    <div className="flex-1 text-center">
                      <div className="font-medium">DC Fast Charging</div>
                      <div className="text-xs text-gray-500">High Speed</div>
                    </div>
                    {form.type === 'DC' && (
                      <svg className="w-5 h-5 text-yellow-500 absolute top-2 right-2" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                      </svg>
                    )}
                  </label>
                </div>
              </div>

              {/* Available Slots */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                  Available Charging Slots
                </label>
                <input
                  type="number"
                  placeholder="Number of charging slots"
                  min="1"
                  max="20"
                  value={form.availableSlots}
                  onChange={(e) => setForm({ ...form, availableSlots: parseInt(e.target.value) })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                  required
                />
              </div>

              {/* Operating Hours */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  Operating Hours
                </label>
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-3 space-y-3">
                  <label className="flex items-center group cursor-pointer">
                    <div className="relative">
                      <input
                        type="checkbox"
                        checked={form.operatingHours.isOpen24Hours}
                        onChange={(e) => setForm({
                          ...form,
                          operatingHours: { ...form.operatingHours, isOpen24Hours: e.target.checked }
                        })}
                        className="sr-only"
                      />
                      <div className={`w-5 h-5 border-2 rounded-md transition-all duration-200 flex items-center justify-center ${
                        form.operatingHours.isOpen24Hours 
                          ? 'bg-blue-500 border-blue-500' 
                          : 'border-gray-300 group-hover:border-gray-400'
                      }`}>
                        {form.operatingHours.isOpen24Hours && (
                          <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                          </svg>
                        )}
                      </div>
                    </div>
                    <span className="ml-3 font-medium text-gray-700">Open 24 Hours</span>
                    <div className="ml-2 px-2 py-1 bg-green-100 text-green-700 text-xs rounded-full">Recommended</div>
                  </label>
                  
                  {!form.operatingHours.isOpen24Hours && (
                    <div className="grid grid-cols-2 gap-3 pt-1">
                      <div>
                        <label className="text-xs font-medium text-gray-600 mb-1 block">Opening Time</label>
                        <input
                          type="time"
                          value={form.operatingHours.openTime}
                          onChange={(e) => setForm({
                            ...form,
                            operatingHours: { ...form.operatingHours, openTime: e.target.value }
                          })}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-600 mb-1 block">Closing Time</label>
                        <input
                          type="time"
                          value={form.operatingHours.closeTime}
                          onChange={(e) => setForm({
                            ...form,
                            operatingHours: { ...form.operatingHours, closeTime: e.target.value }
                          })}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </form>
            </div>
            
            {/* Action Buttons Fixed */}
            <div className="flex-shrink-0 px-4 pb-4">
              <div className="flex space-x-3">
                <button
                  onClick={createStation}
                  className="flex-1 bg-gradient-to-r from-green-600 to-green-700 text-white py-2.5 px-4 rounded-xl font-medium hover:from-green-700 hover:to-green-800 focus:ring-4 focus:ring-green-300 transition-all duration-200 flex items-center justify-center space-x-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Create Station</span>
                </button>
                <button
                  type="button"
                  onClick={closeModals}
                  className="flex-1 bg-gray-100 text-gray-700 py-2.5 px-4 rounded-xl font-medium hover:bg-gray-200 focus:ring-4 focus:ring-gray-300 transition-all duration-200 flex items-center justify-center space-x-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                  <span>Cancel</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Update Station Modal */}
      {showUpdateForm && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto transform transition-all">
            {/* Header */}
            <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-8 py-6 rounded-t-2xl">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="p-2 bg-white/20 rounded-lg">
                    <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-white">Update Station</h2>
                    <p className="text-blue-100 text-sm">Modify station details and settings</p>
                  </div>
                </div>
                <button
                  onClick={closeModals}
                  className="p-2 hover:bg-white/20 rounded-lg transition-colors"
                >
                  <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>

            {/* Form Content */}
            <form onSubmit={updateStation} className="p-8 space-y-6">
              {/* Station Name */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                  Station Name
                </label>
                <input
                  type="text"
                  placeholder="Enter station name"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                  required
                />
              </div>

              {/* Location */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  Location
                </label>
                <input
                  type="text"
                  placeholder="Enter full address"
                  value={form.location}
                  onChange={(e) => setForm({ ...form, location: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                  required
                />
              </div>

              {/* Charging Type */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                  Charging Type
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <label className={`relative flex items-center p-4 border-2 rounded-xl cursor-pointer transition-all duration-200 ${
                    form.type === 'AC' 
                      ? 'border-blue-500 bg-blue-50 text-blue-700' 
                      : 'border-gray-300 hover:border-gray-400 bg-white'
                  }`}>
                    <input
                      type="radio"
                      name="type"
                      value="AC"
                      checked={form.type === 'AC'}
                      onChange={(e) => setForm({ ...form, type: e.target.value })}
                      className="sr-only"
                    />
                    <div className="flex-1 text-center">
                      <div className="font-medium">AC Charging</div>
                      <div className="text-xs text-gray-500">Standard Speed</div>
                    </div>
                    {form.type === 'AC' && (
                      <svg className="w-5 h-5 text-blue-500 absolute top-2 right-2" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                      </svg>
                    )}
                  </label>
                  <label className={`relative flex items-center p-4 border-2 rounded-xl cursor-pointer transition-all duration-200 ${
                    form.type === 'DC' 
                      ? 'border-yellow-500 bg-yellow-50 text-yellow-700' 
                      : 'border-gray-300 hover:border-gray-400 bg-white'
                  }`}>
                    <input
                      type="radio"
                      name="type"
                      value="DC"
                      checked={form.type === 'DC'}
                      onChange={(e) => setForm({ ...form, type: e.target.value })}
                      className="sr-only"
                    />
                    <div className="flex-1 text-center">
                      <div className="font-medium">DC Fast Charging</div>
                      <div className="text-xs text-gray-500">High Speed</div>
                    </div>
                    {form.type === 'DC' && (
                      <svg className="w-5 h-5 text-yellow-500 absolute top-2 right-2" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                      </svg>
                    )}
                  </label>
                </div>
              </div>

              {/* Available Slots */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-2">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                  Available Charging Slots
                </label>
                <input
                  type="number"
                  placeholder="Number of charging slots"
                  min="1"
                  max="20"
                  value={form.availableSlots}
                  onChange={(e) => setForm({ ...form, availableSlots: parseInt(e.target.value) })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 hover:border-gray-400"
                  required
                />
              </div>

              {/* Action Buttons */}
              <div className="flex space-x-4 pt-4">
                <button
                  type="submit"
                  className="flex-1 bg-gradient-to-r from-blue-600 to-blue-700 text-white py-3 px-6 rounded-xl font-medium hover:from-blue-700 hover:to-blue-800 focus:ring-4 focus:ring-blue-300 transition-all duration-200 flex items-center justify-center space-x-2"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Update Station</span>
                </button>
                <button
                  type="button"
                  onClick={closeModals}
                  className="flex-1 bg-gray-100 text-gray-700 py-3 px-6 rounded-xl font-medium hover:bg-gray-200 focus:ring-4 focus:ring-gray-300 transition-all duration-200 flex items-center justify-center space-x-2"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                  <span>Cancel</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Update Schedule Modal */}
      {showScheduleForm && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md transform transition-all">
            {/* Header */}
            <div className="bg-gradient-to-r from-purple-600 to-purple-700 px-8 py-6 rounded-t-2xl">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="p-2 bg-white/20 rounded-lg">
                    <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-white">Update Schedule</h2>
                    <p className="text-purple-100 text-sm">Set operating hours for this station</p>
                  </div>
                </div>
                <button
                  onClick={closeModals}
                  className="p-2 hover:bg-white/20 rounded-lg transition-colors"
                >
                  <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>

            {/* Form Content */}
            <form onSubmit={updateSchedule} className="p-8 space-y-6">
              {/* Operating Hours */}
              <div className="group">
                <label className="flex items-center text-sm font-medium text-gray-700 mb-4">
                  <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  Operating Hours Configuration
                </label>
                
                <div className="bg-gradient-to-br from-gray-50 to-gray-100 border border-gray-200 rounded-2xl p-6 space-y-6">
                  {/* 24/7 Toggle */}
                  <div className="flex items-center justify-between p-4 bg-white rounded-xl border border-gray-200 shadow-sm">
                    <div className="flex items-center space-x-3">
                      <div className="p-2 bg-green-100 rounded-lg">
                        <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                        </svg>
                      </div>
                      <div>
                        <div className="font-medium text-gray-900">24/7 Operations</div>
                        <div className="text-sm text-gray-500">Station available around the clock</div>
                      </div>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        checked={form.operatingHours.isOpen24Hours}
                        onChange={(e) => setForm({
                          ...form,
                          operatingHours: { ...form.operatingHours, isOpen24Hours: e.target.checked }
                        })}
                        className="sr-only peer"
                      />
                      <div className="relative w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-purple-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-purple-600"></div>
                    </label>
                  </div>
                  
                  {/* Time Inputs */}
                  {!form.operatingHours.isOpen24Hours && (
                    <div className="space-y-4">
                      <div className="text-center">
                        <div className="inline-flex items-center px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm font-medium">
                          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                          Custom Hours
                        </div>
                      </div>
                      
                      <div className="grid grid-cols-2 gap-6">
                        <div className="space-y-2">
                          <label className="text-sm font-medium text-gray-700 flex items-center">
                            <svg className="w-4 h-4 mr-2 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707" />
                            </svg>
                            Opening Time
                          </label>
                          <input
                            type="time"
                            value={form.operatingHours.openTime}
                            onChange={(e) => setForm({
                              ...form,
                              operatingHours: { ...form.operatingHours, openTime: e.target.value }
                            })}
                            className="w-auto px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-purple-500 focus:border-transparent text-center font-mono text-lg"
                          />
                        </div>
                        
                        <div className="space-y-2">
                          <label className="text-sm font-medium text-gray-700 flex items-center">
                            <svg className="w-4 h-4 mr-2 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                            </svg>
                            Closing Time
                          </label>
                          <input
                            type="time"
                            value={form.operatingHours.closeTime}
                            onChange={(e) => setForm({
                              ...form,
                              operatingHours: { ...form.operatingHours, closeTime: e.target.value }
                            })}
                            className="w-auto px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-purple-500 focus:border-transparent text-center font-mono text-lg"
                          />
                        </div>
                      </div>
                      
                      {/* Duration Display */}
                      <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
                        <div className="flex items-center justify-center space-x-2 text-blue-700">
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                          <span className="font-medium">
                            Daily Operation: {form.operatingHours.openTime} - {form.operatingHours.closeTime}
                          </span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex space-x-4 pt-4">
                <button
                  type="submit"
                  className="flex-1 bg-gradient-to-r from-purple-600 to-purple-700 text-white py-3 px-6 rounded-xl font-medium hover:from-purple-700 hover:to-purple-800 focus:ring-4 focus:ring-purple-300 transition-all duration-200 flex items-center justify-center space-x-2"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Update Schedule</span>
                </button>
                <button
                  type="button"
                  onClick={closeModals}
                  className="flex-1 bg-gray-100 text-gray-700 py-3 px-6 rounded-xl font-medium hover:bg-gray-200 focus:ring-4 focus:ring-gray-300 transition-all duration-200 flex items-center justify-center space-x-2"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                  <span>Cancel</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Stations Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Location</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Slots</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Hours</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {stations.map((station) => (
              <tr key={station.id}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {station.name}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {station.location}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    station.type === "DC" ? "bg-yellow-100 text-yellow-800" : "bg-green-100 text-green-800"
                  }`}>
                    {station.type}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {station.availableSlots}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {station.operatingHours?.isOpen24Hours ? "24/7" : 
                    `${station.operatingHours?.openTime || "06:00"} - ${station.operatingHours?.closeTime || "22:00"}`}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {station.isActive ? (
                    <span className="px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Active
                    </span>
                  ) : (
                    <span className="px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
                      Inactive
                    </span>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-1">
                  <button
                    onClick={() => openUpdateForm(station)}
                    className="px-2 py-1 bg-blue-600 text-white rounded text-xs hover:bg-blue-700"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => openScheduleForm(station)}
                    className="px-2 py-1 bg-purple-600 text-white rounded text-xs hover:bg-purple-700"
                  >
                    Schedule
                  </button>
                  <button
                    onClick={() => toggleStationStatus(station.id, station.isActive)}
                    className={`px-2 py-1 rounded text-xs text-white ${
                      station.isActive 
                        ? "bg-orange-600 hover:bg-orange-700" 
                        : "bg-green-600 hover:bg-green-700"
                    }`}
                  >
                    {station.isActive ? "Deactivate" : "Activate"}
                  </button>
                  <button
                    onClick={() => deleteStation(station.id, station.name)}
                    className="px-2 py-1 bg-red-600 text-white rounded text-xs hover:bg-red-700"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        
        {stations.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            No charging stations found. Create your first station above.
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div 
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 backdrop-blur-sm"
          onClick={handleDeleteBackdropClick}
        >
          <div className="bg-white rounded-2xl p-8 shadow-2xl transform transition-all duration-200 scale-100 max-w-md w-full mx-4">
            <div className="flex flex-col items-center space-y-6">
              <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center">
                <svg className="w-8 h-8 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z" clipRule="evenodd" />
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8 7a1 1 0 012 0v4a1 1 0 11-2 0V7zM8 15a1 1 0 112 0v.01a1 1 0 11-2 0V15z" clipRule="evenodd" />
                </svg>
              </div>
              
              <div className="text-center">
                <h3 className="text-xl font-semibold text-gray-900 mb-2">Delete Station</h3>
                <p className="text-gray-600">
                  Are you sure you want to delete station "{stationToDelete?.name}"? 
                  <br />
                  <span className="font-medium text-red-600">This action cannot be undone.</span>
                </p>
              </div>
              
              <div className="flex space-x-4 w-full">
                <button
                  onClick={cancelDelete}
                  className="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors duration-200 font-medium"
                >
                  Cancel
                </button>
                <button
                  onClick={confirmDelete}
                  className="flex-1 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors duration-200 font-medium"
                >
                  Yes, Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
