package auto.qinglong.activity.ql.environment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import auto.qinglong.R;
import auto.qinglong.bean.ql.QLEnvironment;
import auto.qinglong.utils.LogUnit;
import auto.qinglong.utils.TimeUnit;

public class EnvItemAdapter extends RecyclerView.Adapter<MyViewHolder> {
    public static final String TAG = "EnvItemAdapter";

    private final Context context;
    private List<QLEnvironment> data;
    private ItemActionListener itemActionListener;
    private boolean checkState;
    private Boolean[] dataCheckState;

    public EnvItemAdapter(@NonNull Context context) {
        this.context = context;
        this.data = new ArrayList<>();
        this.checkState = false;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_env, parent, false);
        return new MyViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        QLEnvironment environment = this.data.get(position);
        holder.layout_name.setText(String.format("[%d] %s", environment.getIndex(), environment.getName()));
        holder.layout_value.setText(environment.getValue());

        if (this.checkState) {
            holder.layout_check.setChecked(this.dataCheckState[position]);
            holder.layout_check.setVisibility(View.VISIBLE);
        } else {
            holder.layout_check.setVisibility(View.GONE);
        }

        if (environment.getRemarks() == null || environment.getRemarks().isEmpty()) {
            holder.layout_remark.setText("--");
        } else {
            holder.layout_remark.setText(environment.getRemarks());
        }

        if (environment.getStatus() == 0) {
            holder.layout_status.setTextColor(context.getColor(R.color.theme_color_shadow));
            holder.layout_status.setText("已启用");
        } else {
            holder.layout_status.setTextColor(context.getColor(R.color.text_color_red));
            holder.layout_status.setText("已禁用");
        }

        holder.layout_createAt.setText(TimeUnit.formatTimeA(environment.getCreated()));

        holder.layout_name.setOnClickListener(v -> {
            if (this.checkState) {
                holder.layout_check.setChecked(!holder.layout_check.isChecked());
            }
        });

        holder.layout_name.setOnLongClickListener(v -> {
            if (!this.checkState) {
                itemActionListener.onMulAction(environment, holder.getAdapterPosition());
            }
            return true;
        });

        holder.layout_body.setOnClickListener(v -> {
            if (this.checkState) {
                holder.layout_check.setChecked(!holder.layout_check.isChecked());
            }
        });

        holder.layout_body.setOnLongClickListener(v -> {
            if (!this.checkState) {
                itemActionListener.onEdit(environment, holder.getAdapterPosition());

            }
            return true;
        });

        holder.layout_check.setOnCheckedChangeListener((buttonView, isChecked) -> dataCheckState[holder.getAdapterPosition()] = isChecked);
    }

    @Override
    public int getItemCount() {
        return this.data == null ? 0 : this.data.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<QLEnvironment> data) {
        this.data.clear();
        this.data.addAll(data);
        this.dataCheckState = new Boolean[this.data.size()];
        Arrays.fill(this.dataCheckState, false);
        notifyDataSetChanged();
    }

    public List<QLEnvironment> getData() {
        return this.data;
    }

    public boolean isCheckState() {
        return checkState;
    }

    public void setCheckState(boolean checkState, int position) {
        this.checkState = checkState;
        Arrays.fill(this.dataCheckState, false);
        if (checkState && position > -1) {
            this.dataCheckState[position] = true;
        }
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setAllChecked(boolean checked) {
        if (checkState) {
            Arrays.fill(this.dataCheckState, checked);
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public List<QLEnvironment> getSelectedItems() {
        List<QLEnvironment> environments = new ArrayList<>();
        for (int k = 0; k < this.dataCheckState.length; k++) {
            if (this.dataCheckState[k]) {
                environments.add(this.data.get(k));
            }
        }
        return environments;
    }

    public void setItemInterface(ItemActionListener itemActionListener) {
        this.itemActionListener = itemActionListener;
    }

    public interface ItemActionListener {
        void onEdit(QLEnvironment environment, int position);

        void onMulAction(QLEnvironment environment, int position);
    }

}


class MyViewHolder extends RecyclerView.ViewHolder {
    public LinearLayout layout_body;
    public CheckBox layout_check;
    public TextView layout_name;
    public TextView layout_value;
    public TextView layout_remark;
    public TextView layout_status;
    public TextView layout_createAt;

    public MyViewHolder(@NonNull View itemView) {
        super(itemView);
        layout_body = itemView.findViewById(R.id.env_detail);
        layout_check = itemView.findViewById(R.id.env_check);
        layout_name = itemView.findViewById(R.id.env_name);
        layout_value = itemView.findViewById(R.id.env_value);
        layout_status = itemView.findViewById(R.id.env_status);
        layout_remark = itemView.findViewById(R.id.env_remark);
        layout_createAt = itemView.findViewById(R.id.env_create_time);
    }
}
