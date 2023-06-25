package auto.qinglong.activity.ql.setting;

import android.content.Intent;

import com.blankj.utilcode.util.ToastUtils;

import auto.qinglong.activity.BaseFragment;
import auto.qinglong.activity.app.LoginActivity;
import auto.qinglong.bean.app.Account;
import auto.qinglong.database.sp.AccountSP;
import auto.qinglong.databinding.FragmentSettingCommonBinding;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.utils.LogUnit;
import auto.qinglong.utils.TextUnit;
import auto.qinglong.utils.WindowUnit;

public class CommonFragment extends BaseFragment<FragmentSettingCommonBinding> {

    private int mOldFrequency = -1;

    @Override
    protected void init() {

        binding.settingLogSave.setOnClickListener(v -> {
            String value = binding.settingLog.getText().toString();
            if (TextUnit.isEmpty(value)) {
                ToastUtils.showShort("请输入正确数值");
                return;
            }
            WindowUnit.hideKeyboard(binding.settingLog);
            netUpdateLogRemove(Integer.parseInt(value), mOldFrequency);
        });

        binding.settingSecuritySave.setOnClickListener(v -> {
            String username = binding.settingSecurityUsername.getText().toString();
            String password = binding.settingSecurityPassword.getText().toString();

            if (username.isEmpty()) {
                ToastUtils.showShort("请输入用户名");
                return;
            }
            if (password.isEmpty()) {
                ToastUtils.showShort("请输入密码");
                return;
            }

            WindowUnit.hideKeyboard(binding.settingSecurityUsername);
            Account account = new Account(username, password, AccountSP.getAddress(), null);
            netUpdateUser(account);
        });

        netGetLogRemove();
    }

    private void netGetLogRemove() {
        QLApiController.getLogRemove(getNetRequestID(), new QLApiController.NetGetLogRemoveCallback() {
            @Override
            public void onSuccess(int frequency) {
                binding.settingLog.setText(String.valueOf(frequency));
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(msg);
            }
        });
    }

    private void netUpdateLogRemove(int newFrequency, int oldFrequency) {
        QLApiController.updateLogRemove(getNetRequestID(), newFrequency, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                binding.settingLog.clearFocus();
                mOldFrequency = newFrequency;
                ToastUtils.showShort("保存成功");
            }

            @Override
            public void onFailure(String msg) {
                binding.settingLog.clearFocus();
                binding.settingLog.clearComposingText();
                if (oldFrequency > -1) {
                    binding.settingLog.setText(String.valueOf(oldFrequency));
                } else {
                    binding.settingLog.setText("");
                }
                ToastUtils.showShort(msg);
            }
        });
    }

    private void netUpdateUser(Account account) {
        QLApiController.updateUser(getNetRequestID(), account, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                AccountSP.updateCurrentAccount(account);
                netLogin(account);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(msg);
            }
        });
    }

    protected void netLogin(Account account) {
        QLApiController.login(this.getNetRequestID(), account, new QLApiController.NetLoginCallback() {
            @Override
            public void onSuccess(Account account) {
                binding.settingSecurityUsername.setText(null);
                binding.settingSecurityPassword.setText(null);
                AccountSP.updateCurrentAccount(account);
                ToastUtils.showShort("更新成功");
            }

            @Override
            public void onFailure(String msg) {
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                requireActivity().startActivity(intent);
                requireActivity().finish();
                LogUnit.log(msg);
            }
        });
    }
}