package auto.qinglong.activity.ql.environment;

import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import auto.qinglong.R;
import auto.qinglong.activity.BaseFragment;
import auto.qinglong.activity.ql.LocalFileAdapter;
import auto.qinglong.bean.ql.QLEnvironment;
import auto.qinglong.network.http.ApiController;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.network.http.RequestManager;
import auto.qinglong.utils.FileUtil;
import auto.qinglong.utils.TextUnit;
import auto.qinglong.utils.TimeUnit;
import auto.qinglong.utils.ToastUnit;
import auto.qinglong.utils.WebUnit;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.popup.EditWindow;
import auto.qinglong.views.popup.EditWindowItem;
import auto.qinglong.views.popup.ListWindow;
import auto.qinglong.views.popup.MiniMoreItem;
import auto.qinglong.views.popup.MiniMoreWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;
import auto.qinglong.views.popup.ProgressWindow;

public class EnvFragment extends BaseFragment {
    public static String TAG = "EnvFragment";
    private String currentSearchValue = "";
    private MenuClickListener menuClickListener;
    private EnvItemAdapter envItemAdapter;

    private LinearLayout ui_root;
    private RelativeLayout ui_bar;
    private LinearLayout ui_bar_nav;
    private ImageView ui_nav_menu;
    private ImageView ui_nav_search;
    private ImageView ui_nav_more;
    private LinearLayout ui_bar_search;
    private ImageView ui_search_back;
    private EditText ui_search_value;
    private ImageView ui_search_confirm;
    private LinearLayout ui_bar_actions;
    private ImageView ui_actions_back;
    private CheckBox ui_actions_select;
    private LinearLayout ui_actions_enable;
    private LinearLayout ui_actions_disable;
    private LinearLayout ui_actions_delete;

    private SmartRefreshLayout ui_refresh;

    private EditWindow ui_pop_edit;
    private ProgressWindow ui_pop_progress;

