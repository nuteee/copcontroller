package com.nute.copcontroller.entities;

import org.jxmapviewer.viewer.GeoPosition;

public class GPSLocation {
	
	private final Double latitude;
	private final Double longitude;
	private static final Double R = 6372797.560856;

	public GPSLocation(Double latitude, Double longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public GPSLocation(WaypointPolice w) {
		this(w.getPosition().getLatitude(), w.getPosition().getLongitude());
	}
	
	public GPSLocation(GeoPosition geoPosition) {
		this.latitude = geoPosition.getLatitude();
		this.longitude = geoPosition.getLongitude();
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	private static Double toRad(Double value) {
        return value * Math.PI / 180;
    }
	
	public Double getDistance(GPSLocation other) {
		Double latDistance = toRad(other.getLatitude() - this.latitude);
		Double lonDistance = toRad(other.getLongitude() - this.longitude);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + 
                Math.cos(toRad(this.latitude)) * Math.cos(toRad(other.getLatitude())) * 
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return R * c;
	}

}
