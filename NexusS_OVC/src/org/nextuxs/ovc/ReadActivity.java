package org.nextuxs.ovc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 *
 * @author NexTuxS
 */
public class ReadActivity extends Activity {

  public Handler handler = new Handler();
  public ProgressDialog dialog;
  public static byte[] card_data;
  public static String log_tag;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    log_tag = getString(R.string.log_tag);
    setContentView(R.layout.main);
    Button btn_readDump = (Button) findViewById(R.id.btn_dump_laden);
    btn_readDump.setOnClickListener(new android.view.View.OnClickListener() {

      public void onClick(View arg0) {
        startReadDump();
      }
    });
    if (getIntent().getBooleanExtra("kaartGevonden", false)) {
      Tag tag = (Tag) getIntent().getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
      final MifareClassic mfc = MifareClassic.get(tag);
      startReadCard(mfc);
    }
  }

  public void startReadDump() {
    Intent intent = new Intent("org.openintents.action.PICK_FILE");
    startActivityForResult(intent, 1);
  }

  public void continueReadDump(Uri uri) {
    try {
      File file = new File(URI.create(uri.toString()));
      card_data = Util.readBinFileIntoByteArray(file);
    } catch (Exception e) {
      Toast.makeText(this, getString(R.string.error_see_log), 1).show();
      e.printStackTrace();
    }

    AlertDialog.Builder b = new AlertDialog.Builder(this);
    b.setTitle(R.string.now_what);
    b.setCancelable(true);
    b.setPositiveButton(getString(R.string.simple), new OnClickListener() {

      public void onClick(DialogInterface arg0, int arg1) {
        toNoob();
      }
    });
    b.setNeutralButton(getString(R.string.expert), new OnClickListener() {

      public void onClick(DialogInterface arg0, int arg1) {
        toNerd();
      }
    });
    b.setNegativeButton(getString(R.string.import_keys), new OnClickListener() {

      public void onClick(DialogInterface arg0, int arg1) {
        toImportKeys();
      }
    });
    b.create().show();
  }

  public void toImportKeys() {
    DatabaseHelper dh = new DatabaseHelper(this);
    dh.makeTables();
    String card_id = "";
    card_id = Util.getCardIdFromBinArray(card_data);
    try {
      Util.getKey(this, card_id, 22, "a");
      Toast.makeText(this, getString(R.string.already_keys), 1).show();
      return;
    } catch (NoKeyInDBRuntimeException e) {
      //niets, dit hoort juist
    }
    for (int a = 0; a < 40; a++) {
      Log.v(log_tag, "a=" + a);
      String s = Util.getSectorStringFromBinArray(a, card_data);
      String[] keys = Util.getKeysFromSectorTrailerBlockString(
              Util.getSectorTrailerBlockFromSectorString(s));
      dh.insertKey(card_id, a, "a", keys[0]); //1 = acces bits
      dh.insertKey(card_id, a, "b", keys[2]);
    }
    Toast.makeText(this, getString(R.string.done), 0).show();
  }

  public void startReadCard(MifareClassic mfc) {
    dialog = ProgressDialog.show(this, getString(R.string.reading_card), getString(R.string.please_wait));
    dialog.setCancelable(true);
    CardReader cr = new CardReader(mfc);
    cr.start();
  }

  public void dismissDialog(Dialog d) {
    try {
      d.dismiss();
    } catch (Exception e) {
      //le nothing
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    NfcManager manager = (NfcManager) getSystemService(NFC_SERVICE);
    if (manager == null) {
      Toast.makeText(this, getString(R.string.no_nfc), 1).show();
      finish();
      return;
    }
    NfcAdapter adapter = manager.getDefaultAdapter();
    if (adapter == null) {
      Toast.makeText(this, getString(R.string.no_nfc), 1).show();
      finish();
      return;
    }
    if (!adapter.isEnabled()) {
      Toast.makeText(this, getString(R.string.not_enabled), 1).show();
      finish();
      return;
    }
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 1337, new Intent(this, ReadActivity.class).putExtra("kaartGevonden", true), PendingIntent.FLAG_CANCEL_CURRENT);
    adapter.enableForegroundDispatch(this, pendingIntent, null, null);
  }

  public void cardReadingDone(byte[] arr) {
    card_data = arr;
    dismissDialog(dialog);
    AlertDialog.Builder b = new AlertDialog.Builder(this);
    b.setTitle(R.string.now_what);
    b.setCancelable(true);
    b.setPositiveButton(R.string.simple, new OnClickListener() {

      public void onClick(DialogInterface arg0, int arg1) {
        toNoob();
      }
    });
    b.setNeutralButton(R.string.expert, new OnClickListener() {

      public void onClick(DialogInterface arg0, int arg1) {
        toNerd();
      }
    });
    b.setNegativeButton(R.string.save, new OnClickListener() {

      public void onClick(DialogInterface arg0, int arg1) {
        toSave();
      }
    });
    b.create().show();
  }

  public void toNoob() {
    Intent i = new Intent();
    i.setClass(this, SimpleActivity.class);
    i.putExtra("card_data", card_data);
    startActivity(i);
  }

  public void toNerd() {
    Intent i = new Intent();
    i.setClass(this, ExpertActivity.class);
    i.putExtra("card_data", card_data);
    startActivity(i);
  }

  public void toSave() {
    Intent intent = new Intent("org.openintents.action.PICK_FILE");
    startActivityForResult(intent, 2);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 1) { //read dump
      continueReadDump(data.getData());
      return;
    } else if (requestCode == 2) { //save dump
      Uri uri = data.getData();
      URI le_uri = URI.create(uri.toString());
      try {
        Util.writeByteArrayToFile(new File(le_uri), card_data);
        Toast.makeText(this, getString(R.string.done), 0).show();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, 0, 0, getString(R.string.clear_db));
    menu.add(0, 1, 0, getString(R.string.copy_db_2_sd));
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    switch (id) {
      case 0: {
        DatabaseHelper dh = new DatabaseHelper(this);
        dh.clearDB();
        break;
      }
      case 1: {
        try {
          Util.copyDbToSd();
        } catch (IOException e) {
          Toast.makeText(this, getString(R.string.error_see_log), 1).show();
          e.printStackTrace();
        }
      }
    }
    return true;
  }

  public class CardReader extends Thread {

    MifareClassic mfc;
    String cardnumber;

    public CardReader(MifareClassic mfc) {
      this.mfc = mfc;
    }

    @Override
    public void run() {
      final byte[] arr = cardToByteArray();
      if (arr != null) {
        handler.post(new Runnable() {

          public void run() {
            cardReadingDone(arr);
          }
        });
      }
    }

    public byte[] cardToByteArray() {
      byte[] carddata = new byte[4096];
      try {
        mfc.connect();
        cardnumber = Util.getCardNumber(mfc);
        for (int sector = 0; sector < 40; sector++) {
          byte[] key_a = Util.getKey(ReadActivity.this, cardnumber, sector, "a");
          byte[] key_b = Util.getKey(ReadActivity.this, cardnumber, sector, "b");
          byte[] sector_data = readSector(mfc, sector, key_a, key_b);
          int carddata_pos = mfc.sectorToBlock(sector) * 16;
          System.arraycopy(sector_data, 0, carddata, carddata_pos, sector_data.length);
        }
      } catch (NoKeyInDBRuntimeException e) {
        Log.e(getString(R.string.log_tag), "NoKeyInDBRuntimeException in: CardReader.cardToByteArray");
        e.printStackTrace();
        handler.post(new Runnable() {

          public void run() {
            Toast.makeText(ReadActivity.this, getString(R.string.error_no_key), 1).show();
          }
        });
        dismissDialog(dialog);
        try {
          mfc.close();
        } catch (Exception ex) {
          Log.e(log_tag, "ex: " + ex);
        }
        return null;
      } catch (Exception e) {
        Log.e(getString(R.string.log_tag), "Exception in: CardReader.cardToByteArray");
        e.printStackTrace();
        handler.post(new Runnable() {

          public void run() {
            Toast.makeText(ReadActivity.this, getString(R.string.error_see_log), 1).show();
          }
        });
        dismissDialog(dialog);
        try {
          mfc.close();
        } catch (Exception ex) {
          Log.e(log_tag, "ex: " + ex);
        }
        return null;
      }

      try {
        mfc.close();
      } catch (Exception ex) {
        Log.e(log_tag, "ex: " + ex);
      }
      return carddata;
    }

    /**
     * read a sector from the card
     * return data on succes,
     * empty array on failure
     * @param mfc the card
     * @param sector the sector
     * @param key_a keya
     * @param key_b keyb
     * @return the data, or an empty array on failure
     */
    public byte[] readSector(MifareClassic mfc, int sector, byte[] key_a, byte[] key_b) {
      int maxBlock = mfc.sectorToBlock(sector);
      byte[] ans;
      if (sector <= 31) {
        maxBlock += 4;
        ans = new byte[4 * 16];
      } else {
        maxBlock += 16;
        ans = new byte[16 * 16];
      }
      try {
        if (!mfc.authenticateSectorWithKeyA(sector, key_a)
                || !mfc.authenticateSectorWithKeyB(sector, key_b)) {
          Log.e(getString(R.string.log_tag), getString(R.string.incorrect_key) + " sector: " + sector);
          Log.e(getString(R.string.log_tag), "key_a: " + Util.byteArrayToString(key_a));
          Log.e(getString(R.string.log_tag), "key_b: " + Util.byteArrayToString(key_b));
          return ans;
        }

        for (int block = mfc.sectorToBlock(sector); block < maxBlock; block++) {
          Log.i(getString(R.string.log_tag), "Read block: " + block + " (sector: " + sector + ")");
          byte[] readBlock = mfc.readBlock(block);
          System.arraycopy(readBlock, 0, ans, (block - mfc.sectorToBlock(sector)) * 16, readBlock.length);
          //KEYS
          if (block == (maxBlock - 1)) { //keys zitten in laatste block v/d sector
            System.arraycopy(key_a, 0, ans, (block - mfc.sectorToBlock(sector)) * 16, key_a.length);
            System.arraycopy(key_b, 0, ans, ((block - mfc.sectorToBlock(sector)) * 16) + 10, key_a.length);
          }
        }
      } catch (IOException e) {
        Log.e(getString(R.string.log_tag), getString(R.string.error_readblock) + " sector: " + sector);
        return ans;
      }
      return ans;
    }
  }
}
