package ch.epfl.sweng.project.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.Message;

/**
 * @author Tim Nguyen
 */

public final class ChatAdapter extends BaseAdapter {

    private ArrayList<Message> chatMsgList;
    private static LayoutInflater inflater = null;

    public ChatAdapter(Activity activity, ArrayList<Message> list) {
        chatMsgList = list;
        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        return chatMsgList.size();
    }

    @Override
    public Message getItem(int position) {
        return chatMsgList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message message = chatMsgList.get(position);
        View view = convertView;
        if (convertView == null) {
            view = inflater.inflate(R.layout.chat_bubble, null);
        }

        TextView msgTxtView = (TextView) view.findViewById(R.id.textViewMsg);
        TextView tvTime = (TextView) view.findViewById(R.id.tvTime);
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.bubble_layout);
        LinearLayout parentLayout = (LinearLayout) view.findViewById(R.id.bubble_layout_parent);
        ImageView ivStatus = (ImageView) view.findViewById(R.id.ivStatus);

        // if I send the message then align the bubble to right
        // If the other user sends it align to left
        // If server sends it align in the center
        if (message.isMine()) {
            layout.setBackgroundResource(R.drawable.bubble2);
            parentLayout.setGravity(Gravity.END);
            ivStatus.setVisibility(View.VISIBLE);

            if(message.getStatus() == Message.Status.SENT) {
                ivStatus.setImageResource(R.drawable.msg_check);
            } else {
                ivStatus.setImageResource(R.drawable.msg_clock);
            }
        } else {
            if (message.getSourceId() != 0) {
                layout.setBackgroundResource(R.drawable.bubble1);
                parentLayout.setGravity(Gravity.START);
                ivStatus.setVisibility(View.GONE);
            } else {
                parentLayout.setGravity(Gravity.CENTER);
                parentLayout.setEnabled(false);
                layout.setBackgroundResource(0);
                ivStatus.setVisibility(View.GONE);
            }
        }


        tvTime.setText(message.getTime());
        msgTxtView.setTextColor(Color.BLACK);
        msgTxtView.setText(message.getContent());

        return view;
    }

    public void add(Message object) {
        chatMsgList.add(object);
    }
}
