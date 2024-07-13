package com.example.android_minigame.Fragments;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.android_minigame.Logic.Score;
import com.example.android_minigame.Logic.ScoreManager;
import com.example.android_minigame.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ScoreManager scoreManager;
    private TextView permissionDeniedText;
    private FrameLayout mapContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        scoreManager = new ScoreManager();

        permissionDeniedText = view.findViewById(R.id.text_permission_denied);
        mapContainer = view.findViewById(R.id.map_container);

        return view;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    public void updateCurrentLocation(Location location) {
        if (mMap != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        }
    }

    public void showScoreLocation(Score score) {
        if (mMap != null) {
            LatLng scoreLocation = new LatLng(score.getLatitude(), score.getLongitude());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(scoreLocation).title(score.getPlayerName() + ": " + score.getScore()));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scoreLocation, 15));
        }
    }

    public void showPermissionDeniedMessage() {
        permissionDeniedText.setVisibility(View.VISIBLE);
        mapContainer.setVisibility(View.GONE);
    }
}