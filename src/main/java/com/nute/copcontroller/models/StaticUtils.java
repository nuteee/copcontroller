package com.nute.copcontroller.models;

import java.io.File;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nute.copcontroller.entities.GPSLocation;

public class StaticUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(StaticUtils.class);

	public static void readMap(java.util.Map<Long, GPSLocation> lmap, String name) throws CopControllerException {
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
	}
}
