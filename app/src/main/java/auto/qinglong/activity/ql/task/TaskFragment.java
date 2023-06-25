package auto.qinglong.activity.ql.task;

import android.content.Intent;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;

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
import auto.qinglong.activity.ql.CodeWebActivity;
import auto.qinglong.activity.ql.LocalFileAdapter;
import auto.qinglong.bean.ql.QLTask;
import auto.qinglong.databinding.FragmentTaskBinding;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.utils.CronUnit;
import auto.qinglong.utils.FileUtil;
import auto.qinglong.utils.LogUnit;
import auto.qinglong.utils.TextUnit;
import auto.qinglong.utils.TimeUnit;
import auto.qinglong.utils.VibratorUtil;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.popup.PopEditItem;
import auto.qinglong.views.popup.PopEditWindow;
import auto.qinglong.views.popup.PopListWindow;
import auto.qinglong.views.popup.PopMenuItem;
import auto.qinglong.views.popup.PopMenuWindow;
import auto.qinglong.views.popup.PopProgressWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;

public class TaskFragment extends BaseFragment<FragmentTaskBinding> {
    public static String TAG = "TaskFragment";

    private String mCurrentSearchValue;
    private MenuClickListener mMenuClickListener;
    private TaskAdapter mAdapter;
    private PopEditWindow ui_pop_edit;
    private PopProgressWindow ui_pop_progress;

    private enum BarType {NAV, SEARCH, MUL_ACTION}

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
        if (binding.taskBarActions.getVisibility() == View.VISIBLE) {
            changeBar(BarType.NAV);
            return true;
        } else if (binding.taskBarSearch.getVisibility() == View.VISIBLE) {
            changeBar(BarType.NAV);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init() {
        mAdapter = new TaskAdapter(requireContext());
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        Objects.requireNonNull(binding.recyclerView.getItemAnimator()).setChangeDuration(0);//取消更新动画，避免刷新闪烁

        //列表操作接口
        mAdapter.setTaskInterface(new TaskAdapter.ItemActionListener() {
            @Override
            public void onLog(QLTask task) {
                Intent intent = new Intent(getContext(), CodeWebActivity.class);
                intent.putExtra(CodeWebActivity.EXTRA_TYPE, CodeWebActivity.TYPE_LOG);
                intent.putExtra(CodeWebActivity.EXTRA_TITLE, task.getName());
                intent.putExtra(CodeWebActivity.EXTRA_LOG_PATH, task.getLastLogPath());
                startActivity(intent);
            }

            @Override
            public void onStop(QLTask task) {
                if (NetManager.isRequesting(getNetRequestID())) {
                    return;
                }
                List<String> ids = new ArrayList<>();
                ids.add(task.getId());
                netStopTasks(ids, false);
            }

            @Override
            public void onRun(QLTask task) {
                List<String> ids = new ArrayList<>();
                ids.add(task.getId());
                netRunTasks(ids, false);
            }

            @Override
            public void onEdit(QLTask task) {
                showPopWindowEdit(task);
            }

            @Override
            public void onScript(String parent, String fileName) {
                VibratorUtil.vibrate(requireContext(), VibratorUtil.VIBRATE_SHORT);
                Intent intent = new Intent(getContext(), CodeWebActivity.class);
                intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_NAME, fileName);
                intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_PARENT, parent);
                intent.putExtra(CodeWebActivity.EXTRA_TITLE, fileName);
                intent.putExtra(CodeWebActivity.EXTRA_TYPE, CodeWebActivity.TYPE_SCRIPT);
                intent.putExtra(CodeWebActivity.EXTRA_CAN_EDIT, true);
                startActivity(intent);
            }
        });

        //刷新
        binding.refreshLayout.setOnRefreshListener(refreshLayout -> {
            if (binding.taskBarSearch.getVisibility() != View.VISIBLE) {
                mCurrentSearchValue = null;
            }
            netGetTasks(mCurrentSearchValue, true);
        });

        //唤起导航栏
        binding.taskBarNavMenu.setOnClickListener(v -> {
            if (mMenuClickListener != null) {
                mMenuClickListener.onMenuClick();
            }
        });

        //更多操作
        binding.taskBarNavMore.setOnClickListener(this::showPopWindowMenu);

        //搜索栏进入
        binding.taskBarNavSearch.setOnClickListener(v -> changeBar(BarType.SEARCH));

