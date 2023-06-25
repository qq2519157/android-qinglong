package auto.qinglong.activity.ql.environment;

import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

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
import auto.qinglong.bean.ql.MoveInfo;
import auto.qinglong.bean.ql.QLEnvironment;
import auto.qinglong.databinding.FragmentEnvBinding;
import auto.qinglong.network.http.ApiController;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.utils.FileUtil;
import auto.qinglong.utils.TextUnit;
import auto.qinglong.utils.TimeUnit;
import auto.qinglong.utils.WebUnit;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.popup.PopEditItem;
import auto.qinglong.views.popup.PopEditWindow;
import auto.qinglong.views.popup.PopListWindow;
import auto.qinglong.views.popup.PopMenuItem;
import auto.qinglong.views.popup.PopMenuWindow;
import auto.qinglong.views.popup.PopProgressWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;

public class EnvFragment extends BaseFragment<FragmentEnvBinding> {
    public static String TAG = "EnvFragment";
    private String mCurrentSearchValue;
    private MenuClickListener mMenuClickListener;
    private EnvItemAdapter mAdapter;
    private PopEditWindow ui_pop_edit;
    private PopProgressWindow ui_pop_progress;
    enum BarType {NAV, SEARCH, MUL_ACTION}


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
        if (binding.envBarSearch.getVisibility() == View.VISIBLE) {
            changeBar(BarType.NAV);
            return true;
        } else if (binding.envBarActions.getVisibility() == View.VISIBLE) {
            changeBar(BarType.NAV);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init() {
        mAdapter = new EnvItemAdapter(requireContext());
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        Objects.requireNonNull(binding.recyclerView.getItemAnimator()).setChangeDuration(0);

        ItemMoveHelper itemMoveHelper = new ItemMoveHelper(mAdapter);
        new ItemTouchHelper(itemMoveHelper).attachToRecyclerView(binding.recyclerView);

        //列表操作接口
        mAdapter.setItemInterface(new EnvItemAdapter.ItemActionListener() {
            @Override
            public void onEdit(QLEnvironment environment) {
                showPopWindowCommonEdit(environment);
            }

            @Override
            public void onMove(MoveInfo info) {
                netMoveEnvironment(info);
            }
        });

        //刷新
        binding.refreshLayout.setOnRefreshListener(refreshLayout -> {
            if (binding.envBarSearch.getVisibility() != View.VISIBLE) {
                mCurrentSearchValue = null;
            }
            netGetEnvironments(mCurrentSearchValue, true);
        });

        //导航栏
        binding.envMenu.setOnClickListener(v -> mMenuClickListener.onMenuClick());

        //更多操作
        binding.envMore.setOnClickListener(this::showPopWindowMenu);

        //搜索栏进入
        binding.envSearch.setOnClickListener(v -> {
            changeBar(BarType.SEARCH);
        });

        //搜索栏返回
        binding.envBarSearchBack.setOnClickListener(v -> changeBar(BarType.NAV));

        //搜索栏确定
        binding.envBarSearchConfirm.setOnClickListener(v -> {
            mCurrentSearchValue = binding.envBarSearchInput.getText().toString().trim();
            WindowUnit.hideKeyboard(binding.envBarSearchInput);
            netGetEnvironments(mCurrentSearchValue, true);
        });

        //操作栏返回
        binding.envBarActionsBack.setOnClickListener(v -> changeBar(BarType.NAV));

        //全选
        binding.envBarActionsSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> mAdapter.setAllChecked(isChecked));

        //删除
        binding.envBarActionsDelete.setOnClickListener(v -> {
            if (NetManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = mAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUtils.showShort(getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.getId());
            }
            netDeleteEnvironments(ids);
        });

        //禁用
        binding.envBarActionsDisable.setOnClickListener(v -> {
            if (NetManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = mAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUtils.showShort(getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.getId());
            }
            netDisableEnvironments(ids);
        });

        //启用
        binding.envBarActionsEnable.setOnClickListener(v -> {
            if (NetManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = mAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUtils.showShort(getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.getId());
            }
            netEnableEnvironments(ids);
        });

    }

