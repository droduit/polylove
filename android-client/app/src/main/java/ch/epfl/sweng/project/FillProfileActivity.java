package ch.epfl.sweng.project;

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.makeramen.roundedimageview.RoundedImageView;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ch.epfl.sweng.project.fragments.CameraFragment;
import ch.epfl.sweng.project.fragments.DateDialogFragment;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;
import ch.epfl.sweng.project.network.NetworkStatus;
import ch.epfl.sweng.project.network.ProfileSender;
import ch.epfl.sweng.project.utils.AndroidUtils;
import ch.epfl.sweng.project.utils.DBUtils;

import static ch.epfl.sweng.project.R.id.datePicker;
import static ch.epfl.sweng.project.R.id.hobbies;

/**
 * Activity to fill the user's profile.
 * This activity is used for first filling and subsequent update of the user's profile.
 *
 * @author Lucie Perrota, Dominique Roduit
 */

public final class FillProfileActivity extends AppCompatActivity {

    // Network status
    private static Handler networkStatusHandler;

    // For getting the infos from other activities
    private String returnedLanguage = "";
    private final int code_language = 1001;

    // Profile to be filled
    private Profile newProfile = new Profile();
    private boolean profileDone = false;
    private boolean updatingMode = false;

    // Will contains the list of hobbies
    private List<String> arrayListHobbies;

    // Visual elements
    private Button btLang;
    private EditText hobbiesEditText, editHobbies, descriptionEditText;
    private ListView lvHobbies;
    private TextView tvDeleteItem;
    private RangeSeekBar<Integer> ageInterestSeekBar;
    private View progressOverlay;
    private Menu menu;

