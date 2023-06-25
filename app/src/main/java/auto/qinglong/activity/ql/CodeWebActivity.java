package auto.qinglong.activity.ql;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.blankj.utilcode.util.ToastUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;

import auto.qinglong.R;
import auto.qinglong.activity.BaseActivity;
import auto.qinglong.bean.ql.QLDependence;
import auto.qinglong.database.sp.AccountSP;
import auto.qinglong.databinding.ActivityCodeWebBinding;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.network.web.QLWebJsManager;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.WebViewBuilder;

public class CodeWebActivity extends BaseActivity<ActivityCodeWebBinding> {
    public static final String TAG = "CodeWebActivity";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_LOG_PATH = "logPath";
    public static final String EXTRA_SCRIPT_NAME = "scriptName";
    public static final String EXTRA_SCRIPT_PARENT = "scriptParent";
    public static final String EXTRA_DEPENDENCE_ID = "dependenceId";
    public static final String EXTRA_CAN_REFRESH = "canRefresh";
    public static final String EXTRA_CAN_EDIT = "canEdit";
    public static final String TYPE_LOG = "log";
    public static final String TYPE_SCRIPT = "script";
    public static final String TYPE_DEPENDENCE = "dependence";
    public static final String TYPE_CONFIG = "config";

    private boolean mInitFlag = false;
    private String mContent;
    private String mTitle;
    private String mType;
    private boolean mCanRefresh;
    private boolean mCanEdit;
    private String mScriptName;
    private String mScriptParent;
    private String mLogPath;
    private String mDependenceId;
    private WebView ui_webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityCodeWebBinding.class);

        mTitle = getIntent().getStringExtra(EXTRA_TITLE);
        mType = getIntent().getStringExtra(EXTRA_TYPE);
        mCanRefresh = getIntent().getBooleanExtra(EXTRA_CAN_REFRESH, true);
        mCanEdit = getIntent().getBooleanExtra(EXTRA_CAN_EDIT, false);
        mScriptName = getIntent().getStringExtra(EXTRA_SCRIPT_NAME);
        mScriptParent = getIntent().getStringExtra(EXTRA_SCRIPT_PARENT);
        mLogPath = getIntent().getStringExtra(EXTRA_LOG_PATH);
        mDependenceId = getIntent().getStringExtra(EXTRA_DEPENDENCE_ID);
        init();
    }

    @Override
    protected void init() {
        //设置标题
        binding.scriptName.setText(mTitle);

        //返回监听
        binding.scriptBack.setOnClickListener(v -> finish());

        //刷新监听
        if (mCanRefresh) {
            binding.scriptRefresh.setOnClickListener(v -> {
                //禁用点击
                binding.scriptRefresh.setEnabled(false);
                //开启动画
                Animation animation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        binding.scriptRefresh.setEnabled(true);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                animation.setRepeatCount(-1);
                animation.setDuration(1000);
                binding.scriptRefresh.startAnimation(animation);
                load(mType);
            });
        }

        //编辑
        if (mCanEdit) {
            binding.scriptEdit.setOnClickListener(v -> {
                binding.scriptBar.setVisibility(View.INVISIBLE);
                binding.codeBarEdit.setVisibility(View.VISIBLE);
                ui_webView.setFocusable(true);
                ui_webView.setFocusableInTouchMode(true);
                QLWebJsManager.setEditable(ui_webView, true);
            });

            binding.codeBarEditBack.setOnClickListener(v -> {
                binding.codeBarEdit.setVisibility(View.INVISIBLE);
                binding.scriptBar.setVisibility(View.VISIBLE);
                WindowUnit.hideKeyboard(ui_webView);
                ui_webView.clearFocus();
                ui_webView.setFocusable(false);
                ui_webView.setFocusableInTouchMode(false);
                QLWebJsManager.setEditable(ui_webView, false);
                QLWebJsManager.setContent(ui_webView, mContent);
            });

            binding.codeBarEditSave.setOnClickListener(v -> QLWebJsManager.getContent(ui_webView, value -> {
                try {
                    ui_webView.clearFocus();
                    WindowUnit.hideKeyboard(ui_webView);
                    StringBuilder stringBuilder = new StringBuilder(URLDecoder.decode(value, "UTF-8"));
                    if (stringBuilder.length() >= 2) {
                        stringBuilder.deleteCharAt(0);
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                    save(stringBuilder.toString());
                } catch (UnsupportedEncodingException e) {
                    ToastUtils.showShort(e.getMessage());
                }
            }));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mInitFlag) {
            initWebView();
        }
    }

    @Override
    protected void onDestroy() {
        WebViewBuilder.destroy(ui_webView);
        ui_webView = null;
        super.onDestroy();
    }

    private void initWebView() {
        if (mCanRefresh) {
            binding.scriptRefresh.setVisibility(View.VISIBLE);
        }

        ui_webView = WebViewBuilder.build(getBaseContext(), binding.webContainer, new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                load(mType);
            }
        }, null);

        ui_webView.setFocusable(false);
        ui_webView.loadUrl("file:///android_asset/web/editor.html");

        mInitFlag = true;
    }

    /**
     * 加载对应类型内容
     *
     * @param type 类型
     */
    private void load(String type) {
        String authorization = AccountSP.getAuthorization();
        if (TextUtils.isEmpty(authorization)) {
            ToastUtils.showShort("登录信息不存在");
            return;
        }
        switch (type) {
            case TYPE_SCRIPT:
                if (authorization.length() < 100) {
                    ToastUtils.showShort("ClientId方式登录不支持查看脚本");
                    return;
                }
                netGetScriptDetail("api/scripts/" + mScriptName + "?path=" + mScriptParent);
                break;
            case TYPE_LOG:
                netGetLogDetail("api/logs/" + mScriptName + "?path=" + mScriptParent);
                break;
            case TYPE_DEPENDENCE:
                if (authorization.length() < 100) {
                    ToastUtils.showShort("ClientId方式登录不支持查看依赖");
                    return;
                }
                netGetDependenceLog("api/dependencies/" + mDependenceId);
                break;
            case TYPE_CONFIG:
                netGetConfig();
                break;
        }
    }

    /**
     * 网络加载结束 关闭刷新动画
     */
    private void loadFinish() {
        if (binding.scriptRefresh.getAnimation() != null) {
            binding.scriptRefresh.getAnimation().cancel();
        }
    }

    /**
     * 保存内容，当前仅支持配置文件和脚本文件
     *
     * @param content 内容
     */
    private void save(String content) {
        if (Objects.equals(mType, TYPE_CONFIG)) {
            netSaveConfig(content);
        } else if (Objects.equals(mType, TYPE_SCRIPT)) {
            netSaveScript(content);
        }
    }

    private void netGetConfig() {
        QLApiController.getConfigDetail(getNetRequestID(), new QLApiController.NetConfigCallback() {
            @Override
            public void onSuccess(String content) {
                mContent = content;
                binding.scriptEdit.setVisibility(View.VISIBLE);
                QLWebJsManager.setContent(ui_webView, content);
                loadFinish();
                ToastUtils.showShort(getString(R.string.tip_load_success));
            }

            @Override
            public void onFailure(String msg) {
                binding.scriptEdit.setVisibility(View.INVISIBLE);
                loadFinish();
                ToastUtils.showShort(msg);
            }
        });
    }

    private void netSaveConfig(String content) {
        QLApiController.saveConfig(getNetRequestID(), content, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                mContent = content;
                ToastUtils.showShort("保存成功");
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(msg);
            }
        });
    }

    private void netGetScriptDetail(String path) {
        QLApiController.getScriptDetail(getNetRequestID(), path, new QLApiController.NetSimpleCallBack() {
            @Override
            public void onSuccess(String content) {
                //防止内容过大导致崩溃
                if (content.length() > 1500000) {
                    ToastUtils.showShort(getString(R.string.tip_text_too_long));
                    binding.scriptRefresh.setVisibility(View.GONE);
                    return;
                }
                mContent = content;
                binding.scriptEdit.setVisibility(View.VISIBLE);
                QLWebJsManager.setContent(ui_webView, content);
                loadFinish();
                ToastUtils.showShort(getString(R.string.tip_load_success));
            }

            @Override
            public void onFailure(String msg) {
                binding.scriptEdit.setVisibility(View.INVISIBLE);
                loadFinish();
                ToastUtils.showShort(msg);
            }
        });
    }

    private void netSaveScript(String content) {
        QLApiController.saveScript(getNetRequestID(), content, mScriptName, mScriptParent, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                mContent = content;
                ToastUtils.showShort("保存成功");
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(msg);
            }
        });
    }

    private void netGetLogDetail(String path) {
        QLApiController.getLogDetail(getNetRequestID(), path, new QLApiController.NetSimpleCallBack() {
            @Override
            public void onSuccess(String content) {
                QLWebJsManager.setContent(ui_webView, content);
                loadFinish();
                ToastUtils.showShort(getString(R.string.tip_load_success));
            }

            @Override
            public void onFailure(String msg) {
                loadFinish();
                ToastUtils.showShort(msg);
            }
        });
    }

    private void netGetDependenceLog(String path) {
        QLApiController.getDependence(getNetRequestID(), path, new QLApiController.NetGetDependenceCallback() {
            @Override
            public void onSuccess(QLDependence dependence) {
                QLWebJsManager.setContent(ui_webView, dependence.getLogStr());
                loadFinish();
                ToastUtils.showShort(getString(R.string.tip_load_success));
            }

            @Override
            public void onFailure(String msg) {
                loadFinish();
                ToastUtils.showShort(msg);
            }
        });
    }
}
