package shinyhunttracker;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import javafx.scene.text.TextAlignment;
import javafx.stage.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import static shinyhunttracker.CustomWindowElements.*;

//                                                                                                  | ADDED BY: real-eo
import shinyhunttracker.GlobalKeyListener;
//                                                                                                  | END

public class HuntController {
    static Stage huntControls = new Stage();
    static ScrollPane huntControlsScroll = new ScrollPane();
    static GridPane huntControlsLayout = new GridPane();

    static MenuItem previouslyCaught = new MenuItem("Previously Caught Window Settings");
    static MenuItem editSavedHunts = new MenuItem("Edit Saved Hunts");

    static Vector<HuntWindow> windowsList = new Vector<>();

    /**
     * Creates the Hunt Controller and
     * opens hunts from previous session
     */
    public static void createHuntController(){
        //Initial Hunt Controller setup
        huntControls.setTitle("Hunt Controller");
        huntControlsLayout.setAlignment(Pos.CENTER);
        huntControlsLayout.setVgap(10);
        huntControlsLayout.setHgap(10);
        huntControlsLayout.setPadding(new Insets(10, 15, 10, 15));

        if(huntControls.getScene() == null)
            huntControls.initStyle(StageStyle.UNDECORATED);

        huntControlsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        huntControlsScroll.setContent(huntControlsLayout);
        huntControlsScroll.setId("background");

        Label startHuntLabel = new Label("Press + to add a new hunt");
        startHuntLabel.setAlignment(Pos.CENTER);
        startHuntLabel.setMinWidth(250);
        ChangeListener<Number> resizeListener = (observableValue, o, t1) -> {
            //Sets stage size when there are hunts
            if(huntControlsLayout.getHeight() < 25){
                huntControls.setHeight(100);
                huntControls.setWidth(300);
                if(huntControlsLayout.getChildren().size() == 0)
                    huntControlsLayout.add(startHuntLabel, 0, 0);
                return;
            }

            //caps window height at 540
            if(huntControlsLayout.getHeight() + 70 <= 540) {
                huntControls.setHeight(huntControlsLayout.getHeight() + 70);
                huntControls.setWidth(huntControlsLayout.getWidth() + 5);
            }
            else {
                huntControls.setHeight(540);
                huntControls.setWidth(huntControlsLayout.getWidth() + 5);
            }
        };

        huntControlsLayout.heightProperty().addListener(resizeListener);
        huntControlsLayout.widthProperty().addListener(resizeListener);

        //add hunt and general settings buttons
        Button addHunt = new Button();
        addHunt.setFocusTraversable(false);
        ImageView addHuntIcon = new ImageView(new Image("file:Images/plus.png"));
        addHunt.setPadding(new Insets(0, 0, 0, 0));
        addHunt.setGraphic(addHuntIcon);
        addHunt.setMinSize(25, 25);
        addHunt.setTooltip(new Tooltip("Add Hunt"));

        //Settings that apply to none or all of the hunts
        MenuButton masterSettings = new MenuButton();
        masterSettings.setFocusTraversable(false);
        ImageView settingsIcon = new ImageView(new Image("file:Images/gear.png"));
        masterSettings.setPadding(new Insets(0, 0, 0, 0));
        masterSettings.setGraphic(settingsIcon);
        masterSettings.setMinSize(30, 25);
        MenuItem keyBinding = new MenuItem("Key Bind Settings");

        masterSettings.setTooltip(new Tooltip("General Settings"));
        masterSettings.getItems().addAll(editSavedHunts, keyBinding, previouslyCaught);

        try {
            //Read JSON file
            JSONArray caughtList = new JSONArray(new JSONTokener(new FileInputStream("SaveData/caughtPokemon.json")));
            JSONArray prevHuntsList = new JSONArray(new JSONTokener(new FileInputStream("SaveData/previousHunts.json")));

            //hides settings if they lead to empty windows
            previouslyCaught.setVisible(caughtList.length() > 0);
            editSavedHunts.setVisible(prevHuntsList.length() > 0);
        }catch (IOException e) {
            e.printStackTrace();
        }

        //puts addHunt and masterSettings to bottom left and right respectively
        BorderPane masterButtonsPane = new BorderPane();
        masterButtonsPane.setRight(addHunt);
        masterButtonsPane.setLeft(masterSettings);
        masterButtonsPane.setPadding(new Insets(0,5,5,5));

        //Puts controls at the top, hunts in the middle, and master buttons at the bottom
        BorderPane huntControlsLayout = new BorderPane();
        huntControlsLayout.setFocusTraversable(false);
        huntControlsLayout.setTop(titleBar(huntControls));
        huntControlsLayout.setCenter(huntControlsScroll);
        huntControlsLayout.setBottom(masterButtonsPane);
        huntControlsLayout.setId("background");

        Scene huntControlsScene = new Scene(huntControlsLayout, 300, 100);
        huntControlsScene.getStylesheets().add("file:shinyTracker.css");
        huntControls.setScene(huntControlsScene);
        huntControls.show();

        windowsList.clear();

        //Check to see if there were hunts open when the hunt controller was last closed
        try {
            //reads the number of hunts that were open when the program was last closed
            FileInputStream reader = new FileInputStream("SaveData/previousSession.dat");
            DataInputStream previousSessionData = new DataInputStream(reader);
            int previousHuntsNum = previousSessionData.read();

            //Read JSON file
            JSONArray huntList = new JSONArray(new JSONTokener(new FileInputStream("SaveData/previousHunts.json")));

            //Load the last n hunts in the json
            if(huntList.length() != 0)
                for(int i = 1; i <= previousHuntsNum; i++)
                    SaveData.loadHunt(huntList.length() - i);
        }catch (IOException ignored) {

        }

        if(windowsList.size() == 0)
            refreshMiscWindows();

        editSavedHunts.setOnAction(e -> editSavedHuntsWindow());
        keyBinding.setOnAction(e -> keyBindingSettings());
        previouslyCaught.setOnAction(e -> PreviouslyCaught.previouslyCaughtPokemonSettings());

        
        //Listener for KeyBinds
        // ! WARNING: Brainlet code ahead 💩💩💩
        // huntControlsScene.setOnKeyPressed(e -> {
        //     for(int i = windowsList.size() - 1; i >= 0 ; i--){
        //         if (windowsList.get(i).getKeyBinding() == e.getCode())
        //             windowsList.get(i).incrementEncounters();
        //         saveHuntOrder();
        //     }
        // });
        // !

        //                                                                                                  | ADDED BY: real-eo
        // Start global key listener for handling input 
        GlobalKeyListener.getInstance().startListening();
        //                                                                                                  | END
            
        //Opens pop up for Selection Window
        addHunt.setOnAction(e -> {
            //check save data file for previous saves
            //if anything is found, ask the user if they would like to start a new hunt or a previous one
            try {
                //Read JSON file
                JSONArray huntList = new JSONArray(new JSONTokener(new FileInputStream("SaveData/previousHunts.json")));

                //If there are no previous hunts, it goes straight to selection page
                if(huntList.length() > 0 && windowsList.size() < huntList.length())
                    newOrOld();
                else
                    HuntSelection.createHuntSelection();
            }catch(IOException f){
                HuntSelection.createHuntSelection();
            }
        });

        //Make window draggable
        makeDraggable(huntControlsScene);

        // ! I'm not convinced this code handles closing the program in a good matter at ALL 😭🙏🙏
        //Makes sure to save all currently open hunts before closing
        huntControls.setOnCloseRequest(e -> {
            //                                                                                                  | ADDED BY: real-eo
            GlobalKeyListener.getInstance().stopListening();
            //                                                                                                  | END

            System.exit(0);
        });
    }