    private BroadcastReceiver receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fill_profile);

        long userId = Settings.getInstance(getApplicationContext()).getUserID();
        newProfile.setUserId(userId);

        Profile existingProf = DBHandler.getInstance(getApplicationContext()).getProfile(userId);
        updatingMode = (existingProf != null);

        // Register BroadcastReceiver to track network connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                performNetworkActions();
            }
        };
        this.registerReceiver(receiver, filter);

        // Get UI components -----------------
        Button btDate = (Button) findViewById(datePicker);
        hobbiesEditText = (EditText) findViewById(hobbies);
        lvHobbies = (ListView) findViewById(R.id.lvHobbies);
        tvDeleteItem = (TextView) findViewById(R.id.tvDeleteItem);
        editHobbies = (EditText) findViewById(hobbies);
        descriptionEditText = (EditText) findViewById(R.id.description);
        btLang = (Button) findViewById(R.id.languagesButton);
        ageInterestSeekBar = (RangeSeekBar<Integer>) findViewById(R.id.ageInterest);
        progressOverlay = findViewById(R.id.progress_overlay);
        Button btAddHobby = (Button) findViewById(R.id.btAddHobby);
        // -----------------------------------


        // Button Choose Section -----------------
        final Button btSection = (Button) findViewById(R.id.btSection);
        final String[] sectionsArray = new String[Profile.Section.values().length];
        int i = 0;
        for (Profile.Section sect : Profile.Section.values()) {
            sectionsArray[i] = getString(sect.getStringId());
            ++i;
        }

        btSection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FillProfileActivity.this);
                builder.setTitle(R.string.section)
                        .setItems(sectionsArray, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int index) {
                                Profile.Section chosenSection = Profile.Section.values()[index];
                                btSection.setText(getString(chosenSection.getStringId()));
                                newProfile.setSection(chosenSection);
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
        // ----------------------------------------

        // Button Choose Gender -----------------
        final Button btGender = (Button) findViewById(R.id.btGender);
        final String[] gendersArray = new String[Profile.Gender.values().length];
        int j = 0;
        for (Profile.Gender gen : Profile.Gender.values()) {
            gendersArray[j] = gen.toString();
            ++j;
        }

        btGender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FillProfileActivity.this);
                builder.setTitle(R.string.genderInput)
                        .setItems(gendersArray, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int index) {
                                Profile.Gender chosenGender = Profile.Gender.values()[index];
                                btGender.setText(chosenGender.toString());
                                newProfile.setGender(chosenGender);
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
        // ----------------------------------------

        // Button Choose Gender Interest -----------------
        final Button btGenderInterest = (Button) findViewById(R.id.btGenderInterest);
        final String[] genderInterestArray = new String[Profile.GenderInterest.values().length];
        int k = 0;
        for (Profile.GenderInterest inter : Profile.GenderInterest.values()) {
            genderInterestArray[k] = inter.toString();
            ++k;
        }

        btGenderInterest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FillProfileActivity.this);
                builder.setTitle(R.string.genderInterest_input)
                        .setItems(genderInterestArray, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int index) {
                                Profile.GenderInterest chosenGenderInterest = Profile.GenderInterest.values()[index];
                                btGenderInterest.setText(chosenGenderInterest.toString());
                                newProfile.setGenderInterest(chosenGenderInterest);
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
        // ----------------------------------------

        // Birthday Calendar -----------------
        Button btCalendar = (Button) findViewById(datePicker);
        btCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateDialogFragment dialog = new DateDialogFragment(v);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                dialog.show(ft, "DatePicker");
            }
        });
        // ------------------------------------

        // Hobbies  ---------------------------
        arrayListHobbies = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, arrayListHobbies) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black_color));
                return textView;
            }
        };
        lvHobbies.setAdapter(adapter);

        // Add hobby item
        btAddHobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editHobbies.getText().toString().trim().isEmpty()) {
                    if (editHobbies.getText().toString().length() >= 3) {
                        lvHobbies.setVisibility(View.VISIBLE);
                        tvDeleteItem.setVisibility(View.VISIBLE);

                        arrayListHobbies.add(0, editHobbies.getText().toString().trim());
                        adapter.notifyDataSetChanged();

                        AndroidUtils.setListViewHeightBasedOnItems(lvHobbies);
                        editHobbies.setText("");
                    } else {
                        AndroidUtils.displayDialog(FillProfileActivity.this, null, getString(R.string.hobbies_error_msg));
                    }
                }
            }
        });

        // Delete Hobbies items
        lvHobbies.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {

                arrayListHobbies.remove(pos);
                adapter.notifyDataSetChanged();

                AndroidUtils.setListViewHeightBasedOnItems(lvHobbies);

                if (arrayListHobbies.size() <= 0) {
                    tvDeleteItem.setVisibility(View.GONE);
                }

                return true;
            }
        });
        // ---------------------------------------------------

        // Age Interest Range Seekbar  -----------------
        ageInterestSeekBar.setRangeValues(Profile.MIN_AGE, Profile.MAX_AGE);
        ageInterestSeekBar.setSelectedMinValue(18);
        ageInterestSeekBar.setSelectedMaxValue(26);
        ageInterestSeekBar.setTextAboveThumbsColorResource(R.color.black_color);
        // ---------------------------------------------

        // If user's profile did already exists, we are in edit mode -> autocomplete the inputs
        updatingMode(updatingMode, existingProf, btDate, btSection, btGender, btGenderInterest, adapter);

        // Select language on click button  ----------------------------
        // let this block at this place, because we need the state variables it uses at this point
        btLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(FillProfileActivity.this, FillProfileActivity_lang.class);
                intent.putExtra(FillProfileActivity_lang.EXTRA_SELECTED_ITEMS, returnedLanguage);
                startActivityForResult(intent, code_language);
            }
        });
        // -------------------------------------------------------------

    }

    protected void updatingMode(boolean updatingMode, Profile existingProf, Button btDate, Button btSection, Button btGender, Button btGenderInterest, ArrayAdapter<String> adapter) {
        if (updatingMode) {
            // Initialize fields set by a button press
            newProfile.setSection(existingProf.getSection());
            newProfile.setGender(existingProf.getGender());
            newProfile.setGenderInterest(existingProf.getGenderInterest());

            // Birthday ---------------------------
            SharedPreferences.Editor editableSettings = Settings.getInstance(getApplicationContext()).getEditableSettings();
            assert existingProf != null;
            editableSettings.putString("tempBirthday", DBUtils.DateToString(existingProf.getBirthday()));
            editableSettings.commit();

            Settings settings = Settings.getInstance(getApplicationContext());
            Locale locale = settings.getLocale();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", locale);
            Date date = DBUtils.StringToDate(DBUtils.DateToString(existingProf.getBirthday()));
            btDate.setText(dateFormat.format(date));
            // ------------------------------------

            // Section ---------------------------
            btSection.setText(getString(existingProf.getSection().getStringId()));
            // -----------------------------------

            // Hobbies ----------------------------
            arrayListHobbies.addAll(existingProf.getHobbies());
            if (arrayListHobbies.size() > 0) {
                lvHobbies.setVisibility(View.VISIBLE);

                adapter.notifyDataSetChanged();
                AndroidUtils.setListViewHeightBasedOnItems(lvHobbies);
            }
            // ------------------------------------

            // Description ----------------------------
            descriptionEditText.setText(existingProf.getDescription());

            // Languages ----------------------------
            returnedLanguage = existingProf.getLanguagesString();
            btLang.setText(newProfile.getLanguageSetToString(getApplicationContext(), existingProf.getLanguages()));

            // Gender --------------------------------
            btGender.setText(existingProf.getGender().toString());
            btGenderInterest.setText(existingProf.getGenderInterest().toString());

            // Age Interest ----------------------------
            ageInterestSeekBar.setSelectedMinValue(existingProf.getAgeInterestStart());
            ageInterestSeekBar.setSelectedMaxValue(existingProf.getAgeInterestEnd());

            // User Profile's photo
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_camera);
            if (fragment != null) {
                RoundedImageView thumbPhoto = (RoundedImageView) fragment.getView().findViewById(R.id.thumbPhoto);
                if (thumbPhoto != null) {
                    Bitmap photo = existingProf.getPhoto();
                    if (photo != null) {
                        thumbPhoto.setImageBitmap(photo);
                        thumbPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    } else {
                        thumbPhoto.setImageResource(R.drawable.ic_local_see_black_24dp);
                        thumbPhoto.setScaleType(ImageView.ScaleType.CENTER);
                    }
                }
            }
        } else {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_camera);
            if (fragment != null) {
                RoundedImageView thumbPhoto = (RoundedImageView) fragment.getView().findViewById(R.id.thumbPhoto);
                if (thumbPhoto != null) {
                    thumbPhoto.setImageResource(R.drawable.ic_local_see_black_24dp);
                    thumbPhoto.setScaleType(ImageView.ScaleType.CENTER);
                }
            }
        }
    }


    private void performNetworkActions() {
        boolean networkAvailable = NetworkStatus.getInstance(getApplicationContext()).isConnected();

        AndroidUtils.displayNetworkStatus(FillProfileActivity.this, networkAvailable);
        if (menu != null) {
            MenuItem saveItem = menu.getItem(0);
            if (saveItem != null) {
                saveItem.setVisible(networkAvailable);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    @Override
    public void onBackPressed() {
        if (!updatingMode) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_action:
                item.setVisible(false);
                progressOverlay.setVisibility(View.VISIBLE);
                goToNext(item);
                break;
        }
        return true;
    }

    /**
     * Get the spoken languages from the languages_activity
     *
     * @param requestCode code to recognize who called languages_activity
     * @param resultCode  check if the result is RESULT_OK
     * @param data        actual spoken languages as a string of numbers
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_camera);
        fragment.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == code_language) {
                returnedLanguage = data.getData().toString();
                Set<Profile.Language> langs = DBUtils.StringToLanguageSet(returnedLanguage);
                String langStr = (langs.size() > 0) ? newProfile.getLanguageSetToString(getApplicationContext(), langs) : getString(R.string.language);

                if (langs.size() > 0)
                    btLang.setText(langStr);

                Log.d("language", returnedLanguage);

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*


    /**
     * Collects the fields infos, put them into the Profile object
     *
     * @param newProfile profile to be filled
     * @return the actual profile (even if an error occured)
     */
    public Profile fillProfile(Profile newProfile) {
        CameraFragment cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera);
        if (cameraFragment != null) {
            Bitmap image = cameraFragment.getBitmapImg();
            newProfile.setPhoto(image);
        }

        // BIRTHDAY
        SharedPreferences settings = Settings.getInstance(getApplicationContext()).getObject();
        String today = DBUtils.DateToString(new Date());
        String birthday = settings.getString("tempBirthday", today);
        newProfile.setBirthday(birthday);
        Log.d("Birthday", birthday);

        // DESCRIPTION
        String description = descriptionEditText.getText().toString();
        newProfile.setDescription(description);
        Log.d("Description", description);

        // HOBBIES
        String hobbies = "";
        if (arrayListHobbies != null && arrayListHobbies.size() > 0) {
            for (String hobby : arrayListHobbies) {
                hobbies += hobby.trim() + ", ";
            }
            hobbies = hobbies.substring(0, hobbies.length() - 2);
        } else {
            hobbies = hobbiesEditText.getText().toString();
        }

        if (hobbies.isEmpty()) {
            AndroidUtils.displayDialog(FillProfileActivity.this, null, this.getResources().getString(R.string.interError));
            return newProfile;
        } else {
            newProfile.setHobbies(hobbies);
        }
        Log.d("Hobbies", hobbies);

        // LANGUAGE
        if (returnedLanguage.isEmpty()) {
            AndroidUtils.displayDialog(FillProfileActivity.this, null, this.getResources().getString(R.string.langError));
            return newProfile;
        }
        newProfile.setLanguages(returnedLanguage);
        Log.d("Languages", returnedLanguage);

        // DESIRED AGE
        int ageInterestStart = ageInterestSeekBar.getSelectedMinValue();
        int ageInterestEnd = ageInterestSeekBar.getSelectedMaxValue();
        newProfile.setAgeInterestStart(ageInterestStart);
        newProfile.setAgeInterestEnd(ageInterestEnd);
        Log.d("AgeStart", String.valueOf(ageInterestStart));
        Log.d("AgeEnd", String.valueOf(ageInterestEnd));

        // DONE
        profileDone = true;
        return newProfile;
    }


    /**
     * Activates when pressing the menu "thick" button,
     * fills the profile and send it to both the local and server DB's
     */
    public void goToNext(MenuItem item) {

        fillProfile(newProfile);
        if (!profileDone) {
            item.setVisible(true);
            progressOverlay.setVisibility(View.GONE);
            return;
        }

        // Send profile data to server DB (and then store in local db if success)
        try {

            ProfileSender.sendProfile(newProfile, new NetworkResponseManager() {

                @Override
                public void onError(int errorCode) {
                    progressOverlay.setVisibility(View.GONE);
                    AndroidUtils.displayDialog(FillProfileActivity.this, "Network error", "Code " + errorCode);
                    if (errorCode == 401) {
                        startActivity(AndroidUtils.gotoLogin(FillProfileActivity.this));
                    }
                }

                @Override
                public void onSuccess(JSONObject jsonObj) {
                    new SavingTask().execute();
                }
            });

        } catch (Exception error) {
            System.err.println("Oooops... Something went very wrong: " + error.getMessage());
        }
    }

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
            DBHandler.getInstance(getApplicationContext()).storeProfile(newProfile);

            if (!updatingMode) {
                Intent intent = new Intent(FillProfileActivity.this, EditAvatar.class);
                Profile.Gender slctGender = (profileDone) ? newProfile.getGender() : Profile.Gender.Male;
                intent.putExtra("gender", slctGender);
                startActivity(intent);
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(FillProfileActivity.this, getString(R.string.saved_infos), Toast.LENGTH_SHORT).show();
                    }
                });
                finish();
            }
            return null;
        }
    }
}
