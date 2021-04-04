package com.example.calc.activities;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.calc.R;
import com.example.calc.action.ActionContext;
import com.example.calc.action.ActionType;
import com.example.calc.action.ArithmeticAction;
import com.example.calc.fragments.FragmentBasic;
import com.example.calc.fragments.FragmentScientific;

public class MainActivity extends AppCompatActivity {
    private static final String SHARED_PREFERENCES_NAME = "calc-shared";

    private static final String STORED_OUTPUT_KEY = "output";
    private static final String STORED_ACTION_KEY = "action";
    private static final String STORED_VALUE_KEY = "value";

    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;

    private TextView outputText;
    private String action = "";
    private double value;

    private FragmentBasic basicFragment;
    private FragmentScientific scientificFragment;

    public MainActivity() {
        Log.d("Lifecycle", this.toString() + ".new");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Lifecycle", this.toString() + ".onCreate");
        setContentView(R.layout.activity_main);

        outputText = findViewById(R.id.outputText);
        outputText.setMovementMethod(new ScrollingMovementMethod());

        // Set text to outputText based on saved instance state / shared preferences
        initializeTextBasedOnSavedState(savedInstanceState);

        // Start with basic fragment
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        basicFragment = new FragmentBasic();
        fragmentTransaction.add(R.id.fragmentsLayout, basicFragment).commit();
        fragmentTransaction = fragmentManager.beginTransaction();
        scientificFragment = new FragmentScientific();
        fragmentTransaction.add(R.id.scientificLayout, scientificFragment).commit();
        findViewById(R.id.scientificLayout).setVisibility(View.GONE);

        ((Switch)findViewById(R.id.modeSwitch)).setOnCheckedChangeListener(((buttonView, isChecked) -> {
            try {
                Log.d("Event", this.toString() + ".onCheckedChange: Requesting orientation. isChecked=" + isChecked);
                setRequestedOrientation(isChecked ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                Log.d("Event", this.toString() + ".onCheckedChange: Setting fragment visibility");
                findViewById(R.id.scientificLayout).setVisibility(isChecked ? View.VISIBLE : View.GONE);
            } catch (Exception e) {
                Log.d("error", e.getMessage(), e);
            }
        }));
    }

    /**
     * Reads data from saved instance state and set it to data members.<br/>
     * Saved instance state is used when we change screen orientation and the activity is re-created.<br/>
     * When we first start-up, there is no saved instance state. In this case, we get the data from shared preferences.
     * @param savedInstanceState The saved instance state.
     */
    private void initializeTextBasedOnSavedState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Log.d("Lifecycle", this.toString() + ".onCreate: Saved instance is null");

            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
            if (!sharedPreferences.contains(STORED_VALUE_KEY)) {
                Log.d("Lifecycle", this.toString() + ".onCreate: No data in shared preferences");
                value = 0;
                action = "";
                updateOutputValue();
            } else {
                Log.d("Lifecycle", this.toString() + ".onCreate: Loading data from shared preferences: " + sharedPreferences.getString(STORED_VALUE_KEY, "null"));
                try {
                    value = Double.parseDouble(sharedPreferences.getString(STORED_VALUE_KEY, "0"));
                } catch (Exception e) {
                    Log.e("Error", "Failed to read stored value from shared preferences. Value: " + sharedPreferences.getString(STORED_VALUE_KEY, "0"), e);
                    value = 0;
                }

                outputText.setText(getValueText(false));
                outputText.bringPointIntoView(outputText.length());
            }

            Toast.makeText(this, "Welcome!", Toast.LENGTH_LONG).show();
        } else {
            Log.d("Lifecycle", this.toString() + ".onCreate: Restoring saved instance state");
            value = savedInstanceState.getDouble(STORED_VALUE_KEY, 0);
            action = savedInstanceState.getString(STORED_ACTION_KEY, "");
            outputText.setText(savedInstanceState.getString(STORED_OUTPUT_KEY, ""));
            outputText.bringPointIntoView(outputText.length());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("Event", this.toString() + ".onSaveInstanceState");

        outState.putString(STORED_OUTPUT_KEY, outputText.getText().toString());
        outState.putString(STORED_ACTION_KEY, action);
        outState.putDouble(STORED_VALUE_KEY, value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Lifecycle", this.toString() + ".onDestroy");

        String lastValue = getLine();
        if (!lastValue.isEmpty()) {
            double value;
            try {
                value = parseValue(lastValue);
            } catch (Exception e) {
                Log.e("Error", "Error has occurred while trying to get value to store", e);
                value = 0;
            }

            // Save last value to disk so it will be available next time the app is launched
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
            SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
            sharedPrefsEditor.putString(STORED_VALUE_KEY, String.valueOf(value));
            sharedPrefsEditor.apply();

            Log.d("Event", "Stored to shared preferences: " + value);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.d("Event", this.toString() + ".onCreateContextMenu");
        try {
            // Context menu
            menu.add(Menu.NONE, 2, Menu.NONE, v.getId() == R.id.button2 ? "2" : "3");
            menu.add(Menu.NONE, 3, Menu.NONE, v.getId() == R.id.button2 ? getString(R.string.e) : getString(R.string.pi));
            v.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.button_num_design, null));
        } catch (Throwable t) {
            setErrorOutput(String.format(getString(R.string.error), t.getMessage()));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d("Event", this.toString() + ".onContextItemSelected");
        try {
            String charToAppend = item.getTitle().toString();
            if (Character.isDigit(charToAppend.charAt(0))) {
                onOperandButtonClicked(charToAppend.equals("2") ? basicFragment.getView().findViewById(R.id.button2) : basicFragment.getView().findViewById(R.id.button3));
            }
            // Pi or e
            else {
                appendSpecialChar(charToAppend);
            }

            return true;
        } catch (Throwable t) {
            setErrorOutput(String.format(getString(R.string.error), t.getMessage()));
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        Log.d("Event", this.toString() + ".onBackPressed");
        super.onBackPressed();
    }

    /**
     * Appends a special character (pi or e) that we cannot append at the end of a number.
     * @param specialChar The character to append.
     */
    private void appendSpecialChar(String specialChar) {
        if (getLine().matches("0*")) {
            // In case it is not a new line, discard the last line with zeroes.
            discardLastLineInOutput();

            appendOutputTextAndScrollToBottom(specialChar);
        } else {
            String str = outputText.getText().toString();
            char lastChar = str.charAt(str.length() - 1);
            if (!Character.isDigit(lastChar) && (lastChar != '.') && (lastChar != getString(R.string.e).charAt(0)) && (lastChar != getString(R.string.pi).charAt(0))) {
                appendOutputTextAndScrollToBottom(specialChar);
            }
        }
    }

    /**
     * Find the last line in the output, and return it trimmed.<br/>
     * We use this value in order to perform operations, both single argument and two arguments operation types.
     *
     * @return Last line in the output, to perform operations based on.
     */
    private String getLine() {
        String out = outputText.getText().toString().trim();
        int indexOfNewLine = out.lastIndexOf(System.lineSeparator());
        if (indexOfNewLine >= 0) {
            out = out.substring(indexOfNewLine + System.lineSeparator().length());
        }

        return out;
    }

    /**
     * Parses an input value into double. Also handles Pi and e.
     * @param value The value to parse into double
     * @return A double value representing input value
     */
    private double parseValue(String value) {
        if (value.equals(getString(R.string.e))) {
            return Math.E;
        }

        if (value.equals(getString(R.string.pi))) {
            return Math.PI;
        }

        if (value.equals("-" + getString(R.string.infinity))) {
            return Double.NEGATIVE_INFINITY;
        }

        if (value.equals(getString(R.string.infinity))) {
            return Double.POSITIVE_INFINITY;
        }

        return Double.parseDouble(value);
    }

    public void setErrorOutput(String textToSet) {
        outputText.setText(textToSet);
        if (!textToSet.endsWith(System.lineSeparator())) {
            outputText.append(System.lineSeparator());
        }

        outputText.scrollTo(0, 0);
    }

    private void appendOutputTextAndScrollToBottom(CharSequence textToAppend) {
        outputText.append(textToAppend);
        outputText.bringPointIntoView(outputText.length());
    }

    /**
     * Gets current {@link #value} as text. Also handles Pi and e.
     * @param shouldCastToLong Whether we should treat it as an integer, rather than float. (for Factorial output)
     * @return Current value for output
     */
    private String getValueText(boolean shouldCastToLong) {
        return getValueText(value, shouldCastToLong);
    }

    /**
     * Converts a value as text. Also handles Pi and e.
     * @param shouldCastToLong Whether we should treat it as an integer, rather than float. (for Factorial output)
     * @return Current value for output
     */
    private String getValueText(double value, boolean shouldCastToLong) {
        if (Double.isFinite(value)) {
            return ((value == (long) value) || shouldCastToLong) ? String.valueOf((long) value) : (value == Math.PI ? getString(R.string.pi) : (value == Math.E ? getString(R.string.e) : String.valueOf(value)));
        }

        String infinity = getString(R.string.infinity);
        return value < 0 ? "-" + infinity : infinity;
    }

    /**
     * Call this method when data member {@link #value} is updated with a new value, and you want to print it to the output text view.
     */
    private void updateOutputValue() {
        if (getLine().length() > 0) {
            outputText.append(System.lineSeparator());
        }

        appendOutputTextAndScrollToBottom(getValueText(false));
    }

    /**
     * Raised when user presses the clear (C) button
     *
     * @param view Sender button
     */
    public void onClearButtonClicked(View view) {
        Log.d("Event", this.toString() + ".onClearButtonClicked");
        try {
            outputText.setText("");
            action = "";
            value = 0;
            updateOutputValue();
        } catch (Throwable t) {
            setErrorOutput(String.format(getString(R.string.error), t.getMessage()));
        }
    }

    /**
     * Raised when user presses the equals button, to perform calculation and show the result.
     *
     * @param view Sender button
     */
    public void onEqualsButtonClicked(View view) {
        Log.d("Event", this.toString() + ".onEqualsButtonClicked");
        try {
            String currOut = getLine();

            if (!currOut.isEmpty()) {
                // If there is no action, just print latest row again.
                if (action.isEmpty()) {
                    if (!outputText.getText().toString().endsWith(System.lineSeparator())) {
                        outputText.append(System.lineSeparator());
                    }

                    // In order to let user to see the value behind PI, don't format the value here.
                    double value = parseValue(currOut);
                    String infinity = getString(R.string.infinity);
                    appendOutputTextAndScrollToBottom(value == (long) value ? String.valueOf((long) value) : (Double.isFinite(value) ? String.valueOf(value) : (value < 0 ? "-" + infinity : infinity)));
                    outputText.append(System.lineSeparator());
                } else {
                    int lastActionIndex = currOut.lastIndexOf(action);
                    double lastVal = parseValue(currOut.substring(lastActionIndex + 1));
                    value = doAction(value, lastVal, action);
                    updateOutputValue();
                    outputText.append(System.lineSeparator());

                    action = "";
                    value = 0;
                }
            }
        } catch (Throwable t) {
            setErrorOutput(String.format(getString(R.string.error), t.getMessage()));
        }
    }

    /**
     * Raised when user presses operand (0-9) or dot (.) buttons, to update outputText text view accordingly.
     *
     * @param view Sender button, to get its text and append to output
     */
    public void onOperandButtonClicked(View view) {
        Log.d("Event", this.toString() + ".onOperandButtonClicked");
        try {
            Button senderButton = (Button) view;

            if (getLine().matches("0*") && !".".equals(senderButton.getText().toString())) {
                // In case it is not a new line, discard the last line with zeroes.
                discardLastLineInOutput();
            }

            // In case existing text is empty (when we removed last line with 0) just append the operand.
            String str = outputText.getText().toString();
            if (str.isEmpty()) {
                appendOutputTextAndScrollToBottom(senderButton.getText());
            } else {
                // Otherwise, disallow appending digits to PI and e.
                char lastChar = str.charAt(str.length() - 1);
                if ((lastChar != getString(R.string.e).charAt(0)) && (lastChar != getString(R.string.pi).charAt(0))) {
                    appendOutputTextAndScrollToBottom(senderButton.getText());
                }
            }
        } catch (Throwable t) {
            setErrorOutput(String.format(getString(R.string.error), t.getMessage()));
        }
    }

    /**
     * We use this method in order to remove last output line when it contains zeroes only and user is trying
     * to append a numeric value. Leading zeroes are redundant.
     */
    private void discardLastLineInOutput() {
        String str = outputText.getText().toString();
        if (!str.endsWith(System.lineSeparator())) {
            String[] lines = str.split(System.lineSeparator());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length - 1; i++) {
                sb.append(lines[i]).append(System.lineSeparator());
            }
            outputText.setText(sb);
        }
    }

    /**
     * Raised when user presses operator (+,-,*,/) buttons, to update outputText text view accordingly.
     *
     * @param view Sender button
     */
    public void onOperatorButtonClicked(View view) {
        Log.d("Event", this.toString() + ".onOperatorButtonClicked");
        try {
            Button senderButton = (Button) view;

            String currOut = getLine();

            // Disallow appending several operators one by another, or after infinity results.
            char lastChar = currOut.charAt(currOut.length() - 1);
            if (Character.isDigit(lastChar) || (lastChar == getString(R.string.e).charAt(0)) || (lastChar == getString(R.string.pi).charAt(0))) {
                if (!action.isEmpty()) {
                    int lastActionIndex = currOut.lastIndexOf(action);
                    double lastVal = parseValue(currOut.substring(lastActionIndex + 1));
                    value = doAction(value, lastVal, action);
                } else {
                    value = parseValue(currOut);
                }

                action = senderButton.getText().toString();

                // If user clicks an operator right after clicking equals, copy the last result
                if (outputText.getText().toString().endsWith(System.lineSeparator())) {
                    outputText.append(currOut);
                }

                appendOutputTextAndScrollToBottom(action);
            }
        } catch (Throwable t) {
            setErrorOutput(String.format(getString(R.string.error), t.getMessage()));
        }
    }

    /**
     * Raised when user presses the special function buttons (x^2,x^0.5,n!)
     *
     * @param view Sender button
     */
    public void onFunctionButtonClicked(View view) {
        Log.d("Event", this.toString() + ".onFunctionButtonClicked");
        try {
            String actionText = ((TextView) view).getText().toString();
            doFunction(actionText);
        } catch (Throwable t) {
            setErrorOutput(String.format(getString(R.string.error), t.getMessage()));
        }
    }

    /**
     * Handling a function request. This can be either special function or trigonometric one.<br/>
     * This is a separate method cause we get here from buttons (e.g. x^2) or dropdown list.
     *
     * @param functionText The function to execute.
     */
    public void doFunction(String functionText) {
        try {
            ActionType actionType = ActionType.valueOfActionText(functionText);

            // If there is already an action, calculate it before we can execute a function.
            if (!action.isEmpty()) {
                onEqualsButtonClicked(basicFragment.getView().findViewById(R.id.buttonEquals));
            }

            String currOut = getLine();
            if (!currOut.isEmpty()) {
                value = parseValue(currOut);

                // If user clicks an operator right after clicking equals, copy the last result
                if (!outputText.getText().toString().endsWith(System.lineSeparator())) {
                    outputText.append(System.lineSeparator());
                }

                outputText.append(actionType == ActionType.RAND ? actionType.getDescriptiveText() : String.format(actionType.getDescriptiveText(), getValueText(actionType == ActionType.FACTORIAL)));
            }

            // For functions, act as user pressed equals.
            value = doAction(value, 0, functionText);
            updateOutputValue();
            outputText.append(System.lineSeparator());
            action = "";
            value = 0;
        } catch (Throwable t) {
            setErrorOutput(String.format(getString(R.string.error), t.getMessage()));
        }
    }

    /**
     * Execute an action based on operator or special function.
     *
     * @param value      The value to execute an action on
     * @param lastVal    For operators that take two arguments, this is the right argument
     * @param actionText Action text to execute. See {@link ActionType#valueOfActionText(String)}
     * @return Result of the calculation.
     * @see ActionType
     */
    private double doAction(double value, double lastVal, String actionText) {
        double result = value;

        ActionType actionType = ActionType.valueOfActionText(actionText);
        ArithmeticAction action = actionType.newActionInstance();
        if (action == null) {
            System.out.println("Unknown action: " + actionText);
        } else {
            result = action.executeAsDouble(new ActionContext(lastVal, value));
            if (Double.isNaN(result)) {
                if (actionType == ActionType.DIVIDE) {
                    setErrorOutput(getString(R.string.divideByZero));
                } else {
                    setErrorOutput(String.format(getString(R.string.notEligibleForNegative), actionType.getActionText()));
                    appendOutputTextAndScrollToBottom("Was: " + getValueText(value, false));
                }
                result = 0;
            } else if (Double.isFinite(result) && (result < 1)) {
                // Round until the 15th digit right to the floating point, in order to
                // round sin(pi) to 0, and not -1.2246467991473532E-16. (Because Math.PI keeps 16 digits only)
                result = Math.rint(1e15*result) / 1e15;
            }
        }

        return result;
    }
}