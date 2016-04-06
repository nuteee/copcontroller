package com.nute.copcontroller.entities;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jxmapviewer.viewer.Waypoint;

public class WaypointPolice extends WaypointCar implements Waypoint {

	private final String name;
	private final Long id;
	private Boolean selected = false;

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
	
	public Boolean isSelected() {
		return selected;
	}
	
	public void setSelected(Boolean selected) {
		this.selected = selected;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof WaypointPolice))
			return false;
		WaypointPolice other = (WaypointPolice) obj;

		return new EqualsBuilder().append(this.id, other.id).build();
	}

}