    /**
     * Adds a new HuntWindow to the windowsList
     * @param newWindow HuntWindow with new hunt information
     */
    public static void addHunt(HuntWindow newWindow){
        editSavedHunts.setVisible(true);
        if(windowsList.size() == 0)
            huntControlsLayout.getChildren().clear();

        //Set huntNum to first available
        int huntNum = 0;
        for (; huntNum < windowsList.size(); ++huntNum)
            if (windowsList.get(huntNum).getHuntNumber() != huntNum + 1)
                break;
        newWindow.setHuntNumber(huntNum + 1);
        windowsList.add(huntNum, newWindow);

        refreshHunts();
    }

    /**
     * Adds appropriate elements to controller with new hunt's information
     * @param newWindow HuntWindow with new hunt information
     */
    public static void addHuntControls(HuntWindow newWindow){
        editSavedHunts.setVisible(true);

        //Add hunt Labels to controller
        newWindow.getStage().setTitle("Hunt " + newWindow.getHuntNumber());

        int row = -1;
        for(int i = 0; i < windowsList.size(); i++) {
            if (newWindow.getHuntNumber() < windowsList.get(i).getHuntNumber()) {
                row = i - 1;
                break;
            }
        }
        if(row == -1)
            row = windowsList.size() - 1;

        //Hunt Information to add to controller vbox
        Button exitHuntButton = new Button();
        exitHuntButton.setFocusTraversable(false);
        ImageView exitHuntIcon = new ImageView(new Image("file:Images/x.png"));
        exitHuntButton.setPadding(new Insets(0, 0, 0, 0));
        exitHuntButton.setGraphic(exitHuntIcon);
        exitHuntButton.setTooltip(new Tooltip("Close Hunt"));
        exitHuntButton.setFocusTraversable(false);
        exitHuntButton.setMinSize(25, 25);
        huntControlsLayout.add(exitHuntButton, 0, row);

        Label huntNumberLabel = new Label(String.valueOf(newWindow.getHuntNumber()));
        huntNumberLabel.setAlignment(Pos.CENTER);
        GridPane.setHalignment(huntNumberLabel, HPos.CENTER);
        GridPane.setValignment(huntNumberLabel, VPos.CENTER);
        huntControlsLayout.add(huntNumberLabel, 1, row);

        Button incrementButton = new Button();
        incrementButton.setFocusTraversable(false);
        ImageView incrementIcon = new ImageView(new Image("file:Images/increment.png"));
        incrementButton.setPadding(new Insets(0, 0, 0, 0));
        incrementButton.setGraphic(incrementIcon);
        incrementButton.setTooltip(new Tooltip("+ Encounters"));
        incrementButton.setMinSize(25, 12);

        Button decrementButton = new Button();
        decrementButton.setFocusTraversable(false);
        ImageView decrementIcon = new ImageView(new Image("file:Images/decrement.png"));
        decrementButton.setPadding(new Insets(0, 0, 0, 0));
        decrementButton.setGraphic(decrementIcon);
        decrementButton.setTooltip(new Tooltip("- Encounters"));
        decrementButton.setMinSize(25, 12);

        VBox encountersControl = new VBox();
        encountersControl.setSpacing(1);
        encountersControl.setMaxHeight(25);
        encountersControl.setAlignment(Pos.CENTER);
        encountersControl.getChildren().addAll(incrementButton, decrementButton);

        huntControlsLayout.add(encountersControl, 2, row);

        Label nameLabel = new Label(newWindow.getPokemon().getName());
        GridPane.setHalignment(nameLabel, HPos.CENTER);
        GridPane.setValignment(nameLabel, VPos.CENTER);
        huntControlsLayout.add(nameLabel, 3, row);

        Label encounterLabel = new Label();
        GridPane.setHalignment(encounterLabel, HPos.CENTER);
        GridPane.setValignment(encounterLabel, VPos.CENTER);
        encounterLabel.textProperty().bind(Bindings.createStringBinding(() -> String.format("%,d", newWindow.encounterProperty().getValue()), newWindow.encounterProperty()));
        huntControlsLayout.add(encounterLabel, 4, row);

        Button caughtButton = new Button();
        caughtButton.setFocusTraversable(false);
        ImageView caughtIcon = new ImageView(new Image("file:Images/caught.png"));
        caughtButton.setPadding(new Insets(0, 0, 0, 0));
        caughtButton.setGraphic(caughtIcon);
        caughtButton.setTooltip(new Tooltip("Caught"));
        caughtButton.setMinSize(25, 25);
        huntControlsLayout.add(caughtButton, 5, row);

        Button popOutButton = new Button();
        popOutButton.setFocusTraversable(false);
        ImageView popOutIcon = new ImageView(new Image("file:Images/popout.png"));
        popOutButton.setPadding(new Insets(0, 0, 0, 0));
        popOutButton.setGraphic(popOutIcon);
        popOutButton.setTooltip(new Tooltip("Hunt Window Popout"));
        popOutButton.setMinSize(25, 25);
        popOutButton.setVisible(!newWindow.isShowing());

        Button windowSettingsButton = new Button();
        windowSettingsButton.setFocusTraversable(false);
        ImageView windowSettingsIcon = new ImageView(new Image("file:Images/popoutSettings.png"));
        windowSettingsButton.setPadding(new Insets(0, 0, 0, 0));
        windowSettingsButton.setGraphic(windowSettingsIcon);
        windowSettingsButton.setTooltip(new Tooltip("Hunt Window Settings"));
        windowSettingsButton.setMinSize(25, 25);
        windowSettingsButton.setVisible(newWindow.isShowing());

        StackPane windowPopout = new StackPane();
        windowPopout.getChildren().addAll(popOutButton, windowSettingsButton);
        huntControlsLayout.add(windowPopout, 6, row);

        MenuButton settingsButton = new MenuButton();
        settingsButton.setFocusTraversable(false);
        ImageView settingsIcon = new ImageView(new Image("file:Images/gear.png"));
        settingsButton.setPadding(new Insets(0, 0, 0, 0));
        settingsButton.setGraphic(settingsIcon);
        settingsButton.setMinSize(30, 25);
        settingsButton.setTooltip(new Tooltip("Hunt Settings"));
        MenuItem increment= new MenuItem("Change Increment");
        MenuItem resetEncounters = new MenuItem("Fail");
        MenuItem phaseHunt = new MenuItem("Phase");
        MenuItem DVTable = new MenuItem("DV Table");

        settingsButton.getItems().addAll(increment, resetEncounters, phaseHunt);
        if(newWindow.getGame().getGeneration() == 1 && newWindow.getPokemon().getBase() != null)
            settingsButton.getItems().add(DVTable);

        huntControlsLayout.add(settingsButton, 7, row);

        Button helpButton = new Button();
        helpButton.setFocusTraversable(false);
        ImageView helpIcon = new ImageView(new Image("file:Images/questionmark.png"));
        helpButton.setPadding(new Insets(0, 0, 0, 0));
        helpButton.setGraphic(helpIcon);
        helpButton.setTooltip(new Tooltip("Info"));
        helpButton.setMinSize(25, 25);
        huntControlsLayout.add(helpButton, 8, row);

        //Saves the order of window list
        saveHuntOrder();

        //Set keybinds
        try {
            //Read JSON file
            JSONArray keybindList = new JSONArray(new JSONTokener(new FileInputStream("SaveData/keybinds.json")));

            newWindow.setKeybind(KeyCode.valueOf(keybindList.get(newWindow.getHuntNumber() - 1).toString()));
        }catch(IOException  e){
            e.printStackTrace();
        }

        exitHuntButton.setOnAction(e -> {
            updatePreviousSessionDat(-1);
            newWindow.close();
            windowsList.remove(newWindow);
            saveHuntOrder();
            refreshHunts();
        });

        incrementButton.setOnAction(e -> {
            newWindow.incrementEncounters();
            saveHuntOrder();
        });

        decrementButton.setOnAction(e -> {
            newWindow.decrementEncounters();
            saveHuntOrder();
        });

        caughtButton.setOnAction(e -> {
            updatePreviousSessionDat(-1);
            newWindow.pokemonCaught();
            windowsList.remove(newWindow);
            previouslyCaught.setVisible(true);
            PreviouslyCaught.updateSelection();
            saveHuntOrder();
            refreshHunts();
        });

        popOutButton.setOnAction(e -> {
            popOutButton.setVisible(false);
            windowSettingsButton.setVisible(true);
            if(newWindow.getScene().getChildren().size() == 0)
                newWindow.createHuntWindow();
            else
                newWindow.getStage().show();

            huntControls.toFront();
        });

        increment.setOnAction(e -> {
            TextInputDialog changeIncrementDialog = new TextInputDialog(String.valueOf(newWindow.getIncrement()));
            changeIncrementDialog.setHeaderText("Increment encounters by: ");
            changeIncrementDialog.getDialogPane().getStylesheets().add("file:shinyTracker.css");
            makeDraggable(changeIncrementDialog.getDialogPane().getScene());
            changeIncrementDialog.initStyle(StageStyle.UNDECORATED);
            changeIncrementDialog.showAndWait().ifPresent(response -> {
                try{
                    newWindow.setIncrement(Integer.parseInt(response.replaceAll(",", "")));
                }catch(NumberFormatException ignored){

                }
            });
        });
        resetEncounters.setOnAction(e -> newWindow.resetEncounters());
        phaseHunt.setOnAction(e -> {
            ChoiceDialog<Pokemon> phaseDialog = new ChoiceDialog<>();
            phaseDialog.getDialogPane().getStylesheets().add("file:shinyTracker.css");
            makeDraggable(phaseDialog.getDialogPane().getScene());
            phaseDialog.initStyle(StageStyle.UNDECORATED);
            try {
                JSONArray pokemonListJSON = new JSONArray(new JSONTokener(new FileInputStream("GameData/pokemon.json")));

                for (int i = 0; i < pokemonListJSON.length(); i++)
                    phaseDialog.getItems().add(new Pokemon(pokemonListJSON.getJSONObject(i), i));
            } catch (IOException f) {
                f.printStackTrace();
            }
            phaseDialog.setTitle("Phase Hunt");
            phaseDialog.setHeaderText("Phased Pokemon: ");
            phaseDialog.showAndWait().ifPresent(response -> {
                newWindow.phaseHunt(response.getDexNumber());
                PreviouslyCaught.updateSelection();
                previouslyCaught.setVisible(true);
            });
        });

        DVTable.setOnAction(e -> generateDVTable(newWindow.getPokemon()));

        windowSettingsButton.setOnAction(e -> newWindow.customizeHuntWindowSettings());

        helpButton.setOnAction(e -> {
            Alert huntInformation = new Alert(Alert.AlertType.INFORMATION);
            huntInformation.initModality(Modality.NONE);
            huntInformation.setTitle("Hunt Information");
            huntInformation.setHeaderText(null);
            huntInformation.initStyle(StageStyle.UNDECORATED);
            huntInformation.getDialogPane().getStylesheets().add("file:shinyTracker.css");
            makeDraggable(huntInformation.getDialogPane().getScene());

            VBox dialogLayout = new VBox();
            dialogLayout.setSpacing(10);
            dialogLayout.setPadding(new Insets(10));
            Label huntInfoLabel = new Label("Pokemon: " + newWindow.getPokemon().getName() + "\n" +
                                                "Game: " + newWindow.getGame().getName() + "\n" +
                                                "Method: " + newWindow.getMethod().getName() + "\n\n");

            Label methodInfoLabel = new Label("Method Info: \n" + newWindow.getMethod().getMethodInfo());
            methodInfoLabel.setMaxWidth(275);
            methodInfoLabel.setWrapText(true);

            dialogLayout.getChildren().addAll(huntInfoLabel, methodInfoLabel);

            Pane masterPane = new Pane();
            masterPane.getChildren().add(dialogLayout);

            VBox resourceSection = new VBox();
            Label methodResourcesLabel = new Label("Resources:");
            resourceSection.getChildren().add(methodResourcesLabel);
            for(int i = 0; i < newWindow.getMethod().getResources().get(0).size(); i++){
                Hyperlink link = new Hyperlink(newWindow.getMethod().getResources().get(0).get(i));
                resourceSection.getChildren().add(link);

                String url = newWindow.getMethod().getResources().get(1).get(i);
                link.setOnAction(f -> {
                    try {
                        Desktop.getDesktop().browse(new URL(url).toURI());
                    } catch (IOException | URISyntaxException g) {
                        g.printStackTrace();
                    }
                });
            }
            dialogLayout.getChildren().add(resourceSection);

            huntInformation.getDialogPane().setContent(masterPane);
            huntInformation.show();
            huntInformation.setHeight(dialogLayout.getHeight() + 33);
            huntInformation.setWidth(360);
        });

        newWindow.getStage().setOnCloseRequest(e -> {
            e.consume();
            popOutButton.setVisible(true);
            windowSettingsButton.setVisible(false);
            newWindow.close();
        });
    }

