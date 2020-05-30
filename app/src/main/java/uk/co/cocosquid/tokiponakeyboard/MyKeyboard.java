package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;

public class MyKeyboard extends LinearLayout implements View.OnLongClickListener, View.OnClickListener {

    // Input connection
    private InputConnection inputConnection;
    private EditorInfo editorInfo;

    // Key detection
    private int previousKey;
    private int currentKey;
    private int startKey;

    // Arrays
    private SparseArray<String> keyValues = new SparseArray<>();
    private String[] shortcuts;
    private String[] words;

    // Word construction
    private String currentShortcut = "";
    private boolean inBrackets = false;
    private boolean unfinishedCompound = false;
    private String prefix = "";
    private String suffix = " ";
    private CharSequence currentText;
    private CharSequence beforeCursorText;
    private CharSequence afterCursorText;

    // Colours
    private int letterKeyColour = 0xFF565956;
    private int commonWordKeyColour = 0xFF454745;
    private int specialKeyColour = 0xFF353634;

    private int letterKeyTextColour = 0xFFE8EDDF;
    private int commonWordKeyTextColour = 0xFF8C8F8C;
    private int specialKeyTextColour = 0xFF818481;

    private int lastStateKeyColour = 0xFFEFDC9E;
    private int intermediateKeyColour = 0xFFF5CB5C;

    private int lastStateKeyTextColour = 0xFF242423;
    private int intermediateTextKeyColour = 0xFF242423;

    private int backgroundColour = 0xFF242423;

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
        keys[26] = findViewById(R.id.delete);
        keys[27] = findViewById(R.id.enter);

        // Set key listeners
        for (int i = 0; i < keys.length; i++) {
            Button key = keys[i];
            key.setOnClickListener(this);
            key.setOnLongClickListener(this);
            key.setTextSize(18);

            // Set base colours
            if (i < 14) {

                // Letter keys
                key.setBackgroundTintList(ColorStateList.valueOf(letterKeyColour));
                key.setTextColor(letterKeyTextColour);

            } else if (i < 22) {

                // Common word keys
                key.setBackgroundTintList(ColorStateList.valueOf(commonWordKeyColour));
                key.setTextColor(commonWordKeyTextColour);

            } else {

                // Special keys
                key.setBackgroundTintList(ColorStateList.valueOf(specialKeyColour));
                key.setTextColor(specialKeyTextColour);
            }
        }

        // Set background colour
        findViewById(R.id.keyboard).setBackgroundColor(backgroundColour);

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
        keyValues.put(R.id.quote, "%\"");
        keyValues.put(R.id.question, "%?");
        keyValues.put(R.id.enter, "%enter");

        keyValues.put(R.id.delete, "%delete");

