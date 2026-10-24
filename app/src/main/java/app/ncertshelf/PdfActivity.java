package app.ncertshelf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;

/** Simple built-in PDF reader, used only if no PDF app is installed. */
public class PdfActivity extends Activity {

    private PdfRenderer renderer;
    private ParcelFileDescriptor pfd;
    private final LruCache<Integer, Bitmap> cache = new LruCache<>(6);
    private int pageWidth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String path = getIntent().getStringExtra("path");
        String title = getIntent().getStringExtra("title");
        if (title != null) setTitle(title);

        try {
            pfd = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open this PDF", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        pageWidth = getResources().getDisplayMetrics().widthPixels;
        ListView list = new ListView(this);
        list.setDivider(null);
        list.setBackgroundColor(Color.parseColor("#DDDDDD"));
        list.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return renderer.getPageCount(); }
            @Override public Object getItem(int i) { return null; }
            @Override public long getItemId(int i) { return i; }

            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                ImageView iv = convert instanceof ImageView ? (ImageView) convert : new ImageView(PdfActivity.this);
                iv.setAdjustViewBounds(true);
                iv.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                iv.setPadding(0, 0, 0, 6);
                iv.setImageBitmap(render(pos));
                return iv;
            }
        });
        setContentView(list);
    }

    private Bitmap render(int index) {
        Bitmap bmp = cache.get(index);
        if (bmp != null) return bmp;
        synchronized (this) {
            PdfRenderer.Page page = renderer.openPage(index);
            int h = Math.max(1, (int) ((float) page.getHeight() * pageWidth / page.getWidth()));
            bmp = Bitmap.createBitmap(pageWidth, h, Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.WHITE);
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
        }
        cache.put(index, bmp);
        return bmp;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (renderer != null) renderer.close();
            if (pfd != null) pfd.close();
        } catch (Exception ignored) { }
    }
}