    /**
     * Prompts the user if they would like to continue an old hunt, or start a new one
     */
    public static void newOrOld(){
        Stage promptStage = new Stage();
        promptStage.setTitle("Hunt Prompt");
        promptStage.initStyle(StageStyle.UNDECORATED);

        Label prompt = new Label("Would you like to continue a previous hunt or\nstart a new one?");
        prompt.setTextAlignment(TextAlignment.CENTER);
        Button continuePrevious = new Button("Continue Previous Hunt");
        Button newHunt = new Button("Start New Hunt");

        VBox loadLayout = new VBox();
        loadLayout.setId("background");
        loadLayout.getChildren().addAll(titleBar(promptStage), prompt, continuePrevious, newHunt);
        loadLayout.setSpacing(10);
        loadLayout.setAlignment(Pos.TOP_CENTER);

        Scene loadScene = new Scene(loadLayout, 275, 155);
        loadScene.getStylesheets().add("file:shinyTracker.css");
        promptStage.setTitle("Load previous save");
        promptStage.setResizable(false);
        promptStage.setScene(loadScene);
        makeDraggable(loadScene);
        promptStage.show();

        //show the previously created Selection Page Window in order of last used
        newHunt.setOnAction(e -> {
            //creates selection page window
            HuntSelection.createHuntSelection();

            //closes prompt window
            prevHuntsStage.close();
            promptStage.close();
        });

        //if they would like to continue, show a list of the previous hunts found on the file
        continuePrevious.setOnAction(e -> {
            loadSavedHuntsWindow();
            promptStage.close();
        });
    }

