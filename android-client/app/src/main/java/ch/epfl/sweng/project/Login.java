package ch.epfl.sweng.project;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.tequila.AuthenticationProcess;
import ch.epfl.sweng.project.utils.AndroidUtils;


/**
 * Login with Tequila connection
 */
public final class Login extends AppCompatActivity {

    public static final String EXTRA_AUTO_REDIRECT = "auto-redirect";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DBHandler db = DBHandler.getInstance(getApplicationContext());
        Settings settings = Settings.getInstance(getApplicationContext());

        Bundle bundle = getIntent().getExtras();
        boolean autoRedirect = settings.hasAutoLogin();
        if(bundle != null) {
            if(bundle.containsKey(EXTRA_AUTO_REDIRECT)) {
                autoRedirect = bundle.getBoolean(EXTRA_AUTO_REDIRECT);
            }
        }

        // Redirect the user to the right activity according to what
        // we already have in local cache
        // -------------------------------------------------------------
        if(autoRedirect) {
            User user = db.getUser(settings.getUserID());
            Profile profile = null;
            Avatar avatar = null;

            if (user != null) {
                profile = user.get_profile(getApplicationContext());
                avatar = user.getAvatar(getApplicationContext());
            }

            Class<?> nextIntent = AndroidUtils.getNextActivity(user, profile, avatar);
            if(nextIntent != Login.class) {
                startActivity(new Intent(this, nextIntent));
            }
        }
        // -------------------------------------------------------------

        // Handle click of the Login button ------------------------------------
        Button btLogin = (Button) findViewById(R.id.btLogin);
        btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthenticationProcess.connect(Login.this);
            }
        });
        // -------------------------------------------------------------

    }

}