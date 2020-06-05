package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

public abstract class MyKeyboardAbstract extends LinearLayout implements View.OnLongClickListener, View.OnClickListener {

    // Input connection
    protected InputConnection inputConnection;
    protected EditorInfo editorInfo;
    protected InputMethodManager inputMethodManager;
    protected TokiPonaIME inputMethodService;

    // Key detection
    protected int previousKey;
    protected int currentKey;
    protected int startKey;
    protected boolean actionComplete = false;

    // Arrays
    protected SparseArray<String> keyValues = new SparseArray<>();
    protected String[] shortcuts;
    protected String[] words;
    protected String[] unofficialWords;

    // Word construction
    protected int quoteNestingLevel = 0;
    protected String currentShortcut = "";

    // Text manipulation
    protected CharSequence currentText;
    protected CharSequence beforeCursorText;
    protected CharSequence afterCursorText;
    protected boolean stopDelete = false;

    // Preferences
    SharedPreferences sharedPreferences;

    // Colours
    protected int letterKeyColour;
    protected int commonWordKeyColour;
    protected int specialKeyColour;

    protected int letterKeyTextColour;
    protected int commonWordKeyTextColour;
    protected int specialKeyTextColour;

    protected int lastStateKeyColour;
    protected int intermediateKeyColour;
    protected int lastStateUnofficialKeyColour;
    protected int intermediateUnofficialKeyColour;

    protected int lastStateKeyTextColour;
    protected int intermediateTextKeyColour;
    protected int lastStateUnofficialKeyTextColour;
    protected int intermediateTextUnofficialKeyColour;

    protected int backgroundColour;

    protected Button[] keys = new Button[28];

    public MyKeyboardAbstract(Context context) {
        this(context, null, 0);
    }

    public MyKeyboardAbstract(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyKeyboardAbstract(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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

    @SuppressLint("ClickableViewAccessibility")
    protected void setDeleteListener(Button delete) {

        // Hold delete key for continuous delete
        delete.setOnTouchListener(new View.OnTouchListener() {

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
    }

    protected void action(String startKey, String endKey) {}

    protected void delete() {}

    protected boolean doesShortcutExist(String shortcutToCheck) {
        for (String shortcut : shortcuts) {
            if (shortcutToCheck.equals(shortcut)) {
                return true;
            }
        }
        return false;
    }

    protected void enter() {

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

    // Returns a shortcut if it exists and returns the finished shortcut if it does not exist yet
    protected String finishShortcut(String shortcutToFinish) {
        if (doesShortcutExist(shortcutToFinish) || shortcutToFinish.isEmpty()) {
            return shortcutToFinish;
        } else {
            return shortcutToFinish + shortcutToFinish.charAt(shortcutToFinish.length() - 1);
        }
    }

    protected String getAdjacentCharacters() {
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

    protected String getNextCharacter() {
        updateTextInfo();
        boolean atEndOfInput = afterCursorText.length() == 0;
        String charOnRight = "";
        if (!atEndOfInput) {
            charOnRight = Character.toString(afterCursorText.charAt(0));
        }
        return charOnRight;
    }

    protected String getPreviousCharacter() {
        updateTextInfo();
        boolean atStartOfInput = beforeCursorText.length() == 0;
        String charOnLeft = "";
        if (!atStartOfInput) {
            charOnLeft = Character.toString(beforeCursorText.charAt(beforeCursorText.length() - 1));
        }
        return charOnLeft;
    }

    protected String getWord(String shortcut) {
        for (int i = 0; i < shortcuts.length; i++) {
            if (shortcut.equals(shortcuts[i])) {
                return words[i];
            }
        }
        return "";
    }

    protected boolean isUnofficial(String word) {
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

    protected void moveCursorBackOne() {
        updateTextInfo();
        int backOne = beforeCursorText.length() - 1;
        inputConnection.setSelection(backOne, backOne);
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
    }

    public void setEditorInfo(EditorInfo ei) {
        editorInfo = ei;
    }

    public void setIMM(InputMethodManager imm) {
        inputMethodManager = imm;
    }

    public void setIMS(TokiPonaIME ims) {
        inputMethodService = ims;
    }

    public void setInputConnection(InputConnection ic) {
        inputConnection = ic;
    }

    protected void setLayout(String layoutShortcut) {
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

    public void updateCurrentState() {}

    protected void updateQuoteNestedLevel() {
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

    protected void updateTextInfo() {
        ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
        if (extractedText != null) {
            currentText = extractedText.text;
            beforeCursorText = inputConnection.getTextBeforeCursor(currentText.length(), 0);
            afterCursorText = inputConnection.getTextAfterCursor(currentText.length(), 0);
        } else {
            currentText = beforeCursorText = afterCursorText = "";
        }
    }
}