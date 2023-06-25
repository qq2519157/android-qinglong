package auto.qinglong.activity.ql.setting;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import auto.qinglong.activity.BaseFragment;
import auto.qinglong.databinding.FragmentSettingBinding;

public class SettingFragment extends BaseFragment<FragmentSettingBinding> {
    public static String TAG = "SettingFragment";

    private MenuClickListener menuClickListener;
    private PagerAdapter mPagerAdapter;

    @Override
    public void init() {
        binding.actionNavBarMenu.setOnClickListener(v -> menuClickListener.onMenuClick());


        mPagerAdapter = new PagerAdapter(requireActivity());//界面适配器
        binding.viewPage.setAdapter(mPagerAdapter);
        binding.viewPage.setUserInputEnabled(false);//禁用用户左右滑动页面
        binding.viewPage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
            }
        });

        //设置界面联动
        new TabLayoutMediator(binding.pageTab, binding.viewPage, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("常规设置");
                    break;
                case 1:
                    tab.setText("登录日志");
                    break;
                case 2:
                    tab.setText("应用设置");
                    break;
            }
        }).attach();
    }

    @Override
    public void setMenuClickListener(MenuClickListener mMenuClickListener) {
        this.menuClickListener = mMenuClickListener;
    }
}