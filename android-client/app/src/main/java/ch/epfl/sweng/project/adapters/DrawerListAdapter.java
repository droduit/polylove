package ch.epfl.sweng.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.ui.NavItemModel;

/**
 * Adapter to transform a NavItemModel object in
 * an item of a ListView Adapter.
 *
 * @author Dominique Roduit
 */
public final class DrawerListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<NavItemModel> navItemModels;

    /**
     * Create an Adapter to fit a list of NavItemModel objects in a ListView
     * @param context Context of the application
     * @param navItemModels Items to adapt
     */
    public DrawerListAdapter(Context context, ArrayList<NavItemModel> navItemModels) {
        this.context = context;
        this.navItemModels = navItemModels;
    }

    @Override
    public int getCount() {
        return navItemModels.size();
    }

    @Override
    public Object getItem(int position) {
        return navItemModels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.swipe_menu_item, null);
        }
        else {
            view = convertView;
        }

        TextView titleView = (TextView) view.findViewById(R.id.title);
        TextView subtitleView = (TextView) view.findViewById(R.id.subTitle);
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);

        titleView.setText( navItemModels.get(position).getTitle() );
        subtitleView.setText( navItemModels.get(position).getSubtitle() );
        iconView.setImageResource(navItemModels.get(position).getIcon());

        return view;
    }
}
