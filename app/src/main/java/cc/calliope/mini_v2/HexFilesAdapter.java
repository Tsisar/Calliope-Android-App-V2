package cc.calliope.mini_v2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;

public class HexFilesAdapter extends ArrayAdapter<HexFile> {

    Button btnUpload, btnShare;
    ExtendedBluetoothDevice device;
    Context ParentContext;
    Locale locale;

    public HexFilesAdapter(Context context, ArrayList<HexFile> items, ExtendedBluetoothDevice SelectedDevice) {

        super(context, 0, items);
        device = SelectedDevice;
        ParentContext = context;
        locale = getCurrentLocale(context);
    }

    Locale getCurrentLocale(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position
        HexFile HexFile = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.hex_file_list_item, parent, false);
        }

        // Lookup view for data population
        TextView tvSource = (TextView) convertView.findViewById(R.id.tvSource);
        TextView tvDate = (TextView) convertView.findViewById(R.id.tvDate);
        TextView tvTime = (TextView) convertView.findViewById(R.id.tvTime);

//        Locale LocaleBylanguageTag = Locale.forLanguageTag("de");
//        TimeAgoMessages messages = new TimeAgoMessages.Builder().withLocale(LocaleBylanguageTag).build();
//
//        String timeAgoText = TimeAgo.using(HexFile.lastModified, messages);

        //TODO Are we can use it?
        //String timeAgoText = TimeAgo.using(HexFile.lastModified);

        String timeAgoText = "TimeAgo";

        // Populate the data into the template view using the data object
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE dd.MM.yyyy", locale);
        String formattedDate = new SimpleDateFormat("EEEE dd.MM.yyyy", locale).format(HexFile.lastModified);
        String formattedTime = new SimpleDateFormat("hh:mm:ss", locale).format(HexFile.lastModified) + " Uhr";

        tvDate.setText(formattedDate);
        tvTime.setText(formattedTime + " - " + timeAgoText);
        tvSource.setText(HexFile.File.getName() + "");


        btnUpload = (Button) convertView.findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.i("SELECTED Position", position + "");
//                Log.i("SLECTED HexFile", getHexFileName(position));
                String selectedItem = getHexFile(position).toString();

                if (device != null) {
                    final Intent intent = new Intent(ParentContext, DFUActivity.class); // ParentContext.this ?
                    intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
                    intent.putExtra("EXTRA_FILE", selectedItem);
                    ParentContext.startActivity(intent);
                } else {
                    Toast.makeText(ParentContext, "R.string.upload_no_mini_connected", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnShare = (Button) convertView.findViewById(R.id.btnShare);
        btnShare.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                File file = getHexFile(position);
                Uri uri = FileProvider.getUriForFile(ParentContext, "cc.calliope.file_provider", file);

                if (file.exists()) {
                    intentShareFile.setType("text/plain");
                    intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Calliope mini firmware");

                    ParentContext.startActivity(Intent.createChooser(intentShareFile, "Share File"));
                }
            }
        });

        ImageButton btnDelete = (ImageButton) convertView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
//                Log.i("BUTTON Delete", "AT "+position+" File: "+ getHexFile(position));

                File file = getHexFile(position);
                file.delete();
//                HexFilesAdapter.notifyDataSetChanged();
                HexFilesAdapter.this.remove(getItem(position));
                HexFilesAdapter.this.notifyDataSetChanged();

            }
        });


        Button btnRename = (Button) convertView.findViewById(R.id.btnRename);
        btnRename.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
//                Log.i("BUTTON RENAME", "AT "+position+" File: "+ getHexFile(position));

                File file = getHexFile(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("Rename file");

// Set up the input
                final EditText input = new EditText(view.getContext());

                input.setText(file.getName());
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                builder.setView(input);

// Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString();
                        //                HexFilesAdapter.this.remove(getItem(position));
                        File dir = new File(ParentContext.getFilesDir().toString());
                        if (dir.exists()) {
                            File from = getHexFile(position);
                            File to = new File(dir, m_Text);
                            if (from.exists() && !to.exists())
                                from.renameTo(to);
                            HexFile newItem = getItem(position);
                            newItem.File = to;

                            HexFilesAdapter.this.remove(getItem(position));
                            HexFilesAdapter.this.insert(newItem, position);
                            HexFilesAdapter.this.notifyDataSetChanged();
                        }

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

// Return the completed view to render on screen
        return convertView;
    }

    public File getHexFile(int position) {
        HexFile HexFile = getItem(position);
        return HexFile.File;
    }
}