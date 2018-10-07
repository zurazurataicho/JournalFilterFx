package org.zura.JournalFilter;

import java.io.File;
import java.io.IOException;
import java.lang.Runnable;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class DirectoryController implements Initializable {
    private File selectedInputDir;
    private File selectedOutputDir;
    private Stage stage;
    private Properties prop;
    /** 入力ディレクトリ・CSVファイル */
    @FXML
    private Label labelInputDir;
    @FXML
    private TextField locationInputDir;
    @FXML
    private Button openInputDirSelector;
    /** 出力ディレクトリ・CSV/XLSXファイル */
    @FXML
    private Label labelOutputDir;
    @FXML
    private TextField locationOutputDir;
    @FXML
    private Button openOutputDirSelector;
    /** 出力フォーマット */
    @FXML
    private Label labelFormat;
    @FXML
    private ChoiceBox<String> formatSelector;
    /** 拡張子フィルターパターン */
    @FXML
    private Label labelExtFilterPattern;
    @FXML
    private ComboBox<String> selectExtRegexPattern;
    /** イベントフィルターパターン */
    @FXML
    private Label labelEventFilterPattern;
    @FXML
    private ComboBox<String> selectEventRegexPattern;
    /** フィルタ実行ボタン */
    @FXML
    private Button execFiltering;
    /** 処理結果表示エリア */
    @FXML
    private TextArea showResult;
    /** プログレスバー */
    @FXML
    private ProgressBar progressFiltering;

    DirectoryController(Stage stage, Properties prop) {
        this.stage = stage;
        this.prop = prop;
    }
    private List<String> extractFilesInDirectory(String dir) {
        Path pathDir = Paths.get(dir);
        if (!Files.isDirectory(pathDir)) {
            return new ArrayList<>();
        }
        List<String> csvFiles = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(pathDir)) {
            for (Path path : ds) {
                String strPath = path.toString();
                if (getFileExtension(strPath.toString()).equals("csv")) {
                    csvFiles.add(strPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return csvFiles;
    }
    private List<String> getFileList(String path) {
        List<String> files = new ArrayList<>();
        String ext = getFileExtension(path);
        if (ext.equals("csv")) {
            files.add(path);
            return files;
        }
        return extractFilesInDirectory(path);
    }
    private String getFileExtension(String path) {
        Integer extPos = path.lastIndexOf(".") + 1;
        String ext = path.substring(extPos).toLowerCase();
        if (ext.equals("csv") || ext.equals("xlsx")) {
            return ext;
        }
        return new String();
    }
    private List<String> getSettingList(String baseKey) {
        List<String> extList = new ArrayList<>();
        int count = 1;
        while (true) {
            String value = prop.getProperty(baseKey + "_" + count);
            if (value == null) {
                break;
            }
            if (value.isEmpty()) {
                continue;
            }
            extList.add(value);
            count += 1;
        }
        return extList;
    }
    private void createDirectory(String dirName) throws IOException {
        Path pathDir = Paths.get(dirName);
        if (Files.isDirectory(pathDir)) {
            return;
        }
        Files.createDirectories(pathDir);
        showResult.appendText(pathDir + "を新規作成しました。\n");
    }
    private StoreType getStoreType(String ext) {
        return ext.equals("xlsx") ? StoreType.Xlsx : StoreType.Csv;
    }
    private String getBasename(String fullPathname) {
        Path path = Paths.get(fullPathname);
        return path.getFileName().toString();
    }
    private String buildOutputFilename(String csvFilename, StoreType storeType) {
        String baseName = getBasename(csvFilename);
        if (storeType == StoreType.Csv) {
            return baseName;
        }
        Integer dotPos = baseName.lastIndexOf(".");
        return baseName.substring(0, dotPos) + ".xlsx";
    }
    private String createResultMessage(final String message) {
        Date now = new Date();
        return "[" + now.toString() + "] " + message + "\n";
    }
    private void setDirectory(TextField locationDir) {
        DirectoryChooser dirChooser = new DirectoryChooser();
/*
        Optional<File> selectedDir = Optional.ofNullable(dirChooser.showDialog(stage));
        selectedDir.ifPresent(dir -> {
		    Optional<String> path = Optional.ofNullable(dir.getAbsolutePath());
            path.ifPresent(absPath -> {
    	        locationDir.setText(absPath);
            });
        });
*/
        Optional.ofNullable(dirChooser.showDialog(stage)).ifPresent(dir -> {
            Optional.ofNullable(dir.getAbsolutePath()).ifPresent(path -> {
    	        locationDir.setText(path);
            });
        });
    }
    private Runnable createFilterExector(final String message, final Boolean disabled) {
        return new Runnable() {
            @Override
            public void run() {
                execFiltering.setDisable(disabled);
                showResult.appendText(createResultMessage(message));
            }
        };
    }
    private Runnable createMessageUpdater(final long numerator, final long denominator, final String inFilename, final String outFilename) {
        return new Runnable() {
            @Override
            public void run() {
                showResult.appendText(createResultMessage(inFilename + "を" + outFilename + "にフィルタしました。"));
                progressFiltering.setProgress((double)numerator / (double)denominator);
            }
        };
    }
    private Task<Void> createTask(final StoreType storeType, final List<String> csvInFiles, final String outDirectory, final String outFilename) {
        return new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    Platform.runLater(createFilterExector("フィルタ処理を開始します。", true));
                    String outFilepath = outFilename;
                    Boolean isAppend = outFilename.isEmpty() ? false : true;
                    long numerator = 0;
                    FilterManager filter = new FilterManager(prop);
                    for (final String inFilepath : csvInFiles) {
                        if (outFilename.isEmpty()) {
                            outFilepath = outDirectory + File.separator + buildOutputFilename(inFilepath, storeType);
                        }
                        numerator += 1;
System.out.println(selectExtRegexPattern.getValue());
System.out.println(selectEventRegexPattern.getValue());
                        filter.run(storeType, inFilepath, outFilepath, isAppend, selectExtRegexPattern.getValue(), selectEventRegexPattern.getValue());
                        Platform.runLater(createMessageUpdater(numerator, csvInFiles.size(), inFilepath, outFilepath));
                    }
                    filter.close();
                    Platform.runLater(createFilterExector("フィルタ処理が完了しました。", false));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        List<String> outputFormatList = Arrays.asList("csv", "xlsx");
        formatSelector.setItems(FXCollections.observableArrayList(outputFormatList));
        formatSelector.setValue(outputFormatList.get(0));

        List<String> regexTargetExtensions = getSettingList("PRESET_REGEX_OFFICE_FILE_EXTENSION");
        selectExtRegexPattern.setItems(FXCollections.observableArrayList(regexTargetExtensions));
        selectExtRegexPattern.setValue(regexTargetExtensions.get(0));

        List<String> regexTargetEvents = getSettingList("PRESET_REGEX_EVENTINFO");
        selectEventRegexPattern.setItems(FXCollections.observableArrayList(regexTargetEvents));
        selectEventRegexPattern.setValue(regexTargetEvents.get(0));
    }
    @FXML
    public void onSelectMenuQuitApplication(ActionEvent event) {
        Platform.exit();
    }
    @FXML
    public void onSelectMenuInputDir(ActionEvent event) {
        setDirectory(locationInputDir);
    }
    @FXML
    public void onSelectMenuOutputDir(ActionEvent event) {
        setDirectory(locationOutputDir);
    }
    @FXML
    public void onClickOpenInputDirSelector(ActionEvent event) {
        setDirectory(locationInputDir);
    }
	@FXML
	public void onDragOverInputDir(DragEvent event) {
        Dragboard board = event.getDragboard();
        if (board.hasFiles()) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
	}
	@FXML
	public void onDragDroppedInputDir(DragEvent event) {
        Dragboard board = event.getDragboard();
        List<File> fileList = board.getFiles();
        selectedInputDir = fileList.get(0);
		locationInputDir.setText(selectedInputDir.getAbsolutePath());
        event.setDropCompleted(true);
	}
    @FXML
    public void onClickOpenOutputDirSelector(ActionEvent event) {
        setDirectory(locationOutputDir);
    }
	@FXML
	public void onDragOverOutputDir(DragEvent event) {
        Dragboard board = event.getDragboard();
        if (board.hasFiles()) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
	}
	@FXML
	public void onDragDroppedOutputDir(DragEvent event) {
        Dragboard board = event.getDragboard();
        List<File> fileList = board.getFiles();
        selectedOutputDir = fileList.get(0);
		locationOutputDir.setText(selectedOutputDir.getAbsolutePath());
        event.setDropCompleted(true);
	}
	@FXML
	public void onClickExecFiltering(ActionEvent event) {
        progressFiltering.setProgress(0.0f);
        if (locationInputDir.getText().isEmpty()) {
            showResult.appendText("入力ディレクトリもしくは入力ファイルを指定してください。\n");
            return;
        }
        if (locationOutputDir.getText().isEmpty()) {
            showResult.appendText("出力ディレクトリもしくは出力ファイルを指定してください。\n");
            return;
        }
        /**
         * 入力ディレクトリ欄がファイル名(.csv)なら単一ファイルからの入力。
         * それ以外は除去。
         */
        List<String> csvInputFiles = getFileList(locationInputDir.getText());
        if (csvInputFiles.size() == 0) {
            showResult.appendText("CSVファイルの含まれる入力ディレクトリもしくはCSVファイルを指定してください。\n");
            return;
        }
        /**
         * 出力ディレクトリ欄がファイル名なら単一ファイルに出力。拡張子で出力フォーマット(csv/xlsx)判定。
         * それ以外ならディレクトリ内に入力ファイルと同じファイル名で出力。プルダウンで出力フォーマット判定。
         */
        try {
            String outputFilename = new String();
            String outputDirectory = locationOutputDir.getText();
            final String ext = getFileExtension(outputDirectory);
            StoreType storeType;
            if (ext.isEmpty()) {    // ディレクトリ
                createDirectory(outputDirectory);
                storeType = getStoreType(formatSelector.getValue());
            } else {                // ファイル名
                outputFilename = outputDirectory;
                outputDirectory = new String();
                storeType = getStoreType(ext);
            }
            /**
             * 入力ファイル数を処理
             * 処理はスレッドに依頼して実行ボタン処理は終了させる。
             */
            Task task = createTask(storeType, csvInputFiles, outputDirectory, outputFilename);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(task);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
