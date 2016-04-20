package com.nute.copcontroller.commons;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.jxmapviewer.viewer.Waypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nute.copcontroller.entities.CopControllerException;
import com.nute.copcontroller.entities.GPSLocation;
import com.nute.copcontroller.entities.WaypointPolice;

public class StaticUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(StaticUtils.class);

	public static void readMap(Map<Long, GPSLocation> lmap, String name) throws CopControllerException {
		LOGGER.debug("Reading lmap...");
		File file = new File(name);

		Long ref = 0L;
		Double lat;
		Double lon;
		try (Scanner scan = new Scanner(file)) {
			while (scan.hasNext()) {
				ref = scan.nextLong();
				lat = scan.nextDouble();
				lon = scan.nextDouble();

				lmap.put(ref, new GPSLocation(lat, lon));
			}
		} catch (Exception e) {
			throw new CopControllerException("Hibás noderef2GPS leképezés.", e);
		}
		LOGGER.debug("lmap is processed.");
	}

	public static Long getClosestNode(Map<Long, GPSLocation> lmap, GPSLocation loc) {
		// LOGGER.debug("Getting closest node to: [{}, {}]", loc.getLatitude(),
		// loc.getLongitude());
		Long ret = 0L;
		Double distance = Double.MAX_VALUE;
		for (Entry<Long, GPSLocation> entry : lmap.entrySet()) {
			Double tmp = loc.getDistance(entry.getValue());
			if (tmp < distance) {
				distance = tmp;
				ret = entry.getKey();
				// LOGGER.debug("Found a closer node: {}, ({} m)", ret,
				// distance);
			}
		}

		return ret;
	}

	public static Long selectClosestCop(Map<Long, GPSLocation> lmap, Set<Waypoint> waypoints, GPSLocation mouseClicked) {
		Long selected = 0l;
		Double distance = Double.MAX_VALUE;
		WaypointPolice selectedCop = null;
		for (Waypoint w : waypoints) {
			if (w instanceof WaypointPolice) {
				Double tmpDistance = mouseClicked.getDistance(new GPSLocation((WaypointPolice) w));
				if (tmpDistance < distance) {
					if (selectedCop != null)
						selectedCop.setSelected(false);
					if (tmpDistance < 2500l) {
						selectedCop = (WaypointPolice) w;
						selectedCop.setSelected(true);
						selected = selectedCop.getId();
						distance = tmpDistance;
					}
				}
			}
		}

		if (selectedCop != null)
			LOGGER.debug("Cop ID: {}, Distance: {}, Selected: {}", selectedCop.getId(), distance, selectedCop.isSelected());
		else 
			LOGGER.debug("No cop was selected.");
		return selected;
	}

	/**
	 * Reads the relative path to the resource directory from the
	 * <code>RESOURCE_PATH</code> file located in
	 * <code>src/main/resources</code>
	 * 
	 * @return the relative path to the <code>resources</code> in the file
	 *         system, or <code>null</code> if there was an error
	 */
	public static String getResourcePath() {
		try {
			URI resourcePathFile = StaticUtils.class.getResource("/RESOURCE_PATH").toURI();
			String resourcePath = Files.readAllLines(Paths.get(resourcePathFile)).get(0);
			URI rootURI = new File("").toURI();
			URI resourceURI = new File(resourcePath).toURI();
			URI relativeResourceURI = rootURI.relativize(resourceURI);
			return relativeResourceURI.getPath();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return null;
		}
	}

	public static void sendCop(Map<Long, GPSLocation> lmap, Set<Waypoint> waypoints, Long nodeTo) throws URISyntaxException, IOException,
			InterruptedException {
		WaypointPolice cop = null;

		for (Waypoint w : waypoints) {
			if (w instanceof WaypointPolice) {
				if (((WaypointPolice) w).isSelected())
					cop = (WaypointPolice) w;
			}
		}

		LOGGER.debug("CopID: {}, nodeTo: {}", cop.getId(), nodeTo);

		String cmd = "echo '<innerroute " + cop.getId() + " " + nodeTo + ">' | telnet localhost 10007";
		List<String> cmdList = Arrays.asList(cmd);
		Path file = Paths.get(getResourcePath() + "route.sh");
		LOGGER.debug(file.toString());
		Files.write(file, cmdList, Charset.forName("UTF-8"), StandardOpenOption.WRITE);
		Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", file.toString() });
		p.waitFor();

	}
}
