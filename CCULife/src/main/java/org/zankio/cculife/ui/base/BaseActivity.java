package org.zankio.cculife.ui.base;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.zankio.cculife.CCUService.portal.Portal;
import org.zankio.cculife.CCUService.portal.service.BasePortal;
import org.zankio.cculife.R;
import org.zankio.cculife.override.AsyncTaskWithErrorHanding;
import org.zankio.cculife.ui.SettingsActivity;

public abstract class BaseActivity extends AppCompatActivity {

    private boolean toolbarInited = false;
    protected View MainView;
    protected View messagePanel;
    protected TextView messageView;
    protected ProgressBar messageLoaging;
    protected ImageView messageIcon;
    protected BasePortal ssoService = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_show_in_browser:
                if (ssoService != null) showInBrowser(ssoService);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public android.support.v7.app.ActionBar getSupportActionBar() {
        initToolbar();
        return super.getSupportActionBar();
    }

    protected void initToolbar() {
        if (toolbarInited) return;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbarInited = true;
    }

    public void setSSOService (BasePortal Service) {
        this.ssoService = Service;
    }

    public void setMessageView(){
        this.MainView = findViewById(R.id.content);
        this.messagePanel = findViewById(R.id.message_panel);
        messageView = (TextView) findViewById(R.id.message);
        messageLoaging = (ProgressBar) findViewById(R.id.loading);
        messageIcon = (ImageView) findViewById(R.id.icon);
    }

    public void setMessageView(int mainview){
        this.MainView = findViewById(mainview);
        this.messagePanel = findViewById(R.id.message_panel);
        messageView = (TextView) findViewById(R.id.message);
        messageLoaging = (ProgressBar) findViewById(R.id.loading);
        messageIcon = (ImageView) findViewById(R.id.icon);
    }

    public void setMessageView(View MainView, View messagePanel){

        this.MainView = MainView;
        this.messagePanel = messagePanel;
        if (messagePanel != null) {
            messageView = (TextView) messagePanel.findViewById(R.id.message);
            messageLoaging = (ProgressBar) messagePanel.findViewById(R.id.loading);
            messageIcon = (ImageView) messagePanel.findViewById(R.id.icon);
        }
    }

    public void hideMessage() {
        if (MainView != null) MainView.setVisibility(View.VISIBLE);

        if (messagePanel != null) messagePanel.setVisibility(View.GONE);
    }

    public void showMessage(String msg) {
        if (MainView != null && messagePanel != null && messageView != null) {
            messageLoaging.setVisibility(View.GONE);
            messageIcon.setVisibility(View.GONE);
            messageView.setText(msg);
            messagePanel.setVisibility(View.VISIBLE);
            MainView.setVisibility(View.GONE);
        }
    }

    public void showMessage(String msg, boolean loading) {
        if (MainView != null && messagePanel != null && messageView != null) {
            messageLoaging.setVisibility(loading ? View.VISIBLE : View.GONE);
            messageIcon.setVisibility(loading ? View.GONE : View.VISIBLE);
            messageView.setText(msg);
            messagePanel.setVisibility(View.VISIBLE);
            MainView.setVisibility(View.GONE);
        }
    }

    public void showMessage(String msg, int resId) {
        if (MainView != null && messagePanel != null && messageView != null) {
            messageLoaging.setVisibility(View.GONE);
            messageIcon.setVisibility(View.VISIBLE);
            messageIcon.setImageResource(resId);
            messageView.setText(msg);
            messagePanel.setVisibility(View.VISIBLE);
            MainView.setVisibility(View.GONE);
        }
    }

    public Toast makeToast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
        return toast;
    }

    public Toast makeToast(String msg, Toast toast) {
        if (toast == null) return makeToast(msg);

        toast.setText(msg);
        toast.show();

        return toast;
    }

    public void showInBrowser(final BasePortal ssoService) {
        new showInBrowserAsyncTask(ssoService).execute();
    }

    public class showInBrowserAsyncTask extends AsyncTaskWithErrorHanding<Void, Void, Void> {
        private BasePortal ssoService;
        private Toast toast;

        public showInBrowserAsyncTask(BasePortal ssoService) {
            this.ssoService = ssoService;
        }

        @Override
        protected void onError(Exception e, String msg) {
            toast = makeToast("載入失敗", toast);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            toast = makeToast("載入中...", toast);
        }

        @Override
        protected Void _doInBackground(Void... params) throws Exception {
            Portal portal;
            String launchURL[];

            portal = new Portal(BaseActivity.this);
            portal.init();
            launchURL = portal.getSSOPortal(ssoService);

            if (launchURL == null) throw new Exception();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(launchURL[0])));

            for (int i = 1; i < launchURL.length; i++) {
                Thread.sleep(1000);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(launchURL[i])));
            }

            return null;
        }

        @Override
        protected void _onPostExecute(Void aVoid) {
            toast.cancel();
        }
    }

}
