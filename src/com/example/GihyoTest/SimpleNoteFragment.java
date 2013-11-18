package com.example.GihyoTest;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.OnClientCallback;
import com.evernote.client.conn.mobile.FileData;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.ResourceAttributes;
import com.evernote.thrift.transport.TTransportException;

import java.io.*;
import java.util.List;

public class SimpleNoteFragment extends Fragment implements ListNotebookDialogFragment.OnNotebookSelectedListener{
    // Evernoteのセッション保持用変数
    private EvernoteSession mEvernoteSession;

    // レイアウト変数
    private EditText mTitle, mContent;
    private Button mSelectButton, mAttachButton, mSaveButton;
    private ProgressDialogFragment mDialogFragment;

    // ユーザが選択したNotebookのGuid格納用変数
    private String[] guids;
    private String guid;
    private static final String LOGTAG = "SimpleNoteFragment";

    // ギャラリーからの結果取得用
    private String mFilename, mFilePath, mMimeType;
    private static final int REQUEST_GALLERY = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_simplenote, container, false);

        mTitle = (EditText) mView.findViewById(R.id.edit_title);
        mContent = (EditText) mView.findViewById(R.id.edit_content);
        mSelectButton = (Button) mView.findViewById(R.id.button_select);
        mAttachButton = (Button) mView.findViewById(R.id.button_attach);
        mSaveButton = (Button) mView.findViewById(R.id.button_save);
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Sesssionオブジェクトのインスタンス生成/取得
        mEvernoteSession = EvernoteSession.getInstance(getActivity(),
                MainActivity.CONSUMER_KEY, MainActivity.CONSUMER_SECRET, MainActivity.EVERNOTE_SERVICE);

        // 「ノートブックの選択」ボタンを押下時の処理
        mSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // ノートブック一覧の取得
                    mEvernoteSession.getClientFactory().createNoteStoreClient().listNotebooks(new OnClientCallback<List<Notebook>>() {
                        @Override
                        public void onSuccess(final List<Notebook> notebooks) {
                            CharSequence[] names = new CharSequence[notebooks.size()];
                            guids = new String[notebooks.size()];

                            // 選択ダイアログの表示用にノートブックの名前,
                            // 最終的にノートを作成するために, 選択されたノートブックを指定するためにノートブックのguidを保持
                            for(int i = 0; i < notebooks.size(); i++){
                                Notebook notebook = notebooks.get(i);
                                names[i] = notebook.getName();
                                guids[i] = notebook.getGuid();
                            }

                            // ノートブックの名前を渡してノートブック選択ダイアログ（フラグメント）を開く
                            Bundle args = new Bundle();
                            args.putCharSequenceArray("names", names);

                            FragmentManager fm = getFragmentManager();
                            ListNotebookDialogFragment mDialog = new ListNotebookDialogFragment();
                            mDialog.setArguments(args);
                            mDialog.setTargetFragment(getFragmentManager().findFragmentByTag("SimpleNote"), 0);

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

        // 「画像を選択」した際にはギャラリーにインテントを飛ばす
        mAttachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, REQUEST_GALLERY);
            }
        });

        // ノート保存時の処理
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mTitle.getText().toString();
                String content = mContent.getText().toString();

                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
                    Toast.makeText(getActivity(), R.string.error_empty_content, Toast.LENGTH_LONG).show();
                } else {
                    Note note = new Note();
                    note.setTitle(title);

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

                    // ユーザがnotebookを選択していた場合はguidも加える
                    if(guid != null) {
                        note.setNotebookGuid(guid);
                    }

                    // ユーザが画像を選択していた場合は画像も加える
                    if(mFilePath != null) {
                        try {
                            // 画像のハッシュデータを取得
                            InputStream in = new BufferedInputStream(new FileInputStream(mFilePath));
                            FileData data = new FileData(EvernoteUtil.hash(in), new File(mFilePath));
                            in.close();

                            // リソースの作成、ノートへの追加
                            Resource resource = new Resource();
                            resource.setData(data);
                            resource.setMime(mMimeType);
                            ResourceAttributes attributes = new ResourceAttributes();
                            attributes.setFileName(mFilename);
                            resource.setAttributes(attributes);
                            note.addToResources(resource);

                            content = content + "<br/>" + EvernoteUtil.createEnMediaTag(resource);
                        } catch (IOException exception){
                            Log.e(LOGTAG, "Error attaching an image", exception);
                            Toast.makeText(getActivity(), R.string.error_attaching_image, Toast.LENGTH_LONG).show();
                        }
                    }
                    content = content + EvernoteUtil.NOTE_SUFFIX;
                    note.setContent(content);

                    mDialogFragment = ProgressDialogFragment.newInstance();
                    mDialogFragment.show(getActivity().getFragmentManager(), "progress");

                    // ノート作成の処理
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

    // ギャラリーへのインテントの戻り値を受け取る。
    // ギャラリーで選択されたファイルのパス、MimeType、ファイル名を保存
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_GALLERY && resultCode == getActivity().RESULT_OK) {
            try {
                // データ情報を取得
                Cursor c = getActivity().getContentResolver().query(data.getData(), null, null, null, null);
                c.moveToFirst();
                mFilename = c.getString(c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                mMimeType = c.getString(c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
                mFilePath = c.getString(c.getColumnIndex(MediaStore.MediaColumns.DATA));

                // ファイル名をボタンに表示
                mAttachButton.setText(mFilename);

            } catch (Exception exception) {
                Log.e(LOGTAG, "Error retrieving image source", exception);
            }
        }
    }

    // ノートブックが選択された事を検知するリスナー
    @Override
    public void onNotebookSelected(int position) {
           if(guids != null) {
               guid = guids[position];
           }
    }
}

