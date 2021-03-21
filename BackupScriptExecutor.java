import java.io.*;

public class BackupScriptExecutor {

    private String backupScript;

    public BackupScriptExecutor(String backupScript) {
        this.backupScript = backupScript;
    }

    public void executeBackkup() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/bash", "-c", this.backupScript);
        pb.directory(new File(System.getProperty("user.home")));
        Process proc = pb.start();

        int exitCode = proc.waitFor();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        stdInput.lines().forEach(System.out::println);
        stdError.lines().forEach(System.out::println);

        assert exitCode == 0;
    }

    public boolean isScriptValid() {
        boolean valid = true;

        File file = new File(this.backupScript);
        try {
            file.getCanonicalPath();
        } catch (IOException e) {
            valid = false;
        }

        return valid;
    }
}