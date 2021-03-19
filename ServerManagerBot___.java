///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.discord4j:discord4j-core:3.1.3
//DEPS info.picocli:picocli:4.5.0
//DEPS commons-validator:commons-validator:1.7
//SOURCES WakeOnLan.java
//JAVA 11+

import org.apache.commons.validator.routines.InetAddressValidator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(name = "smb", mixinStandardHelpOptions = true, version = "0.1",
        description = "Bot to manage server over discord.")
public class ServerManagerBot implements Callable<Integer> {

    private final String START_SERVER_COMMAND = "!start server";

    //@Parameters(index = "0", description = "Discord bot token.")
    @Option(names = {"-t", "--token", "-token"}, description = "Ip of the server to WOL")
    private String discordToken;

    @Option(names = {"-m", "--master", "-master"}, description = "Defines if bot ist master or slave. If set bot starts in master mode.", defaultValue = "false")
    private Boolean master;

    @Option(names = {"-ip", "--ip"}, description = "IP of the server to WOL")
    private String ip;

    @Option(names = {"-mac", "--mac"}, description = "MAC of the server to WOL")
    private String mac;

    @Option(names = {"-s", "--service", "-service"}, description = "Name of the service to start.")
    private String serviceName;

    private final InetAddressValidator validator = InetAddressValidator.getInstance();

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

            if (this.ip != null || this.ip.isBlank()) {
                System.out.println("Server IP: Is ignored, running in master mode!");
            }

            if (this.mac != null || this.mac.isBlank()) {
                System.out.println("Server MAC: Is ignored, running in master mode!");
            }

            if (this.serviceName == null || this.serviceName.isBlank()) {
                System.err.println("No service name to start, quitting ...");
                return 0;
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

        gateway.on(MessageCreateEvent.class)
                .subscribe(event -> {
                            final Message message = event.getMessage();
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

                                channel.createMessage("Waiting for service start ...").block();
                            }
                        }
                );

        gateway.onDisconnect().block();
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