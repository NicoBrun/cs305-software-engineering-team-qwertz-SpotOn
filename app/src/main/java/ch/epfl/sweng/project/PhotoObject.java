package ch.epfl.sweng.project;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.NoSuchElementException;

import static com.google.maps.android.SphericalUtil.computeDistanceBetween;

/**
 *  This class represents a picture and all associated information : name, thumbnail, pictureID, author, timestamps, position, radius
 *  It is mean to be used locally, as opposed to the PhotoObjectStoredInDatabase which is used to be stored in the database
 */
public class PhotoObject {

    private final long DEFAULT_PICTURE_LIFETIME = 24*60*60*1000; // in milliseconds - 24H
    private final int THUMBNAIL_SIZE = 128; // in pixels
    private final String DEFAULT_MEDIA_PATH = "MediaDirectory"; // used for Database Reference
    private final String STORAGE_REFERENCE_URL = "gs://spoton-ec9ed.appspot.com/images";

    private final String DEFAULT_PICTURE_PATH = "gs://spoton-ec9ed.appspot.com";
    private final long FIVE_MEGABYTES = 5*1024*1024;

    private Bitmap mFullSizeImage;
    private String mFullSizeImageLink;   // needed for the "cache-like" behaviour of getFullSizeImage()
    private boolean mHasFullSizeImage;   // false -> no Image, but has link; true -> has image, can't access link (don't need it => no getter)
    private Bitmap mThumbnail;
    private String mPictureId;
    private String mAuthorID;
    private String mPhotoName;
    private Timestamp mCreatedDate;
    private Timestamp mExpireDate;
    private double mLatitude;
    private double mLongitude;
    private int mRadius;

    /** This constructor will be used when the user takes a photo with his device, and create the object from locally obtained information
     *  pictureId should be created by calling .push().getKey() on the DatabaseReference where the object should be stored */
    public PhotoObject(Bitmap fullSizePic, String authorID, String photoName,
                       Timestamp createdDate, double latitude, double longitude, int radius){
        mFullSizeImage = fullSizePic.copy(fullSizePic.getConfig(), true);
        mHasFullSizeImage=true;
        mFullSizeImageLink = null;  // there is no need for a link
        mThumbnail = createThumbnail(mFullSizeImage);
        mPictureId = FirebaseDatabase.getInstance().getReference(DEFAULT_MEDIA_PATH).push().getKey();   //available even offline
        mPhotoName = photoName;
        mCreatedDate = createdDate;
        mExpireDate = new Timestamp(createdDate.getTime()+DEFAULT_PICTURE_LIFETIME);
        mLatitude = latitude;
        mLongitude = longitude;
        mRadius = radius;
        mAuthorID = authorID;
    }

    /** This constructor is called to convert an object retrieved from the database into a PhotoObject.     */
    public PhotoObject(String fullSizeImageLink, Bitmap thumbnail, String pictureId, String authorID, String photoName, long createdDate,
                       long expireDate, double latitude, double longitude, int radius){
        mFullSizeImage = null;
        mHasFullSizeImage=false;
        mFullSizeImageLink=fullSizeImageLink;
        mThumbnail = thumbnail;
        mPictureId = pictureId;
        mPhotoName = photoName;
        mCreatedDate = new Timestamp(createdDate);
        mExpireDate = new Timestamp(expireDate);
        mLatitude = latitude;
        mLongitude = longitude;
        mRadius = radius;
        mAuthorID = authorID;
    }


//FUNCTIONS PROVIDED BY THIS CLASS

    // Stores the object into the database (with intermediary steps : storing fullSIzeImage in the fileServer, converting the object into a PhotoObjectStoredInDatabase)
    // It is the responsibility of the sender to use the correct DBref, accordingly with the pictureId chosen, since the object will be used in a child named after the pictureId
    public void sendToDatabase(){
        DatabaseReference DBref = FirebaseDatabase.getInstance().getReference(DEFAULT_MEDIA_PATH);
        PhotoObjectStoredInDatabase DBobject = this.convertForStorageInDatabase();
        System.out.println(DBobject);
        DBref.child(mPictureId).setValue(DBobject);
    }

    //return true if the coordinates in parameters are in the scope of the picture
    public boolean isInPictureCircle(LatLng position){
        return computeDistanceBetween(
                new LatLng(mLatitude, mLongitude),
                position
        ) <= mRadius;
    }


