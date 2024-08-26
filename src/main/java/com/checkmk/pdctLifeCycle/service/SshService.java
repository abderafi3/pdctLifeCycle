package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Locale;

@Service
public class SshService {

    private final CheckmkConfig checkmkConfig;
    private static final int SSH_PORT = 22;

    @Autowired
    public SshService(CheckmkConfig checkmkConfig) {
        this.checkmkConfig = checkmkConfig;
    }


    public String installCheckmkAgent(String host, String username, String password) throws Exception {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        InputStream in = null;
        InputStream err = null;

        try {
            // Connect to the remote host
            session = jsch.getSession(username, host, SSH_PORT);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            System.out.println("Connected to host: " + host);

            // Detect the operating system on the remote machine
            String osType = detectOSType(session);
            System.out.println("Detected OS type: " + osType);

            // Define the installation command based on OS type
            String installCommand = generateInstallCommand(osType, session);

            System.out.println("Installation command: " + installCommand);

            // Execute the installation command
            String installationOutput = executeCommand(session, installCommand);
            System.out.println("Installation output: " + installationOutput);

            return installationOutput;

        } catch (Exception e) {
            throw new Exception("Failed to install Checkmk agent: " + e.getMessage(), e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    // Method to detect the OS type
    private String detectOSType(Session session) throws Exception {
        String osCommand = "uname -s";  // Default to Linux/Unix systems
        String osType = executeCommand(session, osCommand);

        // If uname fails or returns nothing, assume it's a Windows system and use the 'ver' command
        if (osType.isEmpty()) {
            osCommand = "ver";  // Use 'ver' command for Windows
            osType = executeCommand(session, osCommand);
        }

        return osType.trim().toLowerCase(Locale.ROOT);
    }

    // Method to generate installation command based on detected OS
    private String generateInstallCommand(String osType, Session session) throws Exception {
        if (osType.contains("linux")) {
            // Detect Linux distribution
            String distroCommand = "cat /etc/os-release | grep '^ID=' | cut -d'=' -f2";
            String distro = executeCommand(session, distroCommand).trim().toLowerCase(Locale.ROOT);
            System.out.println("Detected Linux distribution: " + distro);

            // Generate appropriate install command based on distribution
            if (distro.contains("ubuntu") || distro.contains("debian")) {
                return "wget " + checkmkConfig.getApiUrl() + "/agents/check-mk-agent_2.3.0p4-1_all.deb -O /tmp/check-mk-agent.deb && dpkg -i /tmp/check-mk-agent.deb";
            } else if (distro.contains("centos") || distro.contains("redhat") || distro.contains("fedora") || distro.contains("ol")) {
                return "wget " + checkmkConfig.getApiUrl() + "/agents/check-mk-agent-2.3.0p4-1.noarch.rpm -O /tmp/check-mk-agent.rpm && yum install -y /tmp/check-mk-agent.rpm";
            } else {
                throw new Exception("Unsupported Linux distribution: " + distro);
            }

        } else if (osType.contains("darwin")) {
            // macOS
            return "brew install check-mk-agent";

        } else if (osType.contains("windows")) {
            // Windows
            return "powershell.exe -Command \"Invoke-WebRequest -Uri " + checkmkConfig.getApiUrl() + "/agents/windows/check_mk_agent.msi -OutFile C:\\Windows\\Temp\\check_mk_agent.msi; Start-Process msiexec.exe -ArgumentList '/i', 'C:\\Windows\\Temp\\check_mk_agent.msi', '/quiet', '/norestart' -Wait\"";
        } else {
            throw new Exception("Unsupported operating system: " + osType);
        }
    }

    // Method to execute a command on the remote session and capture both output and error streams
    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();

        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder errorBuffer = new StringBuilder();

        byte[] tmp = new byte[1024];
        channel.connect();

        try {
            while (true) {
                // Capture standard output
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    outputBuffer.append(new String(tmp, 0, i));
                }

                // Capture error output
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    errorBuffer.append(new String(tmp, 0, i));
                }

                if (channel.isClosed()) {
                    if (errorBuffer.length() > 0) {
                        System.out.println("Error output: " + errorBuffer.toString());
                    }
                    break;
                }

                Thread.sleep(1000);  // Prevent tight loop
            }
        } finally {
            in.close();
            err.close();
            channel.disconnect();
        }

        return outputBuffer.toString();
    }
}
