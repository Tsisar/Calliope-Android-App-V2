package cc.calliope.mini_v2.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cc.calliope.mini_v2.DFUActivity;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.RecyclerAdapter;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.FragmentHomeBinding;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;

public class HomeFragment extends Fragment implements PopupMenu.OnMenuItemClickListener{

    private FragmentHomeBinding binding;
    private ExtendedBluetoothDevice device;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        ScannerViewModel scannerViewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), result -> this.device = result.getCurrentDevice());

        Activity activity = getActivity();
        if(activity != null) {
            File dir = new File(getActivity().getFilesDir().toString());
            File[] filesArray = dir.listFiles();
            if(filesArray != null) {
                List<File> filesList = Arrays.asList(filesArray);

                RecyclerView recyclerView = binding.myCodeRecyclerView;
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

                RecyclerAdapter recyclerAdapter = new RecyclerAdapter(inflater, filesList);
                recyclerAdapter.setOnItemClickListener(this::openDFUActivity);
                recyclerAdapter.setOnItemLongClickListener((view, file) -> showPopup(view));
                recyclerView.setAdapter(recyclerAdapter);
            }
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openDFUActivity(File file){
        if (device != null) {
            final Intent intent = new Intent(getActivity(), DFUActivity.class);
            intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
            intent.putExtra("EXTRA_FILE", file.getAbsolutePath());
            startActivity(intent);
        } else {
            Utils.showErrorMessage(binding.getRoot(), "No mini connected");
        }
    }

    public void showPopup(View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.my_code_popup_menu);
        popup.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.rename:
                Log.v("MENU", "rename");
                return true;
            case R.id.share:
                Log.v("MENU", "share");
                return true;
            case R.id.delete:
                Log.v("MENU", "delete");
                return true;
            default:
                return false;
        }
    }
}