    enum BarType {NAV, SEARCH, MUL_ACTION}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_env, null);

        ui_root = view.findViewById(R.id.root);
        ui_bar = view.findViewById(R.id.env_top_bar);
        ui_bar_nav = view.findViewById(R.id.env_bar_nav);
        ui_nav_menu = view.findViewById(R.id.env_menu);
        ui_nav_search = view.findViewById(R.id.env_search);
        ui_nav_more = view.findViewById(R.id.env_more);
        ui_bar_search = view.findViewById(R.id.env_bar_search);
        ui_search_back = view.findViewById(R.id.env_bar_search_back);
        ui_search_value = view.findViewById(R.id.env_bar_search_value);
        ui_search_confirm = view.findViewById(R.id.env_bar_search_confirm);
        ui_bar_actions = view.findViewById(R.id.env_bar_actions);
        ui_actions_back = view.findViewById(R.id.env_bar_actions_back);
        ui_actions_select = view.findViewById(R.id.env_bar_actions_select_all);
        ui_actions_enable = view.findViewById(R.id.env_bar_actions_enable);
        ui_actions_disable = view.findViewById(R.id.env_bar_actions_disable);
        ui_actions_delete = view.findViewById(R.id.env_bar_actions_delete);

        ui_refresh = view.findViewById(R.id.refresh_layout);
        RecyclerView ui_recycler = view.findViewById(R.id.recycler_view);

        envItemAdapter = new EnvItemAdapter(requireContext());
        ui_recycler.setAdapter(envItemAdapter);
        ui_recycler.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        Objects.requireNonNull(ui_recycler.getItemAnimator()).setChangeDuration(0);

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
    public boolean onBackPressed() {
        if (ui_bar_search.getVisibility() == View.VISIBLE) {
            changeBar(BarType.NAV);
            return true;
        } else if (ui_bar_actions.getVisibility() == View.VISIBLE) {
            changeBar(BarType.NAV);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init() {
        envItemAdapter.setItemInterface(new EnvItemAdapter.ItemActionListener() {
            @Override
            public void onEdit(QLEnvironment environment, int position) {
                showPopWindowCommonEdit(environment);
            }

            @Override
            public void onMulAction() {
                envItemAdapter.setCheckState(true);
                changeBar(BarType.MUL_ACTION);
            }
        });

        //导航栏
        ui_nav_menu.setOnClickListener(v -> menuClickListener.onMenuClick());

        ui_refresh.setOnRefreshListener(refreshLayout -> netGetEnvironments(currentSearchValue, true));

        //更多操作
        ui_nav_more.setOnClickListener(v -> showPopWindowMiniMore());

        //搜索栏进入
        ui_nav_search.setOnClickListener(v -> {
            ui_search_value.setText(currentSearchValue);
            changeBar(BarType.SEARCH);
        });

        //搜索栏确定
        ui_search_confirm.setOnClickListener(v -> {
            String value = ui_search_value.getText().toString().trim();
            if (!value.isEmpty()) {
                currentSearchValue = value;
                WindowUnit.hideKeyboard(ui_search_value);
                netGetEnvironments(currentSearchValue, true);
            }
        });

        //搜索栏返回
        ui_search_back.setOnClickListener(v -> changeBar(BarType.NAV));

        //动作栏返回
        ui_actions_back.setOnClickListener(v -> changeBar(BarType.NAV));

        //全选
        ui_actions_select.setOnCheckedChangeListener((buttonView, isChecked) -> envItemAdapter.setAllChecked(isChecked));

        //删除
        ui_actions_delete.setOnClickListener(v -> {
            if (RequestManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = envItemAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUnit.showShort(getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.get_id());
            }
            netDeleteEnvironments(ids);
        });

        //禁用
        ui_actions_disable.setOnClickListener(v -> {
            if (RequestManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = envItemAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUnit.showShort(getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.get_id());
            }
            netDisableEnvironments(ids);
        });

        //启用
        ui_actions_enable.setOnClickListener(v -> {
            if (RequestManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = envItemAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUnit.showShort(getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.get_id());
            }
            netEnableEnvironments(ids);
        });

    }

    @Override
    public void setMenuClickListener(MenuClickListener menuClickListener) {
        this.menuClickListener = menuClickListener;
    }

    @Override
    public boolean onDispatchTouchEvent() {
        if (ui_pop_progress != null && ui_pop_progress.isShowing()) {
            return true;
        }
        return super.onDispatchTouchEvent();
    }

    private void initData() {
        if (initDataFlag || RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        ui_refresh.autoRefreshAnimationOnly();
        new Handler().postDelayed(() -> {
            if (isVisible()) {
                netGetEnvironments(currentSearchValue, true);
            }
        }, 1000);
    }

    private void showPopWindowMiniMore() {
        MiniMoreWindow miniMoreWindow = new MiniMoreWindow();
        miniMoreWindow.setTargetView(ui_bar);
        miniMoreWindow.setGravity(Gravity.END);
        miniMoreWindow.addItem(new MiniMoreItem("add", "新建变量", R.drawable.ic_gray_add));
        miniMoreWindow.addItem(new MiniMoreItem("quickAdd", "快捷导入", R.drawable.ic_gray_flash_on));
        miniMoreWindow.addItem(new MiniMoreItem("localAdd", "本地导入", R.drawable.ic_gray_file));
        miniMoreWindow.addItem(new MiniMoreItem("remoteAdd", "远程导入", R.drawable.ic_gray_download));
        miniMoreWindow.addItem(new MiniMoreItem("backup", "变量备份", R.drawable.ic_gray_backup));
        miniMoreWindow.addItem(new MiniMoreItem("deleteMul", "变量去重", R.drawable.ic_gray_delete));
        miniMoreWindow.addItem(new MiniMoreItem("mulAction", "批量操作", R.drawable.ic_gray_mul_setting));
        miniMoreWindow.setOnActionListener(key -> {
            switch (key) {
                case "add":
                    showPopWindowCommonEdit(null);
                    break;
                case "quickAdd":
                    showPopWindowQuickEdit();
                    break;
                case "localAdd":
                    localAddData();
                    break;
                case "remoteAdd":
                    showPopWindowRemoteEdit();
                    break;
                case "deleteMul":
                    compareAndDeleteData();
                    break;
                case "mulAction":
                    changeBar(BarType.MUL_ACTION);
                    break;
                case "backup":
                    showPopWindowBackupEdit();
                    break;
                default:
                    break;
            }
            return true;
        });
        PopupWindowBuilder.buildMiniMoreWindow(requireActivity(), miniMoreWindow);
    }

    private void showPopWindowCommonEdit(QLEnvironment environment) {
        ui_pop_edit = new EditWindow("新建变量", "取消", "确定");
        EditWindowItem itemName = new EditWindowItem("name", null, "名称", "请输入变量名称");
        EditWindowItem itemValue = new EditWindowItem("value", null, "值", "请输入变量值");
        EditWindowItem itemRemark = new EditWindowItem("remark", null, "备注", "请输入备注(可选)");

        if (environment != null) {
            ui_pop_edit.setTitle("编辑变量");
            itemName.setValue(environment.getName());
            itemValue.setValue(environment.getValue());
            itemRemark.setValue(environment.getRemarks());
        }

        ui_pop_edit.addItem(itemName);
        ui_pop_edit.addItem(itemValue);
        ui_pop_edit.addItem(itemRemark);
        ui_pop_edit.setActionListener(new EditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String name = map.get("name");
                String value = map.get("value");
                String remarks = map.get("remark");

                if (TextUnit.isEmpty(name)) {
                    ToastUnit.showShort("变量名称不能为空");
                    return false;
                }
                if (TextUnit.isEmpty(value)) {
                    ToastUnit.showShort("变量值不能为空");
                    return false;
                }

                WindowUnit.hideKeyboard(ui_pop_edit.getView());

                List<QLEnvironment> environments = new ArrayList<>();
                QLEnvironment newEnv;
                newEnv = new QLEnvironment();
                newEnv.setName(name);
                newEnv.setValue(value);
                newEnv.setRemarks(remarks);
                environments.add(newEnv);
                if (environment == null) {
                    netAddEnvironments(environments);
                } else {
                    newEnv.set_id(environment.get_id());
                    netUpdateEnvironment(newEnv);
                }

                return false;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });

        PopupWindowBuilder.buildEditWindow(requireActivity(), ui_pop_edit);
    }

    private void showPopWindowQuickEdit() {
        ui_pop_edit = new EditWindow("快捷导入", "取消", "确定");
        EditWindowItem itemValue = new EditWindowItem("values", null, "文本", "请输入文本");
        EditWindowItem itemRemark = new EditWindowItem("remark", null, "备注", "请输入备注(可选)");

        ui_pop_edit.addItem(itemValue);
        ui_pop_edit.addItem(itemRemark);
        ui_pop_edit.setActionListener(new EditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String values = map.get("values");
                String remarks = map.get("remark");

                if (TextUnit.isEmpty(values)) {
                    ToastUnit.showShort("文本不能为空");
                    return false;
                }

                WindowUnit.hideKeyboard(ui_pop_edit.getView());

                List<QLEnvironment> environments = QLEnvironment.parseExport(values, remarks);
                if (environments.size() == 0) {
                    ToastUnit.showShort("提取变量失败");
                } else {
                    netAddEnvironments(environments);
                }
                return false;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });

        PopupWindowBuilder.buildEditWindow(requireActivity(), ui_pop_edit);
    }

    private void showPopWindowRemoteEdit() {
        ui_pop_edit = new EditWindow("远程导入", "取消", "确定");
        EditWindowItem itemValue = new EditWindowItem("url", null, "链接", "请输入远程地址");
        ui_pop_edit.addItem(itemValue);
        ui_pop_edit.setActionListener(new EditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String url = map.get("url");

                if (WebUnit.isInvalid(url)) {
                    ToastUnit.showShort("地址不合法");
                    return false;
                }

                String baseUrl = WebUnit.getHost(url) + "/";
                String path = WebUnit.getPath(url, "");
                netGetRemoteEnvironments(baseUrl, path);

                return false;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });

        PopupWindowBuilder.buildEditWindow(requireActivity(), ui_pop_edit);
    }

    private void showPopWindowBackupEdit() {
        ui_pop_edit = new EditWindow("变量备份", "取消", "确定");
        EditWindowItem itemName = new EditWindowItem("file_name", null, "文件名", "选填");

        ui_pop_edit.addItem(itemName);

        ui_pop_edit.setActionListener(new EditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String fileName = map.get("file_name");
                WindowUnit.hideKeyboard(ui_pop_edit.getView());
                backupData(fileName);
                return true;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });

        PopupWindowBuilder.buildEditWindow(requireActivity(), ui_pop_edit);
    }

    private void changeBar(BarType barType) {
        if (ui_bar_search.getVisibility() == View.VISIBLE) {
            WindowUnit.hideKeyboard(ui_root);
            ui_bar_search.setVisibility(View.INVISIBLE);
            currentSearchValue = "";
        }

        if (ui_bar_actions.getVisibility() == View.VISIBLE) {
            ui_bar_actions.setVisibility(View.INVISIBLE);
            envItemAdapter.setCheckState(false);
            ui_actions_select.setChecked(false);
        }

        ui_bar_nav.setVisibility(View.INVISIBLE);

        if (barType == BarType.NAV) {
            ui_bar_nav.setVisibility(View.VISIBLE);
        } else if (barType == BarType.SEARCH) {
            ui_bar_search.setVisibility(View.VISIBLE);
        } else {
            ui_actions_select.setChecked(false);
            envItemAdapter.setCheckState(true);
            ui_bar_actions.setVisibility(View.VISIBLE);
        }
    }

    private void sortAndSetData(List<QLEnvironment> data) {
        if (data.size() != 0) {
            Collections.sort(data);
            //设置序号
            int size = data.size();
            int current = 0;
            int index = 1;
            while (true) {
                data.get(current).setIndex(index);
                if (current < size - 1) {
                    if (data.get(current).getName().equals(data.get(current + 1).getName())) {
                        index += 1;
                    } else {
                        index = 1;
                    }
                } else {
                    break;
                }
                current += 1;
            }
        }
        envItemAdapter.setData(data);
    }

    private void compareAndDeleteData() {
        List<String> ids = new ArrayList<>();
        Set<String> set = new HashSet<>();
        List<QLEnvironment> qlEnvironments = this.envItemAdapter.getData();
        for (QLEnvironment qlEnvironment : qlEnvironments) {
            String key = qlEnvironment.getName() + qlEnvironment.getValue();
            if (set.contains(key)) {
                ids.add(qlEnvironment.get_id());
            } else {
                set.add(key);
            }
        }
        if (ids.size() == 0) {
            ToastUnit.showShort("无重复变量");
        } else {
            netDeleteEnvironments(ids);
        }
    }

    private void backupData(String fileName) {
        if (FileUtil.isNeedRequestPermission()) {
            ToastUnit.showShort("请授予应用获取存储权限");
            FileUtil.requestPermission(requireActivity());
            return;
        }

        List<QLEnvironment> environments = envItemAdapter.getData();
        if (environments == null || environments.size() == 0) {
            ToastUnit.showShort("数据为空,无需备份");
            return;
        }

        JsonArray jsonArray = new JsonArray();
        for (QLEnvironment environment : environments) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", environment.getName());
            jsonObject.addProperty("value", environment.getValue());
            jsonObject.addProperty("remarks", environment.getRemarks());
            jsonArray.add(jsonObject);
        }

        if (TextUnit.isFull(fileName)) {
            fileName += ".json";
        } else {
            fileName = TimeUnit.formatCurrentTime() + ".json";
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String content = gson.toJson(jsonArray);

        try {
            boolean result = FileUtil.save(FileUtil.getEnvPath(), fileName, content);
            if (result) {
                ToastUnit.showShort("备份成功：" + fileName);
            } else {
                ToastUnit.showShort("备份失败");
            }
        } catch (Exception e) {
            ToastUnit.showShort("备份失败：" + e.getMessage());
        }

    }

    private void localAddData() {
        if (FileUtil.isNeedRequestPermission()) {
            ToastUnit.showShort("请授予应用读写存储权限");
            FileUtil.requestPermission(requireActivity());
            return;
        }

        List<File> files = FileUtil.getFiles(FileUtil.getEnvPath(), (dir, name) -> name.endsWith(".json"));
        if (files.size() == 0) {
            ToastUnit.showShort("无本地备份数据");
            return;
        }

        ListWindow<LocalFileAdapter> listWindow = new ListWindow<>("选择文件");
        LocalFileAdapter fileAdapter = new LocalFileAdapter(getContext());
        fileAdapter.setData(files);
        listWindow.setAdapter(fileAdapter);

        PopupWindow popupWindow = PopupWindowBuilder.buildListWindow(requireActivity(), listWindow);

        fileAdapter.setListener(file -> {
            try {
                popupWindow.dismiss();
                if (ui_pop_progress == null) {
                    ui_pop_progress = PopupWindowBuilder.buildProgressWindow(requireActivity(), null);
                }
                ui_pop_progress.setTextAndShow("加载文件中...");
                BufferedReader bufferedInputStream = new BufferedReader(new FileReader(file));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedInputStream.readLine()) != null) {
                    stringBuilder.append(line);
                }
                ui_pop_progress.setTextAndShow("解析文件中...");
                Type type = new TypeToken<List<QLEnvironment>>() {
                }.getType();
                List<QLEnvironment> environments = new Gson().fromJson(stringBuilder.toString(), type);
                ui_pop_progress.setTextAndShow("导入变量中...");
                netAddEnvironments(environments);
            } catch (Exception e) {
                ToastUnit.showShort("导入失败：" + e.getLocalizedMessage());
            }
        });
    }

    private void netGetEnvironments(String searchValue, boolean needTip) {
        if (RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.getEnvironments(getNetRequestID(), searchValue, new QLApiController.NetGetEnvironmentsCallback() {
            @Override
            public void onSuccess(List<QLEnvironment> environments) {
                initDataFlag = true;
                if (needTip) {
                    ToastUnit.showShort("加载成功：" + environments.size());
                }
                sortAndSetData(environments);
                ui_refresh.finishRefresh(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("加载失败：" + msg);
                ui_refresh.finishRefresh(false);
            }
        });
    }

    private void netUpdateEnvironment(QLEnvironment environment) {
        if (RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.updateEnvironment(getNetRequestID(), environment, new QLApiController.NetEditEnvCallback() {
            @Override
            public void onSuccess(QLEnvironment environment) {
                ui_pop_edit.dismiss();
                ToastUnit.showShort("更新成功");
                netGetEnvironments(currentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("更新失败：" + msg);
            }
        });
    }

    private void netAddEnvironments(List<QLEnvironment> environments) {
        if (RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.addEnvironment(getNetRequestID(), environments, new QLApiController.NetGetEnvironmentsCallback() {
            @Override
            public void onSuccess(List<QLEnvironment> qlEnvironments) {
                if (ui_pop_edit != null) {
                    ui_pop_edit.dismiss();
                }
                if (ui_pop_progress != null) {
                    ui_pop_progress.dismiss();
                }
                ToastUnit.showShort("新建成功：" + environments.size());
                netGetEnvironments(currentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("新建失败：" + msg);
            }
        });
    }

    private void netDeleteEnvironments(List<String> ids) {
        if (RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.deleteEnvironments(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                ui_actions_back.performClick();
                ToastUnit.showShort("删除成功：" + ids.size());
                netGetEnvironments(currentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("删除失败：" + msg);
            }
        });
    }

    private void netEnableEnvironments(List<String> ids) {
        if (RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.enableEnvironments(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                ui_actions_back.performClick();
                ToastUnit.showShort("启用成功");
                netGetEnvironments(currentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("启用失败：" + msg);
            }
        });

    }

    private void netDisableEnvironments(List<String> ids) {
        if (RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.disableEnvironments(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                ui_actions_back.performClick();
                ToastUnit.showShort("禁用成功");
                netGetEnvironments(currentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("禁用失败：" + msg);
            }
        });
    }

    private void netGetRemoteEnvironments(String baseUrl, String path) {
        if (RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        ApiController.getRemoteEnvironments(getNetRequestID(), baseUrl, path, new ApiController.NetRemoteEnvCallback() {

            @Override
            public void onSuccess(List<QLEnvironment> environments) {
                if (environments.size() == 0) {
                    ToastUnit.showShort("变量为空");
                } else {
                    netAddEnvironments(environments);
                }
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("加载失败：" + msg);
            }
        });
    }

}