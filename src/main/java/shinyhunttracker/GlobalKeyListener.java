// NOTE: This entire source file was written by real-eo
package shinyhunttracker;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.RenderingHints.Key;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.input.KeyCode;

public class GlobalKeyListener implements NativeKeyListener {
    private static GlobalKeyListener instance;
    
    // * Debug flag for the global key listener implemented by Real
    // * This Debug is separate from the rest of the application
    protected static final boolean DEBUG_GLOBAL_KEY_LISTENER = true;

    private GlobalKeyListener() {
        // No mapping needed!
    }
    
    public static GlobalKeyListener getInstance() {
        if (instance == null) {
            instance = new GlobalKeyListener();
        }
        
        return instance;
    }
    
    public void startListening() {
        try {
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);
            
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException e) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(e.getMessage());
        }
    }
    
    public void stopListening() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            System.err.println("There was a problem unregistering the native hook.");
            System.err.println(e.getMessage());
        }
    }
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // * Get the key code of the pressed key
        final int EVENT_KEY_CODE = e.getKeyCode();
        KeyCode keyCode;


        // | Debug translation of the key code
        if (DEBUG_GLOBAL_KEY_LISTENER) {
            // Print the key code & name of the pressed key
            System.out.println("      Key Code (jNativehook): " + EVENT_KEY_CODE);
            System.out.println("      Key Name (jNativehook): " + NativeKeyEvent.getKeyText(EVENT_KEY_CODE));
            
            // Translate the key code to a JavaFX KeyCode
            System.out.println("Translated Key Code (javaFX): " + KeyCode.getKeyCode(NativeKeyEvent.getKeyText(EVENT_KEY_CODE)));
            
            try {
                System.out.println("Translated Key Name (javaFX): " + KeyCode.getKeyCode(NativeKeyEvent.getKeyText(EVENT_KEY_CODE)).getName());

                // Compare the key names in lowercase version
                String nativeKeyName = NativeKeyEvent.getKeyText(EVENT_KEY_CODE).toLowerCase();
                String javaFXKeyName = KeyCode.getKeyCode(NativeKeyEvent.getKeyText(EVENT_KEY_CODE)).getName().toLowerCase();
                
                System.out.println("        Translate successful: " + nativeKeyName.equals(javaFXKeyName) + "\n");

            } catch (NullPointerException npe) {
                System.out.println("Translated Key Name (javaFX): N/A");
                System.out.println("        Translate successful: false\n");
            }
        }


        // * Ensure that the user is not editing key bindings - early return if so 
        if (HuntController.keyBindingSettingsStage.isFocused()) {
            
            // Warn the user if the key they entered (presumably) conflicts with a blacklisted key
            // | This implementation has been circumvented, and is currently handled by: HuntController.keyBindingSettings(),
            // | as to not prevent the user from entering a blacklisted key if they so want to!
            //// if (Arrays.binarySearch(Keys.BLACKLISTED_KEYS, EVENT_KEY_CODE) >= 0) {
            ////     Alerts.blacklistedKeyWarning(keyCode.getName());
            //// }
            

            return;
        }


        // * Get the javaFX key code from the jNativehook key code
        // Attempt to look up the key code in the correction map
        keyCode = Actions.attemptKeyLookup(EVENT_KEY_CODE);

        // If the a corresponding code was not found in the translation map, attempt to translate it 
        if (keyCode == null) {
            keyCode = Actions.attemptKeyTranslation(EVENT_KEY_CODE);

            // If the translation failed, return early
            if (keyCode == null) {
                return;
            }
        }

        // * Update the encounter count
        Actions.updateEncounterCount(keyCode);
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Not needed
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not needed
    }

    // * Enum for what OS the application is running on
    private enum OS {
        WINDOWS, MACOS, LINUX, UNIX, OTHER;

        public static OS getOS() {
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("win")) return WINDOWS;
            else if (osName.contains("mac")) return MACOS;
            else if (osName.contains("nix") || osName.contains("nux")) return LINUX;
            else if (osName.contains("aix") || osName.contains("sunos")) return UNIX;
            else return OTHER;
        }
    }

    // * Class for key-related constants
    static final class Keys {
        public static KeyCode meta() {                                                  // Gets the corresponding meta key for the current OS
            switch (OS.getOS()) {
                case WINDOWS:  return KeyCode.WINDOWS;
                case   MACOS:  return KeyCode.COMMAND;
                case   LINUX:  return KeyCode.META;
                case    UNIX:  return KeyCode.META;
                     default:  return KeyCode.UNDEFINED;
            }
        }

        
        // * List of keys that cause overlapping within the jNativehook library
        // ! NOTE: List must be sorted in ascending order for Arrays.binarySearch to work correctly
        public static final KeyCode[] BLACKLISTED_JAVAFX_KEYS = {
            KeyCode.ENTER,                  // [10]     Triggered by both the NUMPAD ENTER key and the normal ENTER key
            KeyCode.SHIFT,                  // [16]     Triggered by both the LEFT- and RIGHT-SHIFT keys 
            KeyCode.CONTROL,                // [17]     Triggered by both LEFT- and RIGHT-CTRL keys, and ALT-GROUP key
            KeyCode.ALT,                    // [18]     Triggered by both LEFT-ALT and ALT-GROUP keys
            KeyCode.SLASH,                  // [47]     Also triggered by both the NUMPAD / and QUOTE key
            KeyCode.DIGIT0, KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4,     // [48-52]  The digits are triggered by the numpad 
            KeyCode.DIGIT5, KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9,     // [53-57]  keys and the normal number keys
            KeyCode.MULTIPLY,               // [106]    NUMPAD * triggers PRINTSCREEN while pre-existing PRINTSCREEN key exists
            KeyCode.DIVIDE,                 // [111]    NUMPAD / triggers SLASH, along with QUOTE
            KeyCode.QUOTE,                  // [222]    QUOTE triggers SLASH, along with NUMPAD /
        };


        // * Hash table for keys known to cause translating errors
        public static final Map<Integer, KeyCode> KEY_TRANSLATION_CORRECTION_MAP = new HashMap<>();

        static {
            KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_SEPARATOR, KeyCode.DECIMAL);

            // | Keys that are not 100% certainly to translate incorectly on all language configurations
            KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_EQUALS, KeyCode.PLUS);
            KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_OPEN_BRACKET, KeyCode.BACK_SLASH);
            KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_SEMICOLON, KeyCode.DEAD_DIAERESIS);
            // KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_SLASH, KeyCode.QUOTE);                  // Overlaps with NUMPAD /
            KEY_TRANSLATION_CORRECTION_MAP.put(3638, KeyCode.SHIFT);
            KEY_TRANSLATION_CORRECTION_MAP.put(3654, KeyCode.LESS);
            KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_META, meta());
            KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_ESCAPE, KeyCode.ESCAPE);
            // KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_SLASH, KeyCode.DIVIDE);                 // Overlaps with QUOTE
            // KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_PRINTSCREEN, KeyCode.PRINTSCREEN);      // Overlaps with NUMPAD MULTIPLY
            // KEY_TRANSLATION_CORRECTION_MAP.put(NativeKeyEvent.VC_PRINTSCREEN, KeyCode.MULTIPLY);         // Overlaps with PRINTSCREEN
            KEY_TRANSLATION_CORRECTION_MAP.put(3658, KeyCode.SUBTRACT);
            KEY_TRANSLATION_CORRECTION_MAP.put(3662, KeyCode.ADD);

        }
    }


    // * Class grouping functions used frequently together as to not clutter the GlobalKeyListener class 
    static class Actions {
        static void updateEncounterCount(KeyCode keyCode) {
            Platform.runLater(() -> {
                for (int i = HuntController.windowsList.size() - 1; i >= 0; i--) {
                    if (HuntController.windowsList.get(i).getKeyBinding() == keyCode) {
                        HuntController.windowsList.get(i).incrementEncounters();
                        HuntController.saveHuntOrder();
                        break;
                    }
                }
            });
        }

        static KeyCode attemptKeyLookup(int EVENT_KEY_CODE) {
            return Keys.KEY_TRANSLATION_CORRECTION_MAP.getOrDefault(EVENT_KEY_CODE, null);
        }

        static KeyCode attemptKeyTranslation(int EVENT_KEY_CODE) {
            KeyCode keyCode;

            try {
                keyCode = KeyCode.getKeyCode(NativeKeyEvent.getKeyText(EVENT_KEY_CODE));            // If not, default to the standard translation
            } catch (Exception err) {
                // If the translation fails, display an alert box to the user
                Alerts.translationFailedError();

                return null;
            } 

            // If the keyCode is null, display an error message
            // ! NOTE: This if statement can be removed and directly implemented into the if statement within the GlobalKeyListener class
            // !       so that it doesn't check if keyCode is null twice, but i've left it here to keep the code more readable
            if (keyCode == null) {
                if (GlobalKeyListener.DEBUG_GLOBAL_KEY_LISTENER) {
                    System.out.println("Error: Unable to translate the key code " + EVENT_KEY_CODE + " to a JavaFX KeyCode.");
                }
                
                if (HuntController.keyBindingSettingsStage.isFocused()) {
                    Alerts.translatedKeyError(EVENT_KEY_CODE);
                }

                return null;
            }

            return keyCode;
        }

        // This function is only called when the user presses a key within the key bind settings menu, hence the known JavaFX KeyCode
        static void warnIfBlacklistedKeybindUsed(KeyCode JAVAFX_KEY_CODE) {
            if (Arrays.binarySearch(Keys.BLACKLISTED_JAVAFX_KEYS, JAVAFX_KEY_CODE) >= 0) {
                Alerts.blacklistedKeyWarning(JAVAFX_KEY_CODE.getName());
            }
        }
    }


    // * Class for displaying alerts to the user
    static class Alerts {
        static void blacklistedKeyWarning(String keyName) {
            Platform.runLater(() -> {
                Alert warning = new Alert(AlertType.WARNING);
                warning.setTitle("Warning");
                warning.setHeaderText("Blacklisted key pressed: " + keyName);
                warning.setContentText("" + 
                    "The key \"" + keyName + "\" is blacklisted and you should be wary of unwanted side effects when " + 
                    "using it.\n\nThis might be due to the fact that jNativeHook, one of the libraries used to monitor keyboard " + 
                    "inputs, contains overlapping key codes for certain keys.\n\nSo, depending on the key, this mapping may or may not " + 
                    "work, along with there being another key that can also trigger this key's selected action.\n\n"
                );
                warning.showAndWait();
            });
        }

        static void translatedKeyError(int keyCode) {
            Platform.runLater(() -> {
                Alert error = new Alert(AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText("Key translation resulted in: null");
                error.setContentText("" + 
                    "Due to missing keycode names in either library, translating \"" + NativeKeyEvent.getKeyText(keyCode) + 
                    "\" failed! There is nothing you can do about this, so just refrain from using this key for now!\n\n"
                );
                error.showAndWait();
            });
        }

        static void translationFailedError() {
            Platform.runLater(() -> {
                    Alert error = new Alert(AlertType.ERROR);
                    error.setTitle("Oops...");
                    error.setHeaderText("Key translation did a fucky wucky!");
                    error.setContentText("" + 
                        "This shouldn't be possible, so idfk what goofed.\n\n" + 
                        "k thx cya o/ ..." 
                    );
                    error.showAndWait();
                });
        }
    }
}
