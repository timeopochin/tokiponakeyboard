package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class TokiPonaIME extends InputMethodService {

    private View keyboardWrapper;
    MyKeyboard keyboard;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView() {
        keyboardWrapper = getLayoutInflater().inflate(R.layout.keyboard_wrapper, null);
        return keyboardWrapper;
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        keyboard.finishAction("finish");
        keyboard.setBracket(false);
    }

    @Override
    public void onStartInputView (EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        InputConnection ic = getCurrentInputConnection();
        keyboard = keyboardWrapper.findViewById(R.id.keyboard);
        keyboard.setInputConnection(ic);
        keyboard.setEditorInfo(info);
        keyboard.setIME((InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE));
        keyboard.updateCurrentState();
    }

    @Override
    public void onUpdateSelection (int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        keyboard.updateCurrentState();
    }
}