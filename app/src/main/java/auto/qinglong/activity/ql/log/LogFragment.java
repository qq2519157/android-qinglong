package auto.qinglong.activity.ql.log;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.blankj.utilcode.util.ToastUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import auto.qinglong.R;
import auto.qinglong.activity.BaseFragment;
import auto.qinglong.activity.ql.CodeWebActivity;
import auto.qinglong.bean.ql.QLLog;
import auto.qinglong.databinding.FragmentLogBinding;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.network.http.QLApiController;


public class LogFragment extends BaseFragment<FragmentLogBinding> {
    public static String TAG = "LogFragment";
    private boolean canBack = false;
    private List<QLLog> oData;
    private MenuClickListener menuClickListener;
    private LogAdapter logAdapter;


    @Override
    public void onResume() {
        super.onResume();
        initData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            initData();
        }
    }

    private void initData() {
        if (initDataFlag || NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        binding.refreshLayout.autoRefreshAnimationOnly();
        new Handler().postDelayed(() -> {
            if (isVisible()) {
                getLogs();
            }
        }, 1000);
    }

    @Override
    public void init() {
        logAdapter = new LogAdapter(requireContext());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        Objects.requireNonNull(binding.recyclerView.getItemAnimator()).setChangeDuration(0);
        binding.recyclerView.setAdapter(logAdapter);

        binding.logNav.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onMenuClick();
            }
        });

        logAdapter.setItemActionListener(qlLog -> {
            if (qlLog.isDir()) {
                canBack = true;
                sortAndSetData(qlLog.getChildren(), qlLog.getTitle());
            } else {
                Intent intent = new Intent(getContext(), CodeWebActivity.class);
                intent.putExtra(CodeWebActivity.EXTRA_TYPE, CodeWebActivity.TYPE_LOG);
                intent.putExtra(CodeWebActivity.EXTRA_TITLE, qlLog.getTitle());
                intent.putExtra(CodeWebActivity.EXTRA_LOG_PATH, qlLog.getKey());
                intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_NAME, qlLog.getTitle());
                intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_PARENT, qlLog.getParent());
                startActivity(intent);
            }
        });

        binding.refreshLayout.setOnRefreshListener(refreshLayout -> getLogs());
    }


    private void getLogs() {
        QLApiController.getLogs(getNetRequestID(), new QLApiController.NetGetLogsCallback() {
            @Override
            public void onSuccess(List<QLLog> logs) {
                sortAndSetData(logs, "");
                oData = logs;
                canBack = false;
                initDataFlag = true;
                this.onEnd(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("加载失败：" + msg);
                this.onEnd(false);
            }

            protected void onEnd(boolean isSuccess) {
                if (binding.refreshLayout.isRefreshing()) {
                    binding.refreshLayout.finishRefresh(isSuccess);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void sortAndSetData(List<QLLog> data, String dir) {
        Collections.sort(data);
        logAdapter.setData(data);
        binding.logDirTip.setText(getString(R.string.char_path_split) + dir);
    }

    public void setMenuClickListener(MenuClickListener mMenuClickListener) {
        this.menuClickListener = mMenuClickListener;
    }

    @Override
    public boolean onBackPressed() {
        if (canBack) {
            logAdapter.setData(oData);
            binding.logDirTip.setText(getString(R.string.char_path_split));
            canBack = false;
            return true;
        } else {
            return false;
        }

    }
}