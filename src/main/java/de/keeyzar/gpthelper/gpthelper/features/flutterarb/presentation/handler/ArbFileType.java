package de.keeyzar.gpthelper.gpthelper.features.flutterarb.presentation.handler;

import com.intellij.icons.AllIcons;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ArbFileType extends LanguageFileType {
    public static final ArbFileType INSTANCE = new ArbFileType();

    private ArbFileType() {
        super(JsonLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "ARB File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Application resource bundle (.arb) file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "arb";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Json;
    }
}