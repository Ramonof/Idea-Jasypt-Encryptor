package com.ramonof.intellij.plugin.encryptor.cipher;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.ramonof.intellij.plugin.encryptor.Algorithms;
import com.ramonof.intellij.plugin.encryptor.CipherInformationsDialog;
import com.ramonof.intellij.plugin.encryptor.JasyptPluginSettings;
import com.ramonof.intellij.plugin.encryptor.Notifier;
import com.ramonof.intellij.plugin.encryptor.exceptions.JasyptPluginException;
import com.ramonof.intellij.plugin.encryptor.vault.CipherConfiguration;
import com.ramonof.intellij.plugin.encryptor.vault.SecretVault;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ramonof.intellij.plugin.encryptor.CipherInformationsDialog.*;

public class CipherDecryptCommand {
    private final Pattern pattern = Pattern.compile("ENC\\((.*)\\)");
    private final PsiLanguageInjectionHost property;

    private CipherDecryptCommand(PsiLanguageInjectionHost property) {
        this.property = Objects.requireNonNull(property);
    }

    public static CipherDecryptCommand of(PsiLanguageInjectionHost property) {
        return new CipherDecryptCommand(property);
    }

    public boolean check() {
        boolean isTextEncapsulated = Optional.ofNullable(property.getText())
                .map(String::trim)
                .map(s -> s.startsWith("ENC("))
                .orElse(false);
        Optional<CipherConfiguration> storedConfiguration = SecretVault.getSecrets(property.getContainingFile().getVirtualFile().getPath());
        if (storedConfiguration.isEmpty()) {
            return false;
        }

        CipherConfiguration configuration = storedConfiguration.get();
        String textToUncrypt = getTextToUncrypt(property.getText(), isTextEncapsulated);
        try {
            CipherUtils.decrypt(textToUncrypt, configuration.password(), configuration.algorithm().getCode());
            return true;
        } catch (JasyptPluginException e) {
            return false;
        }
    }

    public void execute() {
        boolean isTextEncapsulated = Optional.ofNullable(property.getText())
                .map(String::trim)
                .map(s -> s.startsWith("ENC("))
                .orElse(false);
        Optional<CipherConfiguration> storedConfiguration = SecretVault.getSecrets(property.getContainingFile().getVirtualFile().getPath());
        storedConfiguration.ifPresentOrElse(configuration -> {

                    String textToUncrypt = getTextToUncrypt(property.getText(), isTextEncapsulated);
                    try {
                        String clearText = CipherUtils.decrypt(textToUncrypt, configuration.password(), configuration.algorithm().getCode());
                        setClearText(clearText);
                    } catch (JasyptPluginException e) {
                        askAndDecrypt(isTextEncapsulated);
                    }
                },
                () -> askAndDecrypt(isTextEncapsulated));
    }

    private void askAndDecrypt(boolean isTextEncapsulated) {
        CipherInformationsDialog dialog = new CipherInformationsDialog(JasyptPluginSettings.getInstance(property.getProject()), isTextEncapsulated);
        if (!dialog.showAndGet()) {
            return;
        }
        Map<String, String> values = dialog.getValues();
        String textToUncrypt = getTextToUncrypt(property.getText(), values.get(ENCAPSULATE_FIELD_NAME));
        String password = values.get(PASSWORD_FIELD_NAME);
        String algorithm = values.get(ALGORITHM_FIELD_NAME);
        try {
            String clearText = CipherUtils.decrypt(textToUncrypt, password, algorithm);
            if ("true".equals(values.get(REMEMBER_PASSWORD))) {
                SecretVault.storeSecret(property.getContainingFile().getVirtualFile().getPath(), new CipherConfiguration(Algorithms.fromCode(algorithm), password));
            }
            JasyptPluginSettings.updateSettings(property.getProject(), dialog.getValues());
            setClearText(clearText);
        } catch (JasyptPluginException e) {
            Notifier.notifyError(property.getProject(), "Failed to decrypt string, please verify provided password or algorithm.");
        }
    }

    private String getTextToUncrypt(String text, String isEncapsulated) {
        return getTextToUncrypt(text, "true".equals(isEncapsulated));
    }

    private String getTextToUncrypt(String text, boolean isEncapsulated) {
        Matcher matches = pattern.matcher(text);
        return matches.find() && isEncapsulated ?
                matches.group(1) :
                text;
    }

    private void setClearText(String clearText) {
        WriteCommandAction.runWriteCommandAction(property.getProject(), () -> {
            property.updateText(clearText);
        });
    }
}