    @Override
    public void setMenuClickListener(MenuClickListener menuClickListener) {
        this.mMenuClickListener = menuClickListener;
    }

    @Override
    public boolean onDispatchTouchEvent() {
        if (ui_pop_progress != null && ui_pop_progress.isShowing()) {
            return true;
        }
        return super.onDispatchTouchEvent();
    }

    private void initData() {
        if (initDataFlag || NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        binding.refreshLayout.autoRefreshAnimationOnly();
        new Handler().postDelayed(() -> {
            if (isVisible()) {
                netGetEnvironments(mCurrentSearchValue, true);
            }
        }, 1000);
    }

    private void showPopWindowMenu(View view) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(view, Gravity.END);
        popMenuWindow.addItem(new PopMenuItem("add", "新建变量", R.drawable.ic_gray_add));
        popMenuWindow.addItem(new PopMenuItem("quickAdd", "快捷导入", R.drawable.ic_gray_flash_on));
        popMenuWindow.addItem(new PopMenuItem("localAdd", "本地导入", R.drawable.ic_gray_file));
        popMenuWindow.addItem(new PopMenuItem("remoteAdd", "远程导入", R.drawable.ic_gray_upload));
        popMenuWindow.addItem(new PopMenuItem("backup", "变量备份", R.drawable.ic_gray_download));
        popMenuWindow.addItem(new PopMenuItem("deleteMul", "变量去重", R.drawable.ic_gray_delete));
        popMenuWindow.addItem(new PopMenuItem("mulAction", "批量操作", R.drawable.ic_gray_mul_setting));
        popMenuWindow.setOnActionListener(key -> {
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
        PopupWindowBuilder.buildMenuWindow(requireActivity(), popMenuWindow);
    }

    private void showPopWindowCommonEdit(QLEnvironment environment) {
        ui_pop_edit = new PopEditWindow("新建变量", "取消", "确定");
        PopEditItem itemName = new PopEditItem("name", null, "名称", "请输入变量名称");
        PopEditItem itemValue = new PopEditItem("value", null, "值", "请输入变量值");
        PopEditItem itemRemark = new PopEditItem("remark", null, "备注", "请输入备注(可选)");

        if (environment != null) {
            ui_pop_edit.setTitle("编辑变量");
            itemName.setValue(environment.getName());
            itemValue.setValue(environment.getValue());
            itemRemark.setValue(environment.getRemarks());
        }

        ui_pop_edit.addItem(itemName);
        ui_pop_edit.addItem(itemValue);
        ui_pop_edit.addItem(itemRemark);
        ui_pop_edit.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String name = map.get("name");
                String value = map.get("value");
                String remarks = map.get("remark");

                if (TextUnit.isEmpty(name)) {
                    ToastUtils.showShort("变量名称不能为空");
                    return false;
                }
                if (TextUnit.isEmpty(value)) {
                    ToastUtils.showShort("变量值不能为空");
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
                    newEnv.setId(environment.getId());
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
        ui_pop_edit = new PopEditWindow("快捷导入", "取消", "确定");
        PopEditItem itemValue = new PopEditItem("values", null, "文本", "请输入文本");
        PopEditItem itemRemark = new PopEditItem("remark", null, "备注", "请输入备注(可选)");

        ui_pop_edit.addItem(itemValue);
        ui_pop_edit.addItem(itemRemark);
        ui_pop_edit.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String values = map.get("values");
                String remarks = map.get("remark");

                if (TextUnit.isEmpty(values)) {
                    ToastUtils.showShort("文本不能为空");
                    return false;
                }

                WindowUnit.hideKeyboard(ui_pop_edit.getView());

                List<QLEnvironment> environments = QLEnvironment.parse(values, remarks);
                if (environments.size() == 0) {
                    ToastUtils.showShort("提取变量失败");
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
        ui_pop_edit = new PopEditWindow("远程导入", "取消", "确定");
        PopEditItem itemValue = new PopEditItem("url", null, "链接", "请输入远程地址");
        ui_pop_edit.addItem(itemValue);
        ui_pop_edit.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String url = map.get("url");

                if (WebUnit.isInvalid(url)) {
                    ToastUtils.showShort(getString(R.string.tip_invalid_url));
                    return false;
                }
                WindowUnit.hideKeyboard(ui_pop_edit.getView());
                netGetRemoteEnvironments(url);

                return true;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });

        PopupWindowBuilder.buildEditWindow(requireActivity(), ui_pop_edit);
    }

    private void showPopWindowBackupEdit() {
        ui_pop_edit = new PopEditWindow("变量备份", "取消", "确定");
        PopEditItem itemName = new PopEditItem("file_name", null, "文件名", "选填");

        ui_pop_edit.addItem(itemName);

        ui_pop_edit.setActionListener(new PopEditWindow.OnActionListener() {
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
        if (binding.envBarSearch.getVisibility() == View.VISIBLE) {
            WindowUnit.hideKeyboard(binding.envBarSearchInput);
            binding.envBarSearch.setVisibility(View.INVISIBLE);
        } else if (binding.envBarActions.getVisibility() == View.VISIBLE) {
            binding.envBarActions.setVisibility(View.INVISIBLE);
            mAdapter.setCheckState(false);
            binding.envBarActionsSelectAll.setChecked(false);
        }

        binding.envBarNav.setVisibility(View.INVISIBLE);

        if (barType == BarType.NAV) {
            binding.envBarNav.setVisibility(View.VISIBLE);
        } else if (barType == BarType.SEARCH) {
            binding.envBarSearchInput.setText(mCurrentSearchValue);
            binding.envBarSearch.setVisibility(View.VISIBLE);
        } else {
            binding.envBarActionsSelectAll.setChecked(false);
            mAdapter.setCheckState(true);
            binding.envBarActions.setVisibility(View.VISIBLE);
        }
    }

    private void sortAndSetData(List<QLEnvironment> data) {
        for (int k = 0; k < data.size(); k++) {
            data.get(k).setRealIndex(k);
        }
        if (data.size() != 0) {
            Collections.sort(data);
            //设置同名序号
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
        mAdapter.setData(data);
    }

    private void compareAndDeleteData() {
        List<String> ids = new ArrayList<>();
        Set<String> set = new HashSet<>();
        List<QLEnvironment> qlEnvironments = this.mAdapter.getData();
        for (QLEnvironment qlEnvironment : qlEnvironments) {
            String key = qlEnvironment.getName() + qlEnvironment.getValue();
            if (set.contains(key)) {
                ids.add(qlEnvironment.getId());
            } else {
                set.add(key);
            }
        }
        if (ids.size() == 0) {
            ToastUtils.showShort("无重复变量");
        } else {
            netDeleteEnvironments(ids);
        }
    }

    private void backupData(String fileName) {
        if (FileUtil.isNeedRequestPermission()) {
            ToastUtils.showShort("请授予应用获取存储权限");
            FileUtil.requestPermission(requireActivity());
            return;
        }

        List<QLEnvironment> environments = mAdapter.getData();
        if (environments == null || environments.size() == 0) {
            ToastUtils.showShort("数据为空,无需备份");
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
                ToastUtils.showShort("备份成功：" + fileName);
            } else {
                ToastUtils.showShort("备份失败");
            }
        } catch (Exception e) {
            ToastUtils.showShort("备份失败：" + e.getMessage());
        }

    }

    private void localAddData() {
        if (FileUtil.isNeedRequestPermission()) {
            ToastUtils.showShort("请授予应用读写存储权限");
            FileUtil.requestPermission(requireActivity());
            return;
        }

        List<File> files = FileUtil.getFiles(FileUtil.getEnvPath(), (dir, name) -> name.endsWith(".json"));
        if (files.size() == 0) {
            ToastUtils.showShort("无本地备份数据");
            return;
        }

        PopListWindow<LocalFileAdapter> listWindow = new PopListWindow<>("选择文件");
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
                ToastUtils.showShort("导入失败：" + e.getLocalizedMessage());
            }
        });
    }

    private void netGetEnvironments(String searchValue, boolean needTip) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.getEnvironments(getNetRequestID(), searchValue, new QLApiController.NetGetEnvironmentsCallback() {
            @Override
            public void onSuccess(List<QLEnvironment> environments) {
                initDataFlag = true;
                if (needTip) {
                    ToastUtils.showShort("加载成功：" + environments.size());
                }
                sortAndSetData(environments);
                binding.refreshLayout.finishRefresh(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("加载失败：" + msg);
                binding.refreshLayout.finishRefresh(false);
            }
        });
    }

    private void netUpdateEnvironment(QLEnvironment environment) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.updateEnvironment(getNetRequestID(), environment, new QLApiController.NetEditEnvCallback() {
            @Override
            public void onSuccess(QLEnvironment environment) {
                ui_pop_edit.dismiss();
                ToastUtils.showShort("更新成功");
                netGetEnvironments(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("更新失败：" + msg);
            }
        });
    }

    private void netAddEnvironments(List<QLEnvironment> environments) {
        if (NetManager.isRequesting(getNetRequestID())) {
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
                ToastUtils.showShort("新建成功：" + environments.size());
                netGetEnvironments(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("新建失败：" + msg);
            }
        });
    }

