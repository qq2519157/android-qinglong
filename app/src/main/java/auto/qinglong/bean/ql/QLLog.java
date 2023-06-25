package auto.qinglong.bean.ql;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class QLLog implements Comparable<QLLog> {
    /* 接口属性 */
    private String title;
    private String key;
    private String type;
    private String parent;
    private long mtime;
    private boolean isLeaf;
    private List<QLLog> children;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public List<QLLog> getChildren() {
        return children;
    }

    public void setChildren(List<QLLog> children) {
        this.children = children;
    }

    public boolean isDir(){
       return !TextUtils.isEmpty(type) && type.equals("directory");
    }

    @Override
    public int compareTo(QLLog o) {
        if (this.isDir() && o.isDir()) {
            return this.title.toLowerCase().compareTo(o.getTitle().toLowerCase());
        } else if (this.isDir() && !o.isDir()) {
            return -1;
        } else if (!this.isDir() && o.isDir()) {
            return 1;
        } else {
            return this.title.toLowerCase().compareTo(o.getTitle().toLowerCase());
        }
    }
}
