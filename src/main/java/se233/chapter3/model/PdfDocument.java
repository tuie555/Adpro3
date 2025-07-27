package se233.chapter3.model;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

public class PdfDocument {
    public String getName() {
        return name;
    }

    private String name;

    public String getFilePath() {
        return filePath;
    }

    private String filePath;

    public PDDocument getDocument() {
        return document;
    }

    private PDDocument document;
    private LinkedHashMap<String, List<FileFreq>> uniqueSets;

    public PdfDocument(String filePath) throws IOException {
        this.name = Paths.get(filePath).getFileName().toString();
        this.filePath = filePath;
        File pdfFile = new File(filePath);
        this.document = PDDocument.load(pdfFile);
    }
}
