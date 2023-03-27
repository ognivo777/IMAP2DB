package org.obiz.imap.lister.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = "folders")
public class Folder {
    @DatabaseField(id = true)
    private String name;
    @DatabaseField
    private int total;
    @DatabaseField
    private int recent;
    @DatabaseField
    private int unseen;

    public Folder() {
    }

    public Folder(String name, int total, int recent, int unseen) {
        this.name = name;
        this.total = total;
        this.recent = recent;
        this.unseen = unseen;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getRecent() {
        return recent;
    }

    public void setRecent(int recent) {
        this.recent = recent;
    }

    public int getUnseen() {
        return unseen;
    }

    public void setUnseen(int unseen) {
        this.unseen = unseen;
    }

    @Override
    public String toString() {
        return "Folder{" +
                "name='" + name + '\'' +
                ", total=" + total +
                ", recent=" + recent +
                ", unseen=" + unseen +
                '}';
    }
}
