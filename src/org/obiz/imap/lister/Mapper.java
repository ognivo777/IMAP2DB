package org.obiz.imap.lister;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.*;
import org.obiz.imap.lister.entity.MailInfo;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Mapper {
    public static MailInfo map(IMAPResponse imapResponse) {
        MailInfo result = new MailInfo();

        String imapLine = imapResponse.getRest();
        try {
            if(!imapLine.contains("nothing matched") && !imapLine.contains("FETCH done")) {
                String fixedImapLine = imapLine.replaceAll("\"image\" \"([-\\w]+)\" \\(\\)", "\"image\" \"$1\" (\"NAME\" \"image.$1\")");
                fixedImapLine = fixedImapLine.replaceAll(" BODY \\( \"alternative\"\\)", "");
                fixedImapLine = fixedImapLine.replaceAll(" BODY \\( \"related\"\\)", "");
                fixedImapLine = fixedImapLine.replaceAll("\\( \"related\"\\)", "");
                fixedImapLine = fixedImapLine.replaceAll(" BODY \\(\"text\" \"\" \\(\\)[^\\)]+\\)", "");
                fixedImapLine = fixedImapLine.replaceAll("\\(\"application\" \"([-\\w]+)\" \\(\\)[^\\)]+\\)", "");
                fixedImapLine = fixedImapLine.replaceAll("\\(\"message\" \"([-\\w]+)\" \\(\\)[^\\)]+\\)", "");
//                fixedImapLine = fixedImapLine.replaceAll("\\(\"application\" \"octet-stream\" \\(\\)[^\\)]+\\)", "");
//                fixedImapLine = fixedImapLine.replaceAll("\\(\"message\" \"rfc822\" \\(\\)[^\\)]+\\)", "");
//                fixedImapLine = fixedImapLine.replaceAll("\\(\"message\" \"delivery-status\" \\(\\)[^\\)]+\\)", "");
//                fixedImapLine = fixedImapLine.replaceAll("\\(\"message\" \"disposition-notification\" \\(\\)[^\\)]+\\)", "");
                imapResponse = new IMAPResponse(fixedImapLine);
                FetchResponse response = new FetchResponse(imapResponse);
                for (int i = 0; i < response.getItemCount(); i++) {
                    Item item = response.getItem(i);
                    if (item instanceof ENVELOPE e) {
                        if(e.subject!=null) {
                            try {
                                result.setmSubject(MimeUtility.decodeText(e.subject));
                            } catch (UnsupportedEncodingException ex) {
                                printExceptionLine(imapLine);
                                throw new RuntimeException(ex);
                            }
                        }
                        result.setmDate(e.date);
                        if(e.from!=null) {
                            result.setmFromAddr(e.from[0].getAddress());
                            result.setmFromName(e.from[0].getPersonal());
                        }
                        if (e.to != null) {
                            result.setmToAddr(e.to[0].getAddress());
                            result.setmToName(e.to[0].getPersonal());
                        }
                    } else if (item instanceof RFC822SIZE s) {
                        result.setmSize(s.size);
                    } else if (item instanceof BODYSTRUCTURE bs) {
                        result.setmBodytype(bs.type + '/' + bs.subtype);
                    }
                }
            } else {
                System.out.println("FOUND WRONG LINE:" + imapLine);
            }
        } catch (IOException | ProtocolException e) {
            printExceptionLine(imapLine);
            e.printStackTrace();
        }
        return result;
    }

    private static void printExceptionLine(String imapLine) {
        System.out.println("Exeption line: \n" + imapLine);
    }

    private static String formatAddress(InternetAddress internetAddress) {
        if(internetAddress.getPersonal()==null) {
            return internetAddress.getAddress();
        } else {
            return internetAddress.getPersonal() + "<" + internetAddress.getAddress() + ">";
        }
    }
}
