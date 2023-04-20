package cc.calliope.mini_v2.ui.scripts;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cc.calliope.mini_v2.DFUActivity;
import cc.calliope.mini_v2.FileWrapper;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.utils.StaticExtra;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.FragmentScriptsBinding;
import cc.calliope.mini_v2.ui.editors.Editor;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;
import cc.calliope.mini_v2.views.SimpleDividerItemDecoration;


public class ScriptsFragment extends Fragment {
    private static final String FILE_EXTENSION = ".hex";
    private FragmentScriptsBinding binding;
    private FragmentActivity activity;

    private ScriptsRecyclerAdapter scriptsRecyclerAdapter;
    private ExtendedBluetoothDevice device;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentScriptsBinding.inflate(inflater, container, false);
        activity = requireActivity();

        ScannerViewModel scannerViewModel = new ViewModelProvider(activity).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), result -> device = result.getCurrentDevice());

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.titleTextView.setText(R.string.title_scripts);

        ArrayList<FileWrapper> filesList = new ArrayList<>();

        filesList.addAll(getFiles(Editor.MAKECODE));
        filesList.addAll(getFiles(Editor.ROBERTA));
        filesList.addAll(getFiles(Editor.LIBRARY));

        final TextView infoTextView = binding.infoTextView;
        final RecyclerView recyclerView = binding.scriptsRecyclerView;

        if (!filesList.isEmpty()) {
            infoTextView.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            scriptsRecyclerAdapter = new ScriptsRecyclerAdapter(filesList);
            scriptsRecyclerAdapter.setOnItemClickListener(this::openDFUActivity);
            scriptsRecyclerAdapter.setOnItemLongClickListener(this::openPopupMenu);
            recyclerView.setAdapter(scriptsRecyclerAdapter);
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
        } else {
            infoTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        }
    }

    private ArrayList<FileWrapper> getFiles(Editor editor) {
        File[] filesArray = new File(activity.getFilesDir().toString() + File.separator + editor).listFiles();

        ArrayList<FileWrapper> filesList = new ArrayList<>();

        if (filesArray != null && filesArray.length > 0) {
            for (File file : filesArray) {
                String name = file.getName();
                if (name.contains(FILE_EXTENSION)) {
                    filesList.add(new FileWrapper(file, editor));
                }
            }
        }
        return filesList;
    }

    private void openDFUActivity(FileWrapper file) {
        if (device != null && device.isRelevant()) {
            final Intent intent = new Intent(activity, DFUActivity.class);
            intent.putExtra(StaticExtra.EXTRA_DEVICE, device);
            intent.putExtra(StaticExtra.EXTRA_FILE_PATH, file.getAbsolutePath());
            startActivity(intent);
        } else {
            Utils.errorSnackbar(binding.getRoot(), getString(R.string.error_snackbar_no_connected)).show();
        }
    }

    private void openPopupMenu(View view, FileWrapper file) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.setOnMenuItemClickListener(item -> {
            //Non-constant Fields
            int id = item.getItemId();
            if (id == R.id.rename) {
                return renameFile(file);
            } else if (id == R.id.share) {
                return shareFile(file.getFile());
            } else if (id == R.id.remove) {
                return removeFile(file);
            } else {
                return false;
            }
        });
        popup.inflate(R.menu.scripts_popup_menu);
        popup.show();
    }

    private boolean renameFile(FileWrapper file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit, activity.findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.title_dialog_rename);
        EditText editText = view.findViewById(R.id.editField);
        editText.setText(FilenameUtils.removeExtension(file.getName()));
//        editText.requestFocus();

        ((Button) view.findViewById(R.id.buttonYes)).setText(R.string.button_rename);
        ((Button) view.findViewById(R.id.buttonNo)).setText(R.string.button_cancel);
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 -> {
            File dir = new File(FilenameUtils.getFullPath(file.getAbsolutePath()));
            if (dir.exists()) {
                FileWrapper dest = new FileWrapper(new File(dir, editText.getText().toString() + FILE_EXTENSION), file.getEditor());
                if (file.exists()) {
                    if (!dest.exists() && file.renameTo(dest.getFile())) {
                        scriptsRecyclerAdapter.change(file, dest);
                    } else {
                        Utils.errorSnackbar(view, getString(R.string.error_snackbar_name_exists)).show();
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
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(activity, "cc.calliope.file_provider", file);

        if (file.exists()) {
            intentShareFile.setType("text/plain");
            intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_dialog_share));
            intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.text_dialog_share));

            startActivity(Intent.createChooser(intentShareFile, getString(R.string.title_dialog_share)));
        }
        return true;
    }

    private boolean removeFile(FileWrapper file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_warning, activity.findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.title_dialog_delete);
        ((TextView) view.findViewById(R.id.textMessage)).setText(String.format(
                getString(R.string.info_dialog_delete), FilenameUtils.removeExtension(file.getName())));
        ((Button) view.findViewById(R.id.buttonYes)).setText(R.string.button_continue);
        ((Button) view.findViewById(R.id.buttonNo)).setText(R.string.button_cancel);
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 -> {
            if (file.delete()) {
                scriptsRecyclerAdapter.remove(file);
//                setBottomSheetVisibility(!recyclerAdapter.isEmpty());
                alertDialog.dismiss();
            }
        });
        view.findViewById(R.id.buttonNo).setOnClickListener(view12 -> alertDialog.dismiss());
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
        return true;
    }
}