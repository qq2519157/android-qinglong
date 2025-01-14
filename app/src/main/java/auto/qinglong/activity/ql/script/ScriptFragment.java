package auto.qinglong.activity.ql.script;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.blankj.utilcode.util.ToastUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import auto.qinglong.R;
import auto.qinglong.activity.BaseFragment;
import auto.qinglong.activity.ql.CodeWebActivity;
import auto.qinglong.bean.ql.QLScript;
import auto.qinglong.databinding.FragmentScriptBinding;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.views.popup.PopMenuItem;
import auto.qinglong.views.popup.PopMenuWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;


public class ScriptFragment extends BaseFragment<FragmentScriptBinding> {
    public static String TAG = "ScriptFragment";

    private List<QLScript> rootData;//根数据
    private String curDir;//当前目录
    private boolean canBack = false;//可返回操作
    private MenuClickListener menuClickListener;
    private ScriptAdapter scriptAdapter;


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

    @Override
    public void setMenuClickListener(MenuClickListener mMenuClickListener) {
        this.menuClickListener = mMenuClickListener;
    }

    @Override
    public boolean onBackPressed() {
        if (canBack) {
            scriptAdapter.setData(rootData);
            binding.scriptDirTip.setText(getString(R.string.char_path_split));
            canBack = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init() {
        scriptAdapter = new ScriptAdapter(requireContext());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        Objects.requireNonNull(binding.recyclerView.getItemAnimator()).setChangeDuration(0);
        binding.recyclerView.setAdapter(scriptAdapter);

        scriptAdapter.setScriptInterface(new ScriptAdapter.ItemActionListener() {
            @Override
            public void onEdit(QLScript script) {
                if (script.isDirectory()) {
                    canBack = true;
                    sortAndSetData(script.getChildren(), script.getTitle());
                } else {
                    Intent intent = new Intent(getContext(), CodeWebActivity.class);
                    intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_NAME, script.getTitle());
                    intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_PARENT, script.getParent());
                    intent.putExtra(CodeWebActivity.EXTRA_TITLE, script.getTitle());
                    intent.putExtra(CodeWebActivity.EXTRA_TYPE, CodeWebActivity.TYPE_SCRIPT);
                    intent.putExtra(CodeWebActivity.EXTRA_CAN_EDIT, true);
                    startActivity(intent);
                }
            }

            @Override
            public void onMenu(View view, QLScript script, int position) {
                showPopMenu(view, script, position);
            }
        });

        binding.refreshLayout.setOnRefreshListener(refreshLayout -> netGetScripts());

        binding.scripMenu.setOnClickListener(v -> menuClickListener.onMenuClick());

        binding.scriptMore.setOnClickListener(this::showPopMenu);
    }

    private void initData() {
        if (initDataFlag || NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        binding.refreshLayout.autoRefreshAnimationOnly();
        new Handler().postDelayed(() -> {
            if (isVisible()) {
                netGetScripts();
            }
        }, 1000);

    }

    private void showPopMenu(View v, QLScript script, int position) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(v, Gravity.CENTER);
        popMenuWindow.addItem(new PopMenuItem("copy", "复制路径", R.drawable.ic_gray_crop_free));
//        popMenuWindow.addItem(new PopMenuItem("backup", "脚本备份", R.drawable.ic_gray_download));
        if (script.isFile()) {
//            popMenuWindow.addItem(new PopMenuItem("replace", "脚本替换", R.drawable.ic_gray_copy));
            popMenuWindow.addItem(new PopMenuItem("delete", "删除脚本", R.drawable.ic_gray_delete));
        }

        popMenuWindow.setOnActionListener(key -> {
            switch (key) {
                case "copy":
                    ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, script.getKey()));
                    ToastUtils.showShort(getString(R.string.tip_copy_path_ready));
                    break;
                case "backup":
                    break;
                case "delete":
                    netDeleteScript(script, position);
                    break;
            }
            return true;
        });

        PopupWindowBuilder.buildMenuWindow(requireActivity(), popMenuWindow);
    }

    private void showPopMenu(View v) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(v, Gravity.END);
        popMenuWindow.addItem(new PopMenuItem("add", "新建脚本", R.drawable.ic_gray_add));
        popMenuWindow.addItem(new PopMenuItem("import", "本地导入", R.drawable.ic_gray_upload));
        popMenuWindow.addItem(new PopMenuItem("backup", "脚本备份", R.drawable.ic_gray_download));

        popMenuWindow.setOnActionListener(key -> true);
        PopupWindowBuilder.buildMenuWindow(requireActivity(), popMenuWindow);
    }

    @SuppressLint("SetTextI18n")
    private void sortAndSetData(List<QLScript> scripts, String director) {
        Collections.sort(scripts);
        scriptAdapter.setData(scripts);
        curDir = director;
        binding.scriptDirTip.setText(getString(R.string.char_path_split) + director);
    }

    private void netGetScripts() {
        QLApiController.getScripts(getNetRequestID(), new QLApiController.NetGetScriptsCallback() {
            @Override
            public void onSuccess(List<QLScript> scripts) {
                sortAndSetData(scripts, "");
                rootData = scripts;
                canBack = false;
                initDataFlag = true;
                this.onEnd(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(getString(R.string.tip_load_failure_header) + msg);
                this.onEnd(false);
            }

            private void onEnd(boolean isSuccess) {
                if (binding.refreshLayout.isRefreshing()) {
                    binding.refreshLayout.finishRefresh(isSuccess);
                }
            }
        });
    }

    private void netDeleteScript(QLScript script, int position) {
        QLApiController.deleteScript(getNetRequestID(), script.getTitle(), script.getParent(), new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                ToastUtils.showShort(getString(R.string.tip_delete_success));
                scriptAdapter.removeItem(position);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(getString(R.string.tip_delete_failure_header) + msg);
            }
        });
    }

}