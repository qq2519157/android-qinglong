package auto.qinglong.network.response;

import java.util.List;

import auto.qinglong.activity.module.log.QLLog;

public class LogRes {
    int code;
    List<QLLog> dirs;
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<QLLog> getDirs() {
        return dirs;
    }

    public void setDirs(List<QLLog> dirs) {
        this.dirs = dirs;
    }


}
