package com.gpit.android.we.common;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class MyLocation {
    LocationManager locationManager;
    LocationResult locationResult;
    boolean gps_enabled = false;
    boolean network_enabled = false;

    public boolean getLocation(Context context, LocationResult result) {
    	network_enabled = false;
        gps_enabled 	= false;
        
        //I use LocationResult callback class to pass location value from MyLocation to user code.
        locationResult = result;
        if (locationManager != null) {
        	locationManager.removeUpdates(locationListenerGps);
        	locationManager.removeUpdates(locationListenerNetwork);
        }
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //exceptions will be thrown if provider is not permitted.
        try{gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);}catch(Exception ex){}
        try{network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}catch(Exception ex){}

        //don't start listeners if no provider is enabled
        if(!gps_enabled && !network_enabled) {
        	Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	context.startActivity(intent);
        	return false;
        }
        
        boolean bSuccess = getLastLocation();
        if (bSuccess)
        	return true;

        if(gps_enabled)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.DEFAULT_INTERVAL_TIME, 0, locationListenerGps);
        if(network_enabled)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Constants.DEFAULT_INTERVAL_TIME, 0, locationListenerNetwork);
        return true;
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
            locationResult.gotLocation(location);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            locationResult.gotLocation(location);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };
    
    public boolean getLastLocation()
    {
        Location net_loc = null, gps_loc = null;
        if(gps_enabled)
            gps_loc=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(network_enabled)
            net_loc=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        //if there are both values use the latest one
        if(gps_loc != null && net_loc != null){
            if(gps_loc.getTime() > net_loc.getTime())
                locationResult.gotLocation(gps_loc);
            else
                locationResult.gotLocation(net_loc);
            return true;
        }

        if(gps_loc != null){
            locationResult.gotLocation(gps_loc);
            return true;
        }
        if(net_loc != null){
            locationResult.gotLocation(net_loc);
            return true;
        }
        locationResult.gotLocation(null);
        return false;
    }
    
    public static abstract class LocationResult{
        public abstract void gotLocation(Location location);
    }
}
