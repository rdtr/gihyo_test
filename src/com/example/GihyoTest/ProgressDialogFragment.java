package com.example.GihyoTest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

public class ProgressDialogFragment extends DialogFragment{
    private static ProgressDialog mDialog;

    static ProgressDialogFragment newInstance() {
        return new ProgressDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (mDialog != null) return mDialog;
        mDialog = new ProgressDialog(getActivity());
        mDialog.setMessage(getString(R.string.text_loading));
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setCancelable(false);

        // 戻るボタンを無効化する
        DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {

                if( keyCode == KeyEvent.KEYCODE_BACK){
                    return true;
                }
                return false;
            }
        };
        mDialog.setOnKeyListener(keyListener);
        return mDialog;
    }
}

