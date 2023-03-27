package org.obiz.imap.lister.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "mails")
public class MailInfo {
    @DatabaseField(generatedId = true)
    private long id;
    @DatabaseField(foreign = true)
    private Folder mFolder;
    @DatabaseField
    private Date mDate;
    @DatabaseField
    private String mFromAddr;
    @DatabaseField
    private String mFromName;
    @DatabaseField(width = 1000)
    private String mSubject;
    @DatabaseField
    private String mToAddr;
    @DatabaseField
    private String mToName;
    @DatabaseField
    private long mSize;
    private String mBodytype;

    public MailInfo() {
        // ORMLite needs a no-arg constructor
    }

    public Folder getmFolder() {
        return mFolder;
    }

    public void setmFolder(Folder mFolder) {
        this.mFolder = mFolder;
    }

    public Date getmDate() {
        return mDate;
    }

    public void setmDate(Date mDate) {
        this.mDate = mDate;
    }

    public String getmFromAddr() {
        return mFromAddr;
    }

    //todo сделать отдельный справочник адресов - емейлов, и отдельно к нему справочник емейл-имя, на который тут и ссылаться
    public void setmFromAddr(String mFromAddr) {
        this.mFromAddr = mFromAddr;
    }

    public String getmFromName() {
        return mFromName;
    }

    public void setmFromName(String mFromName) {
        this.mFromName = mFromName;
    }

    public String getmSubject() {
        return mSubject;
    }

    public void setmSubject(String mSubject) {
        this.mSubject = mSubject;
    }

    public String getmToAddr() {
        return mToAddr;
    }

    public void setmToAddr(String mToAddr) {
        this.mToAddr = mToAddr;
    }

    public String getmToName() {
        return mToName;
    }

    public void setmToName(String mToName) {
        this.mToName = mToName;
    }

    public long getmSize() {
        return mSize;
    }

    public void setmSize(long mSize) {
        this.mSize = mSize;
    }

    public String getmBodytype() {
        return mBodytype;
    }

    public void setmBodytype(String mBodytype) {
        this.mBodytype = mBodytype;
    }
}
