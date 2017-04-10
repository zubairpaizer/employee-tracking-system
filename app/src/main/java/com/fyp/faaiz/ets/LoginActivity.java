package com.fyp.faaiz.ets;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fyp.faaiz.ets.model.Employee;
import com.fyp.faaiz.ets.session.Session;
import com.fyp.faaiz.ets.utils.Parser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener {

    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    TextView register_login;
    TextInputLayout textInputLayout;
    EditText login_password;
    EditText login_email;
    RadioButton login_agent;
    RadioButton login_owner;
    Button sigin_button;
    Session _session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // checking login
        auth();
        init();
        events();
    }

    public void events() {
        login_password.setOnFocusChangeListener(this);
        register_login.setOnClickListener(this);
        sigin_button.setOnClickListener(this);
    }

    private void lognRequest() {
        if (validate()) return;

        StringRequest request = new StringRequest(Request.Method.POST, "http://192.168.0.105/Ets/ets_user_login.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("QUERY", response);
                if (response.contains("first_name")) {

                    List<Employee> parse = Parser.parse(response);

                    Log.d("First Name", parse.get(0).getFirst_name());

                    int local_id = parse.get(0).getId();

                    String local_full_name = parse.get(0).getFirst_name() + " " + parse.get(0).getFirst_name();

                    String local_email = parse.get(0).getEmail();

                    _session.createLoginSession(local_id, local_full_name, local_email);

                    Intent i = new Intent(LoginActivity.this, MainActivity.class);

                    startActivity(i);

                    finish();

                } else {
                    Toast.makeText(LoginActivity.this, "username/password invalid", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                NetworkResponse networkResponse = volleyError.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (volleyError.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (volleyError.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404)
                            errorMessage = "Resource not found";
                        else if (networkResponse.statusCode == 401)
                            errorMessage = message + " Please login again";
                        else if (networkResponse.statusCode == 400)
                            errorMessage = message + " Check your inputs";
                        else if (networkResponse.statusCode == 500)
                            errorMessage = message + " Something is getting wrong";

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("Error", errorMessage);
                volleyError.printStackTrace();

            }
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put(EMAIL, login_email.getText().toString());
                params.put(PASSWORD, login_password.getText().toString());
                return params;
            }
        };
        RequestQueue queue = Volley.newRequestQueue(getApplication());
        queue.add(request);
        //AppController.getInstance().addToRequestQueue(request);
    }

    private boolean validate() {
        /* email */
        String EMAIL_PATTERN =
                "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

        if (TextUtils.isEmpty(login_email.getText())) {
            login_email.setError("email field can't be blank");
            return true;
        }

        Pattern p = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = p.matcher(login_email.getText());


        if (!matcher.matches()) {
            login_email.setError("invalid email address");
            return true;
        }

        /* password */
        if (TextUtils.isEmpty(login_password.getText())) {
            login_password.setError("password field can't be blank");
            return true;
        }

        if (login_password.getText().toString().length() > 40 || login_password.getText().toString().length() < 6) {
            login_password.setError("invalid password range");
            return true;
        }

        if (login_email.getText().toString().length() > 40 || login_email.getText().toString().length() < 8) {
            login_email.setError("invalid email range");
            return true;
        }

        if (login_agent.isChecked()) {
            Toast.makeText(LoginActivity.this, login_agent.getText(), Toast.LENGTH_SHORT).show();
        }
        if (login_owner.isChecked()) {
            Toast.makeText(LoginActivity.this, login_owner.getText(), Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    private void init() {
        textInputLayout = (TextInputLayout) findViewById(R.id.password_textinput);
        login_password = (EditText) findViewById(R.id.login_password);
        login_email = (EditText) findViewById(R.id.login_email);

        register_login = (TextView) findViewById(R.id.register_intent);

        login_agent = (RadioButton) findViewById(R.id.loginAgentRadio);
        login_owner = (RadioButton) findViewById(R.id.loginOwnerRadio);

        sigin_button = (Button) findViewById(R.id.signin_button);
    }

    private void auth() {
        _session = new Session(getApplicationContext());
        if (_session.isUserLoggedIn()) {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (_session.isUserLoggedIn()) {
            finish();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (_session.isUserLoggedIn()) {
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_intent:
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
                finish();
                break;
            case R.id.signin_button:
                lognRequest();
                break;
            default:
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.login_password:
                if (hasFocus) {
                    textInputLayout.setPasswordVisibilityToggleEnabled(true);
                }
                if (!hasFocus) {
                    textInputLayout.setPasswordVisibilityToggleEnabled(false);
                }
                break;
            default:
                break;
        }
    }
}