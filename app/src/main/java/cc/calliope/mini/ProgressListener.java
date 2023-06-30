package cc.calliope.mini;

public interface ProgressListener {
    void onDeviceConnecting();

    void onProcessStarting();

    void onEnablingDfuMode();

    void onFirmwareValidating();

    void onDeviceDisconnecting();

    void onCompleted();

    void onAborted();

    void onProgressChanged(int percent);

    void onError(int code, String message);
}