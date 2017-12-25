package ch.epfl.sweng.project;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;

import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;

/**
 * Display the bitmap user profile picture in fullScreen mode
 * @author Dominique Roduit
 */
public final class FullImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        Bundle b = getIntent().getExtras();
        long myUserId = Settings.getInstance(getApplicationContext()).getUserID();
        long userId = (b != null) ? b.getLong(ViewProfileActivity.EXTRA_USER_ID) : myUserId;

        DBHandler db = DBHandler.getInstance(getApplicationContext());
        Profile profile = db.getProfile(userId);
        User user = db.getUser(userId);

        if(profile != null) {
            ImageView iv = (ImageView) findViewById(R.id.fullImage);
            iv.setImageBitmap(profile.getPhoto());
        }

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF000000")));
            actionBar.setDisplayHomeAsUpEnabled(true);
            if(user != null) {
                actionBar.setTitle(user.getFullName());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

}
