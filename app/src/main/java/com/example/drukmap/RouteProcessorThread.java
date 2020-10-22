package com.example.drukmap;

import android.os.HandlerThread;
import android.os.Process;

public class RouteProcessorThread extends HandlerThread {

    public RouteProcessorThread() {
        super("RouteProcessorThread",Process.THREAD_PRIORITY_BACKGROUND);
    }
}
