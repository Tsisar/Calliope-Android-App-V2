package cc.calliope.mini_v2.ui.home;

import cc.calliope.mini_v2.R;

public enum Home {

    WELCOME(
            R.string.title_welcome,
            R.drawable.welcome,
            R.string.info_welcome),
    BATTERY(
            R.string.title_battery,
            R.drawable.anim_battery,
            R.string.info_battery),
    DEMO(
            R.string.title_demo,
            R.drawable.anim_demo,
            R.string.info_demo),
    SCRIPTS(
            R.string.title_scripts,
            R.drawable.ic_board,
            R.string.info_scripts);

    private final int titleResId;
    private final int iconResId;
    private final int infoResId;

    Home(int titleResId, int iconResId, int infoResId) {
        this.titleResId = titleResId;
        this.iconResId = iconResId;
        this.infoResId = infoResId;
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
}