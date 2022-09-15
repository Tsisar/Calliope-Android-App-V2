package cc.calliope.mini_v2.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.DFUActivity;
import cc.calliope.mini_v2.HexFile;
import cc.calliope.mini_v2.HexFilesAdapter;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.FragmentHomeBinding;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ExtendedBluetoothDevice device;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ScannerViewModel scannerViewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), result -> this.device = result.getCurrentDevice());

        // Construct the data source
        ArrayList<HexFile> arrayOfUsers = new ArrayList<>();
        // Create the adapter to convert the array to views
        HexFilesAdapter adapter = new HexFilesAdapter(getActivity(), arrayOfUsers, device);
        // Attach the adapter to a ListView
        ListView listView = binding.codeListView;
        listView.setAdapter(adapter);

        //        File mydir = getFilesDir();
        File dir = new File(getActivity().getFilesDir().toString());
        //        Log.i("Datei", getFilesDir()+"");
        //        File lister = mydir.getAbsoluteFile();
        File[] files = dir.listFiles();
        //        for (String list : lister.list())
        for (File file : files) {
//                Log.i("Datei", file.toString());
            //            adapter.add(new HexFile(list, "San Diego"));
            Long lastmodified = file.lastModified();
            adapter.insert(new HexFile(file, lastmodified), 0);
        }


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.i("SELECTED Position", position + "");
//                Log.i("SELECTED HexFile", adapter.getHexFileName(position));
                String selectedItem = adapter.getHexFile(position).toString();

                if (device != null) {
                    final Intent intent = new Intent(getActivity(), DFUActivity.class);
                    intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
                    intent.putExtra("EXTRA_FILE", selectedItem);
                    startActivity(intent);
                } else {
                    Utils.showErrorMessage(binding.getRoot(), "No mini connected");
                }
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}