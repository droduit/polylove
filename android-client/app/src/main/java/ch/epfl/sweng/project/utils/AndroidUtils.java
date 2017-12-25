package ch.epfl.sweng.project.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ch.epfl.sweng.project.EditAvatar;
import ch.epfl.sweng.project.FillProfileActivity;
import ch.epfl.sweng.project.Login;
import ch.epfl.sweng.project.MainActivity;
import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.User;

/**
 * Class who provides a bunch of util statics methods
 * who can be reused from many different other classes
 * over the application
 *
 * @author Dominique Roduit
 */
public final class AndroidUtils {

    private AndroidUtils() {}

    /**
     * Return the formatted time for a message.
     * The format can be the following :
     *  - HH:mm : for the messages of the current day
     *  - Wed, Thu, ... : for the messages of the current week
     *  - Oct 19, Sept 08, ... : for the messages older than the current week
     *  - 15.12.15, ... : for the messages older than the current year
     * @return Time in the format described above
     */
    public static String getFormattedTime(Date dateTime) {
        if(dateTime == null) {
            return "";
        } else {
            Calendar c = Calendar.getInstance();

            long now = c.getTime().getTime() / (60 * 1000);
            long messageTime = dateTime.getTime() / (60 * 1000);
            long diffInDays = (now - messageTime) / (24 * 60);
            long today = (long) (Math.floor(now / (24 * 60)) * 24 * 60);
            long thisYear = (long) Math.floor(now / (365.25 * 24 * 60)) * (long) (365.25 * 24 * 60);

            SimpleDateFormat timeFormat;

            Locale locale = Locale.ENGLISH;
            if (messageTime >= today) {
                timeFormat = new SimpleDateFormat("HH:mm", locale);
            } else if (diffInDays < 7) {
                timeFormat = new SimpleDateFormat("EEE", locale);
            } else if (messageTime >= thisYear) {
                timeFormat = new SimpleDateFormat("MMM dd", locale);
            } else {
                timeFormat = new SimpleDateFormat("dd.MM.yyyy", locale);
            }

            return timeFormat.format(dateTime);
        }
    }

    /**
     * Bound a value between min and max given values
     * @param val Value to bound
     * @param min Min boundary
     * @param max Max boundary
     * @return Bounded value
     */
    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Resize dynamically a ListView according to these items
     * @param listView  The ListView to update
     * @return true if the operation succeed
     */
    public static boolean setListViewHeightBasedOnItems(ListView listView) {
        if(listView == null) {
            return false;
        } else {
            ListAdapter listAdapter = listView.getAdapter();
            if (listAdapter != null) {

                int numberOfItems = listAdapter.getCount();

                // Get total height of all items.
                int totalItemsHeight = 0;
                for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                    View item = listAdapter.getView(itemPos, null, listView);
                    item.measure(0, 0);
                    totalItemsHeight += item.getMeasuredHeight();
                }

                // Get total height of all item dividers.
                int totalDividersHeight = listView.getDividerHeight() * (numberOfItems - 1);

                // Set list height.
                ViewGroup.LayoutParams params = listView.getLayoutParams();
                if (params != null) {
                    params.height = totalItemsHeight + totalDividersHeight;
                    listView.setLayoutParams(params);
                }

                listView.requestLayout();
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Compress a bitmap in JPEG compress format and
     * convert it to a base64 string.
     * @param bitmap Image to convert in base64
     * @return String of the Bitmap encoded in base64
     */
    public static String toBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

    /**
     * Convert a base64 string to a Bitmap Image
     * @param base64 Base64 String of a Bitmap
     * @return Bitmap of the decoded base64
     */
    public static Bitmap fromBase64(String base64) {
        byte[] decodedBytes = Base64.decode(base64, 0);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    /**
     * Display a Toast on the UI to prevent user that he has no internet connection
     * @param activity Activity where the message need to be displayed
     * @param isConnected If the network is available or not
     */
    public static void displayNetworkStatus(final Activity activity, boolean isConnected) {
        if(activity != null) {
            final RelativeLayout ltDisconnected = (RelativeLayout) activity.findViewById(R.id.ltDisconnected);
            ltDisconnected.setVisibility((!isConnected) ? View.VISIBLE : View.GONE);
        }
        //Toast.makeText(ctx, "You are disconnected", Toast.LENGTH_SHORT).show();
    }

    /**
     * Method to print the error type as alert when user sets wrong infos
     * @param activity Activity where we want to display the dialog
     * @param title   Title of the dialog
     * @param message string from Strings.xml describing the error text to print
     */
    public static void displayDialog(final Activity activity, final String title, final String message) {
        if(activity == null)
            return;

        activity.runOnUiThread(new Runnable() {
            public void run() {
            AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
            alertDialog.setTitle(title);
            alertDialog.setMessage(message);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
            }
        });
    }

    /**
     * Return the next activity to display in a logic order of navigation.
     * @param user User object of the connected user
     * @param profile Profile object of the connected user
     * @param avatar Avatar object of the connected user
     * @return Class of the next intend expected according to received parameters
     */
    public static Class<?> getNextActivity(User user, Profile profile, Avatar avatar) {
        if (user != null) {
            if (profile == null) {
                return FillProfileActivity.class;
            } else {
                if (avatar == null) {
                    return EditAvatar.class;
                } else {
                    return MainActivity.class;
                }
            }
        } else {
            return Login.class;
        }
    }


    /**
     * Used to find an image by its string name, from drawable directory
     * @param context Context of the activity where we call this method
     * @param imageName Physical name of the image we want the resource id
     * @return Resource id
     */
    public static int getImageId(Context context, String imageName) {
        return context.getResources().getIdentifier("drawable/" + imageName,
                null, context.getPackageName());
    }

    /**
     * Redirect the user to the Login Activity
     * @param classFrom Class from where this method is called
     * @return Intent of the login activity with auto-redirect extra bundle
     */
    public static Intent gotoLogin(AppCompatActivity classFrom) {
        Intent loginIntent = new Intent(classFrom, Login.class);
        loginIntent.putExtra(Login.EXTRA_AUTO_REDIRECT, false);
        return loginIntent;
    }
}
