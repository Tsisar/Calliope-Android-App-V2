package cc.calliope.mini;

public interface ProgressListener {
    void onDeviceConnecting();
    void onProcessStarting();
    void onEnablingDfuMode();
    void onFirmwareValidating();
    void onDeviceDisconnecting();
    void onCompleted();
    void onAborted();
    void onDfuControlCompleted(int boardVersion);
    void onProgressChanged(int percent);
    void onBonding(int bondState, int previousBondState);
    void onError(int code, String message);
}