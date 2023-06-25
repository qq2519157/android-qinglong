package auto.qinglong.views.popup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import auto.qinglong.R;
import auto.qinglong.databinding.PopItemEditBinding;
import auto.qinglong.databinding.PopItemMenuBinding;
import auto.qinglong.databinding.PopWindowConfirmBinding;
import auto.qinglong.databinding.PopWindowEditBinding;
import auto.qinglong.databinding.PopWindowListBinding;
import auto.qinglong.databinding.PopWindowLoadingBinding;
import auto.qinglong.databinding.PopWindowMenuBinding;
import auto.qinglong.utils.WindowUnit;

public class PopupWindowBuilder {
    public static final String TAG = "PopupWindowManager";

    @SuppressLint("UseCompatLoadingForDrawables")
    public static void buildMenuWindow(Activity activity, PopMenuWindow popMenuWindow) {
        PopWindowMenuBinding binding = PopWindowMenuBinding.inflate(activity.getLayoutInflater(), null, false);
        PopupWindow popWindow = build(activity.getBaseContext(), true);
        popWindow.setContentView(binding.getRoot());
        for (PopMenuItem item : popMenuWindow.getItems()) {
            PopItemMenuBinding itemMenuBinding = PopItemMenuBinding.inflate(activity.getLayoutInflater(), null, false);
            itemMenuBinding.popCommonMiniMoreIcon.setImageDrawable(activity.getDrawable(item.getIcon()));
            itemMenuBinding.popCommonMiniMoreName.setText(item.getName());
            if (popMenuWindow.getOnActionListener() != null) {
                itemMenuBinding.getRoot().setOnClickListener(v -> {
                    if (popMenuWindow.getOnActionListener().onClick(item.getKey())) {
                        popWindow.dismiss();
                    }
                });
            }
            binding.popCommonLlContainer.addView(itemMenuBinding.getRoot());
        }

        popWindow.setOnDismissListener(() -> {
            popMenuWindow.setOnActionListener(null);
            popWindow.setOnDismissListener(null);
        });

        popWindow.showAsDropDown(popMenuWindow.getTargetView(), popMenuWindow.getGravity(), 0, 0);
    }

    public static void buildEditWindow(@NonNull Activity activity, PopEditWindow popEditWindow) {
        PopWindowEditBinding binding = PopWindowEditBinding.inflate(activity.getLayoutInflater(), null, false);
        PopupWindow popWindow = build(activity.getBaseContext(), true);
        popWindow.setContentView(binding.getRoot());
        popEditWindow.setView(binding.getRoot());
        popEditWindow.setPopupWindow(popWindow);
        binding.popCommonSlContainer.setMaxHeight(popEditWindow.getMaxHeight());
        binding.popCommonTvTitle.setText(popEditWindow.getTitle());
        binding.popCommonBtCancel.setText(popEditWindow.getCancelTip());
        binding.popCommonBtConfirm.setText(popEditWindow.getConfirmTip());

        //添加item
        List<EditText> itemViews = new ArrayList<>();
        List<PopEditItem> items = popEditWindow.getItems();
        for (PopEditItem item : items) {
            PopItemEditBinding itemEditBinding = PopItemEditBinding.inflate(activity.getLayoutInflater(), null, false);
            itemEditBinding.popCommonTvLabel.setText(item.getLabel());
            itemEditBinding.popCommonEtValue.setHint(item.getHint());
            itemEditBinding.popCommonEtValue.setText(item.getValue());
            itemEditBinding.popCommonEtValue.setFocusable(item.isFocusable());
            itemEditBinding.popCommonEtValue.setEnabled(item.isEditable());
            itemViews.add(itemEditBinding.popCommonEtValue);
            binding.popCommonLlContainer.addView(itemEditBinding.getRoot());
        }

        if (popEditWindow.getActionListener() != null) {
            binding.popCommonBtCancel.setOnClickListener(v -> {
                boolean flag = popEditWindow.getActionListener().onCancel();
                if (flag) {
                    popWindow.dismiss();
                }
            });

            binding.popCommonBtConfirm.setOnClickListener(v -> {
                Map<String, String> map = new HashMap<>();
                for (int k = 0; k < itemViews.size(); k++) {
                    map.put(items.get(k).getKey(), itemViews.get(k).getText().toString().trim());
                }
                if (popEditWindow.getActionListener().onConfirm(map)) {
                    popWindow.dismiss();
                }
            });
        }

        popWindow.setOnDismissListener(() -> {
            WindowUnit.setBackgroundAlpha(activity, 1.0f);
            itemViews.clear();
            items.clear();
            popEditWindow.setActionListener(null);
            popWindow.setOnDismissListener(null);
        });

        WindowUnit.setBackgroundAlpha(activity, 0.5f);
        popWindow.showAtLocation(activity.getWindow().getDecorView().getRootView(), Gravity.CENTER, 0, 0);
    }

