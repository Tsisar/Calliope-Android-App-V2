package cc.calliope.mini_v2;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<File> files;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;


    public interface OnItemClickListener {
        void onItemClick(File file);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View view, File file);
    }

    public RecyclerAdapter(LayoutInflater inflater, List<File> files) {
        this.files = files;
        this.inflater = inflater;
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
        View view = inflater.inflate(R.layout.hex_file, parent, false);
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

    @Override
    public int getItemCount() {
        if (files == null) {
            return 0;
        } else {
            return files.size();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private static final String OUTPUT_DATE_FORMAT = "dd.MM.yyyy HH:mm";
        private final TextView name;
        private final TextView date;

        private ViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.hex_file_name_text_view);
            date = view.findViewById(R.id.hex_file_date_text_view);
        }

        void setItem(File file) {
            name.setText(file.getName());
            date.setText(dateFormat(file.lastModified()));
        }

        private String dateFormat(long lastModified) {
            SimpleDateFormat outputDateFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT, Locale.getDefault());
            return outputDateFormat.format(lastModified);
        }
    }
}