package cc.calliope.mini_v2.ui.coding;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CodingViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public CodingViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}