package auto.qinglong.bean.ql.network;

/**
 * Author  ： logan
 * Time    ： 2023/6/21
 * Desc    ：
 */
public class QLTaskWrapper extends QLBaseRes {
    private QLTasksRes data;

    public QLTasksRes getData() {
        return data;
    }

    public void setData(QLTasksRes data) {
        this.data = data;
    }
}
