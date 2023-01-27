package auto.qinglong.activity.ql.script;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import auto.qinglong.R;
import auto.qinglong.activity.BaseFragment;
import auto.qinglong.activity.ql.CodeWebActivity;
import auto.qinglong.bean.ql.QLScript;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.network.http.RequestManager;
import auto.qinglong.utils.ToastUnit;


public class ScriptFragment extends BaseFragment {
    public static String TAG = "ScriptFragment";

    private MenuClickListener menuClickListener;
    private ScriptAdapter scriptAdapter;
    //根数据
    private List<QLScript> oData;
    //可返回操作
    private boolean canBack = false;

    private ImageView ui_menu;
    private SmartRefreshLayout ui_refresh;
    private TextView ui_dir_tip;
    private RecyclerView ui_recycler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fg_script, null, false);

        ui_dir_tip = view.findViewById(R.id.script_dir_tip);
        ui_menu = view.findViewById(R.id.scrip_menu);
        ui_refresh = view.findViewById(R.id.refreshLayout);
        ui_recycler = view.findViewById(R.id.recyclerView);

        init();

        return view;
    }

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
    public void setMenuClickListener(MenuClickListener menuClickListener) {
        this.menuClickListener = menuClickListener;
    }

    @Override
    public boolean onBackPressed() {
        if (canBack) {
            scriptAdapter.setData(oData);
            ui_dir_tip.setText(getString(R.string.char_path_split));
            canBack = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init() {
        scriptAdapter = new ScriptAdapter(requireContext());
        ui_recycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        Objects.requireNonNull(ui_recycler.getItemAnimator()).setChangeDuration(0);
        ui_recycler.setAdapter(scriptAdapter);

        //item回调
        scriptAdapter.setScriptInterface(new ScriptAdapter.ItemActionListener() {
            @Override
            public void onEdit(QLScript script) {
                if (script.getChildren() != null) {
                    canBack = true;
                    sortAndSetData(script.getChildren(), script.getTitle());
                } else {
                    Intent intent = new Intent(getContext(), CodeWebActivity.class);
                    intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_NAME, script.getTitle());
                    intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_PARENT, script.getParent());
                    intent.putExtra(CodeWebActivity.EXTRA_TITLE, script.getTitle());
                    intent.putExtra(CodeWebActivity.EXTRA_TYPE, CodeWebActivity.TYPE_SCRIPT);
                    intent.putExtra(CodeWebActivity.EXTRA_CAN_EDIT,true);
                    startActivity(intent);
                }
            }

            @Override
            public void onMulAction(QLScript QLScript) {
                ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, QLScript.getKey()));
                ToastUnit.showShort(getString(R.string.tip_copy_path_ready));
            }
        });

        //刷新控件//
        //初始设置处于刷新状态
        ui_refresh.autoRefreshAnimationOnly();
        ui_refresh.setOnRefreshListener(refreshLayout -> netGetScripts());

        //唤起主导航栏
        ui_menu.setOnClickListener(v -> menuClickListener.onMenuClick());
    }

    private void initData() {
        if (!initDataFlag && !RequestManager.isRequesting(getNetRequestID())) {
            new Handler().postDelayed(() -> {
                if (isVisible()) {
                    netGetScripts();
                }
            }, 1000);
        }

    }

    private void sortAndSetData(List<QLScript> data, String dir) {
        Collections.sort(data);
        scriptAdapter.setData(data);
        String text = getString(R.string.char_path_split) + dir;
        ui_dir_tip.setText(text);
    }

    private void netGetScripts() {
        QLApiController.getScripts(getNetRequestID(), new QLApiController.NetGetScriptsCallback() {
            @Override
            public void onSuccess(List<QLScript> QLScripts) {
                sortAndSetData(QLScripts, "");
                oData = QLScripts;
                canBack = false;
                initDataFlag = true;
                this.onEnd(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("加载失败：" + msg);
                this.onEnd(false);
            }

            private void onEnd(boolean isSuccess) {
                if (ui_refresh.isRefreshing()) {
                    ui_refresh.finishRefresh(isSuccess);
                }
            }
        });
    }

}