package cc.calliope.mini_v2;

import java.io.File;

import cc.calliope.mini_v2.views.ContentCodingViewPager;

public class FileWrapper {
    private final File file;
    private final ContentCodingViewPager content;

    public FileWrapper(File file, ContentCodingViewPager content) {
        this.file = file;
        this.content = content;
    }

    public File getFile() {
        return file;
    }

    public ContentCodingViewPager getContent() {
        return content;
    }

    public String getName(){
        return file.getName();
    }

    public long lastModified(){
        return file.lastModified();
    }

    public String getAbsolutePath(){
        return file.getAbsolutePath();
    }

    public boolean exists(){
        return file.exists();
    }

    public boolean delete(){
        return file.delete();
    }

    public boolean renameTo(File dest){
        return file.renameTo(dest);
    }
}
