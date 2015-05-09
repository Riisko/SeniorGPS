package seniorgps.controller;

import java.util.ArrayList;

import seniorgps.model.DataPosition;


public class DataCollisionChecker {

	public static int checkDataAgainstRoute(
			ArrayList<DataPosition> routeData,
			ArrayList<DataPosition> rawData, double range) {
		int successRange = rawData.size();
		double rangeGPS = 1;
		for (DataPosition positionRawData : rawData) {
			for (DataPosition dataRouteData : routeData) {

				// range pro počáteční a konečný bod trasy je 10x větší
				if (dataRouteData == routeData.get(0)
						|| dataRouteData == routeData.get(routeData
								.size() - 1)) {
					rangeGPS = range * 10;
				} else {
					rangeGPS = range;
				}

				// kolize
				if (positionRawData.getDataLat() + rangeGPS > dataRouteData
						.getDataLat()
						&& positionRawData.getDataLat() - rangeGPS < dataRouteData
								.getDataLat()
						&& positionRawData.getDataLong() + rangeGPS > dataRouteData
								.getDataLong()
						&& positionRawData.getDataLong() - rangeGPS < dataRouteData
								.getDataLong()) {
					successRange -= 1;
					break; // hledáme pouze jednu shodu
				}
			}
		}
		if (successRange >= 1) {
			return successRange;
		} else {
			return 0;
		}
	}
}
