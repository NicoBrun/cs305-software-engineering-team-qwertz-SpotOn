package ch.epfl.sweng.spotOn.gui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ch.epfl.sweng.spotOn.BuildConfig;
import ch.epfl.sweng.spotOn.R;

public class EditPictureActivity extends AppCompatActivity {

    private final static int REQUEST_TEXT_DRAWING = 30;

    private Uri mURIofBitmap;
    private Bitmap mEditedBitmap;
    private ImageView mEditedImageView;
    public String mTextToDraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);
        mEditedImageView = (ImageView) findViewById(R.id.image_view_edition);

        //Set the parameters of the activity (defined above) with the extras of the TakePicture activity
        Intent intent = getIntent();
        mURIofBitmap = Uri.parse(intent.getExtras().getString("bitmapToEdit"));
        Bitmap receivedBitmap = TakePictureFragment.getBitmap(mURIofBitmap, this);
        mEditedBitmap = receivedBitmap.copy(Bitmap.Config.ARGB_8888, true);
        mEditedImageView.setImageBitmap(mEditedBitmap);
    }

    /**
     * Method that will be called when clicking on the Rotate button. It will rotate the bitmap image
     * and set the image view with the rotated bitmap
     * @param view
     */
    public void rotatePicture(View view){
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(mEditedBitmap, 0, 0, mEditedBitmap.getWidth(),
                mEditedBitmap.getHeight(), rotationMatrix, true);
        mEditedBitmap = rotatedBitmap;
        mEditedImageView.setImageBitmap(rotatedBitmap);
    }

    /**
     * Method that will be called when clicking on the "Add Text" button
     * @param view
     */
    public void goToDrawTextActivity(View view){
        Intent intent = new Intent(this, DrawTextActivity.class);
        startActivityForResult(intent, REQUEST_TEXT_DRAWING);
    }

    /**
     * Method that will be called when clicking on the "Done" button to get back to the
     * TakePictureFragment activity and keeping the edited changes
     * @param view
     */
    public void confirmChanges(View view){
        Intent intent = new Intent();
        mURIofBitmap = storeAndGetImageUri(mEditedBitmap);
        //Give the Uri as a string in extras ! Just giving the Uri is not working
        intent.putExtra("editedBitmap", mURIofBitmap.toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Method that will be used when the user adds text to the picture through the DrawTextActivity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_TEXT_DRAWING && resultCode == Activity.RESULT_OK){
            //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            mTextToDraw = data.getStringExtra("textToDraw");//preferences.getString("TD", "");
            Log.d("TextToDraw", mTextToDraw);
            if(mTextToDraw != null){
                //Edits the bitmap in a canvas
                Log.d("TextToDraw", "entered the loop");
                Canvas canvas = new Canvas(mEditedBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setTextSize(50);
                float x = 50;
                float y = mEditedBitmap.getHeight() - 200;
                paint.setFakeBoldText(true);
                canvas.drawText(mTextToDraw, x, y, paint);
                //Removes string from the preferences so the next picture taken by the user doesn't always draw the same string
                //SharedPreferences.Editor edit = preferences.edit();
                //edit.remove("TD");
                //edit.apply();
                mEditedImageView.setImageBitmap(mEditedBitmap);
            }
        }
    }

    /**
     * Method that will store the edited bitmap on the internal storage and return the uri of
     * where is stored the image
     * @param bitmap the bitmap image we want to store
     * @return the uri of where is stored the image
     */
    private Uri storeAndGetImageUri(Bitmap bitmap) {
        Uri resUri;
        //Define the Uri (same directory where is stored the temporal image we are editing)
        File storageForEdition = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "/SpotOn/EDITED_PICTURE.jpg");
        if(Build.VERSION.SDK_INT <= 23) {
            resUri = Uri.fromFile(storageForEdition);
            Log.d("UriEditedImage", resUri.toString());
        } else {
            //For API >= 24 (was the cause of the crash)
            resUri = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".provider", storageForEdition);
            Log.d("UriEditedImage", resUri.toString());
        }

        //Store the picture at the directory defined above
        try {
            FileOutputStream pictureOutputFile = new FileOutputStream(storageForEdition);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, pictureOutputFile);
            pictureOutputFile.close();

            //Allow the Pictures file to load directly after the image is stored
            MediaScannerConnection.scanFile(this, new String[]{storageForEdition.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        } catch (FileNotFoundException e) {
            Log.d("Store Image", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("Store Image", "File not closed: " + e.getMessage());
        }
        return resUri;
    }
}