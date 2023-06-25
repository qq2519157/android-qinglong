package auto.qinglong.bean.ql;

public class QLLoginData {
    /* 接口属性 */
    private String token;
    private String lastip;
    private String lastaddr;
    private String platform;
    private String token_type;
    private long lastlogon;
    private long expiration;
    private int retries;

    public String getToken() {
        return token;
    }
}
