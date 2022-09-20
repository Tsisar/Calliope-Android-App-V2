package cc.calliope.mini_v2.views;

import cc.calliope.mini_v2.R;

public enum ContentCodingViewPager {

    MAKECODE(
            R.string.title_make_code,
            R.drawable.ic_editors_makecode,
            R.string.info_make_code,
            "https://makecode.calliope.cc/beta?androidapp=1"),
    ROBERTA(
            R.string.title_roberta,
            R.drawable.ic_editors_roberta,
            R.string.info_roberta,
            "https://lab.open-roberta.org/#loadSystem&&calliope2017"),
    LIBRARY(
            R.string.title_library,
            R.drawable.ic_editors_library,
            R.string.info_library,
            "https://calliope.cc/calliope-mini/25programme#17");

    private final int titleResId;
    private final int iconResId;
    private final int infoResId;
    private final String url;

    ContentCodingViewPager(int titleResId, int iconResId, int infoResId, String url) {
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