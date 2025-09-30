import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { GoogleMap, Marker, InfoWindow, useLoadScript } from '@react-google-maps/api';
import api from '../api/axios';
import toast from 'react-hot-toast';
import CreateBookingModal from './bookings/CreateBookingModal';

/*
  OwnerStationsMap: Shows nearest active stations for EVOwner.
  Allows selecting a station and opening the existing booking creation modal.
*/

const libraries = []; // can add 'places' later if needed
const mapContainerStyle = { width: '100%', height: '450px', borderRadius: '0.75rem' };

// Haversine distance (km)
function calcDistanceKm(lat1, lon1, lat2, lon2) {
  if ([lat1, lon1, lat2, lon2].some(v => v == null)) return null;
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI/180) * Math.cos(lat2 * Math.PI/180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return +(R * c).toFixed(2);
}

export default function OwnerStationsMap() {
  const role = localStorage.getItem('role');
  // Hooks must run before any early return
  const { isLoaded, loadError } = useLoadScript({
    googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
    libraries
  });

  const [stations, setStations] = useState([]);
  const [userPosition, setUserPosition] = useState(null);
  const [selectedStation, setSelectedStation] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [form, setForm] = useState({ ownerNic: '', stationId: '', reservationDate: '' });

  // Reuse min/max date logic consistent with existing Bookings page
  const getMinDate = useCallback(() => {
    const now = new Date();
    now.setHours(now.getHours() + 1);
    return now.toISOString().slice(0,16);
  }, []);
  const getMaxDate = useCallback(() => {
    const now = new Date();
    const sevenDaysFromNow = new Date(now.getTime() + 7*24*60*60*1000);
    return sevenDaysFromNow.toISOString().slice(0,16);
  }, []);

  const getOwnerNic = useCallback(() => {
    try {
      return JSON.parse(atob(localStorage.getItem('token').split('.')[1]))['http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name'];
    } catch {
      return '';
    }
  }, []);

  // Load stations
  useEffect(() => {
    (async () => {
      try {
        const res = await api.get('/stations');
        setStations(res.data.filter(s => s.isActive && s.latitude && s.longitude));
      } catch {
        toast.error('Failed to load stations');
      }
    })();
  }, []);

  // Get user geolocation (optional)
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        pos => setUserPosition({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => setUserPosition(null),
        { enableHighAccuracy: true, timeout: 8000 }
      );
    }
  }, []);

  // Pre-compute distances
  const stationsWithDistance = useMemo(() => {
    if (!userPosition) return stations;
    return stations.map(s => ({
      ...s,
      distanceKm: calcDistanceKm(userPosition.lat, userPosition.lng, s.latitude, s.longitude)
    })).sort((a,b) => (a.distanceKm ?? 1e9) - (b.distanceKm ?? 1e9));
  }, [stations, userPosition]);

  // Only keep nearest 3 stations for display
  const nearestStations = useMemo(() => stationsWithDistance.slice(0,3), [stationsWithDistance]);

  const center = userPosition || { lat: stations[0]?.latitude || 6.927079, lng: stations[0]?.longitude || 79.861244 }; // default Colombo fallback

  const openBookingForStation = (station) => {
    const nic = getOwnerNic();
    setForm({ ownerNic: nic, stationId: station.id, reservationDate: '' });
    setSelectedStation(station);
    setShowCreateModal(true);
  };

  const closeModals = () => {
    setShowCreateModal(false);
    setSelectedStation(null);
    setForm(f => ({ ...f, stationId: '', reservationDate: '' }));
  };

  // Local create booking replicating existing validation EXACTLY
  const createBooking = async (e) => {
    e.preventDefault();
    const reservationDate = new Date(form.reservationDate);
    const now = new Date();
    const sevenDaysFromNow = new Date(now.getTime() + 7*24*60*60*1000);
    if (reservationDate <= now) { toast.error('Reservation date must be in the future'); return; }
    if (reservationDate > sevenDaysFromNow) { toast.error('Reservation date must be within 7 days from now'); return; }
    try {
      await api.post('/bookings', form);
      toast.success('Booking created successfully!');
      closeModals();
    } catch (err) {
      toast.error('Error creating booking: ' + (err.response?.data?.message || err.message));
    }
  };

  if (role !== 'EVOwner') return null; // after hooks
  if (loadError) return <div className="mt-6 p-4 bg-red-50 text-red-700 rounded-lg">Failed to load map</div>;

  return (
    <div className="mt-10">
      <h2 className="text-xl font-bold text-gray-900 mb-4">Nearby Charging Stations</h2>
      <div className="bg-white p-4 rounded-lg shadow mb-6">
        {!isLoaded ? (
          <div className="h-[450px] flex items-center justify-center text-gray-500">Loading map...</div>
        ) : (
          <GoogleMap mapContainerStyle={mapContainerStyle} zoom={userPosition ? 13 : 11} center={center} options={{ streetViewControl: false, mapTypeControl: false }}>
            {userPosition && (
              <Marker position={userPosition} icon={{
                path: window.google.maps.SymbolPath.CIRCLE,
                scale: 6,
                fillColor: '#2563eb',
                fillOpacity: 1,
                strokeWeight: 2,
                strokeColor: 'white'
              }} title="Your Location" />
            )}
            {nearestStations.map(st => (
              <Marker key={st.id} position={{ lat: st.latitude, lng: st.longitude }} onClick={() => setSelectedStation(st)} />
            ))}
            {selectedStation && (
              <InfoWindow position={{ lat: selectedStation.latitude, lng: selectedStation.longitude }} onCloseClick={() => setSelectedStation(null)}>
                <div className="max-w-[220px]">
                  <h3 className="font-semibold text-gray-800 mb-1">{selectedStation.name}</h3>
                  <p className="text-xs text-gray-600 mb-1">{selectedStation.location}</p>
                  {selectedStation.distanceKm != null && (
                    <p className="text-xs text-blue-600 mb-2">{selectedStation.distanceKm} km away</p>
                  )}
                  <p className="text-xs text-gray-500 mb-2">Type: {selectedStation.type} | Slots: {selectedStation.availableSlots}</p>
                  <button
                    onClick={() => openBookingForStation(selectedStation)}
                    className="w-full px-3 py-2 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-lg text-xs font-medium hover:from-green-700 hover:to-emerald-700 transition-colors"
                  >
                    Book This Station
                  </button>
                </div>
              </InfoWindow>
            )}
          </GoogleMap>
        )}
      </div>

      {/* Station list (sorted) */}
      <div className="bg-white p-4 rounded-lg shadow">
        <h3 className="font-semibold text-gray-800 mb-3">Nearest Stations</h3>
        <div className="max-h-64 overflow-y-auto divide-y">
          {nearestStations.map(s => (
            <div key={s.id} className="py-2 flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-gray-900">{s.name}</p>
                <p className="text-xs text-gray-500">{s.location}</p>
                <p className="text-xs text-gray-400">{s.type} • Slots {s.availableSlots}{s.distanceKm != null && ` • ${s.distanceKm} km`}</p>
              </div>
              <button
                onClick={() => openBookingForStation(s)}
                className="ml-4 px-3 py-1.5 bg-blue-600 text-white rounded-md text-xs font-medium hover:bg-blue-700"
              >
                Book
              </button>
            </div>
          ))}
          {nearestStations.length === 0 && (
            <p className="text-sm text-gray-500 py-4 text-center">No active stations with coordinates.</p>
          )}
        </div>
      </div>

      <CreateBookingModal
        visible={showCreateModal}
        closeModals={closeModals}
        role={role}
        form={form}
        setForm={setForm}
        stations={stations}
        createBooking={createBooking}
        getMinDate={getMinDate}
        getMaxDate={getMaxDate}
      />
    </div>
  );
}
