package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class TokiPonaIME extends InputMethodService {

    private MyKeyboard keyboard;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView() {
        keyboard = getLayoutInflater().inflate(R.layout.keyboard_wrapper, null).findViewById(R.id.keyboard);
        return keyboard;
    }

    @Override
    public void onInitializeInterface() {
        if (keyboard != null) {
            keyboard.onPreferenceChange();
        }
    }

    @Override
    public void onStartInputView (EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        keyboard.setEditorInfo(info);

        InputConnection ic = getCurrentInputConnection();
        keyboard.setInputConnection(ic);
        keyboard.setIME((InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE));
        keyboard.updateCurrentState();
    }

    @Override
    public void onUpdateSelection (int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        keyboard.updateCurrentState();
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        keyboard.finishAction("finish");
        keyboard.setBracket(false);
    }

}