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

        try {
            // Connect to the remote host
            session = jsch.getSession(username, host, SSH_PORT);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // Detect OS type
            String osType = executeCommand(session, "uname -s").trim().toLowerCase(Locale.ROOT);

            // Determine install command based on OS and distribution
            String installCommand;
            if (osType.contains("linux")) {
                String distro = executeCommand(session, "cat /etc/os-release | grep '^ID=' | cut -d'=' -f2").trim().toLowerCase(Locale.ROOT);
                if (distro.contains("ubuntu") || distro.contains("debian")) {
                    installCommand = "wget " + checkmkConfig.getApiUrl() + "/agents/check-mk-agent_2.3.0p4-1_all.deb -O /tmp/check-mk-agent.deb && dpkg -i /tmp/check-mk-agent.deb";
                } else if (distro.contains("centos") || distro.contains("redhat") || distro.contains("fedora")) {
                    installCommand = "wget " + checkmkConfig.getApiUrl() + "/agents/check-mk-agent-2.3.0p4-1.noarch.rpm -O /tmp/check-mk-agent.rpm && yum install -y /tmp/check-mk-agent.rpm";
                } else {
                    throw new Exception("Unsupported Linux distribution: " + distro);
                }
            } else if (osType.contains("windows")) {
                installCommand = "powershell.exe -Command \"Invoke-WebRequest -Uri " + checkmkConfig.getApiUrl() + "/agents/windows/check_mk_agent.msi -OutFile C:\\Windows\\Temp\\check_mk_agent.msi; Start-Process msiexec.exe -ArgumentList '/i', 'C:\\Windows\\Temp\\check_mk_agent.msi', '/quiet', '/norestart' -Wait\"";
            } else {
                throw new Exception("Unsupported operating system: " + osType);
            }

            // Execute the installation command
            return executeCommand(session, installCommand);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = null;
        InputStream in = null;

        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            in = channel.getInputStream();
            channel.connect();

            StringBuilder outputBuffer = new StringBuilder();
            byte[] tmp = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(tmp)) != -1) {
                outputBuffer.append(new String(tmp, 0, bytesRead));
            }

            return outputBuffer.toString();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
}
