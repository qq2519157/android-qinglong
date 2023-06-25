package auto.qinglong.activity.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import com.blankj.utilcode.util.ToastUtils;

import java.util.Objects;

import auto.qinglong.R;
import auto.qinglong.activity.BaseActivity;
import auto.qinglong.activity.BaseFragment;
import auto.qinglong.activity.extension.web.PluginWebActivity;
import auto.qinglong.activity.ql.CodeWebActivity;
import auto.qinglong.activity.ql.dependence.DepPagerFragment;
import auto.qinglong.activity.ql.environment.EnvFragment;
import auto.qinglong.activity.ql.log.LogFragment;
import auto.qinglong.activity.ql.script.ScriptFragment;
import auto.qinglong.activity.ql.setting.SettingFragment;
import auto.qinglong.activity.ql.task.TaskFragment;
import auto.qinglong.bean.app.Version;
import auto.qinglong.bean.ql.QLSystem;
import auto.qinglong.database.sp.AccountSP;
import auto.qinglong.database.sp.SettingSP;
import auto.qinglong.databinding.ActivityHomeBinding;
import auto.qinglong.network.http.ApiController;
import auto.qinglong.utils.EncryptUtil;
import auto.qinglong.utils.LogUnit;
import auto.qinglong.utils.TextUnit;
import auto.qinglong.utils.WebUnit;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.popup.PopConfirmWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;

public class HomeActivity extends BaseActivity<ActivityHomeBinding> {
    public static final String TAG = "HomeActivity";

