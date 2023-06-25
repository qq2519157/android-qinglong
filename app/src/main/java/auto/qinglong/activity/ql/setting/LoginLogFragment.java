package auto.qinglong.activity.ql.setting;

import android.os.Handler;
import android.text.TextUtils;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ToastUtils;

import java.util.List;
import java.util.Objects;

import auto.qinglong.activity.BaseFragment;
import auto.qinglong.bean.ql.QLLoginLog;
import auto.qinglong.database.sp.AccountSP;
import auto.qinglong.databinding.FragmentSettingLoginLogBinding;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.network.http.QLApiController;


public class LoginLogFragment extends BaseFragment<FragmentSettingLoginLogBinding> {

    private LoginLogItemAdapter itemAdapter;

    @Override
    public void onResume() {
        super.onResume();
        initData();
    }

    @Override
    protected void init() {
        itemAdapter = new LoginLogItemAdapter(getContext());

        Objects.requireNonNull(binding.recyclerView.getItemAnimator()).setChangeDuration(0);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        binding.recyclerView.setAdapter(itemAdapter);

        binding.refreshLayout.setOnRefreshListener(refreshLayout -> netGetLoginLogs());
    }

    private void initData() {
        if (initDataFlag || NetManager.isRequesting(this.getNetRequestID())) {
            return;
        }
        String authorization = AccountSP.getAuthorization();
        if (TextUtils.isEmpty(authorization)) {
            ToastUtils.showShort("登录信息不存在");
            return;
        }
        if (authorization.length() < 100) {
            ToastUtils.showShort("ClientId方式登录不支持查看登录日志");
            return;
        }
        binding.refreshLayout.autoRefreshAnimationOnly();
        new Handler().postDelayed(() -> {
            if (isVisible()) {
                netGetLoginLogs();
            }
        }, 1000);
    }

    private void netGetLoginLogs() {
        QLApiController.getLoginLogs(getNetRequestID(), new QLApiController.NetGetLoginLogsCallback() {
            @Override
            public void onSuccess(List<QLLoginLog> logs) {
                itemAdapter.setData(logs);
                initDataFlag = true;
                this.onEnd(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(msg);
                this.onEnd(false);
            }

            private void onEnd(boolean isSuccess) {
                if (binding.refreshLayout.isRefreshing()) {
                    binding.refreshLayout.finishRefresh(isSuccess);
                }
            }
        });
    }
}