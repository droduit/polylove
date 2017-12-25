package ch.epfl.sweng.project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;
import ch.epfl.sweng.project.network.NetworkStatus;
import ch.epfl.sweng.project.network.ProfileSender;
import ch.epfl.sweng.project.utils.AndroidUtils;

import static ch.epfl.sweng.project.R.array;
import static ch.epfl.sweng.project.R.id;
import static ch.epfl.sweng.project.R.string;
import static ch.epfl.sweng.project.models.Profile.Gender;

/**
 * @author Tim Nguyen
 */

public final class EditAvatar extends AppCompatActivity {

    // Constants for the swipe
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private Avatar avatar = new Avatar(); // we create a default avatar
    private Gender gender;
    private GestureDetector gestureDetector; // used to handle the swipe left-right
    private Mode editMode; // used to choose which part of the Avatar we want to modify

    private DBHandler db = null;
    private boolean firstFill = false;
    private static Handler networkStatusHandler;

    private ImageView baseImage;
    private ImageView eyesImage;
    private ImageView skinImage;
    private ImageView hairImage;
    private ImageView shirtImage;
    private Menu menu;

    private View progressOverlay;

    private BroadcastReceiver receiver = null;

    private enum Mode {
        HairStyle, HairColor, Eyes, Skin, Shirt
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar);

        long userId = Settings.getInstance(getApplicationContext()).getUserID();
        avatar.setUserId(userId);

        Log.d("test", String.valueOf(userId));

        db = DBHandler.getInstance(getApplicationContext());

        progressOverlay = findViewById(R.id.progress_overlay);

