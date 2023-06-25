package auto.qinglong.activity.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.blankj.utilcode.util.ToastUtils;

import java.net.URLEncoder;

import auto.qinglong.R;
import auto.qinglong.activity.BaseActivity;
import auto.qinglong.database.sp.SettingSP;
import auto.qinglong.databinding.ActivitySettingBinding;
import auto.qinglong.utils.DeviceUnit;
import auto.qinglong.utils.WebUnit;

public class SettingActivity extends BaseActivity<ActivitySettingBinding> {
    public static final String TAG = "SettingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivitySettingBinding.class);
        init();
    }

    @Override
    protected void init() {
        binding.barBack.setOnClickListener(v -> finish());

        binding.appSettingNotifySwitch.setChecked(SettingSP.isNotify());
        binding.appSettingVibrateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> SettingSP.setBoolean(SettingSP.FIELD_NOTIFY, isChecked));
        binding.appSettingVibrateSwitch.setChecked(SettingSP.isVibrate());
        binding.appSettingVibrateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> SettingSP.setBoolean(SettingSP.FIELD_VIBRATE, isChecked));

        binding.appSettingDocument.setOnClickListener(v -> WebUnit.open(this, getString(R.string.url_readme)));

        binding.appSettingIssue.setOnClickListener(v -> WebUnit.open(this, getString(R.string.url_issue)));

        binding.appSettingShare.setOnClickListener(v -> DeviceUnit.shareText(this, getString(R.string.app_share_description)));

        binding.appSettingDonate.setOnClickListener(v -> {
            try {
                String scheme = getString(R.string.url_alipay_scheme) + URLEncoder.encode(getString(R.string.url_alipay_qrcode), "UTF-8");
                Uri uri = Uri.parse(scheme);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                startActivity(intent);
            } catch (Exception e) {
                ToastUtils.showShort(e.getLocalizedMessage());
            }

        });
    }
}