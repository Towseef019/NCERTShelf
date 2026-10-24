package app.ncertshelf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQ_PICK_FOLDER = 42;
    private WebView web;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private File pdfDir() {
        File d = new File(getFilesDir(), "pdfs");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private int pdfCount() {
        String[] n = pdfDir().list((dir, name) -> name.endsWith(".pdf"));
        return n == null ? 0 : n.length;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(false);

        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String url = r.getUrl().toString();
                if (url.toLowerCase().endsWith(".pdf")) {
                    String name = url.substring(url.lastIndexOf('/') + 1);
                    openPdf(name.substring(0, name.length() - 4));
                    return true;
                }
                if (url.startsWith("http")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
                    } catch (ActivityNotFoundException ignored) { }
                    return true;
                }
                return false;
            }
        });
        web.loadUrl("file:///android_asset/index.html");

        if (pdfCount() == 0) firstRun();
    }

    private class Bridge {
        @JavascriptInterface
        public int count() { return pdfCount(); }

        @JavascriptInterface
        public void importPdfs() { ui.post(MainActivity.this::pickFolder); }
    }

    private void firstRun() {
        String[] bundled = null;
        try { bundled = getAssets().list("pdfs"); } catch (Exception ignored) { }
        if (bundled != null && bundled.length > 0) {
            copyFromAssets(bundled);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Import your NCERT PDFs")
                .setMessage("Choose the folder on your phone that has the NCERT PDFs "
                        + "(for example the 'ncert' or 'pdfs' folder). They will be copied "
                        + "into the app once, and after that everything works offline.")
                .setCancelable(false)
                .setPositiveButton("Choose folder", (d, w) -> pickFolder())
                .setNegativeButton("Later", null)
                .show();
    }

    private void pickFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(i, REQ_PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PICK_FOLDER || res != RESULT_OK || data == null || data.getData() == null) return;
        DocumentFile root = DocumentFile.fromTreeUri(this, data.getData());
        if (root == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Copying PDFs…");
        pd.setCancelable(false);
        pd.show();
        new Thread(() -> {
            int n = copyTree(root, 0, pd);
            ui.post(() -> {
                pd.dismiss();
                Toast.makeText(this, n == 0 ? "No PDFs found in that folder"
                        : "Imported " + n + " PDFs", Toast.LENGTH_LONG).show();
                web.reload();
            });
        }).start();
    }

    private int copyTree(DocumentFile dir, int depth, ProgressDialog pd) {
        int count = 0;
        DocumentFile[] files = dir.listFiles();
        for (DocumentFile f : files) {
            if (f.isDirectory() && depth < 2) {
                count += copyTree(f, depth + 1, pd);
            } else if (f.isFile() && f.getName() != null
                    && f.getName().toLowerCase().endsWith(".pdf")) {
                File out = new File(pdfDir(), f.getName().toLowerCase());
                if (out.exists() && out.length() == f.length()) { count++; continue; }
                final String label = f.getName();
                ui.post(() -> pd.setMessage("Copying " + label));
                try (InputStream in = getContentResolver().openInputStream(f.getUri());
                     OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[256 * 1024];
                    int r;
                    while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
                    count++;
                } catch (Exception e) {
                    out.delete();
                }
            }
        }
        return count;
    }

    private void copyFromAssets(String[] names) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Setting up your books (one time)…");
        pd.setCancelable(false);
        pd.show();
        new Thread(() -> {
            for (String n : names) {
                File out = new File(pdfDir(), n);
                try (InputStream in = getAssets().open("pdfs/" + n);
                     OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[256 * 1024];
                    int r;
                    while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
                } catch (Exception e) {
                    out.delete();
                }
            }
            ui.post(() -> { pd.dismiss(); web.reload(); });
        }).start();
    }

    private void openPdf(String name) {
        if (!name.matches("[A-Za-z0-9]+")) return;
        File f = new File(pdfDir(), name.toLowerCase() + ".pdf");
        if (!f.exists()) {
            Toast.makeText(this, name + ".pdf is not in the app yet. Use \"Import PDFs\" at the bottom of the page.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", f);
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/pdf");
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(view);
        } catch (ActivityNotFoundException e) {
            Intent in = new Intent(this, PdfActivity.class);
            in.putExtra("path", f.getAbsolutePath());
            in.putExtra("title", name);
            startActivity(in);
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}