package org.obiz.imap.lister;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.obiz.imap.lister.entity.Folder;
import org.obiz.imap.lister.entity.MailInfo;

import java.sql.SQLException;

public class Loader {

    private final String url;
    private final JdbcConnectionSource connectionSource;
    private final Dao<Folder, String> folderDao;
    private final Dao<MailInfo, Long> mailInfoDao;
    public Loader(String url, String username, String password) throws SQLException {
        this.url = url;
        connectionSource = new JdbcConnectionSource(url, username, password);
        TableUtils.createTableIfNotExists(connectionSource, Folder.class);
        folderDao = DaoManager.createDao(connectionSource, Folder.class);
        TableUtils.createTableIfNotExists(connectionSource, MailInfo.class);
        mailInfoDao = DaoManager.createDao(connectionSource, MailInfo.class);
    }

    public void close() {
        try {
            connectionSource.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(MailInfo mailInfo) {
        try {
            mailInfoDao.create(mailInfo);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save(Folder folder) {
        try {
            folderDao.createOrUpdate(folder);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
