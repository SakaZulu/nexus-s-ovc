package org.nextuxs.ovc;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 *
 * @author NexTuxS
 */
public class ExpertActivity extends Activity {

  byte[] carddata;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    carddata = getIntent().getByteArrayExtra("card_data");
    setContentView(R.layout.sector_list);
    LinearLayout lin = (LinearLayout) findViewById(R.id.lin);
    for (int sec = 0; sec < 40; sec++) {
      String sector_string = Util.getSectorStringFromBinArray(sec, carddata);
      TextView t = new TextView(this);
      Spanned sp = Html.fromHtml("<html><h2>Sector: " + sec + "</h2>"
              + sector_string.replace("\n", "<br>\n") + "</html>");
      t.setText(sp);
      lin.addView(t);
    }
  }
}
