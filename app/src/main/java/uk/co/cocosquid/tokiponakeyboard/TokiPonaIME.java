package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.preference.PreferenceManager;


public class TokiPonaIME extends InputMethodService {

    private boolean emojiMode;
    private MyKeyboard keyboard;
    private MyKeyboardEmoji keyboardEmoji;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView() {
        updatePreferences();

        keyboardEmoji = getLayoutInflater().inflate(R.layout.keyboard_wrapper_emoji, null).findViewById(R.id.keyboard_emoji);
        keyboard = getLayoutInflater().inflate(R.layout.keyboard_wrapper, null).findViewById(R.id.keyboard);

        if (emojiMode) {
            return keyboardEmoji;
        }
        return keyboard;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);

        InputConnection ic = getCurrentInputConnection();
        InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);

        updatePreferences();

        keyboardEmoji.setIMS(this);
        keyboardEmoji.loadPreferences();
        keyboardEmoji.setEditorInfo(info);
        keyboardEmoji.setInputConnection(ic);
        keyboardEmoji.setIMM(imm);
        keyboardEmoji.updateCurrentState();

        keyboard.setIMS(this);
        keyboard.loadPreferences();
        keyboard.setEditorInfo(info);
        keyboard.setInputConnection(ic);
        keyboard.setIMM(imm);
        keyboard.updateCurrentState();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        if (emojiMode) {
            keyboardEmoji.updateCurrentState();
        } else {
            keyboard.updateCurrentState();
        }
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();

        keyboardEmoji.finishAction("finish");
        keyboard.finishAction("finish");
    }

    public void setEmojiMode(boolean newEmojiMode) {
        if (newEmojiMode) {
            emojiMode = true;
            setInputView(keyboardEmoji);
            keyboardEmoji.updateCurrentState();
        } else {
            emojiMode = false;
            setInputView(keyboard);
            keyboard.updateCurrentState();
        }
    }

    private void updatePreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //boolean previousEmojiMode = emojiMode;
        emojiMode = sharedPreferences.getBoolean("emoji_mode", false);

        if (keyboard != null) {
            if (emojiMode) {
                setInputView(keyboardEmoji);
            } else {
                setInputView(keyboard);
            }
        }
    }

}