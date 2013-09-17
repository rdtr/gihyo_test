package com.example.GihyoTest;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.EditText;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.Notebook;
import com.evernote.thrift.transport.TTransportException;

import java.util.List;

public class ListNotebookDialogFragment extends DialogFragment {
    private OnNotebookSelectedListener mListener;

    private static int pos = 0;

    static ListNotebookDialogFragment newInstance() {
        return new ListNotebookDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        try {
            if(getTargetFragment() != null) {
                mListener = (OnNotebookSelectedListener)getTargetFragment();
            } else {
                mListener = (OnNotebookSelectedListener)getActivity();
            }
        } catch(ClassCastException e) {
            throw new ClassCastException("Parent activity or fragment must implement OnNotebookSelectedListener");
        }

        Bundle args = getArguments();
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(getActivity());

        mBuilder.setTitle("Select notebook").setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                mListener.onNotebookSelected(pos);
                dialog.dismiss();
            }
        });

        mBuilder.setSingleChoiceItems(args.getCharSequenceArray("names"), pos, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                pos = which;
            }
        });

        return mBuilder.create();
    }

    public interface OnNotebookSelectedListener {
        public void onNotebookSelected(int pos);
    }




}

