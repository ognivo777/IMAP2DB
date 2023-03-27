package org.obiz.imap.lister;

import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;
import com.sun.mail.iap.ParsingException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.Status;
import com.yahoo.imapnio.async.client.*;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.data.ListInfoList;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.request.*;
import com.yahoo.imapnio.async.response.ImapAsyncResponse;
import com.yahoo.imapnio.async.response.ImapResponseMapper;
import io.smallrye.config.ConfigValuePropertiesConfigSource;
import org.obiz.imap.lister.entity.Folder;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Main implements Closeable {

    private String dbUri;
    private String dbLogin;
    private String dbPassword;
    private String imapServerUri;
    private String imapUser;
    private String imapPassword;
    private ImapAsyncSession imapAsyncSession;
    private ImapResponseMapper mapper;

    private List<Folder> folders = new ArrayList<>();
    private Map<String, Folder> folderMap = new HashMap<>();
    private Loader dbLoader;
    private ImapAsyncClient imapClient;

    private int retryCount=0;

    public static void main(String[] args) throws Exception {
        Logger.setGlobalLogLevel(Level.INFO); //ormlite logging level

        URL url = Paths.get("settings.properties").toUri().toURL();
        System.out.println("url = " + url);
        ConfigValuePropertiesConfigSource fileConfig = new ConfigValuePropertiesConfigSource(url);

        try(Main main = new Main(
                fileConfig.getValue("db.jdbcUrl"),
                fileConfig.getValue("db.user"),
                fileConfig.getValue("db.password"),
                fileConfig.getValue("imap.uri"),
                fileConfig.getValue("imap.user"),
                fileConfig.getValue("imap.password")
        )){
            main.init();
            main.start();
            main.loadAllFolders();
        }
    }

    private void loadAllFolders() {
        folders.forEach(folder -> {
            try {
                loadFolder(folder);
                Thread.sleep(1000);
            } catch (ImapAsyncClientException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadFolder(String folderName) throws ImapAsyncClientException, ExecutionException, InterruptedException {
        if(folders.size()==0) {
            throw new RuntimeException("Folders not loaded!");
        } else if (!folderMap.containsKey(folderName)) {
            throw new RuntimeException("Folder '" + folderName + "' not found!");
        }
        Folder folder = folderMap.get(folderName);
        loadFolder(folder);
    }

    private void loadFolder(Folder folder) throws ImapAsyncClientException, ExecutionException, InterruptedException {
        SelectFolderCommand selectFolderCommand = new SelectFolderCommand(folder.getName());
        runCommand(selectFolderCommand);
        int folderTotal = folder.getTotal();
        int batch = 100;
        for (int i = 0; i*batch < folderTotal; i++) {
            int from = i*batch + 1;
            int to = from + batch - 1;
            if(to > folderTotal) {
                to = folderTotal;
            }
            System.out.format("Load batch #%d for folder %s\n", i+1, folder.getName());
//            FetchCommand fetchCommand = new FetchCommand(new MessageNumberSet[]{new MessageNumberSet(1, MessageNumberSet.LastMessage.LAST_MESSAGE)}, FetchMacro.FULL);
            FetchCommand fetchCommand = new FetchCommand(new MessageNumberSet[]{new MessageNumberSet(from, to)}, FetchMacro.FULL);
            ImapAsyncResponse fetchResponse = runCommand(fetchCommand);
            fetchResponse.getResponseLines().stream()
                    .filter(imapResponse -> !imapResponse.getRest().contains("nothing matched"))
                    .filter(imapResponse -> !imapResponse.getRest().contains("FETCH done"))
                    .map(Mapper::map)
                    .peek(mailInfo -> mailInfo.setmFolder(folder))
                    .forEach(mailInfo -> dbLoader.save(mailInfo));
            Thread.sleep(500);
        }
    }

    private Main init() throws SQLException, SSLException, URISyntaxException, ExecutionException, InterruptedException, ImapAsyncClientException, TimeoutException, ParsingException {
        dbLoader = new Loader(dbUri, dbLogin, dbPassword);

        imapClient = new ImapAsyncClient(1);
        final URI serverUri = new URI(imapServerUri);
        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final Future<ImapAsyncCreateSessionResponse> future = imapClient.createSession(serverUri, config, null, null, ImapAsyncSession.DebugMode.DEBUG_OFF);
        imapAsyncSession = future.get().getSession();
        final Future<ImapAsyncResponse> capaCmdFuture = imapAsyncSession.execute(new CapaCommand());
        final ImapAsyncResponse resp = capaCmdFuture.get(5, SECONDS);
        System.out.println("Capability command is done.");
        mapper = new ImapResponseMapper();
        final Capability capa = mapper.readValue(resp.getResponseLines().toArray(new IMAPResponse[0]), Capability.class);
        final Future<ImapAsyncResponse> authFuture = imapAsyncSession.execute(new AuthPlainCommand(imapUser, imapPassword, capa));
        ImapAsyncResponse authResponse = authFuture.get();
        Capability capability = mapper.readValue(authResponse.getResponseLines().toArray(new IMAPResponse[0]), Capability.class);
        if(capability.hasCapability("XLIST")) {
            System.out.println("IMAP login success");
            return this;
        } else {
            throw new RuntimeException("Login falied. 'XLIST' not found");
        }
    }

    public Main(String dbUri, String dbLogin, String dbPassword, String imapServerUri, String imapUser, String imapPassword) {
        this.dbUri = dbUri;
        this.dbLogin = dbLogin;
        this.dbPassword = dbPassword;
        this.imapServerUri = imapServerUri;
        this.imapUser = imapUser;
        this.imapPassword = imapPassword;
    }

    private void start() throws ExecutionException, InterruptedException, ParsingException, ImapAsyncClientException {
        listFolders();
        folders.stream()
                .peek(folder -> folderMap.put(folder.getName(), folder))
                .forEach(System.out::println);
    }

    private void listFolders() throws ExecutionException, InterruptedException, ImapAsyncClientException, ParsingException {
        ListCommand listCommand = new ListCommand("", "*");
        ImapAsyncResponse listResponse = runCommand(listCommand);
        ListInfoList list = mapper.readValue(listResponse.getResponseLines().toArray(new IMAPResponse[0]), ListInfoList.class);
        folders = list.getListInfo().stream().map(listInfo -> {
            StatusCommand command = new StatusCommand(listInfo.name, new String[]{"MESSAGES", "RECENT", "UNSEEN"});
            Status folderStatus = null;
            try {
                folderStatus = mapper.readValue(runCommand(command).getResponseLines().toArray(new IMAPResponse[0]), Status.class);
                Folder folderItem = new Folder(listInfo.name, folderStatus.total, folderStatus.recent, folderStatus.unseen);
                dbLoader.save(folderItem);
                return folderItem;
            } catch (ImapAsyncClientException | ParsingException e) {
                e.printStackTrace();
            }
            return new Folder(listInfo.name, 0, 0, 0);
        }).collect(Collectors.toList());
    }

    private ImapAsyncResponse runCommand(ImapRequest command) {
        try {
            ImapFuture<ImapAsyncResponse> listFuture = imapAsyncSession.execute(command);
            //todo обработать исключение com.yahoo.imapnio.async.exception.ImapAsyncClientException: failureType=CHANNEL_TIMEOUT
            return listFuture.get();
        } catch (ExecutionException | ImapAsyncClientException e) {
            e.printStackTrace();
            if(retryCount++<3){
                System.out.println("Retry " + retryCount + " ...");
                return runCommand(command);
            } else {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            retryCount=0;
        }
    }

    @Override
    public void close() {
        try {
            imapAsyncSession.close().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        imapClient.shutdown();
        dbLoader.close();
    }
}
