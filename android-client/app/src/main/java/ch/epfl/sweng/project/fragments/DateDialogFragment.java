package ch.epfl.sweng.project.fragments;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ch.epfl.sweng.project.utils.DBUtils;
import ch.epfl.sweng.project.models.Settings;

/**
 * Display a dialog calendar and store the picked date as a value
 * of the SharedPreference file, associated with the key "tempBirthday"
 * @author Dominique Roduit
 */
@SuppressLint("ValidFragment")
public final class DateDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    private Button btDate;

    public DateDialogFragment(View view){
        btDate = (Button) view;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Settings settings = Settings.getInstance(getActivity());

        final Calendar c = Calendar.getInstance();

        String today = DBUtils.DateToString(new Date());
        String dateStr = settings.getObject().getString("tempBirthday", today);

        if(!dateStr.equals(today)) {
            Date date = DBUtils.StringToDate(dateStr);
            c.setTime(date);
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(getActivity(), this, year, month, day);
        long currentTime = System.currentTimeMillis();
        long oneYearTime = 31536000000L;
        dp.getDatePicker().setMaxDate(currentTime - oneYearTime*15);
        dp.getDatePicker().setMinDate(currentTime - oneYearTime*80);

        return dp;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        String dateStr = year + "-" + (month + 1) + "-" + day;

        Settings settings = Settings.getInstance(getActivity());
        Locale locale = (settings.getLanguage()==0) ? Locale.FRENCH : Locale.ENGLISH;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", locale);

        Date date = DBUtils.StringToDate(dateStr);

        btDate.setText(dateFormat.format(date));

        SharedPreferences.Editor editableSettings = settings.getEditableSettings();
        editableSettings.putString("tempBirthday", dateStr);
        editableSettings.commit();
    }

}

