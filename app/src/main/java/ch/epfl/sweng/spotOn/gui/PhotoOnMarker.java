package ch.epfl.sweng.spotOn.gui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import ch.epfl.sweng.spotOn.R;

/**
 * Created by olivi on 20.10.2016.
 * Class that will create an information window when clicking on a marker of the cluster manager on the map
 */
public class PhotoOnMarker implements GoogleMap.InfoWindowAdapter {

    private ImageView mPictureView;
    private Pin clickedPin;

    public PhotoOnMarker(Context context, Pin pin){
        clickedPin = pin;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        //This line is to avoid the warning in inflater.inflate -> it is null because the view has no root parent
        final ViewGroup nullParent = null;
        View view = inflater.inflate( R.layout.layout_googlemaps_info_window, nullParent);
        mPictureView = (ImageView) view.findViewById(R.id.infoWindow);
    }

    /**
     * Display an image view of the thumbnail associated to the marker
     * @param marker the marker the user clicked on
     * @return the thumbnail associated to the marker as an image view
     */
    @Override
    public View getInfoWindow(Marker marker){
        Log.d("infoWindow", "accessed3");
        //if the marker is green
        if(clickedPin != null && clickedPin.getAccessibility() && !marker.getTitle().equals("position")){
            Bitmap associatedToMarker = clickedPin.getPhotoObject().getThumbnail();
            mPictureView.setImageBitmap(associatedToMarker);
            return mPictureView;
        } else {
            //the marker is yellow: display nothing
            return null;
        }
    }

    @Override
    public View getInfoContents(Marker marker){
        return null;
    }
}
