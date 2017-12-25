package ch.epfl.sweng.project.tequila;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.sweng.project.FillProfileActivity;
import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;
import ch.epfl.sweng.project.network.NetworkResponseManagerForImages;
import ch.epfl.sweng.project.network.RequestsHelper;
import ch.epfl.sweng.project.network.TequilaUtils;
import ch.epfl.sweng.project.network.firebase.InstanceIDService;
import ch.epfl.sweng.project.utils.AndroidUtils;

/**
 * Login activity which receipts the Tequila code, send it to the server,
 * and in case of success, connect the user to its account after fetching
 * from the server the relevant related information about it
 * (profile, avatar, photo, etc ...) if they already exist before.
 *
 * In case of failure, a simple dialog displaying the error appears.
 *
 * @author Simon Guilloud, Dominique Roduit
 */
public final class LogActivity extends AppCompatActivity {

    private static DBHandler db = null;
    private static Settings settings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            db = DBHandler.getInstance(getApplicationContext());
            settings = Settings.getInstance(getApplicationContext());

            Uri uri = intent.getData();
            final String tequilaCode = uri.getQueryParameter("code");
            final LogActivity thisClass = this;

            TequilaUtils.sendToken(tequilaCode, new NetworkResponseManager() {
                @Override
                public void onSuccess(JSONObject response) {
                    Log.d("LogActivity response", response.toString());

                    Boolean isNewUser = Boolean.FALSE;
                    InstanceIDService.sendRegistrationToServer(FirebaseInstanceId.getInstance().getToken(), thisClass);

                    try {
                        isNewUser = (Boolean) response.get("created");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (isNewUser) {
                        try {
                            User user = null;
                            if (!response.isNull("user")) {
                                user = User.fromJSON(response.getJSONObject("user"));
                            }

                            long createdUserId = db.storeUser(user);
                            if (createdUserId > -1) {
                                settings.setUserID(createdUserId);

                                Intent nextIntent = new Intent(thisClass, FillProfileActivity.class);
                                startActivity(nextIntent);
                            } else {
                                AndroidUtils.displayDialog(thisClass, null, getString(R.string.error_user_insertion));
                                startActivity(AndroidUtils.gotoLogin(thisClass));
                            }

                        } catch (JSONException | ClassCastException e) {
                            e.printStackTrace();
                        }

                    } else {
                        try {
                            // Store received User
                            User user = null;
                            if (!response.isNull("user")) {
                                user = User.fromJSON(response.getJSONObject("user"));
                            }
                            long userId = DBHandler.getInstance(getApplicationContext()).storeUser(user);

                            if (userId > -1) {
                                settings.setUserID(userId);

                                // Store received Profile
                                Profile profile = null;
                                if (!response.isNull("profile")) {
                                    // Need this final variable to be used in the fetch picture request
                                    JSONObject profileJSON = response.getJSONObject("profile");
                                    profileJSON.put(Profile.KEY_ID, userId);
                                    final Profile profileTmp = Profile.fromJSON(profileJSON);

                                    DBHandler.getInstance(getApplicationContext()).storeProfile(profileTmp);

                                    // Request profile's photo
                                    RequestsHelper.requestPhoto(new NetworkResponseManagerForImages() {
                                        @Override
                                        public void onError(int errorCode) {
                                            Log.d("LOG FETCH IMAGE", String.valueOf(errorCode));
                                        }

                                        @Override
                                        public void onSuccess(Bitmap bitmap) {
                                            if (bitmap != null) {
                                                profileTmp.setPhoto(bitmap);
                                                DBHandler.getInstance(getApplicationContext()).storeProfile(profileTmp);
                                            }
                                            assert bitmap != null;
                                            Log.d("LOG FETCH IMAGE", bitmap.toString());
                                        }
                                    });

                                    profile = profileTmp;
                                }

                                // Store received Avatar
                                Avatar avatar = null;
                                if (!response.isNull("avatar")) {
                                    JSONObject avatarJSON = response.getJSONObject("avatar");
                                    avatarJSON.put(Avatar.KEY_ID, userId);
                                    avatar = Avatar.fromJSON(avatarJSON);
                                }
                                db.storeAvatar(avatar);

                                Class<?> nextActivity = AndroidUtils.getNextActivity(user, profile, avatar);
                                startActivity(new Intent(thisClass, nextActivity));
                            } else {
                                AndroidUtils.displayDialog(thisClass, null, getString(R.string.error_user_insertion));
                                startActivity(AndroidUtils.gotoLogin(thisClass));
                            }

                        } catch (JSONException | ClassCastException e) {
                            e.printStackTrace();
                        }
                    }
                }

                public void onError(int errorCode) {
                    new AlertDialog.Builder(thisClass)
                            .setTitle("Network Error")
                            .setMessage("An unexpected error " + errorCode + " has occured. Try again later. Sorry for the inconvenience.")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(AndroidUtils.gotoLogin(thisClass));
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });
        }
    }
}