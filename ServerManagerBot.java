///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.discord4j:discord4j-core:3.1.3
//DEPS info.picocli:picocli:4.5.0
//DEPS commons-validator:commons-validator:1.7
//SOURCES WakeOnLan.java SystemServiceHandler.java BackupScriptExecutor.java
//JAVA 11+

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.commons.validator.routines.InetAddressValidator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@Command(name = "smb", mixinStandardHelpOptions = true, version = "0.1",
        description = "Bot to manage server over discord.")
public class ServerManagerBot implements Callable<Integer> {

    private static final String STATUS_SERVER_COMMAND = "!server status";

    private static final String STOP_SERVER_COMMAND = "!server stop";

    private static final String RESTART_SERVER_COMMAND = "!server restart";

    private final String START_SERVER_COMMAND = "!server start";

    //@Parameters(index = "0", description = "Discord bot token.")
    @Option(names = {"-t", "--token", "-token"}, description = "Ip of the server to WOL")
    private String discordToken;

    @Option(names = {"-m", "--master", "-master"}, description = "Defines if bot ist master or slave. If set bot starts in master mode.", defaultValue = "false")
    private Boolean master;

    @Option(names = {"-ip", "--ip"}, description = "IP of the server to WOL")
    private String ip;

    @Option(names = {"-mac", "--mac"}, description = "MAC of the server to WOL")
    private String mac;

    @Option(names = {"-s", "--service", "-service"}, description = "Name of the service to start.", defaultValue = "valheimserver")
    private String serviceName;

    @Option(names = {"-ci", "--channelId", "-channelId"}, description = "Channel ID for the bot and handling of the server.", defaultValue = "821824110412693564")
    private String serverChannelId;

    @Option(names = {"-bs", "--backupScript", "-backupScript"}, description = "Full path to the backup script ( /user/home/USER/backkup.sh ). If unset no backup will be executed.")
    private String backupScript;

    private final InetAddressValidator validator = InetAddressValidator.getInstance();

    private BackupScriptExecutor bse;

