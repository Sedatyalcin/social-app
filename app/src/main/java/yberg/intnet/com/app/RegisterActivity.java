package yberg.intnet.com.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import yberg.intnet.com.app.security.HttpsTrustManager;

/**
 * The activity for registering a new user.
 * Validates all the fields and sends a request to the server.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText firstName, lastName, email, username, password, passwordConfirm;
    private RelativeLayout registerButton;
    private CoordinatorLayout coordinatorLayout;
    private ProgressBar spinner;
    private RequestQueue requestQueue;
    private ImageView check;

    private String stringPasswordsDoNotMatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        stringPasswordsDoNotMatch = getResources().getString(R.string.passwords_do_not_match);

        HttpsTrustManager.allowAllSSL();
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        spinner = (ProgressBar) findViewById(R.id.progressBar);
        spinner.getIndeterminateDrawable().setColorFilter(
                new LightingColorFilter(0xFF000000, Color.WHITE));

        check = (ImageView) findViewById(R.id.login_ok);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        firstName = (EditText) findViewById(R.id.firstName);
        lastName = (EditText) findViewById(R.id.lastName);
        email = (EditText) findViewById(R.id.email);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        passwordConfirm = (EditText) findViewById(R.id.passwordConfirm);

        registerButton = (RelativeLayout) findViewById(R.id.registerButton);
        registerButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        // Check if any input is empty
                        boolean shouldReturn = setBorderIfEmpty(firstName);
                        shouldReturn = setBorderIfEmpty(lastName) || shouldReturn;
                        shouldReturn = setBorderIfEmpty(email) || shouldReturn;
                        shouldReturn = setBorderIfEmpty(username) || shouldReturn;
                        shouldReturn = setBorderIfEmpty(password) || shouldReturn;
                        shouldReturn = setBorderIfEmpty(passwordConfirm) || shouldReturn;
                        if (!passwordConfirm.getText().toString().equals(password.getText().toString())) {
                            ViewGroup parent = (ViewGroup) passwordConfirm.getParent();
                            passwordConfirm.setText("");
                            passwordConfirm.setBackgroundResource(R.drawable.edittext_red_border);
                            passwordConfirm.requestFocus();
                            ((ImageView) parent.getChildAt(parent.indexOfChild(passwordConfirm) + 1))
                                    .setColorFilter(ContextCompat.getColor(RegisterActivity.this, R.color.red));
                            Snackbar.make(coordinatorLayout, stringPasswordsDoNotMatch, Snackbar.LENGTH_LONG).show();
                            shouldReturn = true;
                        }
                        // Return if some input is empty
                        if (shouldReturn)
                            return;

                        setEnabled(false);

                        StringRequest registerRequest = new StringRequest(Request.Method.POST, Database.REGISTER_URL, new Response.Listener<String>() {

                            @Override
                            public void onResponse(String response) {
                                System.out.println("response: " + response);
                                if (response != null) {
                                    try {
                                        JSONObject jsonResponse = new JSONObject(response);
                                        if (jsonResponse.getBoolean("success")) {

                                            check.setVisibility(View.VISIBLE);

                                            SharedPreferences.Editor editor = RegisterActivity.this.getSharedPreferences(
                                                    "com.intnet.yberg", Context.MODE_PRIVATE
                                            ).edit();
                                            editor.clear();
                                            editor.putString("username", username.getText().toString());
                                            editor.apply();

                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Snackbar.make(coordinatorLayout, jsonResponse.getString("message"), Snackbar.LENGTH_LONG).show();
                                            if (jsonResponse.getInt("error") == 1) {
                                                setBorder(username, R.drawable.edittext_red_border, R.color.red);
                                                username.requestFocus();
                                            }
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                setEnabled(true);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                if (error.networkResponse == null) {
                                    if (error.getClass().equals(TimeoutError.class)) {
                                        setEnabled(true);
                                        Snackbar.make(coordinatorLayout, R.string.request_timeout,
                                                Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }) {
                            @Override
                            protected Map<String, String> getParams() throws AuthFailureError {

                                if (password.getText().toString().equals(passwordConfirm.getText().toString())) {
                                    Map<String, String> parameters = new HashMap<>();
                                    parameters.put("username", username.getText().toString());
                                    parameters.put("firstName", firstName.getText().toString());
                                    parameters.put("lastName", lastName.getText().toString());
                                    parameters.put("email", email.getText().toString());
                                    parameters.put("password", password.getText().toString());
                                    return parameters;
                                }
                                return null;
                            }
                        };
                        registerRequest.setRetryPolicy(Database.getRetryPolicy());
                        requestQueue.add(registerRequest);
                    }
                }
        );
        firstName.addTextChangedListener(new LoginActivity.GenericTextWatcher(firstName));
        lastName.addTextChangedListener(new LoginActivity.GenericTextWatcher(lastName));
        username.addTextChangedListener(new LoginActivity.GenericTextWatcher(username));
        email.addTextChangedListener(new LoginActivity.GenericTextWatcher(email));
        password.addTextChangedListener(new LoginActivity.GenericTextWatcher(password));
        passwordConfirm.addTextChangedListener(new LoginActivity.GenericTextWatcher(passwordConfirm));

        EditText.OnEditorActionListener editorActionListener = new EditText.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (v.getId() == R.id.passwordConfirm)
                    registerButton.performClick();
                return false;
            }
        };
        passwordConfirm.setOnEditorActionListener(editorActionListener);

        findViewById(R.id.loginTextView).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    }
                }
        );

        setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }

    /**
     * Enables or disables login button clicks and shows or hides a loading spinner.
     *
     * @param enabled Whether the button should be enabled or disabled
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            registerButton.setEnabled(true);
            registerButton.setBackgroundResource(R.drawable.button);
            spinner.setVisibility(View.INVISIBLE);
        }
        else {
            registerButton.setEnabled(false);
            registerButton.setBackgroundResource(R.drawable.button_pressed);
            spinner.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the border color of an edittext to red if it is empty.
     *
     * @param editText the edittext to be checked
     * @return true if the edittext is empty
     */
    public boolean setBorderIfEmpty(EditText editText) {
        if (editText.getText().toString().equals("")) {
            setBorder(editText, R.drawable.edittext_red_border, R.color.red);
            return true;
        }
        return false;
    }

    /**
     * Set the border color of an EditText.
     * @param editText The EditText to be modified
     * @param drawableId The drawable resource id
     * @param colorId The color resource id
     */
    public void setBorder(EditText editText, int drawableId, int colorId) {
        ViewGroup parent;
        parent = (ViewGroup) editText.getParent();
        editText.setBackgroundResource(drawableId);
        ((ImageView) parent.getChildAt(parent.indexOfChild(editText) + 1)).setColorFilter(
                ContextCompat.getColor(RegisterActivity.this, colorId));
    }
}
