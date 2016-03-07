package bit.ihainan.me.bitunionforandroid.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONObject;

import java.io.IOException;

import bit.ihainan.me.bitunionforandroid.R;
import bit.ihainan.me.bitunionforandroid.models.Session;
import bit.ihainan.me.bitunionforandroid.utils.network.BUApi;
import bit.ihainan.me.bitunionforandroid.utils.Global;

/**
 * A login screen that offers login via username / password.
 */
public class LoginActivity extends AppCompatActivity {
    public final static String TAG = LoginActivity.class.getSimpleName();

    // UI references.
    private AutoCompleteTextView mUsername;
    private EditText mPassword;
    private SwitchCompat mSwitchCompatOutNetwork;
    ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // init UI references
        mUsername = (AutoCompleteTextView) findViewById(R.id.user_name);
        mPassword = (EditText) findViewById(R.id.password);
        mSwitchCompatOutNetwork = (SwitchCompat) findViewById(R.id.switch_compat_out_network);

        // 读取配置全局配置信息
        Global.readConfig(this);

        // 自动填充，并设置最原始的登录节点
        if (Global.userName != null && Global.networkType != null) {
            mUsername.setText(Global.userName);
            mPassword.setText(Global.password);
            if (Global.networkType == Global.NETWORK_TYPE.OUT_SCHOOL)
                mSwitchCompatOutNetwork.setChecked(true);
            else mSwitchCompatOutNetwork.setChecked(false);
        } else {
            BUApi.currentEndPoint =
                    mSwitchCompatOutNetwork.isChecked() ?
                            BUApi.OUT_SCHOOL_ENDPOINT :
                            BUApi.IN_SCHOOL_ENDPOINT;
        }

        mPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mSwitchCompatOutNetwork.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BUApi.currentEndPoint = isChecked ? BUApi.OUT_SCHOOL_ENDPOINT : BUApi.IN_SCHOOL_ENDPOINT;
                Global.networkType = isChecked ? Global.NETWORK_TYPE.OUT_SCHOOL : Global.NETWORK_TYPE.IN_SCHOOL;
                Global.saveConfig(LoginActivity.this);
            }
        });

        Button mUserSignInButton = (Button) findViewById(R.id.user_sign_in_button);
        mUserSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mUsername.setError(null);
        mPassword.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();

        // Check whether the username is empty
        if (TextUtils.isEmpty(username)) {
            mUsername.setError(getString(R.string.error_field_required));
            return;
        }

        // Check whether the password is empty
        if (TextUtils.isEmpty(password)) {
            mPassword.setError(getString(R.string.error_invalid_password));
            return;
        }

        // showProgress(true);
        mDialog = ProgressDialog.show(this, "",
                "正在登录", false);
        mDialog.show();

        checkPassword(username, password);
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    /*
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }*/

    /**
     * 连接服务器检查用户密码是否正确，若正确则跳转到首页，否则报错
     *
     * @param userName 用户名
     * @param password 密码
     */
    private void checkPassword(final String userName, final String password) {
        BUApi.tryLogin(this, userName, password,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Global.userName = mUsername.getText().toString();
                        if (mDialog != null) mDialog.dismiss();
                        // showProgress(false);
                        if (BUApi.checkStatus(response)) {
                            try {
                                Global.userSession = BUApi.MAPPER.readValue(response.toString(), Session.class);
                                Global.password = mPassword.getText().toString();
                                Global.saveConfig(LoginActivity.this);
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } catch (IOException e) {
                                mUsername.setError(getString(R.string.error_parse_json));
                                Log.e(TAG, getString(R.string.error_parse_json) + "\n" + response, e);
                                return;
                            }
                        } else {
                            if (mDialog != null) mDialog.dismiss();
                            mUsername.setError(getString(R.string.error_wrong_password));
                            Global.saveConfig(LoginActivity.this);
                            mUsername.setError(getString(R.string.error_wrong_password));
                            return;
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (mDialog != null) mDialog.dismiss();
                        // showProgress(false);
                        mUsername.setError(getString(R.string.error_network));
                        Log.e(TAG, getString(R.string.error_network), error);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 友盟 SDK
        if (Global.uploadData)
            MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 友盟 SDK
        if (Global.uploadData)
            MobclickAgent.onPause(this);
    }
}
