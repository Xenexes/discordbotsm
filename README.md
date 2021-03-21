Discord bot to start server over WoL and monitor / handle a specific process on it. Run the bot in slave mode on a Raspberry and WoL the actual server over it. On the actual server the bot is started in master mode. After the server has been started the given service will be checked and a message will be posted to the given discord channel. Now the service can be managed over discord server and server itself can be shutdown.

- Written with [JBang](https://github.com/jbangdev/jbang) and [picoli](http://picocli.info/) cli.

### Features

- state of the service
- start service
- restart service
- stop service 
- stop server -> stop service, run a specific backup script, shutdown server
- start server over WoL

### Edit Code

Edit with IntelliJ. How to prepare IntelliJ [link](https://www.jetbrains.com/help/idea/working-with-the-ide-features-from-command-line.html#arguments).

`jbang edit --live --open=idea .\ServerManagerBot.java`

Edit with VScode. 

`jbang edit --live –open=code .\ServerManagerBot.java`

With the `--live` parameter jbang will launch your editor while watching for file changes and regenerate the temporary project to pick up changes in dependencies.

### Prepare the server

- Service user needs access rights to start / restart / stop service without password
- Service user needs access rights to shutdown the system
- If needed write / prepare the backup script

Wrap jbang command to a start script

```sh
#!/bin/bash
export PATH=$PATH:~/tools/jbang-0.68.0/bin`
jbang ServerManagerBot.java -t="<DISCORD_BOT-_TOKEN>" -m -bs="<FULL_PATH_TO_BACKUP_SCRIPT>" -s="<PROCESS_NAME_TO _MANAGE>" -ci="<DISCORD_BOT_CHANNEL_ID>"
```

Install system service for the bot using the start script. Create a new file `/etc/systemd/system/discord-bot.service` and add the following.

```sh
[Unit]
Description=Discord bot to start server and manage service
[Service]
#user of your service
User=<USER_NAME>
#group of your service
Group=<USER_GROUP>

#change this to your workspace
WorkingDirectory=<WORKING_DIR>

#path to your start script
ExecStart=<PATH_BOT_TO_SHELL_START_SCRIPT>

SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Install and start the service.

```sh
sudo systemctl daemon-reload && sudo systemctl enable discord-bot.service && sudo systemctl start discord-bot && sudo systemctl status discord-bot
```


### Run the command

Run discord bot in slave mode.

`jbang ServerManagerBot.java -t="<DISCORD_BOT-_TOKEN>" -ip="<SERVER_IP>" -mac="<SERVER_MAC>"`

Run discord bot in master mode.

`jbang ServerManagerBot.java -t="<DISCORD_BOT-_TOKEN>" -m -bs="<FULL_PATH_TO_BACKUP_SCRIPT>" -s="<PROCESS_NAME_TO _MANAGE>" -ci="<DISCORD_BOT_CHANNEL_ID>"`