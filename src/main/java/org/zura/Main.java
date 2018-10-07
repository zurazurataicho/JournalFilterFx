package org.zura.JournalFilter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
    //private static final String PROPERTIES_FILENAME = ".properties";
    private static final String PROPERTIES_FILENAME = "./setting.cfg";
    @Override
    public void start(Stage stage) {
        try {
            Properties prop = new Properties();
            prop.load(Files.newBufferedReader(Paths.get(PROPERTIES_FILENAME), StandardCharsets.UTF_8));
            // JavaFX
            stage.setTitle("NTFSジャーナルフィルタ");
/*
            FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemClassLoader().getResource("JournalFilter.fxml"));
            Parent root = loader.load();
            DirectoryController controller = (DirectoryController)loader.getController();
            controller.setProperties(prop);
            controller.setStage(stage);
*/
            // Scene Builder側でController classを設定していない場合
            FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemClassLoader().getResource("JournalFilter.fxml"));
            DirectoryController controller = new DirectoryController(stage, prop);
            loader.setController(controller);
            Parent root = loader.load();
            /**
            */
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        if (args.length == 0) {
            launch(args);
            return;
        }
        try {
            Properties prop = new Properties();
            prop.load(Files.newBufferedReader(Paths.get(PROPERTIES_FILENAME), StandardCharsets.UTF_8));
            if (args.length < 2) {
                System.out.println("入出力ファイル名を指定してください.");
                return;
            }
            String inFilename = args[0];
            String outFilename = args[1];

            Integer extPos = outFilename.lastIndexOf(".") + 1;
            String ext = outFilename.substring(extPos).toLowerCase();
            StoreType storeType = ext.equals("xlsx") ? StoreType.Xlsx : StoreType.Csv;
            String extText = storeType == StoreType.Xlsx ? "Excelファイル" : "CSVファイル";
            System.out.println("入力ファイル: " + inFilename);
            System.out.println("出力ファイル: " + outFilename + " (" + extText + ")");
            FilterManager c = new FilterManager(inFilename, outFilename, prop);
            c.run(storeType, prop.getProperty("PRESET_REGEX_OFFICE_FILE_EXTENSION_1"), prop.getProperty("PRESET_REGEX_EVENTINFO_1"));
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
