package com.ramonof.intellij.plugin.encryptor.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import com.ramonof.intellij.plugin.encryptor.Notifier;
import com.ramonof.intellij.plugin.encryptor.cipher.CipherUtils;
import com.ramonof.intellij.plugin.encryptor.exceptions.JasyptPluginException;
import com.ramonof.intellij.plugin.encryptor.vault.SecretVault;

import java.util.Map;
import java.util.Optional;

import static com.ramonof.intellij.plugin.encryptor.CipherInformationsDialog.ALGORITHM_FIELD_NAME;
import static com.ramonof.intellij.plugin.encryptor.CipherInformationsDialog.PASSWORD_FIELD_NAME;

public class UncryptStringAction extends JasyptAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        super.actionPerformed(event);
        Optional<String> selectedText = getSelectedText();
        Optional.ofNullable((TextEditorImpl) event.getDataContext().getData("fileEditor"))
                .map(TextEditorImpl::getFile)
                .map(VirtualFile::getPath)
                .flatMap(SecretVault::getSecrets)
                .ifPresentOrElse(configuration -> {
                            try {
                                String clearText = CipherUtils.decrypt(selectedText.orElseThrow(), configuration.password(), configuration.algorithm().getCode());
                                setClearText(clearText);
                            } catch (JasyptPluginException e) {
                                askAndDecrypt();
                            }
                        },
                        this::askAndDecrypt);
    }

    private void setClearText(String clearText) {
        WriteCommandAction.runWriteCommandAction(project,
                () -> document.replaceString(primaryCaret.getSelectionStart(), primaryCaret.getSelectionEnd(), clearText));
    }

    private void askAndDecrypt() {
        if (!dialog.showAndGet()) {
            return;
        }

        Map<String, String> values = dialog.getValues();
        final Optional<String> textToUncrypt = getSelectedText();
        textToUncrypt.ifPresent(text -> {
            WriteCommandAction.runWriteCommandAction(project,
                    () -> {
                        try {
                            String decrypt = CipherUtils.decrypt(text, values.get(PASSWORD_FIELD_NAME), values.get(ALGORITHM_FIELD_NAME));
                            setClearText(decrypt);
                        } catch (JasyptPluginException e) {
                            Notifier.notifyError(project, "Failed to decrypt string, please verify provided password or algorithm.");
                        }
                    });
            updateSettings();
            primaryCaret.removeSelection();
        });
    }
}
