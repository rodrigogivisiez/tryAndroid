package com.tullyapp.tully.Dialogs;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.APIs;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImportDialogFragment extends DialogFragment implements View.OnClickListener {

    private ImageView btn_copy, btn_back;
    private Button btn_got_it;

    public ImportDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public static ImportDialogFragment newInstance(){
        return new ImportDialogFragment();
    }

    @Override
    public int getTheme() {
        return R.style.FullScreenDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btn_copy = view.findViewById(R.id.btn_copy);
        btn_got_it = view.findViewById(R.id.btn_got_it);
        btn_back = view.findViewById(R.id.btn_back);

        btn_copy.setOnClickListener(this);
        btn_got_it.setOnClickListener(this);
        btn_back.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_copy:
                setClipboard(getContext(), APIs.IMPORT_AUDIO_URL);
                break;

            case R.id.btn_got_it:
                    this.dismiss();
                break;

            case R.id.btn_back:
                    this.dismiss();
                break;
        }
    }

    private void setClipboard(Context context, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Link Copied", Toast.LENGTH_LONG).show();
        }
        this.dismiss();
    }
}
