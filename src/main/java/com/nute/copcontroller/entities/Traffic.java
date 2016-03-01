package com.nute.copcontroller.entities;

import java.util.Set;

import org.jxmapviewer.viewer.Waypoint;

public class Traffic {

	private Set<Waypoint> waypoints;
	private String title;

	public Traffic(Set<Waypoint> waypoints, String title) {
		this.waypoints = waypoints;
		this.title = title;
	}

	public Set<Waypoint> getWaypoints() {
		return waypoints;
	}

	public void setWaypoints(Set<Waypoint> waypoints) {
		this.waypoints = waypoints;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
