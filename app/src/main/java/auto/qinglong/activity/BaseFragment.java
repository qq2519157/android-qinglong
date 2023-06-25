package auto.qinglong.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import auto.qinglong.network.http.NetManager;

public abstract class BaseFragment<T extends ViewBinding> extends Fragment {
    public static final String TAG = "BaseFragment";
    protected boolean initDataFlag = false;//数据加载标志

    protected T binding;

    public T getBinding(){
        return binding;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return onCreateView(inflater, container, getTClass());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    private Class<T> getTClass(){
        Class<T> tClass = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return tClass;
    }

    protected View onCreateView(LayoutInflater inflater, ViewGroup container,Class<T> clz){
        try {
            Method inflate = clz.getMethod("inflate", LayoutInflater.class, ViewGroup.class, boolean.class);
            binding = (T) inflate.invoke(clz,inflater, container, false);
            return binding.getRoot();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onStop() {
        super.onStop();
        NetManager.cancelAllCall(getClass().getName());
    }

    public String getNetRequestID() {
        return getClass().getName() + this;
    }


    /**
     * @return 是否需要拦截返回键
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * @return 是否需要拦截点击事件
     */
    public boolean onDispatchTouchEvent() {
        return false;
    }

    protected void init() {
    }

    public void setMenuClickListener(MenuClickListener mMenuClickListener) {
    }

    public interface MenuClickListener {
        void onMenuClick();
    }
}


