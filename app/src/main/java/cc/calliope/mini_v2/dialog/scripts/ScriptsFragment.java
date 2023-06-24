package cc.calliope.mini_v2.dialog.scripts;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cc.calliope.mini_v2.activity.FlashingActivity;
import cc.calliope.mini_v2.adapter.FileWrapper;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.dialog.DialogUtils;
import cc.calliope.mini_v2.utils.StaticExtra;
import cc.calliope.mini_v2.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.FragmentScriptsBinding;
import cc.calliope.mini_v2.fragment.editors.Editor;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;
import cc.calliope.mini_v2.views.SimpleDividerItemDecoration;


public class ScriptsFragment extends BottomSheetDialogFragment {
    private static final String FILE_EXTENSION = ".hex";
    private FragmentScriptsBinding binding;
    private FragmentActivity activity;

    private ScriptsRecyclerAdapter scriptsRecyclerAdapter;
    private ExtendedBluetoothDevice device;
    private FrameLayout bottomSheet;

    private int state = BottomSheetBehavior.STATE_COLLAPSED;

    private final BottomSheetBehavior.BottomSheetCallback bottomSheetCallback =
            new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    state = newState;
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).addBottomSheetCallback(bottomSheetCallback);
                bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        });
        return dialog;
    }

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
        BottomSheetBehavior.from(bottomSheet).removeBottomSheetCallback(bottomSheetCallback);
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ArrayList<FileWrapper> filesList = new ArrayList<>();
        for(Editor editor : Editor.values()){
            filesList.addAll(getFiles(editor));
        }
        TextView infoTextView = binding.infoTextView;
        RecyclerView recyclerView = binding.scriptsRecyclerView;

        if (filesList.isEmpty()) {
            infoTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            infoTextView.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            scriptsRecyclerAdapter = new ScriptsRecyclerAdapter(filesList);
            scriptsRecyclerAdapter.setOnItemClickListener(this::openDFUActivity);
            scriptsRecyclerAdapter.setOnItemLongClickListener(this::openPopupMenu);
            recyclerView.setAdapter(scriptsRecyclerAdapter);
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
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
            final Intent intent = new Intent(activity, FlashingActivity.class);
            intent.putExtra(StaticExtra.EXTRA_DEVICE, device);
            intent.putExtra(StaticExtra.EXTRA_FILE_PATH, file.getAbsolutePath());
            startActivity(intent);
        } else {
            if(state == BottomSheetBehavior.STATE_EXPANDED){
                BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
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
        String title = getResources().getString(R.string.title_dialog_rename);
        String input = FilenameUtils.removeExtension(file.getName());

        DialogUtils.showEditDialog(activity, title, input, output -> {
            File dir = new File(FilenameUtils.getFullPath(file.getAbsolutePath()));
            if (dir.exists()) {
                FileWrapper dest = new FileWrapper(new File(dir, output + FILE_EXTENSION), file.getEditor());
                if (file.exists()) {
                    if (!dest.exists() && file.renameTo(dest.getFile())) {
                        scriptsRecyclerAdapter.change(file, dest);
                    } else {
                        Utils.errorSnackbar(binding.getRoot(), getString(R.string.error_snackbar_name_exists)).show();
                    }
                }
            }
        });
        return true;
    }

    private boolean removeFile(FileWrapper file) {
        String title = getResources().getString(R.string.title_dialog_rename);
        String message = String.format(getString(R.string.info_dialog_delete), FilenameUtils.removeExtension(file.getName()));

        DialogUtils.showWarningDialog(activity, title, message, () -> {
            if (file.delete()) {
                scriptsRecyclerAdapter.remove(file);
            }
        });
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
}