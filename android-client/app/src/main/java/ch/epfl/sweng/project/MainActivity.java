package ch.epfl.sweng.project;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import ch.epfl.sweng.project.adapters.DrawerListAdapter;
import ch.epfl.sweng.project.fragments.MainFragment;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Message;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.models.ui.NavItemModel;
import ch.epfl.sweng.project.network.MessageHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;
import ch.epfl.sweng.project.network.NetworkStatus;

/**
 * Main activity of the application. It show the open conversations
 * with users, and give the possibility to ask for a new proposal.
 * All the different functionality are split in different fragments.
 * 
 * @author Dominique Roduit
 */

public final class MainActivity extends AppCompatActivity {

    private static final boolean DEBUG_MODE = true;

    private static DBHandler db;
    private long userId;
    private Profile profile;
    private Avatar avatar;

    private ActionBarDrawerToggle drawerToggle;
    private BroadcastReceiver receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = DBHandler.getInstance(getApplicationContext());
        userId = Settings.getInstance(getApplicationContext()).getUserID();
        String errorMsg = "no user found";

        // Register BroadcastReceiver to track network connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                performNetworkActions();
            }
        };
        this.registerReceiver(receiver, filter);

        // ---- Swipe Menu -------------------------------------
        User user = db.getUser(userId);
        profile = db.getProfile(userId);
        avatar = db.getAvatar(userId);

        // Display name of the user
        TextView tvUserName = (TextView) findViewById(R.id.userName);
        tvUserName.setText((user == null) ? errorMsg : user.getFullName());

        // Make ribbon clickable to see its own profile
        LinearLayout ribbonProfile = (LinearLayout) findViewById(R.id.ribbonProfile);
        ribbonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ViewProfileActivity.class);
                startActivity(intent);
            }
        });


        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        ListView drawerListView = (ListView) findViewById(R.id.navList);

        final ArrayList<NavItemModel> navItems = getNavigationItems();
        final DrawerListAdapter adapter = new DrawerListAdapter(this, navItems);
        drawerListView.setAdapter(adapter);

        drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 3) {
                    db.cleanTables();
                }
                startActivity(new Intent(MainActivity.this, navItems.get(position).getExpectedActivity()));
            }
        });

        // Display the "menu" icon in the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Handle the click on "menu" icon
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.swipe_open, R.string.swipe_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {

                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        // --------------------------------------------------------

        // Load main content
        loadMainFragment(new MainFragment());
    }


    /**
     * Load the fragment containing the main part of the app.
     * Conversations, Ready button, etc...
     */
    private void loadMainFragment(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.mainContent, fragment)
                .commit();
    }


    /**
     * Send to the server the messages who were send offline (without network),
     * and update accordingly their status (from WAIT_SEND to SENT)
     */
    private void sendOfflineMessages() {
        List<Message> messagesToSend = db.getOfflineMessages();
        int numMessagesToSend = messagesToSend.size();
        int i = 0;

        Log.d("MainActivity", "Nombre de messages offline à envoyer : " + numMessagesToSend);

        long myUserId = Settings.getInstance(getApplicationContext()).getUserID();
        MessageHandler msgHandler = MessageHandler.messagerie();
        MessageHandler.Conversation conversation;

        for (final Message m : messagesToSend) {
            i++;
            final boolean lastMessageIsSent = (i == numMessagesToSend);

            Log.d("MainActivity", "sendOfflineMessage : {id : " + m.getMsgId() + " , " + m.getContent() + " }");


            conversation = msgHandler.getConversationByIdOrCreate(m.getMatchId());

            conversation.sendMessage(m.getContent(), m.getDateTime(), new NetworkResponseManager() {
                @Override
                public void onError(int errorCode) {
                }

                @Override
                public void onSuccess(JSONObject jsonObj) {
                    db.updateMessageStatus(m.getMsgId(), Message.Status.SENT);

                    if (lastMessageIsSent) {
                        Log.d("MainActivity", "lastMessageSent");

                        Intent receiverListView = new Intent();
                        receiverListView.setAction(ChatActivity.ACTION_RELOAD);
                        receiverListView.putExtra(ChatActivity.ACTION_UPDATE_LISTVIEW, "update");
                        getApplicationContext().sendBroadcast(receiverListView);
                    }
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }


    // We want no action when back button is pressed on this activity
    @Override
    public void onBackPressed() {
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    /**
     * We want to refresh some UI components or execute
     * some actions each time we access this activity
     */
    @Override
    protected void onStart() {
        super.onStart();

        new ProfilePhotoTask((RoundedImageView) findViewById(R.id.myAvatarDrawerPane)).execute();

        sendOfflineMessages();

        performNetworkActions();
    }

    private void performNetworkActions() {
        boolean networkAvailable = NetworkStatus.getInstance(getApplicationContext()).isConnected();
        if (networkAvailable) {
            setTitle(R.string.app_name);
            sendOfflineMessages();
        } else {
            setTitle(R.string.waiting_for_network);
        }
    }

    private ArrayList<NavItemModel> getNavigationItems() {
        final ArrayList<NavItemModel> navItems = new ArrayList<>();

        // Items of the menu
        navItems.add(new NavItemModel(
                getString(R.string.editProfile),
                getString(R.string.editProfilDesc),
                R.drawable.ic_account_circle_black_24dp,
                FillProfileActivity.class)
        );
        navItems.add(new NavItemModel(
                getString(R.string.editAvatar),
                getString(R.string.editAvatarDesc),
                R.drawable.ic_face_black_24dp,
                EditAvatar.class)
        );
        navItems.add(new NavItemModel(
                getString(R.string.preferences),
                getString(R.string.preferenceDesc),
                R.drawable.ic_settings_black_24dp,
                SettingsActivity.class)
        );

        if (DEBUG_MODE) {
            navItems.add(new NavItemModel("CLEAR DATA", "Clear la db", R.drawable.ic_public_black_24dp, Login.class));
        }

        return navItems;
    }

    /**
     * Refresh the display of the user profile's picture
     * or of the avatar if the user doesn't have a picture
     **/
    class ProfilePhotoTask extends AsyncTask<Integer, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        ProfilePhotoTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            userId = Settings.getInstance(getApplicationContext()).getUserID();
            profile = db.getProfile(userId);
            avatar = db.getAvatar(userId);

            if (profile != null) {
                Bitmap photo = profile.getPhoto();
                Bitmap avatarIcon = avatar.getIcon(getApplicationContext());
                if (photo != null) {
                    return photo;
                } else if (avatarIcon != null) {
                    return avatarIcon;
                }
            }
            return null;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}
