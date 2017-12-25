package ch.epfl.sweng.project.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.epfl.sweng.project.ChatActivity;
import ch.epfl.sweng.project.MainActivity;
import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.adapters.MatchAdapter;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;
import ch.epfl.sweng.project.network.NetworkResponseManagerForImages;
import ch.epfl.sweng.project.network.RequestsHelper;

import static ch.epfl.sweng.project.network.RequestsHelper.requestAvatar;
import static ch.epfl.sweng.project.network.RequestsHelper.requestMatches;


/**
 * Display the main content of MainActivity.
 * @author Dominique Roduit
 */
public final class MainFragment extends Fragment {
    public static final String ACTION_RELOAD = "ch.epfl.sweng.project.chat.PENDING";
    public static final String EXTRA_UPDATE_MATCH_STATUS = "update_match_status";
    public static final String EXTRA_RELOAD = "update_list_view";

    private MatchAdapter matchAdapter;
    private List<Match> matchs = new ArrayList<>();

    private static DBHandler db = null;

    private ViewSwitcher vsRibbon;
    private Button txtReady;

    private BroadcastReceiver receiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        db = DBHandler.getInstance(getActivity().getApplicationContext());

        // UI Components
        // ---------------------------------------------------------------------
        txtReady = (Button)view.findViewById(R.id.txtReady);
        vsRibbon = (ViewSwitcher)view.findViewById(R.id.switcher);
        // ---------------------------------------------------------------------

