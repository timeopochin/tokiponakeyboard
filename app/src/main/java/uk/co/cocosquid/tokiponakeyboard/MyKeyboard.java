package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;

public class MyKeyboard extends LinearLayout implements View.OnLongClickListener, View.OnClickListener {

    // Input connection
    private InputConnection inputConnection;
    private EditorInfo editorInfo;
    private InputMethodManager inputMethodManager;

    // Key detection
    private int previousKey;
    private int currentKey;
    private int startKey;
    private boolean actionComplete = false;

    // Arrays
    private SparseArray<String> keyValues = new SparseArray<>();
    private String[] shortcuts;
    private String[] words;
    private String[] unofficialWords;

    // Word construction
    private boolean inBrackets = false;
    private int quoteNestingLevel = 0;
    private String currentShortcut = "";
    private String compoundFirstWordShortcut = "";
    private String suffix = "";

    // Text manipulation
    private CharSequence currentText;
    private CharSequence beforeCursorText;
    private CharSequence afterCursorText;
    private boolean stopDelete = false;

    // Preferences
    SharedPreferences sharedPreferences;

    // Colours
    private int letterKeyColour;
    private int commonWordKeyColour;
    private int specialKeyColour;

    private int letterKeyTextColour;
    private int commonWordKeyTextColour;
    private int specialKeyTextColour;

    private int lastStateKeyColour;
    private int intermediateKeyColour;
    private int lastStateUnofficialKeyColour;
    private int intermediateUnofficialKeyColour;

    private int lastStateKeyTextColour;
    private int intermediateTextKeyColour;
    private int lastStateUnofficialKeyTextColour;
    private int intermediateTextUnofficialKeyColour;

    private int backgroundColour;

    private Button[] keys = new Button[28];

    public MyKeyboard(Context context) {
        this(context, null, 0);
    }

