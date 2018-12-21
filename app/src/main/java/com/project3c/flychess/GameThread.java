package com.project3c.flychess;

import android.content.res.Resources;

import com.project3c.flychess.data.LocalServerMap;
import com.project3c.flychess.data.Map;
import com.project3c.flychess.data.PathProvider;
import com.project3c.flychess.data.Player;
import com.project3c.flychess.view.PathNodeView;

/**
 * Created by like1 on 2017/4/29.
 */

public class GameThread extends Thread {
    private LocalServerMap map;

    public GameThread(int meUid, int users, PathNodeView[] comViews, PathNodeView[] priViews, PathNodeView[] homeViews, Resources res) {

    }

    @Override
    public void run() {
        super.run();
    }
}
