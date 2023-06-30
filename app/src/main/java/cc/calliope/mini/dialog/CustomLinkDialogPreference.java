package cc.calliope.mini.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceManager;
import cc.calliope.mini.fragment.editors.Editor;

public class CustomLinkDialogPreference extends DialogPreference {
    private static final String KEY_CUSTOM_LINK = "pref_key_custom_link";

    public CustomLinkDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        String dialogTitle = getDialogTitle() == null ? "" : getDialogTitle().toString();
        DialogUtils.showEditDialog(getContext(), dialogTitle, readCustomLink(), output -> {
                    if (shouldPersist()) {
                        persistString(output);
                    }
                }
        );
    }

    private String readCustomLink() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString(KEY_CUSTOM_LINK, Editor.CUSTOM.getUrl());
    }
}