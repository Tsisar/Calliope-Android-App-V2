package cc.calliope.mini_v2;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cc.calliope.mini_v2.utils.Utils;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final ArrayList<File> files;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    public interface OnItemClickListener {
        void onItemClick(File file);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View view, File file);
    }

    public RecyclerAdapter(LayoutInflater inflater, ArrayList<File> files) {
        this.files = files;
        this.inflater = inflater;
        sort();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_file_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (files == null)
            return;

        File file = files.get(position);
        holder.setItem(file);
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(view -> onItemClickListener.onItemClick(file));
        }

        if(onItemLongClickListener != null){
            holder.itemView.setOnLongClickListener(view -> {
                onItemLongClickListener.onItemLongClick(view, file);
                return false;
            });
        }
    }

    public void remove(File file) {
        int index = files.indexOf(file);
        if(files.remove(file)) {
            notifyItemRemoved(index);
        }
    }

    public void change(File oldFile, File newFile){
        int index = files.indexOf(oldFile);
        if(files.remove(oldFile)) {
            files.add(index, newFile);
            notifyItemChanged(index);
            sort();
        }
    }

    public void sort(){
        files.sort(new CustomComparator());
    }

    static class CustomComparator implements Comparator<File> {
        @Override
        public int compare(File file1, File file2) {
            return Long.compare(file2.lastModified(), file1.lastModified());
        }
    }

    @Override
    public int getItemCount() {
        if (files == null) {
            return 0;
        } else {
            return files.size();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        private final TextView name;
        private final TextView date;

        private ViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.hex_file_name_text_view);
            date = view.findViewById(R.id.hex_file_date_text_view);
        }

        void setItem(File file) {
            this.name.setText(Utils.removeExtension(file.getName()));
            this.date.setText(Utils.dateFormat(file.lastModified()));
        }
    }
}