    // Send the full size image to the file server to be stored
    public void sendToFileServer() {
        Log.d("sendToFileServer", "PictureID: "+mPictureId);
        // Create a storage reference from our app
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(STORAGE_REFERENCE_URL);

        // Create a reference to "PictureID.jpg"
        StorageReference pictureRef = storageRef.child(mPictureId + ".jpg");

        // Create a reference to 'images/"PictureID".jpg'
        StorageReference pictureImagesRef = storageRef.child("images/" + mPictureId +  ".jpg");

        // While the file names are the same, the references point to different files
        pictureRef.getName().equals(pictureImagesRef.getName());    // true
        pictureRef.getPath().equals(pictureImagesRef.getPath());    // false

        // Convert the bitmap image to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mFullSizeImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // upload the file
        UploadTask uploadTask = pictureRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.e("UploadFileToFileServer", "Exception raised by sendToFileServer()");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                // get the download link of the file
                mFullSizeImageLink = taskSnapshot.getDownloadUrl().toString();
                sendToDatabase();
                Log.d("FileUploadedURL", "URL: " + mFullSizeImageLink);
            }
        });
    }

    private void getFromFileServer(){
        // Create a storage reference from our app
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(STORAGE_REFERENCE_URL);

        // Create a reference to a file from a Google Cloud Storage URI
        StorageReference gsReference = FirebaseStorage.getInstance().getReferenceFromUrl(mFullSizeImageLink);

        final long ONE_MEGABYTE = 1024 * 1024;
        gsReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/PictureID.jpg" is returns, use this as needed
                mFullSizeImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Log.d("DownloadFromFileServer", "Downloaded full size image from FileServer");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.e("DownloadFromFileServer", "Exception raised by getFromFileServer()");
            }
        });
    }



//ALL THE GETTER FUNCTIONS

    // This getter functions as a cache, it gets the fullSizeImage only when needed, and stores it for later
    public Bitmap getFullSizeImage() {
        if (mHasFullSizeImage) {
            return mFullSizeImage.copy(mFullSizeImage.getConfig(), true);
        }else{
            if(mFullSizeImageLink==null){
                throw new AssertionError("if there is no image stored, object should have a link to retrieve it");
            }
            else {
                // Download full size image from file server
                getFromFileServer();

                return this.getFullSizeImage();
            }
/*
            StorageReference fullsizeImageReference = FirebaseStorage.getInstance().getReferenceFromUrl(DEFAULT_PICTURE_PATH+mFullSizeImageLink);
            fullsizeImageReference.getBytes(FIVE_MEGABYTES).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    mFullSizeImage=image;
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    System.out.println("ERROR - couldn't retrieve image from file server");
                }
            });
            System.out.println("did fetching from fileserver succeed ? "+mFullSizeImage==null);
            mHasFullSizeImage = true;
            return this.getFullSizeImage();
            //throw new NoSuchElementException("The code to retrieve the full size image doesn't exist yet");

*/
        }
    }
    public String getPhotoName(){
        return mPhotoName;
    }
    public Timestamp getCreatedDate(){
        return new Timestamp(mCreatedDate.getTime());
    }
    public Timestamp getExpireDate(){
        return new Timestamp(mExpireDate.getTime());
    }
    public double getLatitude(){
        return mLatitude;
    }
    public double getLongitude(){
        return mLongitude;
    }
    public int getRadius(){
        return mRadius;
    }
    public String getAuthorId(){
        return mAuthorID;
    }
    public Bitmap getThumbnail(){
        return mThumbnail.copy(mThumbnail.getConfig(), true);
    }
    public String getPictureId() {
        return mPictureId;
    }
    public String getFullSizeImageLink() { return mFullSizeImageLink; }


// PRIVATE HELPERS USED IN THE CLASS ONLY

    private PhotoObjectStoredInDatabase convertForStorageInDatabase(){
        // TODO obtain link for fullSizeImage in fileserver                                                                 <------
        String linkToFullSizeImage = "/default.jpg";
        String thumbnailAsString = encodeBitmapAsString(mThumbnail);
        return new PhotoObjectStoredInDatabase(linkToFullSizeImage, thumbnailAsString, mPictureId,mAuthorID, mPhotoName,
                mCreatedDate, mExpireDate, mLatitude, mLongitude, mRadius);
    }

    private String encodeBitmapAsString(Bitmap img){
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOS);
        //TODO img.recycle() ??;
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap createThumbnail(Bitmap fullSizeImage){
        return ThumbnailUtils.extractThumbnail(fullSizeImage, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
    }

    @Override
    public String toString()
    {
        return "PhotoObject: "+mPictureId+" lat: "+mLatitude+" long: "+mLongitude;
    }
}
