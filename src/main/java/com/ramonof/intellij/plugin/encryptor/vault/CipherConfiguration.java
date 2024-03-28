package com.ramonof.intellij.plugin.encryptor.vault;

import com.intellij.credentialStore.Credentials;
import com.ramonof.intellij.plugin.encryptor.Algorithms;

import java.util.Objects;

public final class CipherConfiguration {
    private final Algorithms algorithm;
    private final String password;

    public CipherConfiguration(Algorithms algorithm, String password) {
        this.algorithm = algorithm;
        this.password = password;
    }

    public Algorithms algorithm() {
        return algorithm;
    }

    public String password() {
        return password;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CipherConfiguration) obj;
        return Objects.equals(this.algorithm, that.algorithm) &&
                Objects.equals(this.password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, password);
    }

    @Override
    public String toString() {
        return "CipherConfiguration[" +
                "algorithm=" + algorithm + ", " +
                "password=" + password + ']';
    }

    public CipherConfiguration(Credentials credentials) {
        this(Algorithms.fromCode(credentials.getUserName()), credentials.getPasswordAsString());
    }
}
