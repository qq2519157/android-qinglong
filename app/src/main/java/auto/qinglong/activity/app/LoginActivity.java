package auto.qinglong.activity.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;

import com.blankj.utilcode.util.NumberUtils;
import com.blankj.utilcode.util.ToastUtils;

import auto.qinglong.R;
import auto.qinglong.activity.BaseActivity;
import auto.qinglong.bean.app.Account;
import auto.qinglong.bean.ql.QLSystem;
import auto.qinglong.database.sp.AccountSP;
import auto.qinglong.databinding.ActivityLoginBinding;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.utils.WebUnit;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.popup.PopProgressWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;

public class LoginActivity extends BaseActivity<ActivityLoginBinding> {
    public static final String TAG = "LoginActivity";

    private PopProgressWindow ui_pop_progress;
    private boolean isUsingPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityLoginBinding.class);
        init();
    }

    @Override
    protected void onDestroy() {
        //关闭pop 防止内存泄漏
        if (ui_pop_progress != null) {
            ui_pop_progress.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (ui_pop_progress != null && ui_pop_progress.isShowing()) {
            ui_pop_progress.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ui_pop_progress != null && ui_pop_progress.isShowing()) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void init() {
        binding.imgLogo.setOnClickListener(v -> WebUnit.open(this, getString(R.string.url_project)));
        binding.switchcompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isUsingPassword = isChecked;
            if (isUsingPassword) {
                binding.etUsername.setHint(R.string.str_username);
                binding.etPassword.setHint(R.string.str_password);
            } else {
                binding.etUsername.setHint(R.string.str_client_id);
                binding.etPassword.setHint(R.string.str_client_secret);
            }
        });
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.etPassword.clearFocus();
                binding.btConfirm.performClick();
                return true;
            }
            return false;
        });

        binding.btConfirm.setOnClickListener(v -> {
            if (ui_pop_progress != null && ui_pop_progress.isShowing()) {
                return;
            }

            String address = binding.etAddress.getText().toString();

            if (!address.matches("(([0-9a-zA-Z])|([.:/_-]))+")) {
                ToastUtils.showShort("地址格式错误");
                return;
            }

            if (address.endsWith("/")) {
                ToastUtils.showShort("请勿以'/'结尾");
                return;
            }

            String username = binding.etUsername.getText().toString().trim();
            if (username.isEmpty()) {
                ToastUtils.showShort("账号不能为空");
                return;
            }

            String password = binding.etPassword.getText().toString().trim();
            if (password.isEmpty()) {
                ToastUtils.showShort("密码不能为空");
                return;
            }
            WindowUnit.hideKeyboard(binding.etPassword);

            binding.btConfirm.setEnabled(false);
            binding.btConfirm.postDelayed(() -> binding.btConfirm.setEnabled(true), 300);

            if (ui_pop_progress == null) {
                ui_pop_progress = PopupWindowBuilder.buildProgressWindow(this, () -> NetManager.cancelAllCall(getNetRequestID()));
            }
            ui_pop_progress.setTextAndShow("登录中...");

            Account account = new Account(username, password, address, "");
            //账号存在本地则尝试旧token 避免重复登录
            account.setToken(AccountSP.getAuthorization(address, username, password));
            //检测系统是否初始化和版本信息(延迟500ms)
            new Handler().postDelayed(() -> netQuerySystemInfo(account), 500);

        });

        //显示之前账号
        Account account = AccountSP.getCurrentAccount();
        if (account != null) {
            binding.etAddress.setText(account.getAddress());
            binding.etUsername.setText(account.getUsername());
            binding.etPassword.setText(account.getPassword());
        }
    }

    /**
     * 进入主界面
     */
    private void enterHome() {
        Intent intent = new Intent(mContext, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    protected void netQuerySystemInfo(Account account) {
        QLApiController.getSystemInfo(this.getNetRequestID(), account, new QLApiController.NetSystemCallback() {
            @Override
            public void onSuccess(QLSystem system) {
                String version = system.getVersion();
                String substring = version.substring(0, 4);
                double versionNum;
                try {
                    versionNum = Double.parseDouble(substring);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    versionNum = 2.09;
                }
                QLSystem.setStaticVersion(version);
                if (versionNum < 2.10) {
                    ToastUtils.showShort("仅支持2.10以上面板");
                    ui_pop_progress.dismiss();
                    return;
                }
                if (system.isInitialized()) {
                    if (!TextUtils.isEmpty(account.getToken())) {
                        netCheckToken(account);
                    } else {
                        netLogin(account);
                    }
                } else {
                    ui_pop_progress.dismiss();
                    ToastUtils.showShort("系统未初始化，无法登录");
                }
            }

            @Override
            public void onFailure(String msg) {
                ui_pop_progress.dismiss();
                ToastUtils.showShort(msg);
            }
        });
    }

    protected void netCheckToken(Account account) {
        QLApiController.checkToken(this.getNetRequestID(), account, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                enterHome();
            }

            @Override
            public void onFailure(String msg) {
                netLogin(account);
            }
        });
    }

    protected void netLogin(Account account) {
        if (isUsingPassword) {
            QLApiController.login(this.getNetRequestID(), account, new QLApiController.NetLoginCallback() {
                @Override
                public void onSuccess(Account account) {
                    AccountSP.updateCurrentAccount(account);
                    enterHome();
                }

                @Override
                public void onFailure(String msg) {
                    ui_pop_progress.dismiss();
                    ToastUtils.showShort(msg);
                }
            });
        } else {
            QLApiController.loginByClientId(this.getNetRequestID(), account, new QLApiController.NetLoginCallback() {
                @Override
                public void onSuccess(Account account) {
                    AccountSP.updateCurrentAccount(account);
                    enterHome();
                }

                @Override
                public void onFailure(String msg) {
                    ui_pop_progress.dismiss();
                    ToastUtils.showShort(msg);
                }
            });
        }

    }
}