        //搜索栏返回
        binding.taskBarSearchBack.setOnClickListener(v -> changeBar(BarType.NAV));

        //搜索栏确定
        binding.taskBarSearchConfirm.setOnClickListener(v -> {
            mCurrentSearchValue = binding.taskBarSearchInput.getText().toString();
            WindowUnit.hideKeyboard(binding.taskBarSearchInput);
            netGetTasks(mCurrentSearchValue, true);
        });

        //操作栏返回
        binding.taskBarActionsBack.setOnClickListener(v -> changeBar(BarType.NAV));

        //全选
        binding.taskBarActionsSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mAdapter.getCheckState()) {
                mAdapter.selectAll(isChecked);
            }
        });

        //执行
        binding.taskBarActionsRun.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUtils.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netRunTasks(ids, true);
                }
            }
        });

        //停止
        binding.taskBarActionsStop.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUtils.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netStopTasks(ids, true);
                }
            }
        });

        //顶置
        binding.taskBarActionsPinned.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUtils.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netPinTasks(ids);
                }
            }
        });

        //取消顶置
        binding.taskBarActionsUnpinned.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUtils.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netUnpinTasks(ids);
                }
            }
        });

        //启用
        binding.taskBarActionsEnable.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUtils.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netEnableTasks(ids);
                }
            }
        });

        //禁用
        binding.taskBarActionsDisable.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUtils.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netDisableTasks(ids);
                }
            }
        });

        //删除
        binding.taskBarActionsDelete.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUtils.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netDeleteTasks(ids);
                }
            }
        });
    }

    @Override
    public void setMenuClickListener(MenuClickListener mMenuClickListener) {
        this.mMenuClickListener = mMenuClickListener;
    }

    private void initData() {
        if (initDataFlag || NetManager.isRequesting(this.getNetRequestID())) {
            return;
        }
        binding.refreshLayout.autoRefreshAnimationOnly();
        new Handler().postDelayed(() -> {
            if (isVisible()) {
                netGetTasks(mCurrentSearchValue, true);
            }
        }, 1000);
    }

    private void showPopWindowMenu(View view) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(view, Gravity.END);
        popMenuWindow.addItem(new PopMenuItem("add", "新建任务", R.drawable.ic_gray_add));
        popMenuWindow.addItem(new PopMenuItem("localAdd", "本地导入", R.drawable.ic_gray_file));
        popMenuWindow.addItem(new PopMenuItem("backup", "任务备份", R.drawable.ic_gray_download));
        popMenuWindow.addItem(new PopMenuItem("deleteMul", "任务去重", R.drawable.ic_gray_delete));
        popMenuWindow.addItem(new PopMenuItem("mulAction", "批量操作", R.drawable.ic_gray_mul_setting));
        popMenuWindow.setOnActionListener(key -> {
            switch (key) {
                case "add":
                    showPopWindowEdit(null);
                    break;
                case "localAdd":
                    localAddData();
                    break;
                case "backup":
                    showPopWindowBackupEdit();
                    break;
                case "deleteMul":
                    compareAndDeleteData();
                    break;
                default:
                    changeBar(BarType.MUL_ACTION);
            }
            return true;
        });
        PopupWindowBuilder.buildMenuWindow(requireActivity(), popMenuWindow);
    }

    private void showPopWindowEdit(QLTask qlTask) {
        ui_pop_edit = new PopEditWindow("新建任务", "取消", "确定");
        PopEditItem itemName = new PopEditItem("name", null, "名称", "请输入任务名称");
        PopEditItem itemCommand = new PopEditItem("command", null, "命令", "请输入要执行的命令");
        PopEditItem itemSchedule = new PopEditItem("schedule", null, "定时规则", "秒(可选) 分 时 天 月 周");

        if (qlTask != null) {
            ui_pop_edit.setTitle("编辑任务");
            itemName.setValue(qlTask.getName());
            itemCommand.setValue(qlTask.getCommand());
            itemSchedule.setValue(qlTask.getSchedule());
        }

        ui_pop_edit.addItem(itemName);
        ui_pop_edit.addItem(itemCommand);
        ui_pop_edit.addItem(itemSchedule);
        ui_pop_edit.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String name = map.get("name");
                String command = map.get("command");
                String schedule = map.get("schedule");

                if (TextUnit.isEmpty(name)) {
                    ToastUtils.showShort(getString(R.string.tip_empty_task_name));
                    return false;
                }
                if (TextUnit.isEmpty(command)) {
                    ToastUtils.showShort(getString(R.string.tip_empty_task_command));
                    return false;
                }
                if (!CronUnit.isValid(schedule)) {
                    ToastUtils.showShort(getString(R.string.tip_invalid_task_schedule));
                    return false;
                }

                WindowUnit.hideKeyboard(ui_pop_edit.getView());

                QLTask newQLTask = new QLTask();
                if (qlTask == null) {
                    newQLTask.setName(name);
                    newQLTask.setCommand(command);
                    newQLTask.setSchedule(schedule);
                    netAddTask(newQLTask);
                } else {
                    newQLTask.setName(name);
                    newQLTask.setCommand(command);
                    newQLTask.setSchedule(schedule);
                    newQLTask.setId(qlTask.getId());
                    netEditTask(newQLTask);
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

    private void showPopWindowBackupEdit() {
        ui_pop_edit = new PopEditWindow("任务备份", "取消", "确定");
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
        if (binding.taskBarSearch.getVisibility() == View.VISIBLE) {
            WindowUnit.hideKeyboard(binding.root);
            binding.taskBarSearch.setVisibility(View.INVISIBLE);
        } else if (binding.taskBarActions.getVisibility() == View.VISIBLE) {
            binding.taskBarActions.setVisibility(View.INVISIBLE);
            mAdapter.setCheckState(false);
            binding.taskBarActionsSelectAll.setChecked(false);
        }

        binding.taskBarNav.setVisibility(View.INVISIBLE);

        if (barType == BarType.NAV) {
            binding.taskBarNav.setVisibility(View.VISIBLE);
        } else if (barType == BarType.SEARCH) {
            binding.taskBarSearchInput.setText(mCurrentSearchValue);
            binding.taskBarSearch.setVisibility(View.VISIBLE);
        } else {
            binding.taskBarActionsScroll.scrollTo(0, 0);
            mAdapter.setCheckState(true);
            binding.taskBarActions.setVisibility(View.VISIBLE);
        }
    }

    private void compareAndDeleteData() {
        List<String> ids = new ArrayList<>();
        Set<String> set = new HashSet<>();
        List<QLTask> tasks = this.mAdapter.getData();
        for (QLTask task : tasks) {
            String key = task.getCommand();
            if (set.contains(key)) {
                ids.add(task.getId());
            } else {
                set.add(key);
            }
        }
        if (ids.size() == 0) {
            ToastUtils.showShort("无重复任务");
        } else {
            netDeleteTasks(ids);
        }
    }

    private void localAddData() {
        if (FileUtil.isNeedRequestPermission()) {
            ToastUtils.showShort("请授予应用读写存储权限");
            FileUtil.requestPermission(requireActivity());
            return;
        }

        List<File> files = FileUtil.getFiles(FileUtil.getTaskPath(), (dir, name) -> name.endsWith(".json"));
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
                Type type = new TypeToken<List<QLTask>>() {
                }.getType();
                List<QLTask> tasks = new Gson().fromJson(stringBuilder.toString(), type);

                netMulAddTask(tasks);
            } catch (Exception e) {
                ToastUtils.showShort("导入失败：" + e.getLocalizedMessage());
            }
        });
    }

    private void backupData(String fileName) {
        if (FileUtil.isNeedRequestPermission()) {
            ToastUtils.showShort("请授予应用读写存储权限");
            FileUtil.requestPermission(requireActivity());
            return;
        }

        List<QLTask> tasks = mAdapter.getData();
        if (tasks == null || tasks.size() == 0) {
            ToastUtils.showShort("数据为空,无需备份");
            return;
        }

        JsonArray jsonArray = new JsonArray();
        for (QLTask task : tasks) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", task.getName());
            jsonObject.addProperty("command", task.getCommand());
            jsonObject.addProperty("schedule", task.getSchedule());
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
            boolean result = FileUtil.save(FileUtil.getTaskPath(), fileName, content);
            if (result) {
                ToastUtils.showShort("备份成功：" + fileName);
            } else {
                ToastUtils.showShort("备份失败");
            }
        } catch (Exception e) {
            ToastUtils.showShort("备份失败：" + e.getMessage());
        }

    }

    private void netGetTasks(String searchValue, boolean needTip) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.getTasks(getNetRequestID(), searchValue, new QLApiController.NetGetTasksCallback() {
            @Override
            public void onSuccess(List<QLTask> tasks) {
                initDataFlag = true;
                Collections.sort(tasks);
                for (int k = 0; k < tasks.size(); k++) {
                    tasks.get(k).setIndex(k + 1);
                }
                mAdapter.setData(tasks);
                if (needTip) {
                    ToastUtils.showShort("加载成功：" + tasks.size());
                }
                binding.refreshLayout.finishRefresh(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("加载失败：" + msg);
                binding.refreshLayout.finishRefresh(false);
            }
        });
    }

    private void netRunTasks(List<String> ids, boolean isFromBar) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.runTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (isFromBar && binding.taskBarActions.getVisibility() == View.VISIBLE) {
                    binding.taskBarActionsBack.performClick();
                }
                ToastUtils.showShort("执行成功");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("执行失败：" + msg);
            }
        });

    }

    private void netStopTasks(List<String> ids, boolean isFromBar) {
        QLApiController.stopTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (isFromBar && binding.taskBarActions.getVisibility() == View.VISIBLE) {
                    binding.taskBarActionsBack.performClick();
                }
                ToastUtils.showShort("终止成功");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("终止失败：" + msg);
            }
        });
    }

    private void netEnableTasks(List<String> ids) {
        QLApiController.enableTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (binding.taskBarActionsBack.getVisibility() == View.VISIBLE) {
                    binding.taskBarActionsBack.performClick();
                }
                ToastUtils.showShort("启用成功");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("启用失败：" + msg);
            }
        });
    }

    private void netDisableTasks(List<String> ids) {
        QLApiController.disableTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (binding.taskBarActionsBack.getVisibility() == View.VISIBLE) {
                    binding.taskBarActionsBack.performClick();
                }
                ToastUtils.showShort("禁用成功");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("禁用失败：" + msg);
            }
        });
    }

    private void netPinTasks(List<String> ids) {
        QLApiController.pinTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (binding.taskBarActionsBack.getVisibility() == View.VISIBLE) {
                    binding.taskBarActionsBack.performClick();
                }
                ToastUtils.showShort("顶置成功");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(getString(R.string.tip_pin_failure) + msg);
            }
        });
    }

    private void netUnpinTasks(List<String> ids) {
        QLApiController.unpinTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (binding.taskBarActionsBack.getVisibility() == View.VISIBLE) {
                    binding.taskBarActionsBack.performClick();
                }
                ToastUtils.showShort(getString(R.string.tip_unpin_success));
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(getString(R.string.tip_unpin_failure) + msg);
            }
        });
    }

    private void netDeleteTasks(List<String> ids) {
        QLApiController.deleteTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (binding.taskBarActionsBack.getVisibility() == View.VISIBLE) {
                    binding.taskBarActionsBack.performClick();
                }
                ToastUtils.showShort("删除成功：" + ids.size());
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("删除失败：" + msg);
            }
        });
    }

    private void netEditTask(QLTask task) {
        QLApiController.editTask(getNetRequestID(), task, new QLApiController.NetEditTaskCallback() {
            @Override
            public void onSuccess(QLTask QLTask) {
                ui_pop_edit.dismiss();
                ToastUtils.showShort("编辑成功");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("编辑失败：" + msg);
            }
        });
    }

    private void netAddTask(QLTask task) {
        QLApiController.addTask(getNetRequestID(), task, new QLApiController.NetEditTaskCallback() {
            @Override
            public void onSuccess(QLTask QLTask) {
                ui_pop_edit.dismiss();
                ToastUtils.showShort("新建任务成功");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort("新建任务失败：" + msg);
            }
        });
    }

    private void netMulAddTask(List<QLTask> tasks) {
        new Thread(() -> {
            final boolean[] isEnd = {false};

            for (int k = 0; k < tasks.size(); k++) {
                ui_pop_progress.setText("导入任务中 " + k + "/" + tasks.size());
                QLApiController.addTask(getNetRequestID(), tasks.get(k), new QLApiController.NetEditTaskCallback() {
                    @Override
                    public void onSuccess(QLTask QLTask) {
                        isEnd[0] = true;
                    }

                    @Override
                    public void onFailure(String msg) {
                        isEnd[0] = true;
                        LogUnit.log(TAG, msg);
                    }
                });
                while (!isEnd[0]) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            ui_pop_progress.dismiss();
            netGetTasks(mCurrentSearchValue, true);
        }).start();
    }


}