        // Register BroadcastReceiver to track network connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                performNetworkActions();
            }
        };
        this.registerReceiver(receiver, filter);

        // get the profile from the FillProfile
        Profile profile = db.getProfile(userId);
        Avatar userAvatar = db.getAvatar(userId);

        Gender defaultGender = Gender.Male;
        if (profile != null) {
            gender = (profile.getGender() != null) ? profile.getGender() : defaultGender;
        } else {
            Bundle b = getIntent().getExtras();
            gender = b != null ? (Gender) ((b.get("gender") != null) ? b.get("gender") : defaultGender) : defaultGender;
        }

        avatar.setGender(gender);

        // set image view
        baseImage = (ImageView) findViewById(id.baseImage);
        eyesImage = (ImageView) findViewById(id.eyesImage);
        skinImage = (ImageView) findViewById(id.skinImage);
        hairImage = (ImageView) findViewById(id.hairImage);
        shirtImage = (ImageView) findViewById(id.shirtImage);

        // set image resources
        baseImage.setImageResource(AndroidUtils.getImageId(this, avatar.getBaseImage()));
        hairImage.setImageResource(AndroidUtils.getImageId(this, avatar.getHairImage()));
        skinImage.setImageResource(AndroidUtils.getImageId(this, avatar.getSkinImage()));
        eyesImage.setImageResource(AndroidUtils.getImageId(this, avatar.getEyesImage()));
        shirtImage.setImageResource(AndroidUtils.getImageId(this, avatar.getShirtImage()));

        gestureDetector = new GestureDetector(this, new SwipeGestureDetector());

        if (userAvatar != null) {
            SharedPreferences.Editor editableSettings = Settings.getInstance(getApplicationContext()).getEditableSettings();
            editableSettings.putInt("Skin", userAvatar.getSkinTone().ordinal());
            editableSettings.putInt("HairColor", userAvatar.getHairColor().ordinal());
            editableSettings.putInt("Eyes", userAvatar.getEyeColor().ordinal());
            editableSettings.putInt("HairStyle", userAvatar.getHairStyle().ordinal());
            editableSettings.commit();

            for (Mode m : Mode.values()) {
                editMode = m;
                fillAvatar();
            }
        } else {
            firstFill = true;
            final RelativeLayout layoutTip = (RelativeLayout) findViewById(id.tipLayout);
            layoutTip.setVisibility(View.VISIBLE);
            layoutTip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Animation out = AnimationUtils.makeOutAnimation(EditAvatar.this, false);
                    v.startAnimation(out);
                    v.setVisibility(View.GONE);
                }
            });
        }
    }

    private void performNetworkActions() {
        boolean networkAvailable = NetworkStatus.getInstance(getApplicationContext()).isConnected();

        AndroidUtils.displayNetworkStatus(EditAvatar.this, networkAvailable);

        if (menu != null) {
            AtomicReference<MenuItem> saveItem = new AtomicReference<>(menu.getItem(0));
            if (saveItem.get() != null) {
                saveItem.get().setVisible(networkAvailable);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    @Override
    public void onBackPressed() {
        if (firstFill) {
            Toast.makeText(this, getString(R.string.disabled_back_button_msg), Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save_btn, menu);
        this.menu = menu;
        performNetworkActions();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_action:
                item.setVisible(false);
                progressOverlay.setVisibility(View.VISIBLE);

                ProfileSender.sendAvatar(avatar, new NetworkResponseManager() {

                    @Override
                    public void onError(int errorCode) {
                        progressOverlay.setVisibility(View.GONE);
                        item.setVisible(true);
                        AndroidUtils.displayDialog(EditAvatar.this, "Network error", "Code " + errorCode);

                        if(errorCode == 401) {
                            startActivity(AndroidUtils.gotoLogin(EditAvatar.this));
                        }
                    }

                    @Override
                    public void onSuccess(JSONObject jsonObj) {
                        new SavingTask().execute();
                    }
                });

                break;
        }
        return true;
    }

    // ================================ Dialog's Interface =================================

    /**
     * Handles choice of skin, hair, and eyes color.
     */
    public void chooseSkinTone(View view) {
        editMode = Mode.Skin;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(string.choose_skintone)
                .setItems(array.skinTone, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int index) {
                        SharedPreferences.Editor editableSettings = Settings.getInstance(getApplicationContext()).getEditableSettings();
                        editableSettings.putInt("Skin", index);
                        editableSettings.commit();
                        fillAvatar();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void chooseHairColor(View view) {
        editMode = Mode.HairColor;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(string.choose_hair_color)
                .setItems(array.hairColor, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int index) {
                        SharedPreferences.Editor editableSettings = Settings.getInstance(getApplicationContext()).getEditableSettings();
                        editableSettings.putInt("HairColor", index);
                        editableSettings.commit();
                        fillAvatar();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void chooseEyeColor(View view) {
        editMode = Mode.Eyes;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(string.choose_eye_color)
                .setItems(array.eyeColor, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int index) {
                        SharedPreferences.Editor editableSettings = Settings.getInstance(getApplicationContext()).getEditableSettings();
                        editableSettings.putInt("Eyes", index);
                        editableSettings.commit();
                        fillAvatar();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // ================================== Fill the Avatar ===================================

    /**
     * Set the image resources of each avatar's features to the view.
     */

    public void fillAvatar() {
        int skinTone;
        int hairColor;
        int hairStyle;
        int eyeColor;
        int shirtStyle;
        SharedPreferences settings = Settings.getInstance(getApplicationContext()).getObject();

        if (editMode.equals(Mode.Skin)) {
            skinTone = settings.getInt("Skin", 0);
            avatar.setSkinTone((Avatar.Skin.values()[skinTone]));
            skinImage.setImageResource(AndroidUtils.getImageId(this, avatar.getSkinImage()));
        } else if (editMode.equals(Mode.HairColor)) {
            hairColor = settings.getInt("HairColor", 0);
            avatar.setHairColor((Avatar.HairColor.values()[hairColor]));
            hairImage.setImageResource(AndroidUtils.getImageId(this, avatar.getHairImage()));
        } else if (editMode.equals(Mode.Eyes)) {
            eyeColor = settings.getInt("Eyes", 0);
            avatar.setEyeColor((Avatar.Eye.values()[eyeColor]));
            eyesImage.setImageResource(AndroidUtils.getImageId(this, avatar.getEyesImage()));
        } else if (editMode.equals(Mode.HairStyle)) {
            hairStyle = settings.getInt("HairStyle", 0);
            avatar.setHairStyle(Avatar.HairStyle.values()[hairStyle]);
            hairImage.setImageResource(AndroidUtils.getImageId(this, avatar.getHairImage()));
        } else if (editMode.equals(Mode.Shirt)) {
            shirtStyle = settings.getInt("Shirt", 0);
            avatar.setShirt(Avatar.Shirt.values()[shirtStyle]);
            shirtImage.setImageResource(AndroidUtils.getImageId(this, avatar.getShirtImage()));
        }
    }

    // =============================== Swipe Gesture Detector ===============================

    /**
     * Handles swipe gesture used to change avatar's hairstyle and clothes.
     */
    class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    onLeftSwipe(e1, e2);
                }
                // left to right swipe
                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    onRightSwipe(e1, e2);
                }
            } catch (Exception e) {
                System.out.println("ERROR on swipe gesture");
            }
            return false;
        }
    }

    private void onLeftSwipe(MotionEvent e1, MotionEvent e2) {
        SharedPreferences.Editor editableSettings = Settings.getInstance(getApplicationContext()).getEditableSettings();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int height = size.y;

        if(e1.getY() < height*0.4 && e2.getY() < height*0.4) {
            editMode = Mode.HairStyle;
        } else if (e1.getY() > height*0.4 && e2.getY() > height*0.4) {
            editMode = Mode.Shirt;
        }

        if (editMode.equals(Mode.HairStyle)) {
            int i = 0;
            while (!avatar.getHairStyle().equals(Avatar.HairStyle.values()[i])) {
                i++;
            }
            if (i == Avatar.HairStyle.values().length - 1) {
                editableSettings.putInt("HairStyle", 0);
            } else {
                editableSettings.putInt("HairStyle", i + 1);
            }
        } else if(editMode.equals(Mode.Shirt)) {
            int i = 0;
            while (!avatar.getShirt().equals(Avatar.Shirt.values()[i])) {
                i++;
            }
            if (i == Avatar.Shirt.values().length - 1) {
                editableSettings.putInt("Shirt", 0);
            } else {
                editableSettings.putInt("Shirt", i + 1);
            }
        }
        editableSettings.commit();
        fillAvatar();
    }

    private void onRightSwipe(MotionEvent e1, MotionEvent e2) {
        SharedPreferences.Editor editableSettings = Settings.getInstance(getApplicationContext()).getEditableSettings();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int height = size.y;

        // we want to swipe the upper part of the screen to change the hairstyle
        // and swipe the lower part to change the clothes
        if(e1.getY() < height*0.4 && e2.getY() < height*0.4) {
            editMode = Mode.HairStyle;
        } else if (e1.getY() > height*0.4 && e2.getY() > height*0.4) {
            editMode = Mode.Shirt;
        }

        if (editMode.equals(Mode.HairStyle)) {
            int i = 0;
            while (!avatar.getHairStyle().equals(Avatar.HairStyle.values()[i])) {
                i++;
            }
            if (i == 0) {
                editableSettings.putInt("HairStyle", Avatar.HairStyle.values().length - 1);
            } else {
                editableSettings.putInt("HairStyle", i - 1);
            }
        } else if(editMode.equals(Mode.Shirt)) {
            int i = 0;
            while (!avatar.getShirt().equals(Avatar.Shirt.values()[i])) {
                i++;
            }
            if (i == 0) {
                editableSettings.putInt("Shirt", Avatar.Shirt.values().length - 1);
            } else {
                editableSettings.putInt("Shirt", i - 1);
            }
        }
        editableSettings.commit();
        fillAvatar();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            return gestureDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    // ======================== Asynchronous tasks gestion ===========================

    /**
     * Asynchronous task to save the data while displaying a progressBar and overlay layout
     * to prevent the user's interaction with UI while saving data before switching activity.
     */
    private class SavingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressOverlay.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressOverlay.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground(Void... params) {

            avatar.updateImage(getApplicationContext());
            db.storeAvatar(avatar);
            Intent intent = new Intent(EditAvatar.this, MainActivity.class);
            startActivity(intent);

            return null;
        }
    }
}