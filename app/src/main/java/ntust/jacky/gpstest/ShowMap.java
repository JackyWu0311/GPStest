package ntust.jacky.gpstest;

import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by User on 2017/1/5.
 */

public class ShowMap extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback {

    MapView mapView;
    final int MIN_TIME = 1000;
    GoogleMap map;
    Location currLoc;
    double lat, lon;
    GoogleApiClient mGoogleApiClient;

    public static ShowMap newInstance(double lat, double lon) {
        ShowMap myShowMap = new ShowMap();
        Bundle args = new Bundle();
        args.putDouble("lat", lat);
        args.putDouble("lon", lon);
        myShowMap.setArguments(args);
        return myShowMap;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.map, container, false);
        mapView = (MapView) v.findViewById(R.id.mvMap);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        lat = getArguments().getDouble("lat");
        lon = getArguments().getDouble("lon");
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }


    @Override
    public void onLocationChanged(Location location) {
        currLoc = location;
        lat = currLoc.getLatitude();
        lon = currLoc.getLongitude();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 14.0f));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title("You are here"));
        ((MainActivity) getActivity()).checkLocation(currLoc);
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(MIN_TIME);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMyLocationEnabled(true);
        MapsInitializer.initialize(getActivity());
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 14.0f));
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 16.0f));
                return true;
            }
        });
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.getTitle().contains("here"))
                    Toast.makeText(getActivity().getApplicationContext(), "You are pressing the Here Maker",
                            Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    class GetPlaces extends AsyncTask {
        String placesRequestStr = "https://maps.googleapis.com/maps/api/places/nearbysearch/json?" +
                "location=" + lat + "," + lon +
                "&radius=2000" +
                "&type=food" +
                "&language=zh-tw" +
                "&key=AIzaSyBbk223jtPZEgf-TNV2x5z_Rtu94FA5tGo";

        private final int MAX_PLACES = 20;
        private Marker[] placesMarkers;
        boolean NODATA = false;
        String result = null;

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            placesMarkers = new Marker[MAX_PLACES];
            if (placesMarkers != null) {
                for (int pm = 0; pm < placesMarkers.length; pm++) {
                    if (placesMarkers[pm] != null) {
                        placesMarkers[pm].remove();
                    }
                }
            }
            if (!NODATA) {
                try {
                    JSONArray places = new JSONArray(result).getJSONArray(Integer.parseInt("results"));
                    MarkerOptions[] aPlaceMarkerOpt = new MarkerOptions[places.length()];
                    for (int p = 0; p < places.length(); p++) {
                        JSONObject aPlace = places.getJSONObject(p);
                        String placeAdd = aPlace.getString("vicinity");
                        String placeName = aPlace.getString("name");
                        JSONObject loc = aPlace.getJSONObject("geometry").getJSONObject("location");
                        LatLng placeLL = new LatLng(Double.valueOf(loc.getString("lat")),
                                Double.valueOf(loc.getString("lng")));
                        aPlaceMarkerOpt[p] = new MarkerOptions()
                                .position(placeLL)
                                .title(placeName)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_point))
                                .snippet(placeAdd);
                        placesMarkers[p] = map.addMarker(aPlaceMarkerOpt[p]);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                OkHttpClient mHttpClient = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(placesRequestStr)
                        .build();
                Response response = mHttpClient.newCall(request).execute();
                if (response.isSuccessful()) {
                    result = response.body().string();
                }
                NODATA = false;
            } catch (IOException e) {
                NODATA = true;
            }
            return null;
        }
    }

    public void pan2Home(double lat, double lon) {

        MarkerOptions markerOpt1 = new MarkerOptions();
        markerOpt1.position(new LatLng(25.013084, 121.540392));
        markerOpt1.title("IB大樓");
        markerOpt1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        map.addMarker(markerOpt1);

        MarkerOptions markerOpt2 = new MarkerOptions();
        markerOpt2.position(new LatLng(25.013155, 121.541196));
        markerOpt2.title("AD大樓");
        markerOpt2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        map.addMarker(markerOpt2);

        MarkerOptions markerOpt3 = new MarkerOptions();
        markerOpt3.position(new LatLng(25.012416, 121.541371));
        markerOpt3.title("MA大樓");
        markerOpt3.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        map.addMarker(markerOpt3);

        MarkerOptions markerOpt4 = new MarkerOptions();
        markerOpt4.position(new LatLng(25.014949, 121.542778));
        markerOpt4.title("TR大樓");
        markerOpt4.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        map.addMarker(markerOpt4);

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 16.0f));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.red_point))
                .title("Home"));
    }

    void updatePlaces() {
        new GetPlaces().execute();
    }
}
