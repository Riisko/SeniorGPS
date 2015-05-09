package seniorgps.model;
/**
 *
 */

import java.util.ArrayList;

public class DataRoute {
	private static ArrayList<DataPosition>	positions	= new ArrayList<DataPosition>();
	private final String				FILENAME	= "file.txt";

	static {
		// LOAD FROM FILE y=easting,x=northing, Lat = Y Long = X
		// TEST VACLAVAK
		clearDataPosition();
	}

	public static ArrayList<DataPosition> getPositions() {
		return positions;
	}

	public static ArrayList<DataPosition> getLastTenPositions() {
		if (positions.size() > 10) {
			ArrayList<DataPosition> positions1 = new ArrayList<DataPosition>();
			for (int i = positions.size() - 1; i >= positions.size() - 10; i--) {
				positions1.add(positions.get(i));
			}
			return positions1;
		} else {
			return positions;
		}
	}

	public static DataPosition getLastPosition() {
		if (positions.size() > 1) {
			return positions.get(positions.size() - 1);
		} else {
			return new DataPosition(0d, 0d);
		}
	}

	public static void addNewPosition(DataPosition position) {
		positions.add(position);
	}

	public static void clearDataPosition() {
		positions.clear();
	}

	public static String getAllDataAsString() {
		String string = "";
		for (DataPosition position : positions) {
			string += position.getDataLat();
			string += ";";
			string += position.getDataLong();
			string += "\n";
		}
		return string;
	}

}
