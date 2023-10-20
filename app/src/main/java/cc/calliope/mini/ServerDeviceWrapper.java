package cc.calliope.mini;


import java.util.Date;

import cc.calliope.mini.utils.Version;
import no.nordicsemi.android.kotlin.ble.core.ServerDevice;
public class ServerDeviceWrapper {
    private static final long RELEVANT_LIMIT; //the time during which the device is relevant in ms

    static {
        if (Version.VERSION_O_AND_NEWER) {
            RELEVANT_LIMIT = 5000;
        } else {
            RELEVANT_LIMIT = 10000;
        }
    }
    private ServerDevice serverDevice;
    private final long recentUpdate = new Date().getTime();

    public ServerDeviceWrapper(ServerDevice serverDevice) {
        this.serverDevice = serverDevice;
    }

    public ServerDevice getServerDevice() {
        return serverDevice;
    }

    public void setServerDevice(ServerDevice serverDevice) {
        this.serverDevice = serverDevice;
    }
    public boolean isRelevant() {
        long currentTime = new Date().getTime();
        return currentTime - recentUpdate < RELEVANT_LIMIT;
    }
}
