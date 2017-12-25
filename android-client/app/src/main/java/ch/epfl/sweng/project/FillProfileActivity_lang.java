package ch.epfl.sweng.project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ch.epfl.sweng.project.adapters.CheckBoxAdapter;
import ch.epfl.sweng.project.models.ui.CheckBoxModel;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.utils.AndroidUtils;
import ch.epfl.sweng.project.utils.DBUtils;

/**
 * Provide an sub-interface to choose several languages the user speaks.
 * Called from the FillProfileActivity.
 * @author Dominique Roduit
 */
public final class FillProfileActivity_lang extends AppCompatActivity {

    public static final String EXTRA_SELECTED_ITEMS = "selectedItems";
    private ListView lv;
    private List<CheckBoxModel> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_languages);

        Intent intent = getIntent();
        Bundle b = intent.getExtras();

        String selectedItems = "";
        Set<Profile.Language> slctItemsSet = new TreeSet<>();
        if(b != null) {
            selectedItems = b.getString(EXTRA_SELECTED_ITEMS);
            slctItemsSet = DBUtils.StringToLanguageSet(selectedItems);
            Log.d("select", selectedItems);
        }

        lv = (ListView) findViewById(R.id.lvLanguages);

        Profile.Language[] langs = Profile.Language.values();

        if (langs != null) {
            boolean selected = false;
            for (int i = 0; i < langs.length; ++i) {
                if(slctItemsSet != null && slctItemsSet.size() > 0) {
                    selected = slctItemsSet.contains(langs[i]);
                }
                items.add(new CheckBoxModel(langs[i].toString(), i, selected));
            }
        }

        CheckBoxModel[] arrayItems = items.toArray(new CheckBoxModel[items.size()]);
        final CheckBoxAdapter checkBoxAdapter = new CheckBoxAdapter(this, arrayItems);
        lv.setAdapter(checkBoxAdapter);
        lv.setItemsCanFocus(false);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save_btn, menu);
        return true;
    }

    private Set<Integer> getSelectedValues() {
        Set<Integer> langs = new TreeSet<>();
        for (int i = 0; i < lv.getAdapter().getCount(); i++) {
            CheckBoxModel m = (CheckBoxModel) lv.getAdapter().getItem(i);
            if (m.isChecked()) {
                langs.add(m.getPosition());
            }
        }
        return langs;
    }

    public String valuesToString(Set<Integer> values) {
        if (values == null) return null;

        String str = "";
        int idx = 0;
        for (Integer l : values) {
            str += l.toString();
            ++idx;
            if (idx < values.size())
                str += ", ";
        }
        return str;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_action:

                if(save()) {
                    Intent data = new Intent();
                    String text = valuesToString(getSelectedValues());
                    // set the data to pass back
                    data.setData(Uri.parse(text));
                    setResult(RESULT_OK, data);

                    // close the activity
                    finish();
                }

                break;
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private boolean save() {
        Set<Integer> slctValues = getSelectedValues();
        Log.d("Selected values", valuesToString(getSelectedValues()));

        if(slctValues.size() <= 0) {
            AndroidUtils.displayDialog(FillProfileActivity_lang.this, null, getString(R.string.langError));
            return false;
        }
        return true;
    }


}
