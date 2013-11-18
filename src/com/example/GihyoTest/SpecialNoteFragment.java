package com.example.GihyoTest;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.OnClientCallback;
import com.evernote.client.conn.mobile.FileData;
import com.evernote.edam.type.*;
import com.evernote.thrift.transport.TTransportException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SpecialNoteFragment extends Fragment implements ListNotebookDialogFragment.OnNotebookSelectedListener{
    // Evernoteのセッション保持用変数
    private EvernoteSession mEvernoteSession;

    // レイアウト変数
    private EditText mTitle, mTag, mContent;
    private Button mSelectButton, mSaveButton;
    private Switch mSwitch;
    private ProgressDialogFragment mDialogFragment;

    // ユーザが選択したNotebookのGuid格納用変数
    private String[] guids;
    private String guid;
    private static final String LOGTAG = "SpecialNoteFragment";

    //Read onlyフラグ
    private boolean isReadOnly = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_specialnote, container, false);

        mTitle = (EditText) mView.findViewById(R.id.edit_title);
        mContent = (EditText) mView.findViewById(R.id.edit_content);
        mSelectButton = (Button) mView.findViewById(R.id.button_select);
        mSaveButton = (Button) mView.findViewById(R.id.button_save);

        mTag = (EditText)mView.findViewById(R.id.edit_tag);
        mSwitch = (Switch)mView.findViewById(R.id.switch_readonly);

        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mEvernoteSession = EvernoteSession.getInstance(getActivity(),
                MainActivity.CONSUMER_KEY, MainActivity.CONSUMER_SECRET, MainActivity.EVERNOTE_SERVICE);

        mSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mEvernoteSession.getClientFactory().createNoteStoreClient().listNotebooks(new OnClientCallback<List<Notebook>>() {
                        @Override
                        public void onSuccess(final List<Notebook> notebooks) {
                            CharSequence[] names = new CharSequence[notebooks.size()];
                            guids = new String[notebooks.size()];

                            for(int i = 0; i < notebooks.size(); i++){
                                Notebook notebook = notebooks.get(i);
                                names[i] = notebook.getName();
                                guids[i] = notebook.getGuid();
                            }

                            Bundle args = new Bundle();
                            args.putCharSequenceArray("names", names);

                            FragmentManager fm = getFragmentManager();
                            ListNotebookDialogFragment mDialog = new ListNotebookDialogFragment();
                            mDialog.setArguments(args);
                            mDialog.setTargetFragment(getFragmentManager().findFragmentByTag("SpecialNote"), 0);

                            mDialog.show(fm, "listnotebooks");
                        }

                        @Override
                        public void onException(Exception exception) {
                            Log.e(LOGTAG, "Error listing notebooks", exception);
                            Toast.makeText(getActivity(), R.string.error_listing_notebooks, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (TTransportException exception) {
                    Log.e(LOGTAG, "Error creating notestore", exception);
                    Toast.makeText(getActivity(), R.string.error_creating_notestore, Toast.LENGTH_LONG).show();
                }
            }
        });

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isReadOnly = isChecked ? true : false;
                Log.d(LOGTAG, "The flag isReadOnly changed");
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mTitle.getText().toString();
                String content = mContent.getText().toString();

                if(content.contains("\n")) {
                    String[] contents = content.split("\n");
                    content = "";
                    for(int i = 0; i < contents.length; i++) {
                        if(contents[i].equals("")){
                            contents[i] = "<br />";
                        }
                        contents[i] = "<div>" + contents[i] + "</div>";
                        content += contents[i];
                    }
                }

                content = EvernoteUtil.NOTE_PREFIX + content;
                String tag = mTag.getText().toString();
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
                    Toast.makeText(getActivity(), R.string.error_empty_content, Toast.LENGTH_LONG).show();
                } else {
                    Note note = new Note();
                    note.setTitle(title);

                    // ユーザがnotebookを選択していた場合はguidも加える
                    if(guid != null) {
                        note.setNotebookGuid(guid);
                    }

                    // タグが入力されていた場合はタグをセット
                    if (!TextUtils.isEmpty(tag)){
                        //カンマで分割
                        String[] tagArray = tag.split(",");
                        List<String> tagList = new ArrayList<String>();

                        int max = (10000 < tagArray.length)? 10000 : tagArray.length;
                        for(int i =0; i < max; i++) {
                            String t = tagArray[i].trim();
                            if(!TextUtils.isEmpty(t) && t.length() < 100) {
                                tagList.add(t);
                            }
                        }
                        note.setTagNames(tagList);
                    }

                    // Read onlyフラグがTrueの場合はContentClassをセット
                    if (isReadOnly) {
                        NoteAttributes mAttr = new NoteAttributes();
                        mAttr.setSourceApplication("GihyoTest");
                        mAttr.setContentClass("test.Gihyo");
                        note.setAttributes(mAttr);
                    }

                    content = content + EvernoteUtil.NOTE_SUFFIX;
                    note.setContent(content);

                    mDialogFragment = ProgressDialogFragment.newInstance();
                    mDialogFragment.show(getActivity().getFragmentManager(), "progress");
                    try {


                        mEvernoteSession.getClientFactory().createNoteStoreClient().createNote(note, new OnClientCallback<Note>() {
                            @Override
                            public void onSuccess(Note data) {
                                Toast.makeText(getActivity(), R.string.note_saved, Toast.LENGTH_LONG).show();
                                mDialogFragment.getDialog().dismiss();
                            }

                            @Override
                            public void onException(Exception exception) {
                                Log.e(LOGTAG, "Error saving note", exception);
                                Toast.makeText(getActivity(), R.string.error_saving_note, Toast.LENGTH_LONG).show();
                                mDialogFragment.getDialog().dismiss();
                            }
                        });
                    } catch (TTransportException exception) {
                        Log.e(LOGTAG, "Error creating notestore", exception);
                        Toast.makeText(getActivity(), R.string.error_creating_notestore, Toast.LENGTH_LONG).show();
                        mDialogFragment.getDialog().dismiss();
                    }
                }
            }

        });


    }

    @Override
    public void onNotebookSelected(int position) {
           if(guids != null) {
               guid = guids[position];
           }
    }
}

