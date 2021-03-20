import java.io.*;

public class SystemServiceHandler {

    private String serviceName;

    public SystemServiceHandler(String serviceName) {
        this.serviceName = serviceName;
    }

    public Boolean isRunning() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/bash", "-c", "sudo service " + this.serviceName + " status");
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
            isRunning = this.isRunning();
            Thread.sleep(5 * 1000);
            retryCounter++;
        }

        return isRunning;
    }

    public Boolean stopService() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/bash", "-c", "sudo service " + this.serviceName + " stop");
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
        boolean isRunning = true;
        while (isRunning && retryCounter <= 10) {
            isRunning = this.isRunning();
            Thread.sleep(5 * 1000);
            retryCounter++;
        }

        return isRunning;
    }

    public void shutdownServer() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/bash", "-c", "sudo shutdown -h now");
        pb.directory(new File(System.getProperty("user.home")));
        Process proc = pb.start();

        int exitCode = proc.waitFor();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        stdInput.lines().forEach(System.out::println);
        stdError.lines().forEach(System.out::println);

        assert exitCode == 0;
    }
}