        // Request matches from server and all information
        // about users for the open state matches
        // ---------------------------------------------------------------------
        requestMatches(new NetworkResponseManager() {
            @Override
            public void onError(int errorCode) {
                Log.d("Error matches", "Code : "+errorCode);
            }

            @Override
            public void onSuccess(JSONObject response) {
                try {
                    Log.d("MATCHES", response.toString());
                    JSONArray matches = response.getJSONArray("matches");

                    for (int i = 0; i < matches.length(); i++) {
                        JSONObject matchJSON = matches.getJSONObject(i);
                        final Match m = Match.fromJSON(matchJSON,  getActivity());

                        if (db.getMatch(m.getId()) == null)
                            db.storeMatch(m);

                        if(db.getUser(m.getPartnerId()) == null) {
                            if (m.getState() == Match.State.Open) {
                                RequestsHelper.requestUser(m.getPartnerId(), new NetworkResponseManager() {
                                    @Override
                                    public void onError(int errorCode) {
                                    }

                                    @Override
                                    public void onSuccess(JSONObject jsonObj) {
                                        Avatar avatar = null;
                                        Profile profile = null;
                                        User user = null;
                                        try {
                                            user = User.fromJSON(jsonObj.getJSONObject("user"));
                                            JSONObject avatarJSON = jsonObj.getJSONObject("avatar");
                                            JSONObject profileJSON = jsonObj.getJSONObject("profile");
                                            if (user != null) {
                                                avatarJSON.put(Avatar.KEY_ID, user.getId());
                                                profileJSON.put(Profile.KEY_ID, user.getId());
                                            }
                                            profile = Profile.fromJSON(profileJSON);
                                            if(profile != null) {
                                                avatarJSON.put(Avatar.KEY_GENDER, profile.getGender().toString());
                                            }
                                            avatar = Avatar.fromJSON(avatarJSON);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        db.storeProfile(profile);
                                        db.storeUser(user);
                                        db.storeAvatar(avatar);

                                        RequestsHelper.requestPhoto(m.getPartnerId(), new NetworkResponseManagerForImages() {
                                            @Override
                                            public void onError(int errorCode) { }

                                            @Override
                                            public void onSuccess(Bitmap bitmap) {
                                                if (bitmap != null) {
                                                    Profile profile = db.getProfile(m.getPartnerId());
                                                    if (profile != null) {
                                                        profile.setPhoto(bitmap);
                                                        db.storeProfile(profile);
                                                        updateMatchList();
                                                    }
                                                }
                                            }
                                        });
                                    }
                                });
                            } else {
                                requestAvatar(m.getPartnerId(), new NetworkResponseManager() {
                                    @Override
                                    public void onError(int errorCode) { }

                                    @Override
                                    public void onSuccess(JSONObject jsonObj) {
                                        try {
                                            Avatar avatar;
                                            if(!jsonObj.isNull("avatar")) {
                                                JSONObject avatarJSON = jsonObj.getJSONObject("avatar");

                                                avatarJSON.put(Avatar.KEY_ID, m.getPartnerId());

                                                if(!jsonObj.isNull(Avatar.KEY_GENDER)) {
                                                    avatarJSON.put(Avatar.KEY_GENDER, jsonObj.getString(Avatar.KEY_GENDER));
                                                }

                                                avatar = Avatar.fromJSON(avatarJSON);
                                                db.storeAvatar(avatar);

                                                updateMatchList();
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    }

                    updateMatchList();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // ---------------------------------------------------------------------


        // Display conversations (matches)
        // ---------------------------------------------------------------------
        ListView lvMatchs = (ListView) view.findViewById(R.id.listView);
        matchs = db.getConversations();
        matchAdapter = new MatchAdapter(getActivity(), matchs);
        lvMatchs.setAdapter(matchAdapter);
        lvMatchs.setFocusable(false);

        lvMatchs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Match clickedMatch = matchs.get(position);

                Log.d("Clicked position", String.valueOf(position));
                Log.d("user corresponding", String.valueOf(clickedMatch.getPartnerId()));
                Log.d("matchId corresponding", String.valueOf(clickedMatch.getId()));

                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_DEST_ID, clickedMatch.getPartnerId());
                intent.putExtra(ChatActivity.EXTRA_MATCH_ID, clickedMatch.getId());
                startActivity(intent);
            }
        });


        lvMatchs.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Match selected = matchs.get(position);
                if(selected.getState() == Match.State.Close) {
                    String[] actions = new String[]{ getString(R.string.delete) };
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setItems(actions, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int index) {
                            switch(index) {
                                case 0:
                                    db.deleteMatch(selected.getId());
                                    updateMatchList();
                                    break;
                            }
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
                return false;
            }
        });
        // ---------------------------------------------------------------------


        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if(bundle != null) {
                    String action = bundle.getString(EXTRA_RELOAD);
                    if(action == null) return;
                    Log.d("action extra", action);

                    switch (action) {
                        case "new-match":
                        case "end-match":
                            if (getActivity() != null) {
                                //getActivity().recreate();
                                startActivity(new Intent(getActivity(), MainActivity.class));
                            }
                            break;

                        case "new-message":
                            updateMatchList();
                            break;
                    }
                }

            }
        };

        if (receiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_RELOAD);
            if(getActivity() != null) {
                getActivity().registerReceiver(receiver, intentFilter);
            }
        }

        return view;
    }


    /**
     * Change the state of this fragment in displaying the pending fragment
     */
    private void switchToMatchPendingDisplay() {
        long timeLeft;
        Match todaysMatch = db.getMatchOfToday();
        if(todaysMatch != null) {
            timeLeft = todaysMatch.getMillisecondsLeft();
            if(timeLeft >= 1000) {
                txtReady.setVisibility(View.GONE);
                if(vsRibbon.getDisplayedChild() == 0) {
                    vsRibbon.showNext();
                }
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null && getActivity() != null) {
            getActivity().unregisterReceiver(receiver);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        // Display PendingFragment in Ribbon if we are in daily match time
        switchToMatchPendingDisplay();

        // Close pending matches older than their validity time if remains some
        db.closeOldMatchesNotConfirmed();

        updateMatchList();
    }

    public void updateMatchList() {
        if(matchs != null) {
            // Refresh the list of conversations
            matchs.clear();
            matchs.addAll(db.getConversations());
            matchAdapter.notifyDataSetChanged();
        }
    }

}