package com.example.GihyoTest;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.thrift.transport.TTransportException;

import java.util.ArrayList;

public class SearchNoteFragment extends Fragment implements View.OnClickListener{
    // Evernoteのセッション保持用変数
    private EvernoteSession mEvernoteSession;

    // 検索クエリ保持
    private String searchQuery = null;

    // レイアウト変数
    private ListView mListView;
    private Button mLoadButton;
    private ProgressDialogFragment mDialogFragment;

    // ユーザが検索したNoteの情報格納用変数
    private ArrayList<ResultNote> mResultNotesList;
    private ArrayAdapter<ResultNote> mAdapter;

    private static final String LOGTAG = "SearchNotesFragment";

    // 一回検索あたりの取得件数
    private final int pageSize = 20;
    // 検索用オフセット
    private int startIdx = 0;
    // 検索ヒット総数を格納
    private int mTotalNotes;
    // 初回検索か、「Read More」かを判別
    private boolean isFirstSearch;
    // リストビューの位置記憶用
    private int pos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_searchnote, container, false);

        mListView = (ListView) mView.findViewById(R.id.list);
        mLoadButton = (Button) mView.findViewById(R.id.button_load_more);
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Sesssionオブジェクトのインスタンス生成/取得
        mEvernoteSession = EvernoteSession.getInstance(getActivity(),
                MainActivity.CONSUMER_KEY, MainActivity.CONSUMER_SECRET, MainActivity.EVERNOTE_SERVICE);

        mResultNotesList = new ArrayList<ResultNote>();
        mAdapter = new ArrayAdapter<ResultNote>(getActivity(), android.R.layout.simple_list_item_1, mResultNotesList);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // 何もしない
            }

            // 「Read moreボタンを表示」
            // 条件:
            //  検索結果が1件以上
            //  最下部までスクロール
            //  まだ読み込むべき検索結果が残っている
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount != 0 && (totalItemCount == firstVisibleItem + visibleItemCount) && (startIdx + pageSize < mTotalNotes)) {
                    mLoadButton.setVisibility(View.VISIBLE);
                } else {
                    mLoadButton.setVisibility(View.GONE);
                }
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;

                ResultNote clickedNote = (ResultNote) listView.getItemAtPosition(position);
                Toast.makeText(getActivity(), clickedNote.guid, Toast.LENGTH_LONG).show();
            }
        });

        mLoadButton.setOnClickListener(this);
        mListView.setAdapter(mAdapter);
    }


    // ActionBarに検索欄を追加する
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);
        SearchView searchView = (SearchView)menu.findItem(R.id.note_search).getActionView();
        searchView.setOnQueryTextListener(queryListener);
    }

    // 検索クエリのリスナ
    final private SearchView.OnQueryTextListener queryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextChange(String newText) {
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            searchQuery = query;
            startIdx = 0;

            findNotesByQuery(searchQuery, startIdx, true);
            return true;
        }
    };

    private void findNotesByQuery(String query, final int offset, boolean isFirstSearch){
        pos = mListView.getFirstVisiblePosition();

        // 検索フィルタ
        NoteFilter filter = new NoteFilter();
        filter.setOrder(NoteSortOrder.UPDATED.getValue());
        filter.setWords(query);

        // メタデータの設定
        NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
        spec.setIncludeTitle(true);

        mDialogFragment = ProgressDialogFragment.newInstance();
        mDialogFragment.show(getActivity().getFragmentManager(), "progress");

        if (isFirstSearch) {
            mAdapter.clear();
            pos = 0;
        }

        try{
            mEvernoteSession.getClientFactory().createNoteStoreClient()
                    .findNotesMetadata(filter, offset, pageSize, spec, new OnClientCallback<NotesMetadataList>() {
                        @Override
                        public void onSuccess(NotesMetadataList data) {
                            Toast.makeText(getActivity(), R.string.notes_searched, Toast.LENGTH_LONG).show();

                            ResultNote mResultNote;
                            mTotalNotes = data.getTotalNotes();

                            for(NoteMetadata note : data.getNotes()) {
                                mResultNote = new ResultNote(note.getGuid(), note.getTitle());
                                mResultNotesList.add(mResultNote);
                            }
                            mAdapter.notifyDataSetChanged();
                            mListView.setSelection(pos);

                            mDialogFragment.dismiss();
                        }

                        @Override
                        public void onException(Exception exception) {
                            onError(exception, "Error listing notes. ", R.string.error_searching_notes);
                            mDialogFragment.dismiss();
                        }
                    });
        } catch (TTransportException exception){
            onError(exception, "Error creating notestore. ", R.string.error_creating_notestore);
            mDialogFragment.dismiss();
        }
    }

    public void onError(Exception exception, String logstr, int id){
        Log.e(LOGTAG, logstr + exception);
        Toast.makeText(getActivity(), id, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_load_more:
                startIdx += pageSize;
                findNotesByQuery(searchQuery, startIdx, false);
                break;
        }
    }

    /*
     * 検索結果のノートの情報を格納するためのクラス
     */
    private class ResultNote {
        private String guid;
        private String title;

        public ResultNote(String g, String t){
            this.guid = g;
            this.title = t;
        }

        public String toString(){
           return this.title;
        }

    }


}



