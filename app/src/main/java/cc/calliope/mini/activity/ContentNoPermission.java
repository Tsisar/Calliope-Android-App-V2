package cc.calliope.mini.activity;

import cc.calliope.mini.R;
import cc.calliope.mini.utils.Permission;

public enum ContentNoPermission {
    BLUETOOTH(R.drawable.ic_bluetooth_disabled, R.string.title_bluetooth_permission, R.string.info_bluetooth_permission),
    LOCATION(R.drawable.ic_location_disabled, R.string.title_location_permission, R.string.info_location_permission);

    private final int icResId;
    private final int titleResId;
    private final int messageResId;

    ContentNoPermission(int icResId, int titleResId, int messageResId) {
        this.icResId = icResId;
        this.titleResId = titleResId;
        this.messageResId = messageResId;
    }

    public static ContentNoPermission getContent(@Permission.RequestType int requestType){
        if(requestType == Permission.LOCATION)
            return LOCATION;
        return BLUETOOTH;
    }

    public int getIcResId() {
        return icResId;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getMessageResId() {
        return messageResId;
    }
}
