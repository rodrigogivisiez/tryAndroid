package com.tullyapp.tully.Utils;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.tullyapp.tully.R;

/**
 * Created by macbookpro on 28/09/17.
 */

public class ViewUtils {

    public static void onGlobalLayout(final View view, final Runnable runnable) {
        final ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            runnable.run();
            }

        };
        view.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    public static Dialog shareAllowDownloadPopup(Context context){
        Dialog dialog = new Dialog(context, R.style.FullDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_share_download_config);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public static Dialog fullScreenTextDialog(Context context){
        Dialog dialog = new Dialog(context, R.style.FullDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.full_screen_text_dialog);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
}
