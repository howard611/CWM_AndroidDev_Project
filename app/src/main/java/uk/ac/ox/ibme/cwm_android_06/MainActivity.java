package uk.ac.ox.ibme.cwm_android_06;


import android.app.FragmentManager;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.os.Bundle;
import android.app.Activity;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;
import android.widget.SeekBar;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.TextView;
import android.app.AlarmManager;
import android.location.Criteria;
import android.app.PendingIntent;
import android.location.Location;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.location.LocationManager;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.location.LocationListener;
import android.preference.PreferenceManager;

import android.os.Environment;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends FragmentActivity  {


    // The definition of the ALARM ID and
    // the creation of a Filter that will
    // enable us to act only on this specific
    // alarm signal
    public static final String ACTION_NAME = "uk.ac.ox.ibme.android_06.MYALARM";
    PendingIntent pendingIntent = null;



    // This is the delay that is set initially (in seconds)
    private int delayAlarmSeconds = 5;

    private Button addLocation = null;
    private Button clear = null;
    private SeekBar alarmDelay = null;
    private TextView alarmTextView = null;
    private SharedPreferences SP = null;
    private boolean locationAvailable = false;
    private LocationManager locationManager = null;
    private int position_index;
    private IntentFilter myFilter = new IntentFilter(ACTION_NAME);


    private ListView listview;
    ArrayAdapter<String> arrayAdapter;

    String[] locations ={"Oxford","London"};
    public List<String> list = new ArrayList<String>(Arrays.asList(locations));


    // A variable that will keep the state of the last known location
    private Location lastKnownLocation;

    // Polyline variables
    PolylineOptions polylineOptions = new PolylineOptions();
    Polyline polyline = null;

    // A variable that will contains the information about
    // the Map we are receiving from the Google API
    private GoogleMap mMap;


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {

            SupportMapFragment smf = null;
            //TODO EX 2: Get the SupportMapFragment from the getSupportFragmentManager() by using the findFragmentById and passing the R.id.map
            smf = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            //TODO EX 2: Get the map from the SupportMapFragment.
            mMap = smf.getMap();

            // This method initialise the blue dot that show our current location on the map
            if (mMap!= null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }



    // This is the BroadcastReceiver, that is
    // the method that will be 'attached'
    // together with a filter manage each signal
    // that we receive from the operating system
    BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Alarm triggered", Toast.LENGTH_LONG).show();

        }
    };



    // Define a listener that responds to location updates. The updates arrives
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Store location data every time the location changes
            lastKnownLocation = location;

            if (locationAvailable && lastKnownLocation != null) {
                Log.d(this.toString(), "geo: " + lastKnownLocation.getLongitude() + "," + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getAltitude());
            }

            // Check if the mMap is initialised
            if (mMap != null && lastKnownLocation != null) {

                LatLng position = null;
                //TODO EX 3: Get the Position as a specific LatLng object. This object represents a location.
                position = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());

                //TODO EX 3:  use the method moveCamera to move the map into your new position.
                //TODO        Use CameraUpdateFactory.newLatLng or CameraUpdateFactory.newLatLngZoom
                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));

                MarkerOptions mo = null;

                //TODO  EX 3: create a MarkerOptions object, give it a title (using and incrementing the position_index variable),
                //TODO        and set it to the current position.
                mo = new MarkerOptions().title(Integer.toString(position_index)).position(position);

                if (mo != null) {
                    position_index = position_index+1;
                    mMap.addMarker(mo);

                    polylineOptions.add(position);
                    polylineOptions.color(Color.RED);
                    polyline = mMap.addPolyline(polylineOptions);
                }
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
    };



    @Override
    protected void onPause() {

        // Here we unregister the information about
        // the alarm receiver so that the application
        // is in background and does not consume battery
        // [ REMOVE ME ]
        //
        // unregisterReceiver(alarmReceiver);

        // Here we do the same for the location manager
        locationManager.removeUpdates(locationListener);

        // and we do not forget to call the parent's method
        super.onPause();
    }

    @Override
    protected void onResume() {

        // Here we activate the location module.
        // It also 'attach' the location Listener
        activateLocationManager();


        // Here we register the receiver of the alarm events
        //
        // registerReceiver(alarmReceiver, myFilter);


        super.onResume();
    }

    // This is the function that returns
    // the name of the best location provider
    // given a certain set of criteria
    private String getBestLocationProvider() {
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);


        // Here return the output value of the getBestProvider method
        return locationManager.getBestProvider(c, true);
    }




    /*
     * This method activates the LocationManager and
     * attach the listener for the location updates
     */
    private void activateLocationManager() {


        position_index = 0;

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        // Here we select the best provider of location available that fit some criteria and is available.
        String bestProvider = getBestLocationProvider();

        // We set this flag to false and later we check the result to understand if the location is
        // available
        locationAvailable = false;


        // If the bestProvider is not accessible and/or enabled
        if ( !locationManager.isProviderEnabled( bestProvider ) ) {
            // .. we crerate a custom AlertDialog (as seen in Module 3) that will
            // initiate the Intent to activate the Location management for the phone
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Your LOCATION provider seems to be disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick( final DialogInterface dialog, final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }


        // In case the location manager and the bestProvider are not empty and previous checks succeeded...
        if ( locationManager != null && bestProvider != "") {
            locationManager.requestLocationUpdates(bestProvider, 0, 0, locationListener);
            lastKnownLocation = locationManager.getLastKnownLocation(bestProvider);
            locationAvailable = true;
        }

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // This function initialise the MAP object
        setUpMapIfNeeded();

        // The SharedPreference Manager to store
        // information we might need after when
        // the application is restarted
        SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        listview = (ListView) findViewById(R.id.list);
        //TODO:  (2.1) initialize the arrayAdapter and attach it to the layout and connect to the relevant data
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, list);
        //TODO:  (2.1) attach the adapter with the listVieW
        listview.setAdapter(arrayAdapter);

        // Find the button and populate our variable
        addLocation = (Button) findViewById(R.id.add_location);
        // Assign the ClickListener that will manage the 'click' event
        addLocation.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int id = SP.getInt("ALARM_ID", -1);

                        if ( id < 0 ) {
                            Random r = new Random();
                            id = r.nextInt(10000);
                            // setAlarm(delayAlarmSeconds*1000, id);


                            Log.d(this.toString(), "Setting Alarm ID: " + id + " every " + delayAlarmSeconds + " seconds");

                            // Here we record the value of the alarm_id in a Shared Preference
                            // so that it can be retrieved later on
                            SP.edit().putInt("ALARM_ID", id).commit();

                            // Also activate the location manager in case it is not already active
                            activateLocationManager();

                        } else {
                            Toast.makeText(getApplicationContext(), "Alarm already set", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


        // Find the Deactivate button and populate our variable
        clear = (Button) findViewById(R.id.clear);
        // Assign the ClickListener that will manage the 'click' event
        clear.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {



                        /*int id = SP.getInt("ALARM_ID", -1);
                        if ( id < 0 ) {
                            Toast.makeText(getApplicationContext(), "Alarm not set", Toast.LENGTH_SHORT).show();
                        } else {
                            cancelAlarm(id);
                            Log.d(this.toString(), "Cancelled Alarm ID: " + id);

                            // Here we remove the value of the alarm_id from the Shared Preference
                            SP.edit().remove("ALARM_ID").commit();*/
                        }
                });


        // Now we find the TextView element

        // TODO JUST IN CASE I NEED THIS IN THE FUTURE
        // alarmTextView = (TextView) findViewById(R.id.alarmTextView);

        // The SeekBar is the element that let
        // you have a scrollbar selector to pick
        // a number between the set MIN and MAX

        // TODO JUST IN CASE I NEED THIS IN THE FUTURE
        // alarmDelay = (SeekBar) findViewById(R.id.alarm_delay);

        // Here we set the standard delay.

        // alarmDelay.setProgress(delayAlarmSeconds);


        // Many types of event listeners exists this onSeekBarChange one does what the
        // names suggest and fires an event every time the SeekBar changes its value

        /*alarmDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // We both display and store for future use the value of the progressbar
                alarmTextView.setText("" + progress);
                delayAlarmSeconds = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });*/
    }


    // We make sure to clean up the alert before leaving...
    // We do not want to keep any resource going after our app is gone.
    @Override
    protected void onDestroy() {


        int id = SP.getInt("ALARM_ID", -1);
        if ( id > 0 ) {
            // When the app cease to live, we cancel the
            // Alarm (that otherwise will continue to fire)
            cancelAlarm(id);

            // Additionally, we remove the value of the alarm_id
            // from the Shared Preference

            SP.edit().remove("ALARM_ID").commit();
        }

        super.onDestroy();
    }


    /*
     * Set an alarm using the delay in milliseconds and providing an ID for the alarm.
     * Having the ID we can cancel the Alarm if needed.
     */
    private void setAlarm(long Interval, int ID) {
        final AlarmManager am = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_NAME);

        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Here we set the Alarm using the tipe RTC_WAKEUP,
        // the current time, the Interval, and the
        // PendingIntent that identifies the Signal
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Interval, pendingIntent);

        Toast.makeText(getApplicationContext(), "Alarm set", Toast.LENGTH_SHORT).show();
    }


    /*
     *   Just using a random integer, and then returning it.
     */
    private int setAlarm (long ms) {
        Random r = new Random();
        int ID = r.nextInt(10000);
        setAlarm(ms, ID);
        return ID;
    }


    /*
     * Cancel an alarm using the ID of the alarm.
     */
    private void cancelAlarm(int ID) {
        final AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_NAME);

        PendingIntent pi = PendingIntent.getBroadcast(this, ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);

        Toast.makeText(getApplicationContext(), "Alarm cancelled", Toast.LENGTH_SHORT).show();
    }



}
