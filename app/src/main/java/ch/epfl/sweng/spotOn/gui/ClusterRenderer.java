package ch.epfl.sweng.spotOn.gui;

import android.content.Context;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Olivier on 31.10.2016.
 * Generate a cluster of Pin with custom condition of when it should render cluster and/or pins, in which colors...
 */
public class ClusterRenderer extends DefaultClusterRenderer<Pin> {

    private boolean zoom = false;
    private GoogleMap mMap;
    //Map of marker titles -> pins
    private static Map<String, Pin> mMarkerPinMap;

    /**
     * Custom clusterRenderer
     * @param context the context of the call
     * @param map the map on which the clusterRenderer works
     * @param clusterManager the manager of all the clusters
     */
    public ClusterRenderer(Context context, GoogleMap map,
                             ClusterManager<Pin> clusterManager) {
        super(context, map, clusterManager);
        mMap = map;
        mMarkerPinMap = new HashMap<>();
    }

    /**
     * @return a copy of the markers, pins map
     */
    public static Map<String, Pin> getMarkerPinMap(){
        return new HashMap<>(mMarkerPinMap);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void onBeforeClusterItemRendered(Pin pin,
                                               MarkerOptions markerOptions) {
        markerOptions.title(pin.getTitle());
        BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.defaultMarker(pin.getColor());
        markerOptions.icon(markerDescriptor);
        markerOptions.zIndex(pin.getZDepth());
    }

    /**
     * Add a marker (title) and its corresponding pin to the mMarkerPinMap
     * @param pin
     * @param marker
     */
    @Override
    protected void onClusterItemRendered(Pin pin, Marker marker){
        // Needs to be the title of the marker and not the marker itself because the title won't
        // change but the marker reference can change overtime so we would add the same element multiple times in the map.
        if(!mMarkerPinMap.containsKey(marker.getTitle())){
            mMarkerPinMap.put(marker.getTitle(), pin);
        }
        Log.d("onClusterItemRendered", String.valueOf(mMarkerPinMap.size()));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                computeZoom();
            }
        });
        return cluster.getSize() >=4  && !zoom;
    }

    private void computeZoom(){
        zoom = mMap.getMaxZoomLevel()-3 < mMap.getCameraPosition().zoom;
    }
}