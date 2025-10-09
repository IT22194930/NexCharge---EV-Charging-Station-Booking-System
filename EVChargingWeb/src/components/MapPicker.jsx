import { useEffect, useRef, useCallback } from 'react';
import { GOOGLE_MAPS_API_KEY } from '../config/maps';
import { Loader } from '@googlemaps/js-api-loader';

/*
Props:
 - lat (number|null)
 - lng (number|null)
 - onChange({lat, lng})
 - height (css) optional
 - mapTypeId optional
*/
export default function MapPicker({ lat, lng, onChange, height = '300px', mapTypeId = 'roadmap' }) {
  const mapDivRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const loaderRef = useRef(null);

  const setMarker = useCallback((latVal, lngVal, moveCenter = true, gOverride) => {
    const g = gOverride || (window.google && window.google.maps);
    if (!g || !mapRef.current) return;
    if (!markerRef.current) {
      markerRef.current = new g.Marker({
        position: { lat: latVal, lng: lngVal },
        map: mapRef.current,
        draggable: true
      });
      markerRef.current.addListener('dragend', () => {
        const pos = markerRef.current.getPosition();
        if (pos) onChange && onChange({ lat: pos.lat(), lng: pos.lng() });
      });
    } else {
      markerRef.current.setPosition({ lat: latVal, lng: lngVal });
    }
    if (moveCenter) mapRef.current.setCenter({ lat: latVal, lng: lngVal });
  }, [onChange]);

  useEffect(() => {
    if (!mapDivRef.current) return;
    if (!loaderRef.current) {
      loaderRef.current = new Loader({
        apiKey: GOOGLE_MAPS_API_KEY,
        version: 'weekly',
        libraries: []
      });
    }

    loaderRef.current.load().then((google) => {
      if (!mapRef.current) {
        mapRef.current = new google.maps.Map(mapDivRef.current, {
          center: { lat: lat || 6.9271, lng: lng || 79.8612 }, // Colombo default
          zoom: lat && lng ? 15 : 11, // Zoom in more if we have specific coordinates
          mapTypeId
        });

        mapRef.current.addListener('click', (e) => {
          const clickedLat = e.latLng.lat();
          const clickedLng = e.latLng.lng();
          setMarker(clickedLat, clickedLng);
          onChange && onChange({ lat: clickedLat, lng: clickedLng });
        });
      } else {
        // If map already exists, update center if we have new coordinates
        if (lat && lng) {
          mapRef.current.setCenter({ lat, lng });
          mapRef.current.setZoom(15);
        }
      }

      if (lat && lng) {
        setMarker(lat, lng, true, google);
      }
    });
  }, [lat, lng, mapTypeId, onChange, setMarker]);

  useEffect(() => {
    if (mapRef.current && lat && lng) {
      setMarker(lat, lng, true);
    }
  }, [lat, lng, setMarker]);

  return (
    <div className="w-full">
      <div ref={mapDivRef} style={{ width: '100%', height, borderRadius: '6px', overflow: 'hidden' }} />
      <p className="text-xs text-gray-500 mt-1">Click map to set location. Drag marker to fine tune.</p>
    </div>
  );
}
