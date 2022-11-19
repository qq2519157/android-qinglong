package auto.qinglong.network.response;

import auto.qinglong.activity.module.environment.QLEnvironment;

public class EditEnvRes {
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public QLEnvironment getData() {
        return data;
    }

    public void setData(QLEnvironment data) {
        this.data = data;
    }

    private int code;
    private String message;
    private QLEnvironment data;
}
