package auto.qinglong.bean.ql;

public class QLSystem {
    private boolean isInitialized;

    private String version;

    private static String VERSION;

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static String getStaticVersion() {
        return VERSION;
    }

    public static void setStaticVersion(String version) {
        VERSION = version;
    }
}
