package cc.calliope.mini_v2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import cc.calliope.mini_v2.databinding.ActivityDfu2Binding;

public class DFUActivity2 extends AppCompatActivity{
    private ActivityDfu2Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDfu2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}