    /**
     * Creates a window of previously saved hunts for the user to load from
     */
    static Stage prevHuntsStage = new Stage();
    public static void loadSavedHuntsWindow(){
        prevHuntsStage.setTitle("Previous Hunts");
        //creates a grid to load the hunts into
        GridPane previousHunts = new GridPane();
        previousHunts.setPadding(new Insets(10, 10, 10, 10));
        previousHunts.setHgap(10);
        previousHunts.setVgap(5);

        previousHunts.heightProperty().addListener((o, oldVal, newVal) -> {
            //caps window height at 540
            if(previousHunts.getHeight() + 35 <= 540) {
                prevHuntsStage.setHeight(previousHunts.getHeight() + 35);
                prevHuntsStage.setWidth(previousHunts.getWidth());
            }
            else {
                prevHuntsStage.setHeight(540);
                prevHuntsStage.setWidth(previousHunts.getWidth() + 10);
            }
        } );

        try {
            //Read JSON file
            JSONArray huntList = new JSONArray(new JSONTokener(new FileInputStream("SaveData/previousHunts.json")));

            //Loops through previously saved hunts
            for(int i = huntList.length() - 1; i >= 0; i--){
                JSONObject huntData = (JSONObject) huntList.get(i);
                //Doesn't add hunt data to the grid if the hunt is already open
                boolean newData = true;
                for(HuntWindow j : windowsList)
                    if(Integer.parseInt(huntData.get("huntID").toString()) == j.getHuntID())
                        newData = false;

                if(newData) {
                    Pokemon pokemon = new Pokemon(Integer.parseInt(huntData.get("pokemon").toString()));
                    Game game = new Game(Integer.parseInt(huntData.get("game").toString()));
                    Method method = new Method(Integer.parseInt(huntData.get("method").toString()));

                    int row = previousHunts.getRowCount();
                    Label pokemonLabel = new Label(pokemon.getName());
                    GridPane.setHalignment(pokemonLabel, HPos.CENTER);
                    GridPane.setValignment(pokemonLabel, VPos.CENTER);
                    previousHunts.add(pokemonLabel, 0, row);

                    Label gameLabel = new Label(game.getName());
                    GridPane.setHalignment(gameLabel, HPos.CENTER);
                    GridPane.setValignment(gameLabel, VPos.CENTER);
                    previousHunts.add(gameLabel, 1, row);

                    Label methodLabel = new Label(method.getName());
                    GridPane.setHalignment(methodLabel, HPos.CENTER);
                    GridPane.setValignment(methodLabel, VPos.CENTER);
                    previousHunts.add(methodLabel, 2, row);

                    Label encounters = new Label(String.format("%,2d", Integer.parseInt(huntData.get("encounters").toString())));
                    GridPane.setHalignment(encounters, HPos.CENTER);
                    GridPane.setValignment(encounters, VPos.CENTER);
                    previousHunts.add(encounters, 3, row);

                    Button loadButton = new Button("Load");
                    GridPane.setHalignment(loadButton, HPos.CENTER);
                    GridPane.setValignment(loadButton, VPos.CENTER);
                    previousHunts.add(loadButton, 4, row);

                    Button delete = new Button("Delete");
                    GridPane.setHalignment(delete, HPos.CENTER);
                    GridPane.setValignment(delete, VPos.CENTER);
                    previousHunts.add(delete, 5, row);

                    int index = i;
                    loadButton.setOnAction(f -> {
                        //loads hunt and refreshes the controller
                        updatePreviousSessionDat(1);
                        SaveData.loadHunt(index);
                    });

                    //removes hunt i
                    delete.setOnAction(e -> {
                        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmAlert.setContentText("Are you sure you would like to delete " + pokemon.getName() + " hunt?");
                        confirmAlert.initStyle(StageStyle.UNDECORATED);
                        confirmAlert.getDialogPane().getStylesheets().add("file:shinyTracker.css");
                        makeDraggable(confirmAlert.getDialogPane().getScene());
                        confirmAlert.showAndWait().ifPresent(f -> {
                            if(!f.getButtonData().toString().equals("OK_DONE"))
                                return;

                            SaveData.removeHunt(index);
                            try {
                                JSONArray checkPrevious = new JSONArray(new JSONTokener(new FileInputStream("SaveData/previousHunts.json")));
                                editSavedHunts.setVisible(checkPrevious.length() > 0);
                            } catch (FileNotFoundException ex) {
                                ex.printStackTrace();
                            }

                            int huntID = Integer.parseInt(huntData.get("huntID").toString());
                            for(int j = 0; j < windowsList.size(); j++)
                                if(windowsList.get(j).getHuntID() == huntID) {
                                    updatePreviousSessionDat(-1);
                                    windowsList.remove(j);
                                    break;
                                }
                            refreshHunts();
                        });
                    });
                }
            }

            if(previousHunts.getRowCount() == 0) {
                prevHuntsStage.close();
                return;
            }
        }catch (IOException f) {
            f.printStackTrace();
        }

        ScrollPane parentPane = new ScrollPane();
        parentPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        parentPane.setId("background");
        parentPane.setContent(previousHunts);

        VBox masterLayout = new VBox();
        masterLayout.setId("background");
        masterLayout.getChildren().addAll(titleBar(prevHuntsStage), parentPane);

        Scene previousHuntsScene = new Scene(masterLayout, 0, 0);
        previousHuntsScene.getStylesheets().add("file:shinyTracker.css");

        if(prevHuntsStage.getScene() == null)
            prevHuntsStage.initStyle(StageStyle.UNDECORATED);

        prevHuntsStage.setScene(previousHuntsScene);
        makeDraggable(previousHuntsScene);
        prevHuntsStage.show();
    }

