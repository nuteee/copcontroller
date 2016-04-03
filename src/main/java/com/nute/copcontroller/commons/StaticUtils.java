package com.nute.copcontroller.commons;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nute.copcontroller.entities.GPSLocation;
import com.nute.copcontroller.models.CopControllerException;

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
//		LOGGER.debug("Getting closest node to: [{}, {}]", loc.getLatitude(), loc.getLongitude());
		Long ret = 0L;
		Double distance = Double.MAX_VALUE;
		for(Entry<Long, GPSLocation> entry : lmap.entrySet()) {
			Double tmp = loc.getDistance(entry.getValue());
			if(tmp < distance) {
				distance = tmp;
				ret = entry.getKey();
//				LOGGER.debug("Found a closer node: {}, ({} m)", ret, distance);
			}
		}
		
		return ret;		
	}
}
