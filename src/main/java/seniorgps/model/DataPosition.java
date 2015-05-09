package seniorgps.model;

public class DataPosition {
	private double	dataLat	= 0;
	private double	dataLong	= 0;

	/**
	 * @param plat
	 *              = Y = E (Europe simplification)
	 * @param plong
	 *              = X = N(Europe simplification)
	 */
	public DataPosition(double plat, double plong) {
		dataLat = plat;
		dataLong = plong;
	}

	public double getDataLat() {
		return dataLat;
	}

	public void setDataLat(double pLat) {
		dataLat = pLat;

	}

	public double getDataLong() {
		return dataLong;
	}

	public void setDataLong(double pLong) {
		dataLong = pLong;
	}

}