    /**
     * Creates a window of previously saved hunts for the user to edit hunt information
     */
    static Stage editHunts = new Stage();
    public static void editSavedHuntsWindow(){
        editHunts.setTitle("Edit Saved Hunts");
        //Grid pane to add all hunts into
        GridPane editHuntsLayout = new GridPane();
        editHuntsLayout.setPadding(new Insets(10, 10, 10, 10));
        editHuntsLayout.setHgap(10);
        editHuntsLayout.setVgap(5);

        editHuntsLayout.heightProperty().addListener((o, oldValue, newValue) -> {
            if(editHuntsLayout.getHeight() + 35 <= 540) {
                editHunts.setHeight(editHuntsLayout.getHeight() + 35);
                editHunts.setWidth(editHuntsLayout.getWidth());
            }
            else {
                editHunts.setHeight(540);
                editHunts.setWidth(editHuntsLayout.getWidth() + 10);
            }
        });

        try {
            JSONArray huntList = new JSONArray(new JSONTokener(new FileInputStream("SaveData/previousHunts.json")));

            if(huntList.length() == 0) {
                editHunts.close();
                return;
            }

            //loops through all saved hunts
            for(int i = huntList.length() - 1; i >= 0; i--){
                JSONObject huntData = (JSONObject) huntList.get(i);
                Pokemon huntPokemon = new Pokemon(Integer.parseInt(huntData.get("pokemon").toString()));
                Game huntGame = new Game(Integer.parseInt(huntData.get("game").toString()));
                Method huntMethod = new Method(Integer.parseInt(huntData.get("method").toString()));

                Button pokemonButton = new Button(huntPokemon.getName());
                GridPane.setHalignment(pokemonButton, HPos.CENTER);
                GridPane.setValignment(pokemonButton, VPos.CENTER);
                editHuntsLayout.add(pokemonButton, 0, i);

                Button gameButton = new Button(huntGame.getName());
                GridPane.setHalignment(gameButton, HPos.CENTER);
                GridPane.setValignment(gameButton, VPos.CENTER);
                editHuntsLayout.add(gameButton, 1, i);

                Button methodButton = new Button(huntMethod.getName());
                GridPane.setHalignment(methodButton, HPos.CENTER);
                GridPane.setValignment(methodButton, VPos.CENTER);
                editHuntsLayout.add(methodButton, 2, i);

                Button encountersButton = new Button(String.format("%,d", Integer.parseInt(huntData.get("encounters").toString())));
                GridPane.setHalignment(encountersButton, HPos.CENTER);
                GridPane.setValignment(encountersButton, VPos.CENTER);
                editHuntsLayout.add(encountersButton, 3, i);

                int index = i;

                //Creates a drop-down for user to select a new pokemon and updates the information
                pokemonButton.setOnAction(f -> {
                    ChoiceDialog<Pokemon> pokemonChoiceDialog = new ChoiceDialog<>();
                    pokemonChoiceDialog.setHeaderText("Select New Pokemon");
                    pokemonChoiceDialog.initStyle(StageStyle.UNDECORATED);
                    makeDraggable(pokemonChoiceDialog.getDialogPane().getScene());
                    pokemonChoiceDialog.getDialogPane().getStylesheets().add("file:shinyTracker.css");
                    try {
                        JSONArray gameList = new JSONArray(new JSONTokener(new FileInputStream("GameData/pokemon.json")));

                        for (int j = 0; j < gameList.length(); j++)
                            pokemonChoiceDialog.getItems().add(new Pokemon(gameList.getJSONObject(j), j));
                    } catch (IOException g) {
                        g.printStackTrace();
                    }
                    pokemonChoiceDialog.setSelectedItem(huntPokemon);
                    pokemonChoiceDialog.showAndWait().ifPresent(g -> {
                        huntData.put("pokemon", pokemonChoiceDialog.getSelectedItem().getDexNumber());
                        huntData.put("pokemon_form", 0);
                        SaveData.updateHunt(index, huntData);
                        createHuntController();
                    });
                });

                //Creates a drop-down for user to select a new game and updates the information
                gameButton.setOnAction(f -> {
                    ChoiceDialog<Game> gameChoiceDialog = new ChoiceDialog<>();
                    gameChoiceDialog.setHeaderText("Select New Game");
                    gameChoiceDialog.initStyle(StageStyle.UNDECORATED);
                    makeDraggable(gameChoiceDialog.getDialogPane().getScene());
                    gameChoiceDialog.getDialogPane().getStylesheets().add("file:shinyTracker.css");
                    try {
                        JSONArray gameList = new JSONArray(new JSONTokener(new FileInputStream("GameData/game.json")));

                        for (int j = 0; j < gameList.length(); j++)
                            gameChoiceDialog.getItems().add(new Game(gameList.getJSONObject(j), j));
                    } catch (IOException g) {
                        g.printStackTrace();
                    }
                    gameChoiceDialog.setSelectedItem(huntGame);
                    gameChoiceDialog.showAndWait().ifPresent(g -> {
                        huntData.put("game", gameChoiceDialog.getSelectedItem().getId());
                        huntData.put("game_mods", new JSONArray());
                        SaveData.updateHunt(index, huntData);
                        createHuntController();
                    });
                });

                //Creates a drop-down for user to select a new method and updates the information
                methodButton.setOnAction(f -> {
                    ChoiceDialog<Method> methodChoiceDialog = new ChoiceDialog<>();
                    methodChoiceDialog.setHeaderText("Select New Method");
                    methodChoiceDialog.initStyle(StageStyle.UNDECORATED);
                    makeDraggable(methodChoiceDialog.getDialogPane().getScene());
                    methodChoiceDialog.getDialogPane().getStylesheets().add("file:shinyTracker.css");
                    try {
                        JSONArray gameList = new JSONArray(new JSONTokener(new FileInputStream("GameData/method.json")));

                        for (int j = 0; j < gameList.length(); j++)
                            methodChoiceDialog.getItems().add(new Method(gameList.getJSONObject(j), j));
                    } catch (IOException g) {
                        g.printStackTrace();
                    }
                    methodChoiceDialog.setSelectedItem(huntMethod);
                    methodChoiceDialog.showAndWait().ifPresent(g -> {
                        huntData.put("method", methodChoiceDialog.getSelectedItem().getId());
                        SaveData.updateHunt(index, huntData);
                        createHuntController();
                    });
                });

                //Creates a text field for the user to enter a new amount of encounters and updates the information
                encountersButton.setOnAction(f -> {
                    TextInputDialog encountersDialog = new TextInputDialog();
                    encountersDialog.setHeaderText("Input New Encounters");
                    encountersDialog.initStyle(StageStyle.UNDECORATED);
                    makeDraggable(encountersDialog.getDialogPane().getScene());
                    encountersDialog.getDialogPane().getStylesheets().add("file:shinyTracker.css");
                    encountersDialog.showAndWait().ifPresent(g -> {
                        huntData.put("encounters", Integer.parseInt(encountersDialog.getEditor().getText().replaceAll(",", "")));
                        SaveData.updateHunt(index, huntData);
                        createHuntController();
                    });
                });
            }

            ScrollPane parentPane = new ScrollPane();
            parentPane.setId("background");
            parentPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            parentPane.setContent(editHuntsLayout);

            VBox masterLayout = new VBox();
            masterLayout.setId("background");
            masterLayout.getChildren().addAll(titleBar(editHunts), parentPane);

            Scene huntListScene = new Scene(masterLayout, 1000, 400);
            huntListScene.getStylesheets().add("file:shinyTracker.css");

            if(editHunts.getScene() == null)
                editHunts.initStyle(StageStyle.UNDECORATED);

            editHunts.setScene(huntListScene);
            makeDraggable(huntListScene);
            editHunts.show();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Pop up listing the keys that the hunts are bound too and allows the user to change them
     */
    static Stage keyBindingSettingsStage = new Stage();
    public static void keyBindingSettings(){
        keyBindingSettingsStage.setTitle("Key Bind Settings");
        VBox keyBindingsLayout = new VBox();
        keyBindingsLayout.setAlignment(Pos.CENTER);
        keyBindingsLayout.setSpacing(10);
        keyBindingsLayout.setPadding(new Insets(10, 10, 10, 10));

        keyBindingsLayout.heightProperty().addListener((o, oldValue, newValue) -> {
            if(keyBindingsLayout.getHeight() + 45 <= 540) {
                keyBindingSettingsStage.setHeight(keyBindingsLayout.getHeight() + 45);
                keyBindingSettingsStage.setWidth(keyBindingsLayout.getWidth());
            }
            else {
                keyBindingSettingsStage.setHeight(540);
                keyBindingSettingsStage.setWidth(keyBindingsLayout.getWidth() + 10);
            }
        });

        try {
            //Read JSON file
            JSONArray keyBindsList = new JSONArray(new JSONTokener(new FileInputStream("SaveData/keyBinds.json")));

            //temporarily stores keys in order
            Vector<KeyCode> keyBindsTemp = new Vector<>();
            for (int i = 0; i < 5 || i < windowsList.size(); i++) {
                keyBindsTemp.add(KeyCode.valueOf((String) keyBindsList.get(i)));

                //adds text-field and label to layout
                HBox windowSettings = new HBox();
                windowSettings.setAlignment(Pos.CENTER);
                windowSettings.setSpacing(10);
                Label huntWindowLabel = new Label("Hunt " + (i + 1));
                TextField keyField = new TextField();
                keyField.setText(String.valueOf(keyBindsList.get(i)));
                keyField.setEditable(false);
                keyField.setMaxWidth(100);
                windowSettings.getChildren().addAll(huntWindowLabel, keyField);
                keyBindingsLayout.getChildren().add(windowSettings);

                //replaces the keybind in the temporary list
                int index = i;
                keyField.setOnKeyPressed(f -> {
                    keyField.setText(f.getCode().toString());
                    keyBindsTemp.set(index, f.getCode());
                    //                                                                                                  | ADDED BY: real-eo
                    GlobalKeyListener.Actions.warnIfBlacklistedKeybindUsed(f.getCode());   
                    //                                                                                                  | END
                });
            }

            //Allows user to save changes and closes window
            Button apply = new Button("Apply");
            keyBindingsLayout.getChildren().add(apply);

            apply.setOnAction(e->{
                try {
                    //updates the JSONArray updates the currently open hunts with the temporarily saved binds
                    for(int i = 0; i < keyBindsTemp.size(); i++) {
                        keyBindsList.put(i, keyBindsTemp.get(i).toString());
                        if(i < windowsList.size() && windowsList.get(i).getHuntNumber() == i + 1)
                            windowsList.get(i).setKeybind(keyBindsTemp.get(i));
                    }

                    //Write to file
                    FileWriter file = new FileWriter("SaveData/keyBinds.json");
                    file.write(keyBindsList.toString());
                    file.close();
                }catch(IOException f){
                    f.printStackTrace();
                }
                keyBindingSettingsStage.close();
            });
        }catch(IOException f){
            f.printStackTrace();
        }

        ScrollPane parentPane = new ScrollPane();
        parentPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        parentPane.setId("background");
        parentPane.setContent(keyBindingsLayout);

        VBox masterLayout = new VBox();
        masterLayout.setId("background");
        masterLayout.getChildren().addAll(CustomWindowElements.titleBar(keyBindingSettingsStage), parentPane);

        Scene keyBindingScene = new Scene(masterLayout, 250, 300);
        keyBindingScene.getStylesheets().add("file:shinyTracker.css");

        if(keyBindingSettingsStage.getScene() == null)
            keyBindingSettingsStage.initStyle(StageStyle.UNDECORATED);

        keyBindingSettingsStage.setScene(keyBindingScene);
        CustomWindowElements.makeDraggable(keyBindingScene);
        keyBindingSettingsStage.show();
    }

    /**
     * Refreshes hunt control window
     */
    public static void refreshHunts(){
        //Removes all hunts from window and list
        huntControlsLayout.getChildren().clear();
        for(HuntWindow i : windowsList)
            addHuntControls(i);
        //Refreshes all other windows
        refreshMiscWindows();
    }

    /**
     * Refreshes Load, Edit, and Key Binding Windows
     */
    public static void refreshMiscWindows(){
        if(keyBindingSettingsStage.isShowing())
            keyBindingSettings();
        if(prevHuntsStage.isShowing())
            loadSavedHuntsWindow();
        if(editHunts.isShowing())
            editSavedHuntsWindow();
        PreviouslyCaught.refreshMiscWindows();
    }

    /**
     * Writes the current hunts to the json in reverse order to list from most recent
     */
    public static void saveHuntOrder(){
        for(int i = windowsList.size() - 1; i >= 0 ; i--)
            SaveData.saveHunt(windowsList.get(i));
    }

    /**
     * Updates the number of hunts that are open at any given time
     * @param change amount to change the value saved in previousSession.dat
     */
    public static void updatePreviousSessionDat(int change){
        //saves current hunts to open next time the program is started
        try {
            OutputStream out = new FileOutputStream("SaveData/previousSession.dat");
            out.write(windowsList.size() + change);
            out.close();
        }catch (IOException f){
            f.printStackTrace();
        }
    }

    /**
     * Creates the window that displays the projected stats with shiny DVs
     * @param selectedPokemon pokemon that is going to have calculated IVs
     */
    public static void generateDVTable(Pokemon selectedPokemon){
        //Select level between 1-100
        ComboBox<Integer> levelSelect = new ComboBox<>();
        for(int i = 1; i <= 100; i++)
            levelSelect.getItems().add(i);
        levelSelect.getSelectionModel().select(49);

        //Labels for each stat
        VBox health = new VBox();
        health.setAlignment(Pos.TOP_CENTER);
        Label healthLabel = new Label("Health");
        healthLabel.setUnderline(true);
        Label healthifShiny = new Label();
        health.getChildren().addAll(healthLabel, healthifShiny);

        VBox attack = new VBox();
        attack.setAlignment(Pos.TOP_CENTER);
        Label attackLabel = new Label("Attack");
        attackLabel.setUnderline(true);
        Label attackifShiny = new Label();
        attack.getChildren().addAll(attackLabel, attackifShiny);

        VBox defense = new VBox();
        defense.setAlignment(Pos.TOP_CENTER);
        Label defenseLabel = new Label("Defense");
        defenseLabel.setUnderline(true);
        Label defenseifShiny = new Label();
        defense.getChildren().addAll(defenseLabel, defenseifShiny);

        VBox speed = new VBox();
        speed.setAlignment(Pos.TOP_CENTER);
        Label speedLabel = new Label("Speed");
        speedLabel.setUnderline(true);
        Label speedifShiny = new Label();
        speed.getChildren().addAll(speedLabel, speedifShiny);

        VBox special = new VBox();
        special.setAlignment(Pos.TOP_CENTER);
        Label specialLabel = new Label("Special");
        specialLabel.setUnderline(true);
        Label specialifShiny = new Label();
        special.getChildren().addAll(specialLabel, specialifShiny);

        updateStatLabels(healthifShiny, attackifShiny, defenseifShiny, speedifShiny, specialifShiny, 50, selectedPokemon);

        HBox statTable = new HBox();
        statTable.setAlignment(Pos.TOP_CENTER);
        statTable.setSpacing(10);
        statTable.getChildren().addAll(health, attack, defense, speed, special);

        Stage DVTableStage = new Stage();
        DVTableStage.initStyle(StageStyle.UNDECORATED);

        VBox DVTableLayout = new VBox();
        DVTableLayout.setAlignment(Pos.TOP_CENTER);
        DVTableLayout.setSpacing(10);
        DVTableLayout.setPadding(new Insets(10, 0, 0, 0));
        DVTableLayout.setId("background");
        DVTableLayout.getChildren().addAll(titleBar(DVTableStage), levelSelect, statTable);

        Scene DVTableScene = new Scene(DVTableLayout, 300, 230);
        DVTableScene.getStylesheets().add("file:shinyTracker.css");
        DVTableStage.setScene(DVTableScene);
        makeDraggable(DVTableScene);
        DVTableStage.setTitle(selectedPokemon.getName() + " Shiny DV Table");

        DVTableStage.show();

        //Updates labels with projected stats with given level
        levelSelect.setOnAction(e -> {
            int level = levelSelect.getSelectionModel().getSelectedItem();
            updateStatLabels(healthifShiny, attackifShiny, defenseifShiny, speedifShiny, specialifShiny, level, selectedPokemon);
        });
    }

    /**
     * Updates stat labels with projected shiny stats
     * @param health Health Label
     * @param attack Attack Label
     * @param defense Defense Label
     * @param speed Speed Label
     * @param special Special Label
     * @param level Pokemon Level
     * @param selectedPokemon Pokemon
     */
    public static void updateStatLabels(Label health, Label attack, Label defense, Label speed, Label special, int level, Pokemon selectedPokemon){
        int speedStat = selectedPokemon.getBase()[0];
        int healthStat = selectedPokemon.getBase()[1];
        int specialStat = selectedPokemon.getBase()[2];
        int attackStat = selectedPokemon.getBase()[3];
        int defenseStat = selectedPokemon.getBase()[4];

        speed.setText(calculateShinyStat(speedStat, 10, level, false) + "");

        health.setText(calculateShinyStat(healthStat, 0,level, true) + "\n"
                     + calculateShinyStat(healthStat, 8,level, true));

        special.setText(calculateShinyStat(specialStat, 10, level, false) + "");

        attack.setText(calculateShinyStat(attackStat, 2, level, false) + "\n"
                     + calculateShinyStat(attackStat, 3, level, false) + "\n"
                     + calculateShinyStat(attackStat, 6, level, false) + "\n"
                     + calculateShinyStat(attackStat, 7, level, false) + "\n"
                     + calculateShinyStat(attackStat, 10, level, false) + "\n"
                     + calculateShinyStat(attackStat, 11, level, false) + "\n"
                     + calculateShinyStat(attackStat, 14, level, false) + "\n"
                     + calculateShinyStat(attackStat, 15, level, false));

        defense.setText(calculateShinyStat(defenseStat, 10, level, false) + "");
    }

    /**
     * Calculates projected stat at given level and returns
     * @param base Base Stat of selected pokemon
     * @param dv Projected DV to calculate
     * @param level Pokemon level
     * @param isHealth Changes calculation if the stat is the health stat
     * @return calculated Stat
     */
    public static int calculateShinyStat(int base, int dv, int level, boolean isHealth){
        int health = isHealth ? 1 : 0;
        return (((base + dv) * 2 * level) / 100) + (level * health) + 5 + (5 * health);
    }
}
