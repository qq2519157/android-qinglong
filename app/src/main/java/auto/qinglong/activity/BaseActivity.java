package auto.qinglong.activity;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import auto.qinglong.network.http.RequestManager;
import auto.qinglong.utils.ToastUnit;

public abstract class BaseActivity extends AppCompatActivity {
    public static final String TAG = "BaseActivity";
    protected Activity self;
    protected Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getBaseContext();
        self = this;
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        res.updateConfiguration(configuration, res.getDisplayMetrics());
        return res;
    }

    protected String getNetRequestID() {
        return getClass().getName();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ToastUnit.cancel();
    }

    @Override
    protected void onDestroy() {
        RequestManager.cancelAllCall(getClass().getName());
        super.onDestroy();
    }

    protected void init() {

    }

}
