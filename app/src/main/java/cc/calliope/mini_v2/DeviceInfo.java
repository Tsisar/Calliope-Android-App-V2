package cc.calliope.mini_v2;

import androidx.annotation.NonNull;

public class DeviceInfo {
    private String modelNumber;
    private String serialNumber;
    private String firmwareRevision;
    private String hardwareRevision;
    private String manufacturerName;

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getHardwareRevision() {
        return hardwareRevision;
    }

    public void setHardwareRevision(String hardwareRevision) {
        this.hardwareRevision = hardwareRevision;
    }

    public String getFirmwareRevision() {
        return firmwareRevision;
    }

    public void setFirmwareRevision(String firmwareRevision) {
        this.firmwareRevision = firmwareRevision;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    @NonNull
    @Override
    public String toString() {
        return "DeviceInfo:" +
                "\nmodelNumber = '" + modelNumber + '\'' +
                "\nserialNumber = '" + serialNumber + '\'' +
                "\nfirmwareRevision = '" + firmwareRevision + '\'' +
                "\nhardwareRevision = '" + hardwareRevision + '\'' +
                "\nmanufacturerName = '" + manufacturerName + '\'';
    }
}
