package cc.calliope.mini_v2.ui.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.viewpager2.widget.ViewPager2;
import cc.calliope.mini_v2.DFUActivity;
import cc.calliope.mini_v2.ConnectFragmentStateAdapter;
import cc.calliope.mini_v2.FileWrapper;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.ZoomOutPageTransformer;
import cc.calliope.mini_v2.adapter.RecyclerAdapter;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.FragmentHomeBinding;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;
import cc.calliope.mini_v2.views.Editor;

public class HomeFragment extends Fragment {
    private static final String FILE_EXTENSION = ".hex";
    private FragmentHomeBinding binding;
    private ExtendedBluetoothDevice device;
    private RecyclerAdapter recyclerAdapter;

    private ViewPager2 viewPager2;

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        ScannerViewModel scannerViewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), result -> this.device = result.getCurrentDevice());

        binding.myCode.linearLayout.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    break;
                case MotionEvent.ACTION_MOVE:
                    Guideline guideLine = binding.guideline;
                    if(guideLine != null) {
                        int height = binding.getRoot().getHeight();
                        int margin = Utils.convertDpToPixel(64, view.getContext());
                        float guidePercent = (motionEvent.getRawY() - margin) / height;

                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
                        if(guidePercent > 0.25f && guidePercent < 0.95f) {
                            params.guidePercent = guidePercent;
                        }
                        guideLine.setLayoutParams(params);
                    }
                    break;
                default:
                    return false;
            }
            return true;
        });

        showRecyclerView(inflater);

        viewPager2 = binding.viewPager2;

        // Employee FragmentStateAdapter.
        FragmentActivity activity = getActivity();
        if(activity != null) {
            ConnectFragmentStateAdapter adapter = new ConnectFragmentStateAdapter(activity);
            viewPager2.setAdapter(adapter);
        }

        // PageTransformer
        viewPager2.setPageTransformer(new ZoomOutPageTransformer());

        return binding.getRoot();
    }

    @Override
    public void onViewStateRestored(Bundle bundle) {
        super.onViewStateRestored(bundle);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openDFUActivity(FileWrapper file) {
        if (device != null && device.isRelevant()) {
            final Intent intent = new Intent(getActivity(), DFUActivity.class);
            intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
            intent.putExtra("EXTRA_FILE", file.getAbsolutePath());
            startActivity(intent);
        } else {
            Utils.errorSnackbar(binding.getRoot(), "No mini connected").show();
        }
    }

    private ArrayList<FileWrapper> getFiles(Activity activity, Editor editor) {
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

    private void showRecyclerView(LayoutInflater inflater) {
        Activity activity = getActivity();
        if (activity == null)
            return;

        ArrayList<FileWrapper> filesList = new ArrayList<>();

        filesList.addAll(getFiles(activity, Editor.MAKECODE));
        filesList.addAll(getFiles(activity, Editor.ROBERTA));
        filesList.addAll(getFiles(activity, Editor.LIBRARY));

        if (!filesList.isEmpty()) {
            setMyCodeVisibility(true);
            RecyclerView recyclerView = binding.myCode.recyclerView;
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
                        return shareFile(file.getFile());
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
        }else {
            setMyCodeVisibility(false);
        }
    }

    private void setMyCodeVisibility(boolean visible){
        binding.myCode.layoutMyCode.setVisibility(visible?View.VISIBLE:View.GONE);
    }

    private boolean renameFile(FileWrapper file) {
        Activity activity = getActivity();
        if (activity == null)
            return false;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_rename, activity.findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textTitle)).setText("Rename file");
        EditText editText = view.findViewById(R.id.textName);
        editText.setText(FilenameUtils.removeExtension(file.getName()));
//        editText.requestFocus();

        ((Button) view.findViewById(R.id.buttonYes)).setText("Rename");
        ((Button) view.findViewById(R.id.buttonNo)).setText("Cancel");
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 -> {
            File dir = new File(FilenameUtils.getFullPath(file.getAbsolutePath()));
            if (dir.exists()) {
                FileWrapper dest = new FileWrapper(new File(dir, editText.getText().toString() + FILE_EXTENSION), file.getEditor());
                if (file.exists()) {
                    if (!dest.exists() && file.renameTo(dest.getFile())) {
                        recyclerAdapter.change(file, dest);
                    } else {
                        Utils.errorSnackbar(view, "The file with this name exists").show();
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

    private boolean removeFile(FileWrapper file) {
        Activity activity = getActivity();
        if (activity == null)
            return false;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_warning, activity.findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textTitle)).setText("Delete file");
        ((TextView) view.findViewById(R.id.textMessage)).setText(String.format(
                "You will permanently delete \"%s\".", FilenameUtils.removeExtension(file.getName())));
        ((Button) view.findViewById(R.id.buttonYes)).setText("Continue");
        ((Button) view.findViewById(R.id.buttonNo)).setText("Cancel");
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 -> {
            if (file.delete()) {
                recyclerAdapter.remove(file);
                setMyCodeVisibility(!recyclerAdapter.isEmpty());
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

    private void showKeyboard(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeKeyboard(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
}