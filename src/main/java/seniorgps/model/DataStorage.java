package seniorgps.model;

import java.util.ArrayList;

public class DataStorage {
	private static ArrayList<DataPosition>	positions	= new ArrayList<DataPosition>();


    private static ArrayList<DataPosition>	geofences	= new ArrayList<DataPosition>();

	static {
		positions.add(new DataPosition(0d, 0d));
	}

	public static ArrayList<DataPosition> getPositions() {
		return positions;
	}
    public static ArrayList<DataPosition> getGeofences() {
        return geofences;
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

	public synchronized static void addNewPosition(DataPosition position) {
		positions.add(position);
	}
    public synchronized static void addNewGeofence(DataPosition position) {
        geofences.add(position);
    }

	public static void clearDataPosition() {
		positions.clear();
	}

    public static void clearGeofences() {
        geofences.clear();
    }
}
