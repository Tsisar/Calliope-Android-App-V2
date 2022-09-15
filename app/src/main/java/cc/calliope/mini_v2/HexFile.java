package cc.calliope.mini_v2;

import java.io.File;

public class HexFile {
    public File File;
    public long lastModified;

    public HexFile(File File, long lastModified) {
        this.File = File;
        this.lastModified = lastModified;
    }
}