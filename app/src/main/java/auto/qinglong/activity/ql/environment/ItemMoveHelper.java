package auto.qinglong.activity.ql.environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author wsfsp4
 * @version 2023.03.01
 */
public class ItemMoveHelper extends ItemTouchHelper.Callback {
    ItemMoveCallback callback;
    private boolean haveMove = false;
    private int fromPosition;
    private int toPosition;

    public ItemMoveHelper(ItemMoveCallback callback) {
        this.callback = callback;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.DOWN | ItemTouchHelper.UP;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        fromPosition = viewHolder.getBindingAdapterPosition();
        toPosition = target.getBindingAdapterPosition();
        callback.onItemMove(fromPosition, toPosition);
        haveMove = true;
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (haveMove) {
            haveMove = false;
            callback.onItemMoveEnd(fromPosition, toPosition);
        }
    }
}
