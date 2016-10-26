package ch.epfl.sweng.project;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class FullsizeImageViewActivity extends Activity {

    public final static String WANTED_IMAGE_PICTUREID = "ch.epfl.sweng.teamqwertz.spoton.FullsizeImageViewActivity.WANTED_IMAGE_PICTUREID";
    private final static int RESOURCE_IMAGE_DOWNLOADING = R.drawable.image_downloading;
    private final static int RESOURCE_IMAGE_FAILURE =  R.drawable.image_failure;

    private PhotoObject mDisplayedMedia = null;
    private ImageView mViewToSet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullsize_image_view);

        mViewToSet = (ImageView) findViewById(R.id.fullSizeImageView);
        mViewToSet.setImageResource(RESOURCE_IMAGE_DOWNLOADING);

        Intent displayImageIntent = getIntent();
        String wantedImagePictureId = displayImageIntent.getExtras().getString(WANTED_IMAGE_PICTUREID);

        if(!LocalDatabase.hasKey(wantedImagePictureId)){
            Log.d("Error", "FullsizeImageViewActivity : LocalDatabase has no matching object for ID "+ wantedImagePictureId);
            mViewToSet.setImageResource(RESOURCE_IMAGE_FAILURE);
        }else {
            mDisplayedMedia = LocalDatabase.getPhoto(wantedImagePictureId);
            Bitmap imageToDisplay = null;
            if (mDisplayedMedia.hasFullSizeImage()) {
                imageToDisplay = mDisplayedMedia.getFullSizeImage();
                mViewToSet.setImageBitmap(imageToDisplay);
            } else {
                // retrieveFullsizeImage throws an IllegalArgumentExceptino if mFullsizeImageLink isn't a valid firebase link
                try {
                    // add a listener that will set the image when it is retrieved
                    mDisplayedMedia.retrieveFullsizeImage(true, newImageViewSetterListener(), true, newFailureImageSetterListener());
                }catch (IllegalArgumentException e){
                    mViewToSet.setImageResource(RESOURCE_IMAGE_FAILURE);
                    Log.d("Error", "couldn't retrieve fullsizeImage from fileserver for Object with ID"+WANTED_IMAGE_PICTUREID);
                }
            }
        }
    }

    /** Factory method that returns a listener that
     * sets the imageView with the result of its query
     */
    private OnSuccessListener newImageViewSetterListener(){
        return new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] imageAsByteArray) {
                Bitmap obtainedImage = BitmapFactory.decodeByteArray(imageAsByteArray, 0, imageAsByteArray.length);
                mViewToSet.setImageBitmap(obtainedImage);
            }
        };
    }
    /** Factory method which creates a Listener that,
     * in case of failure, displays the received exception on console and sets the image displayed on view to an errorImage
     */
    private OnFailureListener newFailureImageSetterListener(){
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Exception", e.toString());
                mViewToSet.setImageResource(RESOURCE_IMAGE_FAILURE);
            }
        };
    }


/*  actually not part of sprint #4 - left for later
    private void recordUpvote(){
        if(mDisplayedMedia!=null){

        }
    }

    private void recordDownvote(){
        if(mDisplayedMedia!=null){

        }
    }
*/


}
