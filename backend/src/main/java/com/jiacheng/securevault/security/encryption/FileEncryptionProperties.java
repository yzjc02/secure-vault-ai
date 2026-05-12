package com.jiacheng.securevault.security.encryption;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "secure-vault.security.file-encryption")
public class FileEncryptionProperties {

    private boolean enabled = true;

    private String key = "";

    private String devKeyFile = ".secure-vault/file-encryption.key";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDevKeyFile() {
        return devKeyFile;
    }

    public void setDevKeyFile(String devKeyFile) {
        this.devKeyFile = devKeyFile;
    }
}