    public static PopupWindow buildListWindow(Activity activity, PopListWindow listWindow) {
        PopWindowListBinding binding = PopWindowListBinding.inflate(activity.getLayoutInflater(), null, false);
        PopupWindow popWindow = build(activity.getBaseContext(), true);
        popWindow.setContentView(binding.getRoot());
        binding.popCommonTvTitle.setText(listWindow.getTitle());
        binding.popCommonBtCancel.setText(listWindow.getCancelTip());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.VERTICAL, false));
        binding.recyclerView.setAdapter((RecyclerView.Adapter) listWindow.getAdapter());

        binding.popCommonBtCancel.setOnClickListener(v -> {
            if (listWindow.getListener() == null || listWindow.getListener().onCancel()) {
                popWindow.dismiss();
            }
        });

        popWindow.setOnDismissListener(() -> {
            WindowUnit.setBackgroundAlpha(activity, 1.0f);
            binding.recyclerView.setAdapter(null);
            listWindow.setListener(null);
            popWindow.setOnDismissListener(null);
        });

        WindowUnit.setBackgroundAlpha(activity, 0.5f);
        popWindow.showAtLocation(activity.getWindow().getDecorView().getRootView(), Gravity.CENTER, 0, 0);

        return popWindow;
    }

    public static PopupWindow buildConfirmWindow(Activity activity, PopConfirmWindow popConfirmWindow) {
        PopWindowConfirmBinding binding = PopWindowConfirmBinding.inflate(activity.getLayoutInflater(), null, false);
        PopupWindow popWindow = build(activity.getBaseContext(), popConfirmWindow.isFocusable());
        popWindow.setContentView(binding.getRoot());
        binding.popCommonSlContainer.setMaxHeight(popConfirmWindow.getMaxHeight());
        binding.popCommonTvTitle.setText(popConfirmWindow.getTitle());
        binding.popCommonTvContent.setText(popConfirmWindow.getContent());
        binding.popCommonBtConfirm.setText(popConfirmWindow.getConfirmTip());
        binding.popCommonBtCancel.setText(popConfirmWindow.getCancelTip());
        if (popConfirmWindow.getOnActionListener() != null) {
            binding.popCommonBtCancel.setOnClickListener(v -> {
                if (popConfirmWindow.getOnActionListener().onConfirm(false)) {
                    popWindow.dismiss();
                }
            });

            binding.popCommonBtConfirm.setOnClickListener(v -> {
                if (popConfirmWindow.getOnActionListener().onConfirm(true)) {
                    popWindow.dismiss();
                }
            });
        }

        popWindow.setOnDismissListener(() -> {
            WindowUnit.setBackgroundAlpha(activity, 1.0f);
            popConfirmWindow.setOnActionListener(null);
            popWindow.setOnDismissListener(null);
        });

        WindowUnit.setBackgroundAlpha(activity, 0.5f);
        popWindow.showAtLocation(activity.getWindow().getDecorView().getRootView(), Gravity.CENTER, 0, 0);
        return popWindow;
    }

    public static PopProgressWindow buildProgressWindow(Activity activity, PopupWindow.OnDismissListener dismissListener) {
        PopWindowLoadingBinding binding = PopWindowLoadingBinding.inflate(activity.getLayoutInflater(), null, false);
        PopupWindow popWindow = build(activity.getBaseContext(), false);
        popWindow.setContentView(binding.getRoot());
        PopProgressWindow progressPopWindow = new PopProgressWindow(activity, popWindow, binding.popCommonProgressTip);

        popWindow.setOnDismissListener(() -> {
            if (dismissListener != null) {
                dismissListener.onDismiss();
            }
            WindowUnit.setBackgroundAlpha(activity, 1.0f);
        });

        WindowUnit.setBackgroundAlpha(activity, 0.5f);
        popWindow.showAtLocation(activity.getWindow().getDecorView().getRootView(), Gravity.CENTER, 0, 0);
        return progressPopWindow;
    }

    private static PopupWindow build(Context context, boolean isFocusable) {
        PopupWindow popupWindow = new PopupWindow(context);
        popupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setAnimationStyle(R.style.anim_pop_common);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(isFocusable);
        popupWindow.setTouchable(true);

        return popupWindow;
    }
}
