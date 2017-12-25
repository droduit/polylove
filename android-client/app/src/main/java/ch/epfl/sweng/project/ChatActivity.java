package ch.epfl.sweng.project;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.makeramen.roundedimageview.RoundedImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.epfl.sweng.project.adapters.ChatAdapter;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Message;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.MessageHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;

/**
 * Chat between two users.
 *
 * @author Tim Nguyen, Dominique Roduit
 */
public final class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_DEST_ID = "destId";
    public static final String EXTRA_MATCH_ID = "matchId";
    public static final String ACTION_RELOAD = "ch.epfl.sweng.project.chat.RELOAD";
    public static final String ACTION_UPDATE_LISTVIEW = "chat.updateListView";

    private EditText editText;
    private static ArrayList<Message> chatList;
    private static ChatAdapter chatAdapter;
    private ListView msgListView;

    private static DBHandler db;

    private long destUserId = -1;
    private User destUser;
    private long myUserId;

    private long matchId;
    private Match match;

    private MessageHandler msgHandler;
    private MessageHandler.Conversation conv;
    private String toolbarUsername;
    private Bitmap destUserPhoto;
    private Bitmap destUserIcon;

    private BroadcastReceiver messageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_chat);


        Toolbar chatToolbar = (Toolbar) findViewById(R.id.chatToolbar);
        setSupportActionBar(chatToolbar);

        db = DBHandler.getInstance(getApplicationContext());
        myUserId = Settings.getInstance(getApplicationContext()).getUserID();

        //initialization
        chatList = new ArrayList<>();
        editText = (EditText) findViewById(R.id.msgEditText);
        msgListView = (ListView) findViewById(R.id.msgListView);

        chatAdapter = new ChatAdapter(this, chatList);
        msgListView.setAdapter(chatAdapter);

        // Loading values received from the previous activity
        Bundle b = getIntent().getExtras();
        long userId = (b != null) ? b.getLong(EXTRA_DEST_ID) : -1;
        matchId = (b != null) ? b.getLong(EXTRA_MATCH_ID) : -1;

        msgHandler = MessageHandler.messagerie();
        conv = msgHandler.getConversationByIdOrCreate(matchId);
        destUser = db.getUser(userId);
        if(destUser != null) {
            Log.d("user", destUser.toString());
            destUserId = destUser.getId();
        } else {
            destUserId = userId;
        }

        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle b = intent.getExtras();
                if(b != null) {
                    chatList.clear();
                    final List<Message> msgList = db.getMessagesForMatch(matchId);
                    updateMsgListView(msgList);
                } else {
                    Message msg = db.getLastMessageFromMatch(matchId);
                    if (msg != null) {
                        List<Message> listM = new ArrayList<>();
                        listM.add(msg);
                        updateMsgListView(listM);
                    }
                }

            }
        };

        if (messageReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_RELOAD);
            registerReceiver(messageReceiver, intentFilter);
        }

        match = db.getMatchWithUser(destUserId);
        Profile destUserProfile = null;
        Avatar destUserAvatar = null;

        destUserProfile = db.getProfile(destUserId);
        destUserAvatar = db.getAvatar(destUserId);
        destUserPhoto = null;
        destUserIcon = null;

        // -------------------------------------------------------
        if(match != null) {
            toolbarUsername = match.getFriendlyUsername(getApplicationContext());
            if(destUserAvatar != null) {
                destUserIcon = destUserAvatar.getIcon(getApplicationContext());
                Log.d("status ", match.getState().toString());
            if(match.getState() == Match.State.Open) {
                if(destUserProfile != null) {
                    destUserPhoto = destUserProfile.getPhoto();
                }
            }
        } else {
                Log.d("Status", "pas de match associé");
            }
        }


        // Filling the Toolbar with user infos
        Toolbar toolbar = (Toolbar) findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);


            TextView tvUsername = (TextView)findViewById(R.id.tvUsername);
            tvUsername.setText(toolbarUsername);

            LinearLayout layoutUserInfos = (LinearLayout) findViewById(R.id.layoutUserInfos);
            layoutUserInfos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUserProfile();
                }
            });

            RoundedImageView avatar = (RoundedImageView) findViewById(R.id.avatarToolbar);
            avatar.setImageBitmap(destUserIcon);

            if(destUserPhoto != null) {
                avatar.setImageBitmap(destUserPhoto);
            }

        }

        // Action on long click on items of the adapter
        final String[] contextualActions = new String[]{ getString(R.string.delete), getString(R.string.copy) };
        msgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Message clickedMessage = chatList.get(position);
                final int pos = position;

                if(view.isEnabled() && clickedMessage.getMsgId() != -10) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                    builder.setTitle(R.string.message)
                            .setItems(contextualActions, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int index) {
                                    switch (index) {
                                        case 0:
                                            int deleteRows = db.deleteMessage(clickedMessage.getMsgId());
                                            if (deleteRows > 0) {
                                                int idx = msgListView.getFirstVisiblePosition();
                                                View v = msgListView.getChildAt(0);
                                                int top = (v == null) ? 0 : (v.getTop() - msgListView.getPaddingTop());

                                                chatList.remove(pos);
                                                updateMsgListView();

                                                msgListView.smoothScrollToPositionFromTop(idx, top, 0);
                                            }
                                            break;
                                        case 1:
                                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                            ClipData clip = ClipData.newPlainText("Message", chatList.get(pos).getContent());
                                            clipboard.setPrimaryClip(clip);
                                            Toast.makeText(ChatActivity.this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        });
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem == 0) {
            // check if we reached the top or bottom of the list
            View v = msgListView.getChildAt(0);
            int offset = (v == null) ? 0 : v.getTop();
            if (offset == 0) {
                final List<Message> msgList = db.getMessagesForMatch(matchId);
                conv.loadMessages(msgList.isEmpty() ? new Date() : msgList.get(0).getDateTime(), new NetworkResponseManager() {
                    @Override
                    public void onError(int errorCode) {

                    }

                    @Override
                    public void onSuccess(JSONObject jsonObj) {
                        ArrayList<Message> list = new ArrayList<Message>();
                        try {
                            JSONArray messages = (JSONArray) jsonObj.get("messages");
                            if (messages != null) {
                                int len = messages.length();
                                for (int i = 0; i < len; i++) {
                                    JSONObject messageJSON = messages.getJSONObject(i);
                                    messageJSON.put(Message.KEY_STATUS, Message.Status.SENT.toString());
                                    messageJSON.put(Message.KEY_MY_ID, myUserId);
                                    Message m = Message.fromJSON(messageJSON);

                                    db.storeMessage(m);
                                    list.add(m);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        updateMsgListView(list);
                    }
                });
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Reload messages list
        chatList.clear();
        updateMsgListView(db.getMessagesForMatch(matchId));

        // Fetch messages from the server
        Message lastMessage = db.getLastMessageFromMatch(matchId);

        Date lastMessageTime = new Date(0);
        if (lastMessage != null)
            lastMessageTime = lastMessage.getDateTime();

        Log.d("Message Fetch", "From date: " + lastMessageTime.getTime());

        // TODO: Will not work if the user gets nut with reloading the activity
        // TODO: Or doesn't it? Ok to use db.storeMessageIfNotExists()?
        conv.loadMessages(lastMessageTime, new NetworkResponseManager() {
            @Override
            public void onError(int errorCode) {
                Log.e("Message Fetch", "An error occured: " + errorCode);
                // TODO: is there something to do to save the situation?
            }

            @Override
            public void onSuccess(JSONObject response) {
                try {
                    Log.d("Message Fetch", "Received: " + response.toString());

                    List<Message>newMessages = new LinkedList<>();
                    JSONArray messages = response.getJSONArray("messages");

                    for (int i = 0, l = messages.length(); i < l; i++) {
                        JSONObject jsonMessage = messages.getJSONObject(i);

                        jsonMessage.put("myId", myUserId);
                        jsonMessage.put("status", "SENT");

                        Message message = Message.fromJSON(messages.getJSONObject(i));

                        db.storeMessageIfNotExists(message);

                        Log.d("Message Fetch", "Adding message: " + message.toString());
                        newMessages.add(message);
                    }

                    updateMsgListView(newMessages);
                } catch (JSONException e) {
                    // Bad response
                    Log.e("Message Fetch", e.toString());
                }
            }
        });




    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(messageReceiver != null) {
            unregisterReceiver(messageReceiver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chat_btn, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
                break;
            case R.id.action_view_profile :
                showUserProfile();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showUserProfile() {
        // The user always has access to the profile, it's the profile who decide what he display
        if(match != null) {
            Intent intent = new Intent(ChatActivity.this, ViewProfileActivity.class);
            intent.putExtra(ViewProfileActivity.EXTRA_USER_ID, destUserId);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public static boolean storeNewReceivedMessage(Map<String, String> data, Context ctx) {
        // convert received data into appropriate parameters for a message
        JSONObject messageJSON = new JSONObject(data);
        try {
            messageJSON.put("status", Message.Status.SENT.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Message msg = null;
        try {
            msg = Message.fromJSON(messageJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return (DBHandler.getInstance(ctx).storeMessage(msg) != -1);
    }

    public void sendUserMessage(View view) {
        // the user types the message
        String message = editText.getText().toString();
        if(!message.trim().isEmpty()) {
            List<Message> myMsg = new ArrayList<>();

            // we store the message into the local database
            Message msg = new Message(-1, myUserId, new Date(), matchId, message, true, Message.Status.WAIT_SENDING);
            long newMessageId = db.storeMessage(msg);
            msg.setMsgId(newMessageId);
            myMsg.add(msg);
            updateMsgListView(myMsg);

            new RetrievedMessage().execute(msg);
        }

        // clean editText after sending the message
        editText.setText("");
    }

    private class RetrievedMessage extends AsyncTask<Message, Void, Message> {

        protected Message doInBackground(Message... messages) {
            final Message message = messages[0];
            if(!message.getContent().trim().isEmpty()) {
                //db.updateMessageStatus(message.getMsgId(), Message.Status.WAIT_ACK);
                // get the right conversation and send the message to the server
                conv.sendMessage(message.getContent(), message.getDateTime(), new NetworkResponseManager() {
                    @Override
                    public void onError(int errorCode) { }

                    @Override
                    public void onSuccess(JSONObject jsonObj) {
                        db.updateMessageStatus(message.getMsgId(), Message.Status.SENT);
                        message.setStatus(Message.Status.SENT);
                        Message msg = chatAdapter.getItem(Math.min(0, chatAdapter.getCount() - 1));
                        if(msg != null) {
                            msg.setStatus(Message.Status.SENT);
                            chatAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }

            return message;
        }

        protected void onPostExecute(Message message) {

        }
    }


    private void updateMsgListView(List<Message> messagesToAdd) {
        if(messagesToAdd != null && chatList != null) {
            Log.d("Message Fetch", chatList.toString());
            chatList.addAll(messagesToAdd);
        }

        displayTextIfNoMessage();

        chatAdapter.notifyDataSetChanged();
    }

    private void updateMsgListView() {
        updateMsgListView(Collections.<Message>emptyList());
    }

    private void displayTextIfNoMessage() {
        TextView tvNoMessage = (TextView)findViewById(R.id.tvNoMessage);
        if(chatList.size() == 0) {
            tvNoMessage.setVisibility(View.VISIBLE);
            tvNoMessage.setText(getString(R.string.write_to_your_match));
        } else {
            tvNoMessage.setVisibility(View.GONE);
        }
    }

}
