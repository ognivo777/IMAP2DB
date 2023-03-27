# IMAP to DB loader

1. Reads list of imap folders
2. For each folder load all messages basic info and save to the database (embedded local H2 database by default).

This as just a basic example of usage [yahoo/imapnio](https://github.com/yahoo/imapnio) in blocking style. 

## Usage
You can specify your custom `*.properties` via first application command line parameter.

If specified param `onlyFolders` - only read list of folders print them and loads to db.

If specified param `singleFolder` and name of folder followed, than load all messages from specified folder.

## Examples

`java -jar IMAP-lister.jar`

`java -jar IMAP-lister.jar settings-my.properties`

`java -jar IMAP-lister.jar settings-my.properties onlyFolders`

`java -jar IMAP-lister.jar settings-my.properties singleFolder bcs`


## Download
You can download build from releases [page](https://github.com/ognivo777/IMAP2DB/releases).

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
