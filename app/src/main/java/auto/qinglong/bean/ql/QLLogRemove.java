package auto.qinglong.bean.ql;

public class QLLogRemove {
    /* 接口字段 */
    private int id;
    private String ip;
    private String type;
    private LogInfo info;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LogInfo getInfo() {
        return info;
    }

    public void setInfo(LogInfo info) {
        this.info = info;
    }
}
