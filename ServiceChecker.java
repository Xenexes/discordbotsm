import discord4j.core.DiscordClient;

import java.io.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ServiceChecker {

    private Boolean isRunning = false;

    private String serviceName;

    public ServiceChecker(String serviceName) {
        this.serviceName = serviceName;
    }

    public Boolean checkIfServiceIsRunning() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();

        builder.command("/usr/bin/bash", "-c", "service " + this.serviceName + " status");

        builder.directory(new File(System.getProperty("user.home")));
        Process process = builder.start();

        this.isRunning = false;

        Consumer<String> consumer = this::checkForMatch;

        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), consumer);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        int exitCode = process.waitFor();

        while (!streamGobbler.hasFinished()) {
            System.out.println("Waiting for process check ...");
        }

        assert exitCode == 0;



        return this.isRunning;
    }

    public Boolean restartService() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();

        builder.command("/usr/bin/bash", "-c", "sudo service " + this.serviceName + " restart");
        builder.directory(new File(System.getProperty("user.home")));
        Process process = builder.start();

        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        int exitCode = process.waitFor();
        assert exitCode == 0;

        this.isRunning = false;

        Thread.sleep(20 * 1000);

        int retryCounter = 0;
        while (!this.isRunning && retryCounter <= 10) {
            this.isRunning = this.checkIfServiceIsRunning();
            Thread.sleep(5 * 1000);
            retryCounter++;
        }

        return this.isRunning;
    }

    public Boolean stopService() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();

        builder.command("/usr/bin/bash", "-c", "sudo service " + this.serviceName + " stop");
        builder.directory(new File(System.getProperty("user.home")));
        Process process = builder.start();

        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        int exitCode = process.waitFor();
        assert exitCode == 0;

        this.isRunning = true;

        Thread.sleep(20 * 1000);

        int retryCounter = 0;
        while (this.isRunning && retryCounter <= 10) {
            this.isRunning = this.checkIfServiceIsRunning();
            Thread.sleep(5 * 1000);
            retryCounter++;
        }

        return this.isRunning;
    }

    private void checkForMatch(String s) {
        if (!this.isRunning) {
            this.isRunning = s.contains("active (running)");
        }
    }

    /**
     * Hook into the input and output streams of a process
     */
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;
        private boolean finished = false;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            this.finished = false;

           new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);

            this.finished = true;
        }

        public boolean hasFinished() {
            return finished;
        }
    }
}