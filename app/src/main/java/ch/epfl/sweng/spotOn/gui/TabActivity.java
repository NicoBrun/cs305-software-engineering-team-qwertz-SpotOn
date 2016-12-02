package ch.epfl.sweng.spotOn.gui;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.facebook.Profile;
import com.facebook.login.LoginManager;

import ch.epfl.sweng.spotOn.R;

import ch.epfl.sweng.spotOn.user.User;
import ch.epfl.sweng.spotOn.user.UserManager;

import ch.epfl.sweng.spotOn.utils.ServicesChecker;
import ch.epfl.sweng.spotOn.utils.ToastProvider;


public class TabActivity extends AppCompatActivity{


    private SeePicturesFragment mPicturesFragment = new SeePicturesFragment();
    private TakePictureFragment mCameraFragment = new TakePictureFragment();
    private MapFragment mMapFragment = new MapFragment();

    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);


        //Set up the toolbar where the different tabs will be located
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout){
            @Override
            public void onPageSelected(int pageNb) {
                checkAndDisplayServicesError();
            }
        });

    }

    @Override
    public void onResume(){
        super.onResume();
        ToastProvider.update(this);
    }


    /*
    Disables the hardware back button of the phone
     */
    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public void dispatchTakePictureIntent(View view) {
        mCameraFragment.dispatchTakePictureIntent(view);
    }

    public void storePictureOnInternalStorage(View view) {
        mCameraFragment.storePictureOnInternalStorage(view);
    }

    public void sendPictureToServer(View view){
        mCameraFragment.sendPictureToServer(view);
    }

    /*
     * Rotates the picture by 90°
     */

    public void editPicture(View view){
        mCameraFragment.editPicture(view);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(mPicturesFragment, getResources().getString(R.string.tab_aroundme));
        adapter.addFragment(mCameraFragment, getResources().getString(R.string.tab_camera));
        adapter.addFragment(mMapFragment, getResources().getString(R.string.tab_map));
        viewPager.setAdapter(adapter);
    }

    /** This method uses the options menu when this activity is launched     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    /* Handles what action to take when the user clicks on a menu item in the options menu     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out:
                if( ! UserManager.getInstance().userIsLoggedIn() ){
                    // to provide a way to log back in - needs to be improved todo
                    finish();
                    return true;
                } else {
                    disconnectFacebook();
                    UserManager user = UserManager.getInstance();
                    user.destroyUser();
                    return true;
                }
            case R.id.action_about:
                Intent intent = new Intent(this, AboutPage.class);
                startActivity(intent);
                return true;
            case R.id.user_profile:
                if( ! UserManager.getInstance().userIsLoggedIn() ){
                    ToastProvider.printOverCurrent(User.NOT_LOGGED_in_MESSAGE, Toast.LENGTH_SHORT);
                    return false;
                }else {
                    Intent profileIntent = new Intent(this, UserProfileActivity.class);
                    startActivity(profileIntent); // go to the UserManager Profile Activity
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void disconnectFacebook() {
        Profile profile = Profile.getCurrentProfile();
        if (profile != null) {
            LoginManager.getInstance().logOut();
            //go to the mainActivity in the activity stack
            finish();
        }
    }

    public void onEmptyGridButtonClick(View v){
        mTabLayout.getTabAt(2).select();
    }

    public void onUpVoteOrderingClick(View v){
        ToastProvider.printOverCurrent("Ordered by most upvoted Picture",Toast.LENGTH_LONG);
        refreshGrid(SeePicturesFragment.UPVOTE_ORDER);
    }

    public void onOldestOrderingClick(View v){
        ToastProvider.printOverCurrent("Ordered by oldest Picture",Toast.LENGTH_LONG);

        refreshGrid(SeePicturesFragment.OLDEST_ORDER);
    }

    public void onNewestOrderingClick(View v){
        ToastProvider.printOverCurrent("Ordered by newest Picture",Toast.LENGTH_LONG);

        refreshGrid(SeePicturesFragment.NEWEST_ORDER);
    }

    public void onHottestOrderingClick(View v){
        ToastProvider.printOverCurrent("Ordered by hottest Picture",Toast.LENGTH_LONG);

        refreshGrid(SeePicturesFragment.HOTTEST_ORDER);
    }

    private void refreshGrid(int ordering){
        if(mPicturesFragment!=null){
            mPicturesFragment.refreshGrid(ordering);
        }
    }

// PRIVATE HELPERS
    /** displays the error message if need be    */
    public void checkAndDisplayServicesError(){
        if( ! ServicesChecker.getInstance().allServicesOk() ){
            String errorMessage = ServicesChecker.getInstance().provideErrorMessage();
            if( errorMessage.isEmpty() ){
                throw new IllegalStateException("incoherent state : error message can't be empty if status isn't good");
            }
            ToastProvider.printOverCurrent(errorMessage, Toast.LENGTH_LONG);
        }
    }


}
