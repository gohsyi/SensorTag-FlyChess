package com.project3c.flychess.listener;

import android.view.View;
import android.widget.ImageView;
import android.widget.TableRow;

import com.project3c.flychess.data.Aircraft;
import com.project3c.flychess.data.Map;
import com.project3c.flychess.data.NetPlayer;
import com.project3c.flychess.data.Player;

/**
 * Created by like1 on 2017/4/30.
 */

public class DiceClickListener implements ImageView.OnClickListener {
    private Player player;
    private NetPlayer netPlayer;

    public DiceClickListener(NetPlayer netPlayer) {
        this.netPlayer = netPlayer;
    }

    @Override
    public void onClick(View v) {
        v.setEnabled(false);
        if ((player = Map.getInstance().getCurPlayer()) != null) {
            if (netPlayer != null) {
                if (player.getUid() != netPlayer.getUid()) {
                    System.out.println("not your turn");
                    return;
                }
            }
            if (!player.canTouch()) {
                v.setEnabled(true);
                return;
            }
            if (!player.isCanDice() || !player.isFlyed()) {
                if (!player.isFlyed()) {
                    System.out.println("not fly");
                } else {
                    System.out.println("can not dice");
                }
                v.setEnabled(true);
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    player.dice();
                }
            }).start();
        }

    }
}
