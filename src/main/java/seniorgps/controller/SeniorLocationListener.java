package seniorgps.controller;

import java.util.Arrays;

import seniorgps.MainActivity;
import seniorgps.config.Constants;
import seniorgps.controller.FetchAddressIntentService;
import seniorgps.model.DataPosition;
import seniorgps.model.DataRoute;
import seniorgps.model.DataStorage;


import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

public class SeniorLocationListener implements LocationListener {

    private static boolean  recording;
    public Location currentLocation;

    final int           SIZE_OF_POSITION_BLOCKS = 10;

    double[]            listLat         = new double[SIZE_OF_POSITION_BLOCKS];
    double[]            listLong            = new double[SIZE_OF_POSITION_BLOCKS];

    private int         positionInArray     = 0;

    @Override
    public void onLocationChanged(Location location) {

        if (location != null) {
            double pLong = location.getLongitude();
            double pLat = location.getLatitude();
            currentLocation = location;

            // Log.e("Získána pozice: " + pLat + ":" + pLong,
            // "Získána pozice: " + pLat + ":" + pLong);
            if (recording) // switch between rec and play
            {
                /*
                 * Finding median position
                 */
                listLat[positionInArray] = pLat;
                listLong[positionInArray] = pLong;
                positionInArray++;

                if (positionInArray == SIZE_OF_POSITION_BLOCKS) {
                    Arrays.sort(listLat);
                    Arrays.sort(listLong);
                    positionInArray = 0;
                    // storing median position if it is different than
                    // the last
                    // one stored
                    // skip redundancy
                    if (listLat[SIZE_OF_POSITION_BLOCKS / 2] != DataRoute
                            .getLastPosition().getDataLat()) {
                        DataRoute.addNewPosition(new DataPosition(
                                listLat[SIZE_OF_POSITION_BLOCKS / 2],
                                listLong[SIZE_OF_POSITION_BLOCKS / 2]));
                    }

                }

            } else {
                DataStorage.addNewPosition(new DataPosition(pLat, pLong));
            }

        }

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    public static boolean isRecording() {
        return recording;
    }

    public static void setRecording(boolean recording) {
        SeniorLocationListener.recording = recording;
    }
}