        // Arrays from xml
        Resources res = getResources();
        shortcuts = res.getStringArray(R.array.shortcuts);
        words = res.getStringArray(R.array.words);
    }

    private void delete() {
        updateTextInfo();
        if (beforeCursorText.length() > 0) {
            if (beforeCursorText.charAt(beforeCursorText.length() - 1) == '[') {
                int endBracket = beforeCursorText.length() - 1;
                while (true) {
                    if (currentText.charAt(endBracket) == ']') {
                        inputConnection.deleteSurroundingText(1, endBracket - beforeCursorText.length() + 2);
                        setBracket(false);
                        return;
                    }
                    endBracket++;
                }
            } else {

                // Delete at least one character
                inputConnection.deleteSurroundingText(1, 0);
                updateTextInfo();

                // Delete the rest of the word
                for (int i = beforeCursorText.length() - 1; i >= 0; i--) {
                    String currentString = Character.toString(beforeCursorText.charAt(i));
                    switch (currentString) {
                        case "_":
                            inputConnection.deleteSurroundingText(beforeCursorText.length() - i, 0);
                            setBracket(true);
                            //updateCurrentState();
                            return;
                        case "]":
                            inputConnection.setSelection(i + 1, i + 1);
                            inputConnection.commitText(" ", 1);
                            inputConnection.setSelection(i, i);
                            delete();
                            return;
                        case "\n":
                        case " ":
                            inputConnection.deleteSurroundingText(beforeCursorText.length() - i - 1, 0);
                            updateCurrentState();
                            return;
                    }
                }
                inputConnection.deleteSurroundingText(beforeCursorText.length(), 0);
            }
        }
    }

    private void updateTextInfo() {
        ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
        if (extractedText != null) {
            currentText = extractedText.text;
            beforeCursorText = inputConnection.getTextBeforeCursor(currentText.length(), 0);
            afterCursorText = inputConnection.getTextAfterCursor(currentText.length(), 0);
        }
    }

    public void updateCurrentState() {

        // Align cursor to a valid position
        updateTextInfo();
        boolean adjust = afterCursorText.length() != 0;
        if (adjust && "]_".contains(Character.toString(afterCursorText.charAt(0)))) {
            adjust = false;
        }
        for (int i = beforeCursorText.length() - 1; i >= 0; i--) {
            String currentString = Character.toString(beforeCursorText.charAt(i));
            switch (currentString) {
                case "\n":
                case " ":
                    setBracket(false);
                    if (adjust) {
                        inputConnection.setSelection(i + 1, i + 1);
                    }
                    return;
                case "_":
                case "]":
                    setBracket(true);
                    if (adjust) {
                        inputConnection.setSelection(i, i);
                    }
                    return;
                case "[":
                    setBracket(true);
                    if (adjust) {
                        inputConnection.setSelection(i + 1, i + 1);
                    }
                    return;
            }
        }
        if (adjust) {
            inputConnection.setSelection(0, 0);
        }
    }

    public void setBracket(boolean newInBrackets) {
        inBrackets = newInBrackets;
        String newBracket;
        if (inBrackets) {
            newBracket = "]";
            prefix = "_";
            suffix = "";
        } else {
            newBracket = "[";
            prefix = "";
            suffix = " ";
        }
        keyValues.put(R.id.bracket, "%" + newBracket);
        ((Button) findViewById(R.id.bracket)).setText(newBracket);
    }

    public void setInputConnection(InputConnection ic) {
        inputConnection = ic;
    }

    public void setEditorInfo(EditorInfo ei) {
        editorInfo = ei;
    }

    private boolean doesShortcutExist(String shortcutToCheck) {
        for (String shortcut : shortcuts) {
            if (shortcutToCheck.equals(shortcut)) {
                return true;
            }
        }
        return false;
    }

    private void write(String toWrite) {
        inputConnection.commitText(toWrite, 1);
    }

    private void writeShortcut(String shortcut) {
        write(prefix + getWord(shortcut) + suffix);
    }

    private String getWord(String shortcut) {
        for (int i = 0; i < shortcuts.length; i++) {
            if (shortcut.equals(shortcuts[i])) {
                return words[i];
            }
        }
        return "";
    }

    public void finishAction() {
        if (!currentShortcut.isEmpty()) {
            writeShortcut(finishShortcut(currentShortcut));
            setLayout("");
            currentShortcut = "";
        }
    }

    private void setLayout(String layoutShortcut) {
        String letters = "aeijklmnopstuw";
        for (int i = 0; i < 14; i++) {
            String potentialShortcut = layoutShortcut + letters.charAt(i);
            Button key = keys[i];
            if (doesShortcutExist(potentialShortcut)) {

                // Key on last state
                key.setText(getWord(potentialShortcut));
                key.setBackgroundTintList(ColorStateList.valueOf(lastStateKeyColour));
                key.setTextColor(lastStateKeyTextColour);

            } else if (doesShortcutExist(finishShortcut(potentialShortcut))) {

                key.setText(getWord(finishShortcut(potentialShortcut)));
                if (potentialShortcut.length() > 1) {

                    // Key is on intermediate state
                    key.setBackgroundTintList(ColorStateList.valueOf(intermediateKeyColour));
                    key.setTextColor(intermediateTextKeyColour);

                } else {

                    // Key is on base state
                    key.setBackgroundTintList(ColorStateList.valueOf(letterKeyColour));
                    key.setTextColor(letterKeyTextColour);
                }
            }
        }
    }

    private String finishShortcut(String shortcutToFinish) {
        if (doesShortcutExist(shortcutToFinish) || shortcutToFinish.isEmpty()) {
            return shortcutToFinish;
        } else {
            return shortcutToFinish + shortcutToFinish.charAt(shortcutToFinish.length() - 1);
        }
    }

    private boolean action(String startKey, String endKey) {
        //Log.e("TAG", currentShortcut + ", " + startKey + ", " + endKey);
        if (endKey == null) {
            int endBracket;
            switch (startKey) {
                case "%[":
                    finishAction();
                    write("[] ");
                    updateTextInfo();
                    inputConnection.setSelection(beforeCursorText.length() - 2, beforeCursorText.length() - 2);
                    setBracket(true);
                    inputConnection.endBatchEdit();
                    return true;
                case "%]":
                    finishAction();
                    updateTextInfo();
                    endBracket = beforeCursorText.length() - 1;
                    while (true) {
                        if (currentText.charAt(endBracket) == ']') {
                            inputConnection.setSelection(endBracket + 2, endBracket + 2);
                            setBracket(false);
                            break;
                        }
                        endBracket++;
                    }
                    inputConnection.endBatchEdit();
                    return true;
                case "%delete":
                    setLayout("");
                    if (currentShortcut.isEmpty()) {
                        delete();
                    } else {
                        currentShortcut = "";
                    }
                    if (unfinishedCompound) {
                        delete();
                        unfinishedCompound = false;
                    }
                    inputConnection.endBatchEdit();
                    return true;
                case "%enter":
                    setLayout("");
                    if (currentShortcut.isEmpty()) {
                        //Log.e("TAG", Integer.toString(editorInfo.imeOptions));
                        switch (editorInfo.imeOptions & (EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
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
                    } else {
                        currentShortcut = "";
                    }
                    if (unfinishedCompound) {
                        finishAction();
                        unfinishedCompound = false;
                    }
                    inputConnection.endBatchEdit();
                    return true;
                default:
                    if (startKey.charAt(0) == '%' && inBrackets) {
                        finishAction();
                        updateTextInfo();
                        endBracket = beforeCursorText.length() - 1;
                        while (true) {
                            if (currentText.charAt(endBracket) == ']') {
                                inputConnection.setSelection(endBracket + 2, endBracket + 2);
                                setBracket(false);
                                break;
                            }
                            endBracket++;
                        }
                    } else {

                        // Click action
                        setLayout("");
                        if (doesShortcutExist(currentShortcut + startKey)) {

                            // End of shortcut reached
                            //Log.e("TAG", "finished: " + prefix);
                            unfinishedCompound = false;
                            writeShortcut(currentShortcut + startKey);
                            currentShortcut = "";
                            if (inBrackets) {
                                prefix = "_";
                            } else {
                                prefix = "";
                            }
                            inputConnection.endBatchEdit();
                            return true;

                        } else if (doesShortcutExist(finishShortcut(currentShortcut + startKey))) {

                            // Unfinished shortcut
                            //Log.e("TAG", "not finished: " + prefix);
                            setLayout(currentShortcut + startKey);
                            currentShortcut += startKey;

                        } else {

                            // Changed to new shortcut
                            //Log.e("TAG", "new: " + prefix);
                            unfinishedCompound = false;
                            finishAction();
                            if (inBrackets) {
                                prefix = "_";
                            } else {
                                prefix = "";
                            }
                            action(startKey, null);
                            inputConnection.endBatchEdit();
                            return true;
                        }
                    }
            }
        } else {

            // Swipe action
            if (startKey.equals("%.") && endKey.equals("%.")) {
                action("%:", null);
            } else if (startKey.equals("%?") && endKey.equals("%?")) {
                action("%!", null);
            } else if (startKey.charAt(0) == '%' || endKey.charAt(0) == '%') {
                while (true) {
                    //Log.e("TAG", "TEST");
                    if (action(startKey, null)) {
                        break;
                    }
                }
                action(endKey, null);
            } else {

                // Compound word
                if (unfinishedCompound) {
                    unfinishedCompound = false;
                    String tempCurrentShortcut;
                    if (doesShortcutExist(finishShortcut(currentShortcut + startKey))) {
                        tempCurrentShortcut = currentShortcut;
                    } else {
                        tempCurrentShortcut = "";
                    }
                    finishAction();
                    if (inBrackets) {
                        prefix = "_";
                    } else {
                        prefix = "";
                    }
                    currentShortcut = tempCurrentShortcut;
                    action(startKey, endKey);
                } else {
                    unfinishedCompound = true;
                    String tempSuffix = suffix;
                    if (!doesShortcutExist(finishShortcut(currentShortcut + startKey))) {
                        finishAction();
                    }
                    suffix = "-";
                    inputConnection.beginBatchEdit();
                    writeShortcut(finishShortcut(currentShortcut + startKey));
                    suffix = tempSuffix;
                    prefix = "";
                    currentShortcut = "";
                    action(endKey, null);
                }
            }
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        if ("%[".equals(keyValues.get(v.getId()))) {
            action("%[", "%]");
        } else if ("%]".equals(keyValues.get(v.getId()))) {
            action("%]", "%[");
        } else {
            action(keyValues.get(v.getId()), keyValues.get(v.getId()));
        }
        return true;
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
                startKey = currentKey;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                setLayout("");
            } else if (event.getAction() == MotionEvent.ACTION_UP && currentKey != startKey) {

                // Swipe complete
                keys[currentKey].setPressed(false);
                action(keyValues.get(keys[startKey].getId()), keyValues.get(keys[currentKey].getId()));
                return true;
            }
        }

        return false;
    }
}