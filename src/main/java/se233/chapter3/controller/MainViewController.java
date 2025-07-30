package se233.chapter3.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import se233.chapter3.Launcher;
import se233.chapter3.model.FileFreq;
import se233.chapter3.model.PdfDocument;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MainViewController {
    LinkedHashMap<String, List<FileFreq>> uniqueSets;
    @FXML
    private ListView<String> inputListView;
    @FXML
    private Button startButton;
    @FXML
    private MenuItem Close;
    @FXML
    private ListView<String> listView;
    private List<File> fileList = new ArrayList<>();

    @FXML
    public void initialize() {
        inputListView.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            final boolean isAccepted = db.getFiles().get(0).getName().toLowerCase().
                    endsWith(".pdf");
            if (db.hasFiles() && isAccepted) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        inputListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                String filePath;
                int total_files = db.getFiles().size();
                WordCountMapTask[] wordCountMapTaskArray = new WordCountMapTask[total_files];
                Map<String, FileFreq>[] wordMap = new Map[total_files];
                fileList.clear();
                inputListView.getItems().clear();
                for (int i = 0; i < total_files; i++) {

                    File file = db.getFiles().get(i);
                    fileList.add(file); // keep the File object
                    inputListView.getItems().add(file.getName()); // show only the name

                }

            }
            event.setDropCompleted(success);
            event.consume();
        });
        startButton.setOnAction(event -> {
            Parent bgRoot = Launcher.primaryStage.getScene().getRoot();
            Task<Void> processTask = new Task<Void>() {
                @Override
                public Void call() throws IOException {
                    ProgressIndicator pi = new ProgressIndicator();
                    VBox box = new VBox(pi);
                    box.setAlignment(Pos.CENTER);
                    Launcher.primaryStage.getScene().setRoot(box);
                    ExecutorService executor = Executors.newFixedThreadPool(4);
                    final ExecutorCompletionService<Map<String, FileFreq>> completionService = new
                            ExecutorCompletionService<>(executor);
                    List<String> inputListViewItems = inputListView.getItems();
                    int total_files = inputListViewItems.size();
                    Map<String, FileFreq>[] wordMap = new Map[total_files];
                    for (int i = 0; i < total_files; i++) {
                        try {
                            String filePath = fileList.get(i).getAbsolutePath(); // retrieve full path from fileList
                            PdfDocument p = new PdfDocument(filePath);
                            completionService.submit(new WordCountMapTask(p));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    for (int i = 0; i < total_files; i++) {
                        try {
                            Future<Map<String, FileFreq>> future = completionService.take();
                            wordMap[i] = future.get();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        WordCountReduceTask merger = new WordCountReduceTask(wordMap);
                        Future<LinkedHashMap<String, List<FileFreq>>> future = executor.submit(
                                merger);
                        uniqueSets = future.get();
                        uniqueSets.forEach((word, fileFreqs) -> {
                            listView.getItems().add(displayFormat(word, fileFreqs));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        executor.shutdown();
                    }
                    return null;
                }
            };
            processTask.setOnSucceeded(e -> {
                Launcher.primaryStage.getScene().setRoot(bgRoot);
            });
            Thread thread = new Thread(processTask);
            thread.setDaemon(true);
            thread.start();
        });
        Close.setOnAction(e -> {
            System.exit(0);
        });
        listView.setOnMouseClicked(event -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            // Extract the word before the first space or parenthesis
            String wordKey = selected.split(" \\(")[0];
            List<FileFreq> listOfLinks = uniqueSets.get(wordKey);

            ListView<FileFreq> popupListView = new ListView<>();
            LinkedHashMap<FileFreq, String> lookupTable = new LinkedHashMap<>();
            for (int i = 0; i < listOfLinks.size(); i++) {
                lookupTable.put(listOfLinks.get(i), listOfLinks.get(i).getPath());
                popupListView.getItems().add(listOfLinks.get(i));
            }
            popupListView.setPrefWidth(Region.USE_COMPUTED_SIZE);
            popupListView.setPrefHeight(popupListView.getItems().size() * 40);
            popupListView.setOnMouseClicked(innerEvent -> {
                Launcher.hs.showDocument("file:///" + lookupTable.get(popupListView.
                        getSelectionModel().getSelectedItem()));
                popupListView.getScene().getWindow().hide();
            });
            Popup popup = new Popup();
            popup.getContent().add(popupListView);
            popup.show(Launcher.primaryStage);
            popupListView.setOnKeyPressed(close ->{
                    if (close.getCode() == KeyCode.ESCAPE){
                    popup.getScene().getWindow().hide();}
        });
    });
    }
    private String displayFormat(String word, List<FileFreq> fileFreqs) {
        // สร้าง list ของจำนวนความถี่
        List<Integer> freqList = fileFreqs.stream()
                .map(FileFreq::getFreq)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        String joined = freqList.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return word + " (" + joined + ")";
    }
}
