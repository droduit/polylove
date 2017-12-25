package ch.epfl.sweng.project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;
import ch.epfl.sweng.project.network.NetworkResponseManagerForImages;
import ch.epfl.sweng.project.network.RequestsHelper;
import ch.epfl.sweng.project.utils.AndroidUtils;

import static ch.epfl.sweng.project.network.RequestsHelper.requestAvatar;

/**
 * Activity to display the profile of a given user.
 * Informations are displayed following some rules, according to
 * the different values of Match.Status.
 * <p>
 * - Match.Status.Pending
 * Users are in the "try period", they can only see a few infos about the user
 * User's avatar is displayed if exists.
 * <p>
 * - Match.Status.Open
 * Users both decided to match together, they can see their full profile.
 * User's photo is displayed if exists.
 * <p>
 * - Match.Status.Close
 * No right ! The "try period" is gone and they don't decided to match together.
 *
 * @author Dominique Roduit
 */
public final class ViewProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_MATCH_CONFIRMATION = "matchConfirmation";

    private static DBHandler db = null;

    private long userId = -1;
    private boolean isMyProfile = true;

    private Match match;

    private ImageView appBarImage;
    private ImageView imageDisplay;
    private TextView tvBirthday, tvDescription, tvLanguages, tvSection;
    private ListView lvHobbies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        Bundle b = getIntent().getExtras();
        long myUserId = Settings.getInstance(getApplicationContext()).getUserID();
        userId = (b != null) ? b.getLong(EXTRA_USER_ID) : myUserId;

        db = DBHandler.getInstance(getApplicationContext());

        User user = db.getUser(userId);
        match = db.getMatchWithUser(userId);

        // UI Components
        // ----------------------------------------------------
        FloatingActionButton btnActionMessage = (FloatingActionButton) findViewById(R.id.btnActionMessage);
        LinearLayout layoutProfile = (LinearLayout) findViewById(R.id.profileLayout);
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        appBarImage = (ImageView) findViewById(R.id.app_bar_image);

        imageDisplay = (ImageView) findViewById(R.id.imageDisplay);

        tvBirthday = (TextView) findViewById(R.id.tvBirthday);
        tvDescription = (TextView) findViewById(R.id.tvDescription);
        tvLanguages = (TextView) findViewById(R.id.tvLanguages);
        tvSection = (TextView) findViewById(R.id.tvSection);
        lvHobbies = (ListView) findViewById(R.id.lvHobbies);
        // ----------------------------------------------------

        // Attach custom toolbar to the activity
        // ----------------------------------------------------
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // ----------------------------------------------------

        userIdCheck(myUserId, btnActionMessage, match);

        profileCheck(isMyProfile, btnActionMessage, layoutProfile, collapsingToolbar);

        Profile profile = db.getProfile(userId);
        updateUI(user, profile);
    }

    protected void profileCheck(boolean isMyProfile, FloatingActionButton btnActionMessage, LinearLayout layoutProfile, CollapsingToolbarLayout collapsingToolbar) {
        if (!isMyProfile) {
            if (match != null) {
                if (match.getState() != Match.State.Open) {
                    layoutProfile.setVisibility(View.GONE);

                    // Set the collapsing toolbar fixed
                    AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) collapsingToolbar.getLayoutParams();
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP);

                    // Set the height of the collapsing toolbar
                    AppBarLayout appBar = (AppBarLayout) findViewById(R.id.appbar);
                    float heightDp = getResources().getDisplayMetrics().heightPixels;
                    CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) appBar.getLayoutParams();
                    lp.height = (int) heightDp;

                    btnActionMessage.setVisibility(View.GONE);
                }

                btnActionMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }
        }
    }

    protected void userIdCheck(long myUserId, FloatingActionButton btnActionMessage, Match match) {
        if (userId != myUserId) {
            isMyProfile = false;

            // Get informations about user from server
            if (match != null) {
                if (match.getState() == Match.State.Open) {
                    executeFetchTask(true);
                } else {
                    executeFetchTask(false);
                }
            }
        } else {
            btnActionMessage.setVisibility(View.GONE);
            displayMyAvatar();
        }
    }

    protected void updateUI(final User user, final Profile profile) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                Log.d("update UI", "appelé");

                String displayedName = "";
                if (match != null && match.getState() == Match.State.Open) {
                    displayedName = match.getFriendlyUsername(getApplicationContext());
                }
                if (isMyProfile && user != null) {
                    displayedName = user.getFullName();
                }
                setTitle(displayedName);

                if (profile != null) {
                    appBarImage = (ImageView) findViewById(R.id.app_bar_image);

                    Bitmap photo = profile.getPhoto();
                    if (photo != null) {
                        appBarImage.setImageBitmap(photo);
                        ImageView avatarDisplay = (ImageView) findViewById(R.id.imageDisplay);
                        avatarDisplay.setVisibility(View.GONE);

                        appBarImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(ViewProfileActivity.this, FullImageActivity.class);
                                intent.putExtra(EXTRA_USER_ID, userId);
                                startActivity(intent);
                            }
                        });
                    }

                    tvBirthday.setText(profile.getFormattedBirthday(getApplicationContext()));
                    tvDescription.setText(profile.getDescription());
                    tvLanguages.setText(profile.getLanguageSetToString(getApplicationContext(), profile.getLanguages()));
                    tvSection.setText(getString(profile.getSection().getStringId()));


                    List<String> arrayListHobbies = new ArrayList<>();
                    final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, android.R.id.text1, arrayListHobbies) {
                        @NonNull
                        @Override
                        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                            TextView textView = (TextView) super.getView(position, convertView, parent);
                            textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black_color));
                            return textView;
                        }
                    };
                    lvHobbies.setAdapter(adapter);
                    arrayListHobbies.addAll(profile.getHobbies());
                    if (arrayListHobbies.size() > 0) {
                        lvHobbies.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                        AndroidUtils.setListViewHeightBasedOnItems(lvHobbies);
                    }
                }

            }
        });


    }

    private void displayMyAvatar() {
        displayAvatar(db.getAvatar(userId));
    }

    private void displayAvatar(final Avatar av) {
        if (av != null) {
            imageDisplay.setImageBitmap(av.getImage(getApplicationContext()));
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public void executeFetchTask(final boolean matchIsOpen) {

        if (matchIsOpen) {
            RequestsHelper.requestUser(userId, new NetworkResponseManager() {
                @Override
                public void onError(int errorCode) {
                    executeWithFailure(errorCode);
                }

                @Override
                public void onSuccess(JSONObject jsonObj) {
                    onSuccessFetch(jsonObj, true);
                }
            });
        } else {
            // Avatar request
            requestAvatar(userId, new NetworkResponseManager() {
                @Override
                public void onError(int errorCode) {
                    executeWithFailure(errorCode);
                }

                @Override
                public void onSuccess(JSONObject jsonObj) {
                    onSuccessAvatarFetch(jsonObj, true);
                }
            });
        }
    }

    protected void onSuccessAvatarFetch(JSONObject jsonObj, boolean update) {
        Log.d("avatar request respons", jsonObj.toString());
        try {
            Avatar avatar = null;
            if (!jsonObj.isNull("avatar")) {
                JSONObject avatarJSON = jsonObj.getJSONObject("avatar");

                if (!jsonObj.isNull(Avatar.KEY_GENDER)) {
                    avatarJSON.put(Avatar.KEY_GENDER, jsonObj.getString(Avatar.KEY_GENDER));
                }

                avatarJSON.put(Avatar.KEY_ID, userId);

                avatar = Avatar.fromJSON(avatarJSON);
                db.storeAvatar(avatar);
            }

            if (update) updateAll(false, null, null, avatar);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void onSuccessFetch(JSONObject jsonObj, boolean update) {
        try {
            User user = null;
            if (!jsonObj.isNull("user")) {
                user = User.fromJSON(jsonObj.getJSONObject("user"));
                db.storeUser(user);
            }

            Profile profile = null;
            if (!jsonObj.isNull("profile")) {
                JSONObject profileJSON = jsonObj.getJSONObject("profile");
                if (user != null) {
                    profileJSON.put(Profile.KEY_ID, user.getId());
                }
                profile = Profile.fromJSON(profileJSON);
                db.storeProfile(profile);
            }

            Avatar avatar = null;
            if (!jsonObj.isNull("avatar")) {
                JSONObject avatarJSON = jsonObj.getJSONObject("avatar");
                if (user != null) {
                    avatarJSON.put(Avatar.KEY_ID, user.getId());
                }
                if (profile != null) {
                    avatarJSON.put(Avatar.KEY_GENDER, profile.getGender().toString());
                }
                avatar = Avatar.fromJSON(avatarJSON);
                db.storeAvatar(avatar);
            }

            if (update) updateAll(true, user, profile, avatar);

            RequestsHelper.requestPhoto(userId, new NetworkResponseManagerForImages() {
                @Override
                public void onError(int errorCode) {
                }

                @Override
                public void onSuccess(Bitmap bitmap) {
                    onSuccessPhoto(bitmap);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void onSuccessPhoto(Bitmap bitmap) {
        if (bitmap != null) {
            Profile profile = db.getProfile(userId);
            if (profile != null) {
                profile.setPhoto(bitmap);
                db.storeProfile(profile);
                updateAll(true, db.getUser(userId), profile, db.getAvatar(userId));
            }
        }
    }

    protected void updateAll(boolean matchIsOpen, User user, Profile profile, Avatar avatar) {
        db.storeAvatar(avatar);
        db.storeProfile(profile);
        db.storeUser(user);

        if (matchIsOpen) {
            updateUI(user, profile);
        }

        displayAvatar(avatar);
    }

    private void executeWithFailure(int errorCode) {
        if (errorCode == 401) {
            startActivity(AndroidUtils.gotoLogin(ViewProfileActivity.this));
        }
    }

}