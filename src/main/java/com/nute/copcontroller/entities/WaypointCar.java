package com.nute.copcontroller.entities;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

public abstract class WaypointCar implements Waypoint {

	protected GeoPosition geoPosition;

	public WaypointCar() {
	}

	public WaypointCar(double lat, double lon) {
		this.geoPosition = new GeoPosition(lat, lon);
	}
	
	@Override
	public GeoPosition getPosition() {
		return this.geoPosition;
	}

	public void setPosition(GeoPosition geoPosition) {
		this.geoPosition = geoPosition;
	}

}