package com.nute.copcontroller.entities;

import org.jxmapviewer.viewer.Waypoint;

public class WaypointPolice extends WaypointCar implements Waypoint {

	private final String name;

	public WaypointPolice(double lat, double lon, String name) {
		super(lat, lon);
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
