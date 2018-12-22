package com.project3c.flychess.listener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.project3c.flychess.LocalServerGameActivity;
import com.project3c.flychess.MainActivity;
import com.project3c.flychess.R;
import com.project3c.flychess.RoomActivity;
import com.project3c.flychess.data.NetPlayer;
import com.project3c.flychess.server.LocalServer;

/**
 * Created by like1 on 2017/5/15.
 */

public class JoinRoomLinstener implements View.OnClickListener {
    private LocalServer localServer;
    private Context context;
    private int roomID;
    private String mAddress;

    public JoinRoomLinstener(@NonNull LocalServer localServer, Activity context, int roomID, String mAddress) {
        this.localServer = localServer;
        this.context = context;
        this.roomID = roomID;
        this.mAddress = mAddress;
    }

    @Override
    public void onClick(View v) {
        TextView more = (TextView) v.findViewById(R.id.more);
        if (more == null)
            return;
        //NetPlayer.JoinRoom(localServer.getAddress());
        RoomActivity.setLocalServer(localServer);
        RoomActivity.setRoomID(roomID);
        Intent i = new Intent();
        i.setClass(context, RoomActivity.class);
        i.putExtra("address", mAddress);
        context.startActivity(i);
    }
}
