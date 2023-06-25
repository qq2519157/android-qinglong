package auto.qinglong.activity.ql.dependence;

import android.view.Gravity;
import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import auto.qinglong.R;
import auto.qinglong.activity.BaseFragment;
import auto.qinglong.bean.ql.QLDependence;
import auto.qinglong.databinding.FragmentDepBinding;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.utils.TextUnit;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.popup.PopEditItem;
import auto.qinglong.views.popup.PopEditWindow;
import auto.qinglong.views.popup.PopMenuItem;
import auto.qinglong.views.popup.PopMenuWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;


public class DepPagerFragment extends BaseFragment<FragmentDepBinding> {
    public static String TAG = "DepFragment";
    private final String TYPE_NODEJS = "nodejs";
    private final String TYPE_PYTHON = "python3";
    private final String TYPE_LINUX = "linux";

    private DepFragment mCurrentFragment;
    private PagerAdapter mPagerAdapter;
    private MenuClickListener mMenuClickListener;

    private PopEditWindow ui_pop_edit;

    enum BarType {NAV, ACTION}


    @Override
    public void init() {
        //导航栏回调
        binding.depNavBarMenu.setOnClickListener(v -> mMenuClickListener.onMenuClick());

        //弹窗-更多
        binding.depNavBarMore.setOnClickListener(this::showPopWindowMenu);

        //操作栏-返回
        binding.depActionBarBack.setOnClickListener(v -> showBar(BarType.NAV));

        //操作栏-全选
        binding.depActionBarSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> mCurrentFragment.setAllItemCheck(isChecked));

        //操作栏-删除
        binding.depActionBarDelete.setOnClickListener(v -> {
            List<String> ids = mCurrentFragment.getCheckedItemIds();
            if (ids != null && ids.size() > 0) {
                netDeleteDependence(ids);
            } else {
                ToastUtils.showShort(getString(R.string.tip_empty_select));
            }
        });

        mPagerAdapter = new PagerAdapter(requireActivity());//界面适配器
        mPagerAdapter.setPagerActionListener(() -> {
            showBar(BarType.ACTION);//进入操作栏
        });

        binding.viewPage.setAdapter(mPagerAdapter);
        binding.viewPage.setUserInputEnabled(false);//禁用用户左右滑动页面
        binding.viewPage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                //如果处于操作栏则切换至导航栏
                if (binding.depActionBar.getVisibility() == View.VISIBLE) {
                    showBar(BarType.NAV);
                    mCurrentFragment.setCheckState(false);
                }
                mCurrentFragment = mPagerAdapter.getCurrentFragment(position);
            }
        });

        //设置界面联动
        new TabLayoutMediator(binding.pageTab, binding.viewPage, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("NodeJs");
                    break;
                case 1:
                    tab.setText("Python3");
                    break;
                case 2:
                    tab.setText("Linux");
                    break;
            }
        }).attach();
    }

    @Override
    public void setMenuClickListener(MenuClickListener mMenuClickListener) {
        this.mMenuClickListener = mMenuClickListener;
    }

    @Override
    public boolean onBackPressed() {
        if (binding.depActionBar.getVisibility() == View.VISIBLE) {
            binding.depActionBarBack.performClick();
            return true;
        } else {
            return false;
        }
    }

    private void showPopWindowEdit() {
        ui_pop_edit = new PopEditWindow("新建依赖", "取消", "确定");
        ui_pop_edit.setMaxHeight(WindowUnit.getWindowHeightPix(requireContext()) / 3);
        String type = mPagerAdapter.getCurrentFragment(binding.viewPage.getCurrentItem()).getType();
        ui_pop_edit.addItem(new PopEditItem("type", type, "类型", null, false, false));
        ui_pop_edit.addItem(new PopEditItem("name", null, "名称", "请输入依赖名称"));
        ui_pop_edit.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String type = map.get("type");
                String name = map.get("name");

                if (TextUnit.isEmpty(name)) {
                    ToastUtils.showShort(getString(R.string.tip_empty_dependence_name));
                    return false;
                }

                List<QLDependence> dependencies = new ArrayList<>();
                QLDependence dependence = new QLDependence();
                dependence.setName(name);
                if (TYPE_NODEJS.equals(type)) {
                    dependence.setType(0);
                } else if (TYPE_PYTHON.equals(type)) {
                    dependence.setType(1);
                } else {
                    dependence.setType(2);
                }
                dependencies.add(dependence);

                netAddDependence(dependencies);
                return false;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });
        PopupWindowBuilder.buildEditWindow(requireActivity(), ui_pop_edit);
    }

    private void showPopWindowMenu(View view) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(view, Gravity.END);
        popMenuWindow.addItem(new PopMenuItem("add", "新建依赖", R.drawable.ic_gray_add));
        popMenuWindow.addItem(new PopMenuItem("mulAction", "批量操作", R.drawable.ic_gray_mul_setting));
        popMenuWindow.setOnActionListener(key -> {
            if (key.equals("add")) {
                showPopWindowEdit();
            } else {
                showBar(BarType.ACTION);
            }
            return true;
        });

        PopupWindowBuilder.buildMenuWindow(requireActivity(), popMenuWindow);
    }

    private void showBar(BarType barType) {
        if (barType == BarType.NAV) {
            binding.depActionBar.setVisibility(View.INVISIBLE);
            mCurrentFragment.setCheckState(false);
            binding.depActionBarSelectAll.setChecked(false);
            binding.depNavBar.setVisibility(View.VISIBLE);
        } else {
            binding.depNavBar.setVisibility(View.INVISIBLE);
            binding.depActionBarSelectAll.setChecked(false);
            mCurrentFragment.setCheckState(true);
            binding.depActionBar.setVisibility(View.VISIBLE);
        }
    }

    private void netAddDependence(List<QLDependence> dependencies) {
        QLApiController.addDependencies(getNetRequestID(), dependencies, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                ui_pop_edit.dismiss();
                mPagerAdapter.getCurrentFragment(binding.viewPage.getCurrentItem()).refreshData();
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(msg);
            }
        });

    }

    private void netDeleteDependence(List<String> ids) {
        QLApiController.deleteDependencies(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                showBar(BarType.NAV);
                mPagerAdapter.getCurrentFragment(binding.viewPage.getCurrentItem()).refreshData();
            }

            @Override
            public void onFailure(String msg) {
                ToastUtils.showShort(msg);
            }
        });
    }


}