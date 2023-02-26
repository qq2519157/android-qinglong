package auto.qinglong.activity.ql.script;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
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
import auto.qinglong.database.db.StatisticsDBHelper;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.utils.ToastUnit;
import auto.qinglong.views.popup.PopMenuItem;
import auto.qinglong.views.popup.PopMenuWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;


public class ScriptFragment extends BaseFragment {
    public static String TAG = "ScriptFragment";

    private MenuClickListener menuClickListener;
    private ScriptAdapter scriptAdapter;
    private List<QLScript> oData;//根数据
    private boolean canBack = false;//可返回操作

    private ImageView ui_menu;
    private ImageView ui_more;
    private SmartRefreshLayout ui_refresh;
    private TextView ui_dir_tip;
    private RecyclerView ui_recycler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_script, null, false);


        ui_menu = view.findViewById(R.id.scrip_menu);
        ui_more = view.findViewById(R.id.script_more);
        ui_dir_tip = view.findViewById(R.id.script_dir_tip);
        ui_refresh = view.findViewById(R.id.refresh_layout);
        ui_recycler = view.findViewById(R.id.recycler_view);

        init();
        StatisticsDBHelper.increase(TAG);
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
    public void setMenuClickListener(MenuClickListener mMenuClickListener) {
        this.menuClickListener = mMenuClickListener;
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
            public void onMenu(View view, QLScript script) {
                showPopMenu(view, script);
            }
        });

        ui_refresh.setOnRefreshListener(refreshLayout -> netGetScripts());

        ui_menu.setOnClickListener(v -> menuClickListener.onMenuClick());

        ui_more.setOnClickListener(this::showPopMenu);
    }

    private void initData() {
        if (initDataFlag || NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        ui_refresh.autoRefreshAnimationOnly();
        new Handler().postDelayed(() -> {
            if (isVisible()) {
                netGetScripts();
            }
        }, 1000);

    }

    private void showPopMenu(View v, QLScript script) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(v, Gravity.END);
        popMenuWindow.addItem(new PopMenuItem("copy", "复制路径", R.drawable.ic_gray_crop_free));
        popMenuWindow.addItem(new PopMenuItem("backup", "备份脚本", R.drawable.ic_gray_download));
        if (script.isFile()) {
            popMenuWindow.addItem(new PopMenuItem("delete", "删除脚本", R.drawable.ic_gray_delete));
        }

        popMenuWindow.setOnActionListener(key -> {
            switch (key) {
                case "copy":
                    ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, script.getKey()));
                    ToastUnit.showShort(getString(R.string.tip_copy_path_ready));
                    break;
                case "backup":
                    break;
            }
            return true;
        });

        PopupWindowBuilder.buildMenuWindow(requireActivity(), popMenuWindow);
    }

    private void showPopMenu(View v) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(v, Gravity.END);
        popMenuWindow.addItem(new PopMenuItem("add", "新建脚本", R.drawable.ic_gray_add));
        popMenuWindow.addItem(new PopMenuItem("backup", "备份脚本", R.drawable.ic_gray_upload));

        PopupWindowBuilder.buildMenuWindow(requireActivity(), popMenuWindow);
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
            public void onSuccess(List<QLScript> scripts) {
                sortAndSetData(scripts, "");
                oData = scripts;
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