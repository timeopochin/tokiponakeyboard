package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.preference.PreferenceManager;

public class MyKeyboardEmoji extends MyKeyboardAbstract {

    public MyKeyboardEmoji(Context context) {
        this(context, null, 0);
    }

    public MyKeyboardEmoji(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyKeyboardEmoji(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
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
                if (currentKey != startKey || keyValues.get(keys[startKey].getId()).equals("%shift")) {
                    if (currentShortcut.equals("X") || currentShortcut.equals("x")) {
                        setLayout("x");
                    } else if (keyValues.get(keys[startKey].getId()).equals("%shift")) {
                        setLayout("X");
                    } else {
                        setLayout("");
                    }
                    stopDelete = true;
                } else if (!actionComplete) {
                    if (currentShortcut.equals("X") || currentShortcut.equals("x")) {
                        setLayout("x");
                    } else if (keyValues.get(keys[startKey].getId()).equals("%shift")) {
                        setLayout("X");
                    } else {
                        setLayout("");
                    }
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

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {

        // Load shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        LayoutInflater.from(context).inflate(R.layout.keyboard_emoji, this, true);

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
        keys[23] = findViewById(R.id.shift);
        keys[24] = findViewById(R.id.dot);
        keys[25] = findViewById(R.id.colon);
        keys[26] = findViewById(R.id.enter);
        keys[27] = findViewById(R.id.delete);

        setDeleteListener(keys[27]);

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

        keyValues.put(R.id.bracket, "_");
        keyValues.put(R.id.dot, ".");
        keyValues.put(R.id.shift, "%shift");
        keyValues.put(R.id.colon, ":");
        keyValues.put(R.id.enter, "%enter");

        keyValues.put(R.id.delete, "%delete");

        // Arrays from xml
        Resources res = getResources();
        shortcuts = res.getStringArray(R.array.shortcuts_emoji);
        words = res.getStringArray(R.array.words_emoji);
        unofficialWords = res.getStringArray(R.array.unofficial_words_emoji);
    }

    protected void action(String startKey, String endKey) {
        if (endKey == null) {

            // Single key sent
            if (startKey.charAt(0) == '%') {

                // Special key sent
                if (!startKey.equals("%delete") && !currentShortcut.equals("X") && !currentShortcut.equals("x")) {
                    finishAction("finish");
                }

                switch (startKey) {
                    case "%shift":

                        // Latin input
                        if (currentShortcut.equals("X") || currentShortcut.equals("x")) {
                            currentShortcut = "";
                        } else {
                            currentShortcut = "X";
                        }
                        setLayout(currentShortcut);

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
                    if (currentShortcut.equals("X") || currentShortcut.equals("x")) {
                        currentShortcut = "x";
                    } else {
                        currentShortcut = "";
                    }
                    setLayout(currentShortcut);

                } else if (doesShortcutExist(finishShortcut(currentShortcut + startKey))) {

                    // Key is part of previous action but it is still unfinished
                    currentShortcut += startKey;
                    setLayout("");
                    setLayout(currentShortcut);

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
        } else {

            // Two keys sent
            if (startKey.charAt(0) == '%' || endKey.charAt(0) == '%') {

                // Two separate keys to be sent (at least one was a special key)
                if (startKey.equals(endKey)) {

                    // Long press actions for special keys
                    finishAction("finish");
                    if (startKey.equals("%enter")) {
                        inputMethodManager.showInputMethodPicker();
                    } else {
                        Log.e(null, "Shortcut: " + startKey + " is not a special key");
                    }

                } else if (startKey.charAt(0) == '%') {

                    // Special key followed by normal key
                    if (startKey.equals("%enter")) {

                        // Switch subtype
                        finishAction("finish");
                        inputMethodService.setEmojiMode(false);

                    } else {
                        action(startKey, null);
                        action(endKey, null);
                    }

                } else {

                    // Normal key followed by special key
                    action(startKey, null);
                    finishAction("finish");
                    action(endKey, null);
                }
            } else {

                // A compound glyph to be sent (Both were letter/word keys)
                finishAction(startKey);
                writeShortcut(finishShortcut(currentShortcut + startKey));
                currentShortcut = "";
                action(endKey, null);
            }
        }
    }

    protected void delete() {
        if ("Xx".contains(currentShortcut)) {
            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            updateCurrentState();
        } else {

            // Cancel current input in progress
            currentShortcut = "";
            setLayout("");
        }
    }

    /* nextKey allows the function to know whether or not to reset currentShortcut in the cases
     * where a new compound glyph has been started.
     */
    public void finishAction(String nextKey) {
        boolean validCombination = doesShortcutExist(finishShortcut(currentShortcut + nextKey));
        if (!validCombination) {
            writeShortcut(finishShortcut(currentShortcut));
            currentShortcut = "";
            setLayout("");
        }
    }

    public void loadPreferences() {
        setColours();
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
        findViewById(R.id.keyboard_emoji).setBackgroundColor(backgroundColour);
    }

    public void updateCurrentState() {

        // Get the adjacent characters
        String charOnLeft = getPreviousCharacter();
        if ("aeijklmnopstuwAEIJKLMNOPSTUW".contains(charOnLeft) && !charOnLeft.isEmpty()) {
            currentShortcut = "x";
        } else {
            currentShortcut = "";
        }
        setLayout(currentShortcut);
    }

    private void write(String toWrite) {
        inputConnection.commitText(toWrite, 1);
    }

    private void writeShortcut(String shortcut) {

        // Do not write anything if the shortcut is empty
        if (shortcut.isEmpty()) {
            return;
        }

        write(getWord(shortcut));
    }
}