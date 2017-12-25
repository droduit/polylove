package ch.epfl.sweng.project;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import ch.epfl.sweng.project.fragments.SettingsFragment;

/**
 * Activity whose purpose is to provide the user an interface to handle
 * its preferences related to this application.
 *
 * The concrete content of this activity is mainly handle by the
 * attached fragment "SettingsFragment".
 *
 * @author Dominique Roduit
 */
public final class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction()
                .replace(R.id.mainContent, new SettingsFragment())
                .commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save_btn, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_action:
                finish();
                break;
        }
        return true;
    }

}