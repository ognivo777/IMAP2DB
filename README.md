# IMAP to DB loader

1. Reads list of imap folders
2. For each folder load all messages basic info and save to the database (embedded local H2 database by default).

## Tables

### folders
| Field   |
|---------|
| name    |
| total   |
| recent  |
| unseen  |

### mails
| Field       |
|-------------|
| id          |
| mFolder_id  |
| mDate       |
| mFromAddr   |
| mFromName   |
| mSubject    |
| mToAddr     |
| mToName     |
| mSize       |

## Download
You can download build from releases [page](https://github.com/ognivo777/IMAP2DB/releases).