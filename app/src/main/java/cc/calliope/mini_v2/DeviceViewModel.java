package cc.calliope.mini_v2;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;

public class DeviceViewModel extends ViewModel {
    private final MutableLiveData<ExtendedBluetoothDevice> selectedItem = new MutableLiveData<>();

    public void setDevice(ExtendedBluetoothDevice device) {
        selectedItem.setValue(device);
    }

    public LiveData<ExtendedBluetoothDevice> getDevice() {
        return selectedItem;
    }
}