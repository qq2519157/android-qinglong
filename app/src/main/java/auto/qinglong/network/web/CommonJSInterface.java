package auto.qinglong.network.web;

import android.webkit.JavascriptInterface;

import com.blankj.utilcode.util.ToastUtils;

import auto.qinglong.utils.LogUnit;

public class CommonJSInterface {
    public final static String TAG = "CommonJSInterface";

    @JavascriptInterface
    public void toast(String content) {
        ToastUtils.showShort(content);
    }

    @JavascriptInterface
    public void log(String content) {
        LogUnit.log(TAG, content);
    }
}
