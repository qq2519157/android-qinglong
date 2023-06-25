package auto.qinglong.bean.ql.network;

import java.util.List;

import auto.qinglong.bean.ql.QLLog;

public class QLLogsRes extends QLBaseRes {
    List<QLLog> data;

    public List<QLLog> getData() {
        return data;
    }

    public void setData(List<QLLog> data) {
        this.data = data;
    }
}
