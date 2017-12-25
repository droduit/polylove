package ch.epfl.sweng.project.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.ui.CheckBoxModel;

/**
 * Adapter to transform a CheckBoxModel object in
 * an item of a ListView Adapter.
 *
 * @author Dominique Roduit
 */
public final class CheckBoxAdapter extends ArrayAdapter {

    private CheckBoxModel[] checkBoxModelItems = null;
    private Context context;

    /**
     * Create an Adapter to fit an Array of CheckBoxModel objects in a ListView
     * @param context Context of the application
     * @param resource Array of CheckBoxModel to adapt
     */
    public CheckBoxAdapter(Context context, CheckBoxModel[] resource) {
        super(context, R.layout.item_row_checkbox, resource);
        this.context = context;
        this.checkBoxModelItems = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        convertView = inflater.inflate(R.layout.item_row_checkbox, parent, false);

        CheckBox cb = (CheckBox) convertView.findViewById(R.id.checkBox1);

        cb.setText(checkBoxModelItems[position].getLabel());
        cb.setChecked(checkBoxModelItems[position].isCheckedDefault() || checkBoxModelItems[position].isChecked());
        cb.setTag(checkBoxModelItems[position]);

        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CheckBoxModel cb = ((CheckBoxModel)buttonView.getTag());
                cb.setToggleChecked();
            }
        });

        return convertView;
    }
}
