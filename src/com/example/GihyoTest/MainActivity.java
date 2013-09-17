package com.example.GihyoTest;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.InvalidAuthenticationException;

public class MainActivity extends Activity implements MenuFragment.OnMenuItemSelectedListener {
    // 認証用の情報
    public static final String CONSUMER_KEY = "";
    public static final String CONSUMER_SECRET = "";
    public static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.SANDBOX;

    // Evernoteのセッション保持用変数
    private EvernoteSession mEvernoteSession;

    // レイアウト変数
    private Menu mMenu = null;
    private RelativeLayout mLogintLayout;

    // ログ用タグ
    private static final String LOGTAG = "MainActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mLogintLayout = (RelativeLayout) findViewById(R.id.loginLayout);

        // Evernoteのセッション情報を取得
        mEvernoteSession = EvernoteSession.getInstance(this, CONSUMER_KEY, CONSUMER_SECRET, EVERNOTE_SERVICE);

        // Fragmentが無ければMenuFragmentを表示
        FragmentManager fm = getFragmentManager();
        fm.executePendingTransactions();
        if(fm.getBackStackEntryCount() == 0) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.fragment_container, new MenuFragment());
            ft.commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAuthUi();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem itemLogout = mMenu.findItem(R.id.action_logout);

        if (mEvernoteSession.isLoggedIn()) {
            itemLogout.setVisible(true);
        } else {
            itemLogout.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu, menu);
        mMenu = menu;
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                // Fragmentの履歴を全て消去
                FragmentManager fm = getFragmentManager();
                for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                    fm.popBackStack();
                }

                // ログアウト処理
                try {
                    mEvernoteSession.logOut(this);
                } catch (InvalidAuthenticationException e) {
                    Log.e(LOGTAG, "未ログイン状態でのログインエラー", e);
                }
                updateAuthUi();
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Oauth認証結果を受け取る
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case EvernoteSession.REQUEST_CODE_OAUTH:
                if (resultCode == Activity.RESULT_OK) {
                    updateAuthUi();
                    invalidateOptionsMenu();
                }
                break;
            default:
                break;
        }
    }

    /*
     * MenuFragmentのアイテム押下のリスナー
     */
    @Override
    public void onMenuItemSelected(int position) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        switch (position) {
            case 0:
                ft.replace(R.id.fragment_container, new SimpleNoteFragment(), "SimpleNote");
                break;
            case 1:
                ft.replace(R.id.fragment_container, new SpecialNoteFragment(), "SpecialNote");
                break;
            default:
                ;
        }
        // 「戻る」ボタンで戻れるようにFragmentをバックスタックに積む
        ft.addToBackStack(null);
        ft.commit();
        fm.executePendingTransactions();
    }

    // ログイン状態に応じてログイン画面の描画/消去を切り替える
    private void updateAuthUi() {
        if(mLogintLayout != null) {
            if (mEvernoteSession.isLoggedIn()) {
                mLogintLayout.setVisibility(View.GONE);
            } else {
                mLogintLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    // ログイン処理
    public void getAuth(View view) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        if (nInfo == null) {
            Toast.makeText(this, "Please connect to 3G/Wi-Fi.", Toast.LENGTH_LONG).show();
        } else {
            mEvernoteSession.authenticate(getApplicationContext());
        }
    }


}
