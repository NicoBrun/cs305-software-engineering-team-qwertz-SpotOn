package ch.epfl.sweng.spotOn.gui;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ch.epfl.sweng.spotOn.R;
import ch.epfl.sweng.spotOn.localObjects.LocalDatabase;
import ch.epfl.sweng.spotOn.media.PhotoObject;
import ch.epfl.sweng.spotOn.singletonReferences.DatabaseRef;
import ch.epfl.sweng.spotOn.singletonReferences.StorageRef;
import ch.epfl.sweng.spotOn.user.User;
import ch.epfl.sweng.spotOn.user.UserManager;

public class UserProfileActivity extends AppCompatActivity {

    private TextView mFirstNameTextView = null;
    private TextView mLastNameTextView = null;
    private TextView mKarmaTextView = null;
    private ListView mPicturesListView = null;
    private User mUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mUser = UserManager.getInstance().getUser();

        if( !mUser.isLoggedIn() || mUser.getUserId()==null){
            Log.e("UserProfileActivity", "UserId is null");
        }
        else {

            mFirstNameTextView = (TextView) findViewById(R.id.profileFirstNameTextView);
            mLastNameTextView = (TextView) findViewById(R.id.profileLastNameTextView);
            mKarmaTextView = (TextView) findViewById(R.id.profileKarmaTextView);

            mPicturesListView = (ListView) findViewById(R.id.profilePicturesListView);

            List<String> pictureIdList = new ArrayList<>(mUser.getPhotosTaken().keySet());
            List<PhotoObject> photoList = new ArrayList<>();

            for(int i=0; i<pictureIdList.size(); i++){
                Log.d("pictureIdList", pictureIdList.get(i));
                Log.d("photoListSize", Integer.toString(photoList.size()));
                photoList.add(LocalDatabase.getInstance().get(pictureIdList.get(i)));
                Log.d("photoListSize", Integer.toString(photoList.size()));
            }

            PictureVoteListAdapter mPictureVoteAdapter = new PictureVoteListAdapter(
                    UserProfileActivity.this, photoList);
            mPicturesListView.setAdapter(mPictureVoteAdapter);

            Context context = getApplicationContext();

            mFirstNameTextView.setText(mFirstNameTextView.getText() + " " + mUser.getFirstName());
            mLastNameTextView.setText(mLastNameTextView.getText() + " " + mUser.getLastName());
            mKarmaTextView.setText(mKarmaTextView.getText() + " " + mUser.getKarma());
        }

        final Button button = (Button) findViewById(R.id.profileBackButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goBackToTabActivity();
            }
        });
    }


    private void goBackToTabActivity(){
        finish();
    }



}