    public static void main(final String[] args) {
        int exitCode = new CommandLine(new ServerManagerBot()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        if (this.discordToken == null || this.discordToken.isBlank()) {
            System.err.println("No discord token, quitting ...");
            return 0;
        } else {
            System.out.println("Bot token: " + this.discordToken);
        }

        if (this.master) {
            System.err.println("mode: master");

            if (this.ip != null) {
                System.out.println("Server IP: Is ignored, running in master mode!");
            }

            if (this.mac != null) {
                System.out.println("Server MAC: Is ignored, running in master mode!");
            }

            if (this.serviceName == null || this.serviceName.isBlank()) {
                System.err.println("No service name to start, quitting ...");
                return 0;
            }

            if (this.serverChannelId == null || this.serverChannelId.isBlank()) {
                System.err.println("No channel ID for the bot has been entered, quitting ...");
                return 0;
            } else {
                System.out.println("Bot chnnel ID: " + this.serverChannelId);
            }

            if (this.backupScript == null || this.backupScript.isBlank()) {
                System.out.println("Backup script: No backup script defined! No Backup will be executed!!!");
            } else {
                this.bse = new BackupScriptExecutor(this.backupScript);
                if (this.bse.isScriptValid()) {
                    System.out.println("Backup script: " + this.backupScript);
                } else {
                    System.err.println("Backup script is not valid!");
                    return 0;
                }
            }
        } else {
            System.err.println("mode: slave");

            if (!this.isIpV4Valid(this.ip)) {
                System.err.println("No valid server IP, quitting ...");
                return 0;
            } else {
                System.out.println("Server MAC: " + this.ip);
            }

            if (!this.isValidMAC(this.mac)) {
                System.err.println("No valid server MAC, quitting ...");
                return 0;
            } else {
                System.out.println("Server MAC: " + this.mac);
            }
        }

        this.initBot();

        return 0;
    }

    private void initBot() {
        final DiscordClient client = DiscordClient.create(this.discordToken);
        final GatewayDiscordClient gateway = client.login().block();

        SystemServiceHandler ssh = new SystemServiceHandler(this.serviceName);

        if (this.master) {
            initialServiceCheck(gateway, ssh);
        }

        gateway.on(MessageCreateEvent.class)
                .subscribe(event -> {
                            final Message message = event.getMessage();

                            if (!this.master) {
                                if (START_SERVER_COMMAND.equals(message.getContent())) {
                                    final MessageChannel channel = message.getChannel().block();
                                    channel.createMessage("Starting server ...").block();

                                    try {
                                        WakeOnLan wakeOnLan = new WakeOnLan();
                                        wakeOnLan.wakeUpServer(ip, mac);
                                    } catch (Exception ex) {
                                        System.out.println("Failed to send WoL packet to " + ip + " " + mac + ": " + ex);
                                        channel.createMessage("Failed to start the server ...").block();
                                    }

                                    channel.createMessage("Waiting for game service start ...").block();
                                }
                            } else {
                                if (STATUS_SERVER_COMMAND.equals(message.getContent())) {
                                    final MessageChannel channel = message.getChannel().block();
                                    channel.createMessage("Checking server status ...").block();

                                    try {
                                        Boolean isRunning = ssh.isRunning();

                                        if (isRunning) {
                                            channel.createMessage("Service is running!").block();
                                        } else {
                                            channel.createMessage("Service is not running!").block();
                                        }
                                    } catch (Exception ex) {
                                        channel.createMessage("Error on reading service status ...").block();
                                    }
                                } else if (STOP_SERVER_COMMAND.equals(message.getContent())) {
                                    final MessageChannel channel = message.getChannel().block();
                                    channel.createMessage("Stopping service ...").block();

                                    Boolean isRunning = true;
                                    try {
                                        isRunning = ssh.stopService();
                                    } catch (Exception ex) {
                                        System.out.println("Failed to stop service!");
                                        ex.printStackTrace();
                                        channel.createMessage("Failed to stop service ...").block();
                                    }

                                    if (!isRunning) {
                                        channel.createMessage("Service has been stoped ...").block();

                                        channel.createMessage("Creating backup ...").block();

                                        try {
                                            this.bse.executeBackkup();
                                        } catch (IOException | InterruptedException e) {
                                            channel.createMessage("Backup failed!").block();
                                            System.out.println("Backup failed!");
                                            e.printStackTrace();
                                        }

                                        channel.createMessage("Backup has been created ...").block();

                                        channel.createMessage("Stopping game server ...").block();

                                        try {
                                            ssh.shutdownServer();
                                        } catch (IOException | InterruptedException ex) {
                                            channel.createMessage("Server shutdown failed ...").block();
                                            System.out.println("Server shutdown failed!");
                                            ex.printStackTrace();
                                        }

                                    } else {
                                        channel.createMessage("Stopping service failed ...").block();
                                    }
                                } else if (RESTART_SERVER_COMMAND.equals(message.getContent())) {
                                    final MessageChannel channel = message.getChannel().block();
                                    channel.createMessage("Restarting service ...").block();

                                    Boolean isRunning = false;
                                    try {
                                        isRunning = ssh.restartService();
                                    } catch (Exception ex) {
                                        System.out.println("Failed to restart service!");
                                        ex.printStackTrace();
                                        channel.createMessage("Failed to restart service ...").block();
                                    }

                                    if (isRunning) {
                                        channel.createMessage("Service has been restarted ...").block();
                                    }
                                }
                            }
                        }
                );

        gateway.onDisconnect().block();
    }

    private void initialServiceCheck(GatewayDiscordClient gateway, SystemServiceHandler serviceChecker) {
        try {
            Boolean isRunning = serviceChecker.isRunning();
            Snowflake id = Snowflake.of(Long.parseLong(this.serverChannelId));

            String message = "Service is running!";
            if (!isRunning) {
                message = "Service is not running!";
            }

            String finalMessage = message;
            gateway.getEventDispatcher().on(ReadyEvent.class)
                    .subscribe(event -> {
                        GatewayDiscordClient client = event.getClient();
                        this.sendAutoMessage(client, id, finalMessage);
                    });

        } catch (IOException | InterruptedException ex) {
            System.err.println("Failed to start master bot!");
            ex.printStackTrace();
        }
    }

    public void sendAutoMessage(GatewayDiscordClient gateway, Snowflake channelId, String message) {
        Mono<Channel> channelById = gateway.getChannelById(channelId);

        if (channelById != null) {
            TextChannel channel = (TextChannel) channelById.block();

            if (channel != null) {
                channel.createMessage(message).block();
            } else {
                System.err.println("Failed to send message to channel ID: " + this.serverChannelId);
            }
        } else {
            System.err.println("Failed to send message to channel ID: " + this.serverChannelId);
        }
    }

    private Boolean isIpV4Valid(String ipV4) {
        return validator.isValidInet4Address(ip);
    }

    public Boolean isValidMAC(String mac) {
        final String macRegex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})|([0-9a-fA-F]{4}\\.[0-9a-fA-F]{4}\\.[0-9a-fA-F]{4})$";
        Pattern pattern = Pattern.compile(macRegex);

        if (mac == null || mac.isBlank()) {
            return false;
        }

        return pattern.matcher(mac).matches();
    }
}