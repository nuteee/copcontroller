package com.nute.copcontroller.entities;

import org.jxmapviewer.viewer.Waypoint;

public class WaypointPolice extends WaypointCar implements Waypoint {

	private final String name;
	private final Long id;

	public WaypointPolice(double lat, double lon, String name, Long cop_id) {
		super(lat, lon);
		this.name = name;
		this.id = cop_id;
	}

	public String getName() {
		return name;
	}

	public Long getId() {
		return id;
	}

}
