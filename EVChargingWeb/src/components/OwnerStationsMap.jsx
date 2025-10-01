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
    const today = new Date();
    return today.toISOString().split('T')[0]; // Today's date
  }, []);
  const getMaxDate = useCallback(() => {
    const today = new Date();
    const sevenDaysFromNow = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);
    return sevenDaysFromNow.toISOString().split('T')[0]; // 7 days from today
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
      <div className="flex items-center mb-6">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-green-600 flex items-center justify-center shadow-md mr-3">
          <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </div>
        <div>
          <h2 className="text-2xl font-bold text-gray-900 leading-tight">Nearby Charging Stations</h2>
          <p className="text-sm text-gray-500">Find and book one of the closest active stations</p>
        </div>
      </div>

      <div className="grid lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <div className="bg-white rounded-2xl shadow relative overflow-hidden">
            <div className="absolute inset-0 bg-gradient-to-br from-emerald-50 to-teal-50 opacity-60 pointer-events-none" />
            <div className="p-4 relative z-10">
              {!isLoaded ? (
                <div className="h-[450px] flex items-center justify-center text-gray-500 animate-pulse">Loading map...</div>
              ) : (
                <GoogleMap mapContainerStyle={mapContainerStyle} zoom={userPosition ? 13 : 11} center={center} options={{ streetViewControl: false, mapTypeControl: false }}>
                  {userPosition && (
                    <Marker position={userPosition} icon={{
                      path: window.google.maps.SymbolPath.CIRCLE,
                      scale: 7,
                      fillColor: '#0ea5e9',
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
                      <div className="max-w-[230px]">
                        <div className="flex items-start mb-2">
                          <div className="flex-1">
                            <h3 className="font-semibold text-gray-800 leading-snug">{selectedStation.name}</h3>
                            <p className="text-[11px] text-gray-500 mt-0.5 line-clamp-2">{selectedStation.location}</p>
                          </div>
                        </div>
                        {selectedStation.distanceKm != null && (
                          <div className="flex items-center text-[11px] text-emerald-600 font-medium mb-1">
                            <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z" />
                            </svg>
                            {selectedStation.distanceKm} km away
                          </div>
                        )}
                        <p className="text-[11px] text-gray-500 mb-2">Type: {selectedStation.type} • Slots: {selectedStation.availableSlots}</p>
                        <button
                          onClick={() => openBookingForStation(selectedStation)}
                          className="w-full px-3 py-2 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-lg text-xs font-medium hover:from-green-700 hover:to-emerald-700 transition-colors shadow-sm hover:shadow"
                        >
                          Book This Station
                        </button>
                      </div>
                    </InfoWindow>
                  )}
                </GoogleMap>
              )}
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="bg-white rounded-2xl shadow p-5 h-full flex flex-col">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-gray-800 flex items-center">
                <svg className="w-4 h-4 text-emerald-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z" />
                </svg>
                Nearest Stations
              </h3>
              <span className="text-[11px] px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-medium">Top {nearestStations.length}</span>
            </div>
            <div className="space-y-3 overflow-y-auto pr-1 custom-scrollbar" style={{maxHeight: '410px'}}>
              {nearestStations.map((s, idx) => (
                <div key={s.id} className="group border border-gray-100 hover:border-emerald-300 rounded-xl p-3 transition-all bg-white hover:shadow-sm">
                  <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center mb-1">
                        <span className="inline-flex items-center justify-center w-5 h-5 text-[10px] font-semibold rounded-full bg-emerald-100 text-emerald-700 mr-2">{idx+1}</span>
                        <p className="font-medium text-sm text-gray-900 truncate">{s.name}</p>
                      </div>
                      <p className="text-[11px] text-gray-500 mb-1 line-clamp-1">{s.location}</p>
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-[10px] text-gray-500">
                        <span className="inline-flex items-center px-2 py-0.5 rounded bg-gray-100 text-gray-700 font-medium">{s.type}</span>
                        <span className="inline-flex items-center px-2 py-0.5 rounded bg-blue-50 text-blue-700 font-medium">Slots {s.availableSlots}</span>
                        {s.distanceKm != null && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 font-medium">
                            <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z" />
                            </svg>
                            {s.distanceKm} km
                          </span>
                        )}
                      </div>
                    </div>
                    <button
                      onClick={() => openBookingForStation(s)}
                      className="ml-3 px-3 py-1.5 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-md text-[11px] font-medium hover:from-blue-700 hover:to-indigo-700 shadow-sm hover:shadow focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition"
                    >
                      Book
                    </button>
                  </div>
                </div>
              ))}
              {nearestStations.length === 0 && (
                <p className="text-sm text-gray-500 py-4 text-center">No active stations with coordinates.</p>
              )}
            </div>
          </div>
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
