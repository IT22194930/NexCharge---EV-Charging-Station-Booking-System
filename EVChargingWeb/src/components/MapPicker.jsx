import { useEffect, useRef } from 'react';
import { TOMTOM_API_KEY } from '../config/maps';
import tt from '@tomtom-international/web-sdk-maps';
import '@tomtom-international/web-sdk-maps/dist/maps.css';

/*
Props:
 - lat (number|null)
 - lng (number|null)
 - onChange({lat, lng, address})
 - height (css) optional
*/
export default function MapPicker({ lat, lng, onChange, height = '300px', language = 'en-GB' }) {
  const mapElement = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);

  useEffect(() => {
    if (!mapElement.current) return;

    // Initialize map only once
    if (!mapRef.current) {
      mapRef.current = tt.map({
        key: TOMTOM_API_KEY,
        container: mapElement.current,
        center: [lng || 79.8612, lat || 6.9271], // default Colombo
        zoom: 11,
        language // enforce English (default 'en-GB')
      });

      mapRef.current.on('click', (e) => {
        const { lat, lng } = e.lngLat;
        setMarker(lat, lng);
        onChange && onChange({ lat, lng });
      });
    }
  }, []);

  // If lat/lng props change externally, update marker
  useEffect(() => {
    if (lat && lng) {
      setMarker(lat, lng, false);
      mapRef.current && mapRef.current.flyTo({ center: [lng, lat], zoom: 14 });
    }
  }, [lat, lng]);

  function setMarker(latVal, lngVal, moveCenter = true) {
    if (!mapRef.current) return;
    if (!markerRef.current) {
      markerRef.current = new tt.Marker({ draggable: true })
        .setLngLat([lngVal, latVal])
        .addTo(mapRef.current);
      markerRef.current.on('dragend', () => {
        const pos = markerRef.current.getLngLat();
        onChange && onChange({ lat: pos.lat, lng: pos.lng });
      });
    } else {
      markerRef.current.setLngLat([lngVal, latVal]);
    }
    if (moveCenter) mapRef.current.setCenter([lngVal, latVal]);
  }

  return (
    <div className="w-full">
      <div ref={mapElement} style={{ width: '100%', height, borderRadius: '6px', overflow: 'hidden' }} />
      <p className="text-xs text-gray-500 mt-1">Click map to set location. Drag marker to fine tune.</p>
    </div>
  );
}
