import { useEffect, useState } from "react";
import api from "../api/axios";
import toast from "react-hot-toast";

export default function Stations() {
  const [stations, setStations] = useState([]);
  const [error, setError] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showUpdateForm, setShowUpdateForm] = useState(false);
  const [showScheduleForm, setShowScheduleForm] = useState(false);
  const [currentStation, setCurrentStation] = useState(null);
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
    if (!confirm(`Are you sure you want to delete station "${stationName}"?`)) return;
    
    try {
      await api.delete(`/stations/${id}`);
      loadStations();
      toast.success("Station deleted successfully!");
    } catch (err) {
      toast.error("Error deleting station: " + (err.response?.data?.message || err.message));
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
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-96 max-h-96 overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">Create New Station</h2>
            <form onSubmit={createStation} className="space-y-4">
              <input
                type="text"
                placeholder="Station Name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full border p-2 rounded"
                required
              />
              <input
                type="text"
                placeholder="Location"
                value={form.location}
                onChange={(e) => setForm({ ...form, location: e.target.value })}
                className="w-full border p-2 rounded"
                required
              />
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}
                className="w-full border p-2 rounded"
                required
              >
                <option value="AC">AC Charging</option>
                <option value="DC">DC Fast Charging</option>
              </select>
              <input
                type="number"
                placeholder="Available Slots"
                min="1"
                value={form.availableSlots}
                onChange={(e) => setForm({ ...form, availableSlots: parseInt(e.target.value) })}
                className="w-full border p-2 rounded"
                required
              />
              <div className="border p-3 rounded bg-gray-50">
                <h3 className="font-medium mb-2">Operating Hours</h3>
                <div className="space-y-2">
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={form.operatingHours.isOpen24Hours}
                      onChange={(e) => setForm({
                        ...form,
                        operatingHours: { ...form.operatingHours, isOpen24Hours: e.target.checked }
                      })}
                      className="mr-2"
                    />
                    Open 24 Hours
                  </label>
                  {!form.operatingHours.isOpen24Hours && (
                    <div className="grid grid-cols-2 gap-2">
                      <input
                        type="time"
                        value={form.operatingHours.openTime}
                        onChange={(e) => setForm({
                          ...form,
                          operatingHours: { ...form.operatingHours, openTime: e.target.value }
                        })}
                        className="border p-1 rounded"
                      />
                      <input
                        type="time"
                        value={form.operatingHours.closeTime}
                        onChange={(e) => setForm({
                          ...form,
                          operatingHours: { ...form.operatingHours, closeTime: e.target.value }
                        })}
                        className="border p-1 rounded"
                      />
                    </div>
                  )}
                </div>
              </div>
              <div className="flex space-x-3">
                <button
                  type="submit"
                  className="flex-1 bg-green-600 text-white py-2 rounded hover:bg-green-700"
                >
                  Create Station
                </button>
                <button
                  type="button"
                  onClick={closeModals}
                  className="flex-1 bg-gray-600 text-white py-2 rounded hover:bg-gray-700"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Update Station Modal */}
      {showUpdateForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-96 max-h-96 overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">Update Station</h2>
            <form onSubmit={updateStation} className="space-y-4">
              <input
                type="text"
                placeholder="Station Name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full border p-2 rounded"
                required
              />
              <input
                type="text"
                placeholder="Location"
                value={form.location}
                onChange={(e) => setForm({ ...form, location: e.target.value })}
                className="w-full border p-2 rounded"
                required
              />
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}
                className="w-full border p-2 rounded"
                required
              >
                <option value="AC">AC Charging</option>
                <option value="DC">DC Fast Charging</option>
              </select>
              <input
                type="number"
                placeholder="Available Slots"
                min="1"
                value={form.availableSlots}
                onChange={(e) => setForm({ ...form, availableSlots: parseInt(e.target.value) })}
                className="w-full border p-2 rounded"
                required
              />
              <div className="flex space-x-3">
                <button
                  type="submit"
                  className="flex-1 bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
                >
                  Update Station
                </button>
                <button
                  type="button"
                  onClick={closeModals}
                  className="flex-1 bg-gray-600 text-white py-2 rounded hover:bg-gray-700"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Update Schedule Modal */}
      {showScheduleForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-96">
            <h2 className="text-xl font-bold mb-4">Update Station Schedule</h2>
            <form onSubmit={updateSchedule} className="space-y-4">
              <div className="border p-3 rounded bg-gray-50">
                <h3 className="font-medium mb-2">Operating Hours</h3>
                <div className="space-y-2">
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={form.operatingHours.isOpen24Hours}
                      onChange={(e) => setForm({
                        ...form,
                        operatingHours: { ...form.operatingHours, isOpen24Hours: e.target.checked }
                      })}
                      className="mr-2"
                    />
                    Open 24 Hours
                  </label>
                  {!form.operatingHours.isOpen24Hours && (
                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <label className="text-sm text-gray-600">Open Time</label>
                        <input
                          type="time"
                          value={form.operatingHours.openTime}
                          onChange={(e) => setForm({
                            ...form,
                            operatingHours: { ...form.operatingHours, openTime: e.target.value }
                          })}
                          className="w-full border p-1 rounded"
                        />
                      </div>
                      <div>
                        <label className="text-sm text-gray-600">Close Time</label>
                        <input
                          type="time"
                          value={form.operatingHours.closeTime}
                          onChange={(e) => setForm({
                            ...form,
                            operatingHours: { ...form.operatingHours, closeTime: e.target.value }
                          })}
                          className="w-full border p-1 rounded"
                        />
                      </div>
                    </div>
                  )}
                </div>
              </div>
              <div className="flex space-x-3">
                <button
                  type="submit"
                  className="flex-1 bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
                >
                  Update Schedule
                </button>
                <button
                  type="button"
                  onClick={closeModals}
                  className="flex-1 bg-gray-600 text-white py-2 rounded hover:bg-gray-700"
                >
                  Cancel
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
    </div>
  );
}
