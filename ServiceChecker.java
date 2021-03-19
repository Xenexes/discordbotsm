import discord4j.core.DiscordClient;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ServiceChecker {

    private String serviceName;

    public ServiceChecker(String serviceName) {
        this.serviceName = serviceName;
    }

    public Boolean checkIfServiceIsRunning() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/bash", "-c", "service " + this.serviceName + " status");
        pb.directory(new File(System.getProperty("user.home")));
        Process proc = pb.start();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        int exitCode = proc.waitFor();

        boolean active = stdInput.lines().anyMatch(s -> s.contains("active (running)"));
        stdInput.lines().forEach(System.out::println);
        stdError.lines().forEach(System.out::println);

        assert exitCode == 0;

        return active;
    }

    public Boolean restartService() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/bash", "-c", "sudo service " + this.serviceName + " restart");
        pb.directory(new File(System.getProperty("user.home")));
        Process proc = pb.start();

        int exitCode = proc.waitFor();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        stdInput.lines().forEach(System.out::println);
        stdError.lines().forEach(System.out::println);

        assert exitCode == 0;

        Thread.sleep(20 * 1000);

        int retryCounter = 0;
        boolean isRunning = false;
        while (!isRunning && retryCounter <= 10) {
            isRunning = this.checkIfServiceIsRunning();
            Thread.sleep(5 * 1000);
            retryCounter++;
        }

        return isRunning;
    }

    public Boolean stopService() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/bash", "-c", "service " + this.serviceName + " stop");
        pb.directory(new File(System.getProperty("user.home")));
        Process proc = pb.start();

        int exitCode = proc.waitFor();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        stdInput.lines().forEach(System.out::println);
        stdError.lines().forEach(System.out::println);

        assert exitCode == 0;

        Thread.sleep(20 * 1000);

        int retryCounter = 0;
        boolean isRunning = false;
        while (isRunning && retryCounter <= 10) {
            isRunning = this.checkIfServiceIsRunning();
            Thread.sleep(5 * 1000);
            retryCounter++;
        }

        return isRunning;
    }

/*
    public static String execCmdSync(String cmd, CmdExecResult callback) throws java.io.IOException, InterruptedException {
        RLog.i(TAG, "Running command:", cmd);

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);

        //String[] commands = {"system.exe", "-get t"};

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        StringBuffer stdOut = new StringBuffer();
        StringBuffer errOut = new StringBuffer();

        // Read the output from the command:
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
            stdOut.append(s);
        }

        // Read any errors from the attempted command:
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
            errOut.append(s);
        }

        if (callback == null) {
            return stdInput.toString();
        }

        int exitVal = proc.waitFor();
        callback.onComplete(exitVal == 0, exitVal, errOut.toString(), stdOut.toString(), cmd);

        return stdInput.toString();
    }

    public interface CmdExecResult{
        void onComplete(boolean success, int exitVal, String error, String output, String originalCmd);
    }

 */
}