    private void netDeleteEnvironments(List<String> ids) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.deleteEnvironments(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                binding.envBarActionsBack.performClick();
                ToastUtils.showShort("删除成功：" + ids.size());
                netGetEnvironments(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("删除失败：" + msg);
            }
        });
    }

    private void netEnableEnvironments(List<String> ids) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.enableEnvironments(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                binding.envBarActionsBack.performClick();
                ToastUtils.showShort("启用成功");
                netGetEnvironments(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("启用失败：" + msg);
            }
        });

    }

    private void netDisableEnvironments(List<String> ids) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.disableEnvironments(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                binding.envBarActionsBack.performClick();
                ToastUtils.showShort("禁用成功");
                netGetEnvironments(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("禁用失败：" + msg);
            }
        });
    }

    private void netGetRemoteEnvironments(String url) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        ApiController.getRemoteEnvironments(getNetRequestID(), url, new ApiController.NetRemoteEnvCallback() {

            @Override
            public void onSuccess(List<QLEnvironment> environments) {
                if (environments.size() == 0) {
                    ToastUtils.showShort("变量为空");
                } else {
                    netAddEnvironments(environments);
                }
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("加载失败：" + msg);
            }
        });
    }

    private void netMoveEnvironment(MoveInfo info) {
        QLEnvironment fromObject = info.getFromObejct();
        QLEnvironment toObject = info.getToObject();
        int realFrom = info.getFromObejct().getRealIndex();
        int realTo = info.getToObject().getRealIndex();
        QLApiController.moveEnvironment(getNetRequestID(), info.getFromObejct().getId(), realFrom, realTo, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                ToastUtils.showShort(getString(R.string.tip_move_success));
                //交换真实序号
                fromObject.setRealIndex(realTo);
                toObject.setRealIndex(realFrom);
                //同名变量交换同名序号 注：调用notifyItemChanged更新会显示异常
                if (fromObject.getName().equals(toObject.getName())) {
                    int index = fromObject.getIndex();
                    fromObject.setIndex(toObject.getIndex());
                    fromObject.resetFormatName();
                    toObject.setIndex(index);
                    toObject.resetFormatName();
                }
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(getString(R.string.tip_move_failure_header) + msg);
                mAdapter.onItemMove(info.getToIndex(), info.getFromIndex());
            }
        });
    }
}