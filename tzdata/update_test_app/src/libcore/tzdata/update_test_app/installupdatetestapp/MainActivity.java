/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package libcore.tzdata.update_test_app.installupdatetestapp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final String UPDATE_CERTIFICATE_KEY = "config_update_certificate";
    private static final String EXTRA_REQUIRED_HASH = "REQUIRED_HASH";
    private static final String EXTRA_SIGNATURE = "SIGNATURE";
    private static final String EXTRA_VERSION_NUMBER = "VERSION";

    public static final String TEST_CERT = "" +
            "MIIDsjCCAxugAwIBAgIJAPLf2gS0zYGUMA0GCSqGSIb3DQEBBQUAMIGYMQswCQYDVQQGEwJVUzET" +
            "MBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEPMA0GA1UEChMGR29v" +
            "Z2xlMRAwDgYDVQQLEwd0ZXN0aW5nMRYwFAYDVQQDEw1HZXJlbXkgQ29uZHJhMSEwHwYJKoZIhvcN" +
            "AQkBFhJnY29uZHJhQGdvb2dsZS5jb20wHhcNMTIwNzE0MTc1MjIxWhcNMTIwODEzMTc1MjIxWjCB" +
            "mDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDU1vdW50YWluIFZp" +
            "ZXcxDzANBgNVBAoTBkdvb2dsZTEQMA4GA1UECxMHdGVzdGluZzEWMBQGA1UEAxMNR2VyZW15IENv" +
            "bmRyYTEhMB8GCSqGSIb3DQEJARYSZ2NvbmRyYUBnb29nbGUuY29tMIGfMA0GCSqGSIb3DQEBAQUA" +
            "A4GNADCBiQKBgQCjGGHATBYlmas+0sEECkno8LZ1KPglb/mfe6VpCT3GhSr+7br7NG/ZwGZnEhLq" +
            "E7YIH4fxltHmQC3Tz+jM1YN+kMaQgRRjo/LBCJdOKaMwUbkVynAH6OYsKevjrOPk8lfM5SFQzJMG" +
            "sA9+Tfopr5xg0BwZ1vA/+E3mE7Tr3M2UvwIDAQABo4IBADCB/TAdBgNVHQ4EFgQUhzkS9E6G+x8W" +
            "L4EsmRjDxu28tHUwgc0GA1UdIwSBxTCBwoAUhzkS9E6G+x8WL4EsmRjDxu28tHWhgZ6kgZswgZgx" +
            "CzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3" +
            "MQ8wDQYDVQQKEwZHb29nbGUxEDAOBgNVBAsTB3Rlc3RpbmcxFjAUBgNVBAMTDUdlcmVteSBDb25k" +
            "cmExITAfBgkqhkiG9w0BCQEWEmdjb25kcmFAZ29vZ2xlLmNvbYIJAPLf2gS0zYGUMAwGA1UdEwQF" +
            "MAMBAf8wDQYJKoZIhvcNAQEFBQADgYEAYiugFDmbDOQ2U/+mqNt7o8ftlEo9SJrns6O8uTtK6AvR" +
            "orDrR1AXTXkuxwLSbmVfedMGOZy7Awh7iZa8hw5x9XmUudfNxvmrKVEwGQY2DZ9PXbrnta/dwbhK" +
            "mWfoepESVbo7CKIhJp8gRW0h1Z55ETXD57aGJRvQS4pxkP8ANhM=";


    public static final String TEST_KEY = "" +
            "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKMYYcBMFiWZqz7SwQQKSejwtnUo" +
            "+CVv+Z97pWkJPcaFKv7tuvs0b9nAZmcSEuoTtggfh/GW0eZALdPP6MzVg36QxpCBFGOj8sEIl04p" +
            "ozBRuRXKcAfo5iwp6+Os4+TyV8zlIVDMkwawD35N+imvnGDQHBnW8D/4TeYTtOvczZS/AgMBAAEC" +
            "gYBxwFalNSwZK3WJipq+g6KLCiBn1JxGGDQlLKrweFaSuFyFky9fd3IvkIabirqQchD612sMb+GT" +
            "0t1jptW6z4w2w6++IW0A3apDOCwoD+uvDBXrbFqI0VbyAWUNqHVdaFFIRk2IHGEE6463mGRdmILX" +
            "IlCd/85RTHReg4rl/GFqWQJBANgLAIR4pWbl5Gm+DtY18wp6Q3pJAAMkmP/lISCBIidu1zcqYIKt" +
            "PoDW4Knq9xnhxPbXrXKv4YzZWHBK8GkKhQ0CQQDBQnXufQcMew+PwiS0oJvS+eQ6YJwynuqG2ejg" +
            "WE+T7489jKtscRATpUXpZUYmDLGg9bLt7L62hFvFSj2LO2X7AkBcdrD9AWnBFWlh/G77LVHczSEu" +
            "KCoyLiqxcs5vy/TjLaQ8vw1ZQG580/qJnr+tOxyCjSJ18GK3VppsTRaBznfNAkB3nuCKNp9HTWCL" +
            "dfrsRsFMrFpk++mSt6SoxXaMbn0LL2u1CD4PCEiQMGt+lK3/3TmRTKNs+23sYS7Ahjxj0udDAkEA" +
            "p57Nj65WNaWeYiOfTwKXkLj8l29H5NbaGWxPT0XkWr4PvBOFZVH/wj0/qc3CMVGnv11+DyO+QUCN" +
            "SqBB5aRe8g==";

    private EditText actionEditText;
    private EditText versionEditText;
    private EditText contentPathEditText;
    private EditText requiredHashEditText;
    private TextView logView;

    private ExecutorService executor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button triggerInstallButton = (Button) findViewById(R.id.trigger_install_button);
        triggerInstallButton.setOnClickListener(this);

        actionEditText = (EditText) findViewById(R.id.action);
        versionEditText = (EditText) findViewById(R.id.version);
        contentPathEditText = (EditText) findViewById(R.id.content_path);
        requiredHashEditText = (EditText) findViewById(R.id.required_hash);
        logView = (TextView) findViewById(R.id.log);
        executor = Executors.newFixedThreadPool(1);
    }

    @Override
    public void onClick(View v) {
        final String action = actionEditText.getText().toString();
        final String contentPath = contentPathEditText.getText().toString();
        final String version = versionEditText.getText().toString();
        final String requiredHash = requiredHashEditText.getText().toString();

        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final File contentFile = new File(contentPath);
                File tempDir = new File(getFilesDir(), "temp");
                if (!tempDir.exists() && !tempDir.mkdir()) {
                    publishProgress("Unable to create: " + tempDir);
                    return null;
                }

                File copyOfContentFile;
                try {
                    copyOfContentFile = File.createTempFile("content", ".tmp", tempDir);
                    copyFile(contentFile, copyOfContentFile);
                } catch (IOException e) {
                    publishProgress("Error", exceptionToString(e));
                    return null;
                }
                publishProgress("Created copy of " + contentFile + " at " + copyOfContentFile);

                String originalCert = null;
                try {
                    originalCert = overrideCert(TEST_CERT);
                    sleep(1000);
                    publishProgress("Overridden update cert");

                    String signature = createSignature(copyOfContentFile, version, requiredHash);
                    sendIntent(copyOfContentFile, action, version, requiredHash, signature);
                    publishProgress("Sent update intent");
                } catch (Exception e) {
                    publishProgress("Error", exceptionToString(e));
                } finally {
                    if (originalCert != null) {
                        sleep(1000);
                        try {
                            overrideCert(originalCert);
                            publishProgress("Reverted update cert");
                        } catch (Exception e) {
                            publishProgress("Unable to revert update cert", exceptionToString(e));
                        }
                    }
                }
                publishProgress("Update intent sent successfully");
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                for (String message : values) {
                    addToLog(message, null);
                }
            }
        }.executeOnExecutor(executor);
    }

    private String overrideCert(String cert) throws Exception {
        final String key = UPDATE_CERTIFICATE_KEY;
        String originalCert = Settings.Secure.getString(getContentResolver(), key);
        if (!Settings.Secure.putString(getContentResolver(), key, cert)) {
            throw new Exception("Unable to override update certificate");
        }
        return originalCert;
    }

    private void sleep(long millisDelay) {
        try {
            Thread.sleep(millisDelay);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    private void sendIntent(
            File contentFile, String action, String version, String required, String sig) {
        Intent i = new Intent();
        i.setAction(action);
        Uri contentUri =
                FileProvider.getUriForFile(
                        getApplicationContext(), "libcore.tzdata.update_test_app.fileprovider",
                        contentFile);
        i.setData(contentUri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.putExtra(EXTRA_VERSION_NUMBER, version);
        i.putExtra(EXTRA_REQUIRED_HASH, required);
        i.putExtra(EXTRA_SIGNATURE, sig);
        sendBroadcast(i);
    }

    private void addToLog(String message, Exception e) {
        logString(message);
        if (e != null) {
            String text = exceptionToString(e);
            logString(text);
        }
    }

    private void logString(String value) {
        logView.append(new Date() + " " + value + "\n");
        int scrollAmount =
                logView.getLayout().getLineTop(logView.getLineCount()) - logView.getHeight();
        logView.scrollTo(0, scrollAmount);
    }

    private static String createSignature(File contentFile, String version, String requiredHash)
            throws Exception {
        byte[] contentBytes = readBytes(contentFile);
        Signature signer = Signature.getInstance("SHA512withRSA");
        signer.initSign(createKey());
        signer.update(contentBytes);
        signer.update(version.trim().getBytes());
        signer.update(requiredHash.getBytes());
        return new String(Base64.encode(signer.sign(), Base64.DEFAULT));
    }

    private static byte[] readBytes(File contentFile) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(contentFile)) {
            int count;
            byte[] buffer = new byte[8192];
            while ((count = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, count);
            }
        }
        return baos.toByteArray();
    }

    private static PrivateKey createKey() throws Exception {
        byte[] derKey = Base64.decode(TEST_KEY.getBytes(), Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(derKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private static String exceptionToString(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.getBuffer().toString();
    }

    private static void copyFile(File from, File to) throws IOException {
        byte[] buffer = new byte[8192];
        int count;
        try (
                FileInputStream in = new FileInputStream(from);
                FileOutputStream out = new FileOutputStream(to)
        ) {
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }
    }
}