    public MyKeyboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {

        // Load shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        LayoutInflater.from(context).inflate(R.layout.keyboard, this, true);

        // Set the keys
        keys[0] = findViewById(R.id.ali);
        keys[1] = findViewById(R.id.en);
        keys[2] = findViewById(R.id.ike);
        keys[3] = findViewById(R.id.jan);
        keys[4] = findViewById(R.id.kama);
        keys[5] = findViewById(R.id.la);
        keys[6] = findViewById(R.id.ma);
        keys[7] = findViewById(R.id.nimi);
        keys[8] = findViewById(R.id.o);
        keys[9] = findViewById(R.id.pi);
        keys[10] = findViewById(R.id.sina);
        keys[11] = findViewById(R.id.tawa);
        keys[12] = findViewById(R.id.utala);
        keys[13] = findViewById(R.id.wile);

        keys[14] = findViewById(R.id.a);
        keys[15] = findViewById(R.id.ala);
        keys[16] = findViewById(R.id.e);
        keys[17] = findViewById(R.id.li);
        keys[18] = findViewById(R.id.mi);
        keys[19] = findViewById(R.id.ni);
        keys[20] = findViewById(R.id.pona);
        keys[21] = findViewById(R.id.toki);

        keys[22] = findViewById(R.id.bracket);
        keys[23] = findViewById(R.id.quote);
        keys[24] = findViewById(R.id.dot);
        keys[25] = findViewById(R.id.question);
        keys[26] = findViewById(R.id.enter);
        keys[27] = findViewById(R.id.delete);

        // Hold delete key for continuous delete
        keys[27].setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (mHandler != null) {
                        return true;
                    }
                    stopDelete = false;
                    mHandler = new Handler();
                    mHandler.postDelayed(mAction, 200);
                } else if (event.getAction() == MotionEvent.ACTION_UP || stopDelete) {
                    if (mHandler == null) {
                        return true;
                    }
                    mHandler.removeCallbacks(mAction);
                    mHandler = null;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    delete();
                    mHandler.postDelayed(this, 30);
                }
            };

        });

        // Set colours
        setColours();

        // Set key listeners
        for (Button key : keys) {
            key.setOnClickListener(this);
            key.setOnLongClickListener(this);
            key.setTextSize(18);
        }

        // Set the button strings
        keyValues.put(R.id.ali, "a");
        keyValues.put(R.id.en, "e");
        keyValues.put(R.id.ike, "i");
        keyValues.put(R.id.jan, "j");
        keyValues.put(R.id.kama, "k");
        keyValues.put(R.id.la, "l");
        keyValues.put(R.id.ma, "m");
        keyValues.put(R.id.nimi, "n");
        keyValues.put(R.id.o, "o");
        keyValues.put(R.id.pi, "p");
        keyValues.put(R.id.sina, "s");
        keyValues.put(R.id.tawa, "t");
        keyValues.put(R.id.utala, "u");
        keyValues.put(R.id.wile, "w");

        keyValues.put(R.id.a, "a%");
        keyValues.put(R.id.ala, "ala%");
        keyValues.put(R.id.e, "e%");
        keyValues.put(R.id.li, "li%");
        keyValues.put(R.id.mi, "mi%");
        keyValues.put(R.id.ni, "ni%");
        keyValues.put(R.id.pona, "pona%");
        keyValues.put(R.id.toki, "toki%");

        keyValues.put(R.id.bracket, "%[");
        keyValues.put(R.id.dot, "%.");
        keyValues.put(R.id.quote, "%“");
        keyValues.put(R.id.question, "%?");
        keyValues.put(R.id.enter, "%enter");

        keyValues.put(R.id.delete, "%delete");

        // Arrays from xml
        Resources res = getResources();
        shortcuts = res.getStringArray(R.array.shortcuts);
        words = res.getStringArray(R.array.words);
        unofficialWords = res.getStringArray(R.array.unofficial_words);
    }

    @Override
    public void onClick(View v) {
        action(keyValues.get(v.getId()), null);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        // Find what key the event is on
        boolean found = false;
        boolean keyChanged = false;
        final Rect childRect = new Rect();
        final Rect parentRect = new Rect();

        for (int i = 0; i < keys.length; i++) {
            Button key = keys[i];
            key.getHitRect(childRect);
            ((View) key.getParent()).getHitRect(parentRect);
            if (event.getX() > childRect.left && event.getX() < childRect.right && event.getY() > parentRect.top && event.getY() < parentRect.bottom) {
                if (i != currentKey) {
                    previousKey = currentKey;
                    currentKey = i;
                    keyChanged = true;
                }
                found = true;
                break;
            }
        }

        // Highlight key under finger
        if (keyChanged) {
            keys[previousKey].setPressed(false);
        }
        keys[currentKey].setPressed(found);

        if (found) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                actionComplete = false;
                startKey = currentKey;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                /*
                if (actionComplete && currentKey != startKey) {
                    //currentShortcut = "";
                    setLayout("");
                } else if (!actionComplete) {
                    setLayout("");
                }
                 */
                if (currentKey != startKey) {
                    setLayout("");
                    stopDelete = true;
                } else if (!actionComplete) {
                    setLayout("");
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP && currentKey != startKey) {

                // Swipe complete
                if (actionComplete) {
                    currentShortcut = "";
                    action(keyValues.get(keys[currentKey].getId()), null);
                } else {
                    actionComplete = true;
                    action(keyValues.get(keys[startKey].getId()), keyValues.get(keys[currentKey].getId()));
                }
                keys[currentKey].setPressed(false);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        action(keyValues.get(v.getId()), keyValues.get(v.getId()));
        actionComplete = true;
        return true;
    }

    private void action(String startKey, String endKey) {
        if (endKey == null) {

            // Single key sent
            boolean nothingWritten = false;
            if (getPreviousCharacter().equals("“") && !getNextCharacter().isEmpty() && !getNextCharacter().equals("”") && !startKey.equals("%“") && !startKey.equals("%delete") && !startKey.equals("%enter")) {
                suffix = " ";
            }
            if (startKey.charAt(0) == '%') {

                // Special key sent
                if (!startKey.equals("%delete")) {
                    finishAction("finish");
                    if (inBrackets && !startKey.equals("%]") && !startKey.equals("%[")) {
                        action("%]", null);
                    }
                }
                switch (startKey) {
                    case "%]":

                        // Move cursor to the next space (or the end of the input if none are found)
                        int endBracketLocation = getEndBracketLocation();
                        inputConnection.setSelection(endBracketLocation, endBracketLocation);

                        // Place a closing bracket if it is missing
                        if (currentText.charAt(endBracketLocation - 1) != ']') {
                            write("]");
                        }

                        setBracket(false);
                        break;
                    case "%[":
                        if (inBrackets) {
                            action("%]", null);
                        } else {
                            writeShortcut("[%");

                            // Move cursor inside brackets
                            moveCursorBackOne();

                            setBracket(true);
                        }
                        break;
                    case "%“":
                        if (quoteNestingLevel > 0) {
                            write("”");
                            if (!",.:?!\n".contains(getNextCharacter()) && !getNextCharacter().isEmpty()) {
                                write(" ");
                            }
                        } else {
                            writeShortcut("“%");
                            if (getNextCharacter().equals(" ")) {
                                inputConnection.deleteSurroundingText(0, 1);
                            }
                        }
                        break;
                    case "%.":
                    case "%?":
                        write(Character.toString(startKey.charAt(1)));
                        //action(startKey.charAt(1) + "%", null);
                        break;
                    case "%delete":
                        delete();
                        break;
                    case "%enter":
                        enter();
                        break;
                    default:
                        Log.e(null, "Shortcut: " + startKey + " is not a special key");
                }

            } else {

                // Letter/word key sent
                if (doesShortcutExist(currentShortcut + startKey)) {

                    // Key is part of previous action and it is now finished
                    writeShortcut(currentShortcut + startKey);
                    currentShortcut = "";
                    setLayout("");

                } else if (doesShortcutExist(finishShortcut(currentShortcut + startKey))) {

                    // Key is part of previous action but it is still unfinished
                    currentShortcut += startKey;
                    setLayout("");
                    setLayout(currentShortcut);
                    nothingWritten = true;

                } else {

                    // Need to finish previous action
                    finishAction("finish");
                    if (doesShortcutExist(startKey)) {

                        // Word key sent
                        writeShortcut(startKey);

                    } else {

                        // Letter key sent
                        currentShortcut = startKey;
                        setLayout(currentShortcut);
                    }
                }
            }
            if (!suffix.isEmpty() && !nothingWritten) {
                moveCursorBackOne();
                suffix = "";
            }
        } else {

            // Two keys sent
            if (startKey.charAt(0) == '%' || endKey.charAt(0) == '%') {

                // Two separate keys to be sent (at least one was a special key)
                if (startKey.equals(endKey)) {

                    // Long press actions for special keys
                    finishAction("finish");
                    switch (startKey) {
                        case "%[":
                            write(",");
                            break;
                        case "%“":
                            if (quoteNestingLevel > 0) {
                                writeShortcut("“%");
                                if (getNextCharacter().equals(" ")) {
                                    inputConnection.deleteSurroundingText(0, 1);
                                }
                            } else {
                                action(startKey, null);
                            }
                            break;
                        case "%.":
                            write(":");
                            break;
                        case "%?":
                            write("!");
                            break;
                        case "%enter":
                            inputMethodManager.showInputMethodPicker();
                            break;
                        default:
                            Log.e(null, "Shortcut: " + startKey + " is not a special key");
                    }
                } else if (startKey.charAt(0) == '%') {

                    // Special key followed by normal key
                    action(startKey, null);
                    action(endKey, null);

                } else {

                    // Normal key followed by special key
                    action(startKey, null);
                    finishAction("finish");
                    action(endKey, null);
                }
            } else {

                // A compound glyph to be sent (Both were letter/word keys)
                finishAction(startKey);
                compoundFirstWordShortcut = finishShortcut(currentShortcut + startKey);
                currentShortcut = "";
                action(endKey, null);
            }
        }
    }

    // Returns true if the cursor is at the start of an input, newline or quote
    private boolean cursorAtStart() {
        updateTextInfo();
        if (beforeCursorText.length() == 0) {
            return true;
        } else {
            String previousCharacter = getPreviousCharacter();
            return "\n“".contains(previousCharacter);
        }
    }

    private void delete() {
        if (currentShortcut.isEmpty()) {

            // Delete some text
            updateTextInfo();
            label:
            for (int i = beforeCursorText.length() - 1; i >= 0; i--) {
                String currentString = Character.toString(beforeCursorText.charAt(i));
                switch (currentString) {
                    case "“":
                        if (i == beforeCursorText.length() - 1) {
                            inputConnection.deleteSurroundingText(1, 0);
                        } else {
                            inputConnection.deleteSurroundingText(beforeCursorText.length() - i - 1, 0);
                        }
                        break label;
                    case " ":
                    case "_":
                    case ",":
                    case "”":
                    case ".":
                    case ":":
                    case "?":
                    case "!":
                    case "\n":
                        inputConnection.deleteSurroundingText(beforeCursorText.length() - i, 0);
                        break label;
                    case "]":

                        // Move inside the brackets and delete from there
                        inputConnection.setSelection(i, i);
                        setBracket(true);
                        delete();
                        break label;

                    case "[":

                        // Delete everything from the opening bracket up to the closing bracket
                        int endBracket = getEndBracketLocation();
                        inputConnection.deleteSurroundingText(1, endBracket - beforeCursorText.length());
                        //if (getNextCharacter().equals(" ")) {
                        //    inputConnection.deleteSurroundingText(0, 1);
                        //}
                        setBracket(false);
                        break label;
                    //return;
                }
                if (i == 0) {

                    // Start of the input was reached
                    inputConnection.deleteSurroundingText(beforeCursorText.length(), 0);
                }
            }

            if ("% |“ ".contains(getAdjacentCharacters())) {
                inputConnection.deleteSurroundingText(0, 1);
            } else if (" %|  | ,| ”| .| :| ?| !| \n".contains(getAdjacentCharacters())) {
                inputConnection.deleteSurroundingText(1, 0);
            }
        } else {

            // Cancel current input in progress
            currentShortcut = "";
            compoundFirstWordShortcut = "";
            setLayout("");
        }
    }

    private boolean doesShortcutExist(String shortcutToCheck) {
        for (String shortcut : shortcuts) {
            if (shortcutToCheck.equals(shortcut)) {
                return true;
            }
        }
        return false;
    }

    private void enter() {

        // Send the correct IME action specified by the editor
        switch (editorInfo.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                inputConnection.performEditorAction(EditorInfo.IME_ACTION_GO);
                break;
            case EditorInfo.IME_ACTION_NEXT:
                inputConnection.performEditorAction(EditorInfo.IME_ACTION_NEXT);
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
                break;
            case EditorInfo.IME_ACTION_SEND:
                inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEND);
                break;
            default:
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
        }
    }

    /* nextKey allows the function to know whether or not to reset currentShortcut in the cases
     * where a new compound glyph has been started.
     */
    public void finishAction(String nextKey) {
        boolean validCombination = doesShortcutExist(finishShortcut(currentShortcut + nextKey));
        if (!validCombination || !compoundFirstWordShortcut.isEmpty()) {
            writeShortcut(finishShortcut(currentShortcut));
            if (!validCombination) {
                currentShortcut = "";
            }
            setLayout("");
        }
    }

    // Returns a shortcut if it exists and returns the finished shortcut if it does not exist yet
    private String finishShortcut(String shortcutToFinish) {
        if (doesShortcutExist(shortcutToFinish) || shortcutToFinish.isEmpty()) {
            return shortcutToFinish;
        } else {
            return shortcutToFinish + shortcutToFinish.charAt(shortcutToFinish.length() - 1);
        }
    }

    private String getAdjacentCharacters() {
        updateCurrentState();
        String previous = getPreviousCharacter();
        if (previous.isEmpty()) {
            previous = "%";
        }
        String next = getNextCharacter();
        if (next.isEmpty()) {
            next = "%";
        }
        return previous + next;
    }

    private int getEndBracketLocation() {
        updateTextInfo();
        int endBracket = beforeCursorText.length() - 1;
        while (true) {
            if (currentText.charAt(endBracket) == ']' || endBracket == currentText.length() - 1) {

                // A bracket was found or the end of the input was reached
                return endBracket + 1;

            }
            endBracket++;
        }
    }

    private String getNextCharacter() {
        updateTextInfo();
        boolean atEndOfInput = afterCursorText.length() == 0;
        String charOnRight = "";
        if (!atEndOfInput) {
            charOnRight = Character.toString(afterCursorText.charAt(0));
        }
        return charOnRight;
    }

    private String getPreviousCharacter() {
        updateTextInfo();
        boolean atStartOfInput = beforeCursorText.length() == 0;
        String charOnLeft = "";
        if (!atStartOfInput) {
            charOnLeft = Character.toString(beforeCursorText.charAt(beforeCursorText.length() - 1));
        }
        return charOnLeft;
    }

    private String getWord(String shortcut) {
        for (int i = 0; i < shortcuts.length; i++) {
            if (shortcut.equals(shortcuts[i])) {
                return words[i];
            }
        }
        return "";
    }

    private boolean isUnofficial(String word) {
        for (String unofficialWord : unofficialWords) {
            if (unofficialWord.equals(word)) {
                return true;
            }
        }
        return false;
    }

    public void loadPreferences() {
        setColours();
    }

    private void moveCursorBackOne() {
        updateTextInfo();
        int backOne = beforeCursorText.length() - 1;
        inputConnection.setSelection(backOne, backOne);
    }

    public void setBracket(boolean newInBrackets) {
        inBrackets = newInBrackets;
        if (inBrackets) {
            ((Button) findViewById(R.id.bracket)).setText("]");
        } else {
            ((Button) findViewById(R.id.bracket)).setText("[");
        }
    }

    public void setColours() {
        switch (sharedPreferences.getString("themes", "default")) {
            case "default":

                // Set colours
                letterKeyColour = 0xFFbfd5ff;
                commonWordKeyColour = 0xFF7fffd4;
                specialKeyColour = 0xFF6f95df;

                letterKeyTextColour = 0xFFffffff;
                commonWordKeyTextColour = 0xFF00947f;
                specialKeyTextColour = 0xFFffffff;

                lastStateKeyColour = 0xFF7fffd4;
                intermediateKeyColour = 0xFF00947f;
                lastStateUnofficialKeyColour = 0xFFff947f;
                intermediateUnofficialKeyColour = 0xFFff4f3f;

                lastStateKeyTextColour = 0xFF00947f;
                intermediateTextKeyColour = 0xFF7fffd4;
                lastStateUnofficialKeyTextColour = 0xFFff4f3f;
                intermediateTextUnofficialKeyColour = 0xFFff947f;

                backgroundColour = 0xFF7faaff;
                break;
            case "light":

                // Set colours
                letterKeyColour = 0xFFffffff;
                commonWordKeyColour = 0xFF7faaff;
                specialKeyColour = 0xFFc0c0c0;

                letterKeyTextColour = 0xFF101010;
                commonWordKeyTextColour = 0xFFffffff;
                specialKeyTextColour = 0xFF101010;

                lastStateKeyColour = 0xFF7faaff;
                intermediateKeyColour = 0xFF2E40A4;
                lastStateUnofficialKeyColour = 0xFFff3f80;
                intermediateUnofficialKeyColour = 0xFFaf1767;

                lastStateKeyTextColour = 0xFFffffff;
                intermediateTextKeyColour = 0xFFffffff;
                lastStateUnofficialKeyTextColour = 0xFFffffff;
                intermediateTextUnofficialKeyColour = 0xFFffffff;

                backgroundColour = 0xFFe0e0e0;
                break;
            case "dark":

                // Set colours
                letterKeyColour = 0xFF202020;
                commonWordKeyColour = 0xFF405580;
                specialKeyColour = 0xFF101010;

                letterKeyTextColour = 0xFFe0e0e0;
                commonWordKeyTextColour = 0xFFffffff;
                specialKeyTextColour = 0xFFe0e0e0;

                lastStateKeyColour = 0xFF405580;
                intermediateKeyColour = 0xFF172052;
                lastStateUnofficialKeyColour = 0xFF802040;
                intermediateUnofficialKeyColour = 0xFF580C34;

                lastStateKeyTextColour = 0xFFffffff;
                intermediateTextKeyColour = 0xFFffffff;
                lastStateUnofficialKeyTextColour = 0xFFffffff;
                intermediateTextUnofficialKeyColour = 0xFFffffff;

                backgroundColour = 0xFF000000;
                break;
        }
        for (int i = 0; i < keys.length; i++) {
            // Set base colours
            if (i < 14) {

                // Letter keys
                keys[i].setBackgroundTintList(ColorStateList.valueOf(letterKeyColour));
                keys[i].setTextColor(letterKeyTextColour);

            } else if (i < 22) {

                // Common word keys
                keys[i].setBackgroundTintList(ColorStateList.valueOf(commonWordKeyColour));
                keys[i].setTextColor(commonWordKeyTextColour);

            } else {

                // Special keys
                keys[i].setBackgroundTintList(ColorStateList.valueOf(specialKeyColour));
                keys[i].setTextColor(specialKeyTextColour);
            }
        }

        // Set background colour
        findViewById(R.id.keyboard).setBackgroundColor(backgroundColour);
    }

    public void setEditorInfo(EditorInfo ei) {
        editorInfo = ei;
    }

    public void setIME(InputMethodManager imm) {
        inputMethodManager = imm;
    }

    public void setInputConnection(InputConnection ic) {
        inputConnection = ic;
    }

    private void setLayout(String layoutShortcut) {
        String letters = "aeijklmnopstuw";
        for (int i = 0; i < 14; i++) {
            String potentialShortcut = layoutShortcut + letters.charAt(i);
            Button key = keys[i];
            String keyText;
            if (doesShortcutExist(potentialShortcut)) {

                // Key on last state
                keyText = getWord(potentialShortcut);
                key.setText(keyText);

                // Set the colours
                if (isUnofficial(keyText)) {
                    key.setBackgroundTintList(ColorStateList.valueOf(lastStateUnofficialKeyColour));
                    key.setTextColor(lastStateUnofficialKeyTextColour);
                } else {
                    key.setBackgroundTintList(ColorStateList.valueOf(lastStateKeyColour));
                    key.setTextColor(lastStateKeyTextColour);
                }

            } else if (doesShortcutExist(finishShortcut(potentialShortcut))) {

                keyText = getWord(finishShortcut(potentialShortcut));
                key.setText(keyText);
                if (potentialShortcut.length() > 1) {

                    // Key is on intermediate state
                    // Set the colours
                    if (isUnofficial(keyText)) {
                        key.setBackgroundTintList(ColorStateList.valueOf(intermediateUnofficialKeyColour));
                        key.setTextColor(intermediateTextUnofficialKeyColour);
                    } else {
                        key.setBackgroundTintList(ColorStateList.valueOf(intermediateKeyColour));
                        key.setTextColor(intermediateTextKeyColour);
                    }
                } else {

                    // Key is on base state
                    // Set the colours
                    key.setBackgroundTintList(ColorStateList.valueOf(letterKeyColour));
                    key.setTextColor(letterKeyTextColour);
                }
            }
        }
    }

    public void updateCurrentState() {

        // Get the adjacent characters
        String charOnRight = getNextCharacter();
        String charOnLeft = getPreviousCharacter();

        boolean adjust = true;
        if ("],”.:?!\n".contains(charOnLeft) || " _],”.:?!\n".contains(charOnRight)) {

            // Do not adjust cursor position
            adjust = false;
        }

        int moveTo = 0;
        int i;
        label:
        for (i = beforeCursorText.length() - 1; i >= 0; i--) {
            String currentString = Character.toString(beforeCursorText.charAt(i));
            switch (currentString) {
                case "“":
                    if (moveTo == 0) {
                        moveTo = i + 1;
                    }
                    break;
                case "\n":
                    if (moveTo == 0) {
                        moveTo = i + 1;
                    }
                    setBracket(false);
                    break label;
                case " ":
                case "]":
                    setBracket(false);
                    break label;
                case "_":
                case "[":
                    setBracket(true);
                    break label;
            }
            if (i == 0) {
                setBracket(false);
                break;
            }
        }
        if (adjust) {
            if (moveTo == 0) {
                moveTo = i;
            }
            inputConnection.setSelection(moveTo, moveTo);
        }

        // Ensure the correct quote is on the key
        updateQuoteNestedLevel();
        if (quoteNestingLevel > 0) {
            ((Button) findViewById(R.id.quote)).setText("”");
        } else {
            ((Button) findViewById(R.id.quote)).setText("“");
        }
    }

    private void updateQuoteNestedLevel() {
        updateTextInfo();
        quoteNestingLevel = 0;
        for (int i = beforeCursorText.length() - 1; i >= 0; i--) {
            if (beforeCursorText.charAt(i) == '“') {
                quoteNestingLevel += 1;
            } else if (beforeCursorText.charAt(i) == '”') {
                quoteNestingLevel -= 1;
            }
        }
    }

    private void updateTextInfo() {
        ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
        if (extractedText != null) {
            currentText = extractedText.text;
            beforeCursorText = inputConnection.getTextBeforeCursor(currentText.length(), 0);
            afterCursorText = inputConnection.getTextAfterCursor(currentText.length(), 0);
        } else {
            currentText = beforeCursorText = afterCursorText = "";
        }
    }

    private void write(String toWrite) {
        if (inBrackets && ",:!".contains(toWrite)) {
            action("%]", null);
        }
        inputConnection.commitText(toWrite + suffix, 1);
    }

    private void writeShortcut(String shortcut) {

        // Do not write anything if the shortcut is empty
        if (shortcut.isEmpty()) {
            return;
        }

        // Decide the correct word spacer to put before the word
        String wordSpacer = " ";
        if (cursorAtStart()) {
            wordSpacer = "";
        } else if (inBrackets) {
            wordSpacer = "_";
        }

        // Prepare first part of a compound glyph if it exists
        String compoundFirstWord = "";
        if (!compoundFirstWordShortcut.isEmpty()) {
            compoundFirstWord = getWord(compoundFirstWordShortcut) + "-";
            compoundFirstWordShortcut = "";
        }

        write(wordSpacer + compoundFirstWord + getWord(shortcut));
    }
}