    private long mLastBackPressedTime = 0;//上次按下返回键时间
    private BaseFragment mCurrentFragment;//当前帧
    private String mCurrentMenu;
    private BaseFragment.MenuClickListener mMenuClickListener;
    // 碎片界面列表
    private TaskFragment fg_task;
    private LogFragment fg_log;
    private ScriptFragment fg_script;
    private EnvFragment fg_environment;
    private DepPagerFragment fg_dependence;
    private SettingFragment fg_setting;
    private PopupWindow ui_pop_notice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityHomeBinding.class);
        init();
    }

    @Override
    protected void init() {
        //变量初始化
        mMenuClickListener = () -> binding.drawerLayout.openDrawer(binding.drawerLeft);
        //导航栏初始化
        initDrawerBar();
        //初始化第一帧页面
        showFragment(TaskFragment.TAG);
        //版本检查
//        netGetVersion();
    }

    @Override
    public void onBackPressed() {
        if (!mCurrentFragment.onBackPressed()) {
            long current = System.currentTimeMillis();
            if (current - mLastBackPressedTime < 2000) {
                finish();
            } else {
                mLastBackPressedTime = current;
                ToastUtils.showShort(getString(R.string.tip_exit_app));
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //进度pop存在阻止点击
        if (ui_pop_notice != null && ui_pop_notice.isShowing()) {
            return false;
        }
        //询问当前帧是否阻止点击
        if (mCurrentFragment != null && mCurrentFragment.onDispatchTouchEvent()) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }


    private void initDrawerBar() {
        binding.drawerLeft.setVisibility(View.INVISIBLE);
        //用户名
        binding.menuTopInfoUsername.setText(Objects.requireNonNull(AccountSP.getCurrentAccount()).getUsername());
        //面板地址
        binding.menuTopInfoAddress.setText(AccountSP.getCurrentAccount().getAddress());
        //面板版本
        binding.menuTopInfoVersion.setText(String.format(getString(R.string.format_tip_version), QLSystem.getStaticVersion()));

        //面板功能
        binding.menuTask.setOnClickListener(v -> showFragment(TaskFragment.TAG));

        binding.menuLog.setOnClickListener(v -> showFragment(LogFragment.TAG));

        binding.menuConfig.setOnClickListener(v -> {
            Intent intent = new Intent(this, CodeWebActivity.class);
            intent.putExtra(CodeWebActivity.EXTRA_TYPE, CodeWebActivity.TYPE_CONFIG);
            intent.putExtra(CodeWebActivity.EXTRA_TITLE, "config.sh");
            intent.putExtra(CodeWebActivity.EXTRA_CAN_EDIT, true);
            startActivity(intent);
        });

        binding.menuScript.setOnClickListener(v -> showFragment(ScriptFragment.TAG));

        binding.menuEnv.setOnClickListener(v -> showFragment(EnvFragment.TAG));

        binding.menuDep.setOnClickListener(v -> showFragment(DepPagerFragment.TAG));

        binding.menuSetting.setOnClickListener(v -> showFragment(SettingFragment.TAG));

        //拓展模块
        binding.menuExtensionWeb.setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), PluginWebActivity.class);
            startActivity(intent);
        });

        //应用功能
        binding.menuExit.setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.activity_alpha_enter, R.anim.activity_alpha_out);
            finish();
        });

        binding.menuAppSetting.setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), SettingActivity.class);
            startActivity(intent);
        });
    }

    private void showFragment(String menu) {
        //点击当前界面导航则直接返回
        if (menu.equals(mCurrentMenu)) {
            return;
        } else {
            mCurrentMenu = menu;
        }
        //记录之前帧
        BaseFragment old = mCurrentFragment;

        if (menu.equals(TaskFragment.TAG)) {
            if (fg_task == null) {
                fg_task = new TaskFragment();
                fg_task.setMenuClickListener(mMenuClickListener);
                getSupportFragmentManager().beginTransaction().add(R.id.frame_layout, fg_task, TaskFragment.TAG).commit();
            }
            mCurrentFragment = fg_task;
        } else if (menu.equals(LogFragment.TAG)) {
            if (fg_log == null) {
                fg_log = new LogFragment();
                fg_log.setMenuClickListener(mMenuClickListener);
                getSupportFragmentManager().beginTransaction().add(R.id.frame_layout, fg_log, LogFragment.TAG).commit();
            }
            mCurrentFragment = fg_log;
        } else if (menu.equals(ScriptFragment.TAG)) {
            if (fg_script == null) {
                fg_script = new ScriptFragment();
                fg_script.setMenuClickListener(mMenuClickListener);
                getSupportFragmentManager().beginTransaction().add(R.id.frame_layout, fg_script, ScriptFragment.TAG).commit();
            }
            mCurrentFragment = fg_script;
        } else if (menu.equals(EnvFragment.TAG)) {
            if (fg_environment == null) {
                fg_environment = new EnvFragment();
                fg_environment.setMenuClickListener(mMenuClickListener);
                getSupportFragmentManager().beginTransaction().add(R.id.frame_layout, fg_environment, EnvFragment.TAG).commit();
            }
            mCurrentFragment = fg_environment;
        } else if (menu.equals(DepPagerFragment.TAG)) {
            if (fg_dependence == null) {
                fg_dependence = new DepPagerFragment();
                fg_dependence.setMenuClickListener(mMenuClickListener);
                getSupportFragmentManager().beginTransaction().add(R.id.frame_layout, fg_dependence, EnvFragment.TAG).commit();
            }
            mCurrentFragment = fg_dependence;
        } else if (menu.equals(SettingFragment.TAG)) {
            if (fg_setting == null) {
                fg_setting = new SettingFragment();
                fg_setting.setMenuClickListener(mMenuClickListener);
                getSupportFragmentManager().beginTransaction().add(R.id.frame_layout, fg_setting, EnvFragment.TAG).commit();
            }
            mCurrentFragment = fg_setting;
        }

        //隐藏旧页面
        if (old != null) {
            getSupportFragmentManager().beginTransaction().hide(old).commit();
        }
        //显示新页面
        getSupportFragmentManager().beginTransaction().show(mCurrentFragment).commit();
        //关闭导航栏
        if (binding.drawerLayout.isDrawerOpen(binding.drawerLeft)) {
            binding.drawerLayout.closeDrawer(binding.drawerLeft);
        }
    }

    private void checkVersion(Version version) {
        try {
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            //若版本强制更新 即使停用更新推送仍会要求更新
            if (version.getVersionCode() > versionCode && (version.isForce() || SettingSP.isNotify())) {
                showVersionNotice(version);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showVersionNotice(Version version) {
        String content = "最新版本：" + version.getVersionName() + "\n\n";
        content += "更新时间：" + version.getUpdateTime() + "\n\n";
        content += TextUnit.join(version.getUpdateDetail(), "\n\n");

        PopConfirmWindow popConfirmWindow = new PopConfirmWindow("版本更新", content, "取消", "更新");
        popConfirmWindow.setMaxHeight(WindowUnit.getWindowHeightPix(getBaseContext()) / 3);
        popConfirmWindow.setFocusable(false);
        popConfirmWindow.setOnActionListener(isConfirm -> {
            if (isConfirm) {
                WebUnit.open(this, version.getDownloadUrl());
                return !version.isForce();
            } else {
                if (version.isForce()) {
                    finish();
                }
                return true;
            }
        });
        ui_pop_notice = PopupWindowBuilder.buildConfirmWindow(this, popConfirmWindow);
    }

    private void netGetVersion() {
        ApiController.getProject(getNetRequestID());
        String uid = EncryptUtil.md5(AccountSP.getAddress());
        ApiController.getVersion(getNetRequestID(), uid, new ApiController.VersionCallback() {
            @Override
            public void onSuccess(Version version) {
                checkVersion(version);
            }

            @Override
            public void onFailure(String msg) {
                LogUnit.log(TAG, msg);
            }
        });
    }
}