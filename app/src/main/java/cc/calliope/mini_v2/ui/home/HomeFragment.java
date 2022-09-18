package cc.calliope.mini_v2.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
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

public class HomeFragment extends Fragment {
    private static final String FILE_EXTENSION = ".hex";
    private FragmentHomeBinding binding;
    private ExtendedBluetoothDevice device;
    private RecyclerAdapter recyclerAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        ScannerViewModel scannerViewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), result -> this.device = result.getCurrentDevice());

        showRecyclerView(inflater);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openDFUActivity(File file) {
        if (device != null) {
            final Intent intent = new Intent(getActivity(), DFUActivity.class);
            intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
            intent.putExtra("EXTRA_FILE", file.getAbsolutePath());
            startActivity(intent);
        } else {
            Utils.showErrorMessage(binding.getRoot(), "No mini connected");
        }
    }

    private void showRecyclerView(LayoutInflater inflater) {
        Activity activity = getActivity();
        if (activity == null)
            return;

        File dir = new File(getActivity().getFilesDir().toString());
        File[] filesArray = dir.listFiles();
        if (filesArray != null) {
            ArrayList<File> filesList = new ArrayList<>(Arrays.asList(filesArray));
            filesList.removeIf(file -> !file.getName().contains(FILE_EXTENSION));

            RecyclerView recyclerView = binding.myCodeRecyclerView;
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            recyclerAdapter = new RecyclerAdapter(inflater, filesList);
            recyclerAdapter.setOnItemClickListener(this::openDFUActivity);
            recyclerAdapter.setOnItemLongClickListener((view, file) -> {
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                popup.setOnMenuItemClickListener(item -> {
                    //Non-constant Fields
                    int id = item.getItemId();
                    if (id == R.id.rename) {
                        return renameFile(file);
                    } else if (id == R.id.share) {
                        return shareFile(file);
                    } else if (id == R.id.remove) {
                        return removeFile(file);
                    } else {
                        return false;
                    }
                });
                popup.inflate(R.menu.my_code_popup_menu);
                popup.show();
            });
            recyclerView.setAdapter(recyclerAdapter);
        }
    }

    private boolean renameFile(File file) {
        Activity activity = getActivity();
        if (activity == null)
            return false;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_rename, activity.findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textTitle)).setText("Rename file");
        EditText editText = view.findViewById(R.id.textName);
        editText.setText(Utils.removeExtension(file.getName()));
//        editText.requestFocus();

        ((Button) view.findViewById(R.id.buttonYes)).setText("Rename");
        ((Button) view.findViewById(R.id.buttonNo)).setText("Cancel");
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 -> {
            File dir = new File(activity.getFilesDir().toString());
            if (dir.exists()) {
                File dest = new File(dir, editText.getText().toString() + FILE_EXTENSION);
                if (file.exists()) {
                    if (!dest.exists() && file.renameTo(dest)) {
                        recyclerAdapter.change(file, dest);
                    }else {
                        Utils.showErrorMessage(view, "The file with this name exists");
                        return;
                    }
                }
            }
            alertDialog.dismiss();
        });
        view.findViewById(R.id.buttonNo).setOnClickListener(view12 -> alertDialog.dismiss());
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
        return true;
    }

    private boolean shareFile(File file) {
        Activity activity = getActivity();
        if (activity == null)
            return false;

        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(activity, "cc.calliope.file_provider", file);

        if (file.exists()) {
            intentShareFile.setType("text/plain");
            intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
            intentShareFile.putExtra(Intent.EXTRA_TEXT, "Calliope mini firmware");

            startActivity(Intent.createChooser(intentShareFile, "Share File"));
        }
        return true;
    }

    private boolean removeFile(File file) {
        Activity activity = getActivity();
        if (activity == null)
            return false;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_warning, activity.findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textTitle)).setText("Delete file");
        ((TextView) view.findViewById(R.id.textMessage)).setText(String.format("You will permanently delete \"%s\".", Utils.removeExtension(file.getName())));
        ((Button) view.findViewById(R.id.buttonYes)).setText("Continue");
        ((Button) view.findViewById(R.id.buttonNo)).setText("Cancel");
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 -> {
            if (file.delete()) {
                recyclerAdapter.remove(file);
            }
        });
        view.findViewById(R.id.buttonNo).setOnClickListener(view12 -> alertDialog.dismiss());
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
        return true;
    }

    private void showKeyboard(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeKeyboard(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
}