package cc.calliope.mini_v2;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class ConnectFragmentStateAdapter extends FragmentStateAdapter {

    private static final String TAG = "ConnectInfoAdapter";
    private final List<ConnectInfo> connectInfoList;
    Context context;

    public ConnectFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        context = fragmentActivity;
        connectInfoList = getConnectInfo();
    }

    private List<ConnectInfo> getConnectInfo()  {
        List<ConnectInfo> list = new ArrayList<>();
        for(int i = 0; i < 5; i ++){
            ConnectInfo connectInfo = new ConnectInfo(
                    getResourceId(R.array.first_steps_illustrations, i),
                    getResourceId(R.array.first_steps_descriptions, i)
            );
            list.add(connectInfo);
        }

        return list;
    }

    public int getResourceId(int arrayId, int index){
        try {
            TypedArray array = context.getResources().obtainTypedArray(arrayId);
            int resId = array.getResourceId(index, R.mipmap.ic_launcher);
            array.recycle();
            return resId;
        } catch (ArrayIndexOutOfBoundsException e){
            Log.e(TAG, "getResourceId: " + e);
        }
        return R.mipmap.ic_launcher;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        ConnectInfo connectInfo = connectInfoList.get(position);
        return new ConnectPageFragment(connectInfo);
    }


    @Override
    public int getItemCount() {
        return connectInfoList.size();
    }
}
