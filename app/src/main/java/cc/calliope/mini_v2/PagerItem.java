package cc.calliope.mini_v2;

public enum PagerItem {

    MAKECODE(
            R.string.title_coding,
            R.drawable.ic_launcher_background,
            R.string.title_coding,
            "https://makecode.calliope.cc"),
    NEPO(
            R.string.title_coding,
            R.drawable.ic_launcher_background,
            R.string.title_coding,
            "https://lab.open-roberta.org/#loadSystem&&calliope2017"),
    BIBLIOTHEK(
            R.string.title_coding,
            R.drawable.ic_launcher_background,
            R.string.title_coding,
            "https://calliope.cc/calliope-mini/25programme#17");

    private final int titleResId;
    private final int iconResId;
    private final int infoResId;
    private final String url;

    PagerItem(int titleResId, int iconResId, int infoResId, String url) {
        this.titleResId = titleResId;
        this.iconResId = iconResId;
        this.infoResId = infoResId;
        this.url = url;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getInfoResId() {
        return infoResId;
    }

    public String getUrl() {
        return url;
    }
}