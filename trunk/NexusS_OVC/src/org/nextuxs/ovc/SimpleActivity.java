package org.nextuxs.ovc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.math.BigInteger;
import java.util.GregorianCalendar;

/**
 *
 * @author Frankkie
 */
public class SimpleActivity extends Activity {

  public byte[] card_data;
  public static String log_tag;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    log_tag = getString(R.string.log_tag);
    setContentView(R.layout.ovc);
    card_data = getIntent().getByteArrayExtra("card_data");
    findSaldo();
    findCardNumber();
    findExpDate();
    findCardType();
  }

  public void findSaldo() {
    TextView tv = (TextView) findViewById(R.id.tv_saldo);
    int block1 = 3984; //0xf90
    int block2 = 4000; //0xfa0
    //tijd om te gaan knoeien...
    //BEWARE ! BAD CODE AHEAD
    byte[] gebeure1 = new byte[16];
    System.arraycopy(card_data, block1, gebeure1, 0, 16);
    BigInteger bi = new BigInteger(gebeure1);
    String binary_str = bi.toString(2);
    Log.i(log_tag, "binstr=" + binary_str + "&length=" + binary_str.length());
    //add 0 to the front
    String better_bin = "";
    for (int a = 0; a < (128 - binary_str.length()); a++) {
      better_bin += "0";
    }
    better_bin += binary_str;
    String credit_str = better_bin.substring(77, 93);
    BigInteger le_credit = new BigInteger(credit_str.substring(1), 2);
    int le_credit_int = Integer.parseInt(le_credit.toString());
    double le_credit_double = le_credit_int / 100.0;
    Log.v(log_tag, "Credit: " + le_credit_double);
    String le_credit_string = formatMoney(le_credit_double);

    tv.setText(getString(R.string.credit) + ": " + le_credit_string);
  }

  /**
   * @param amount
   * @return amount string
   */
  public static String formatMoney(double amount) {
    String[] split = (amount + "").split("\\.");
    if (split.length == 1) return amount + ".00";
    else if (split.length == 2) {
      if (split[1].length() == 1) return amount + "0";
      else if (split[1].length() == 2) return amount + "";
      else return (double) ((int) (amount * 100) / 100.0) + "";
    }
    return amount + "";
  }

  public void findCardNumber(){
    String cardId = Util.getCardIdFromBinArray(card_data);
//    Log.v("OVC", "cardid=" + cardId);
//    String top = "";
//    BigInteger bi = new BigInteger(cardId.substring(0,3),16);
//    top += bi.toString() + " ";
//    bi = new BigInteger(cardId.substring(2,4),16);
//    top += bi.toString() + " ";
//    bi = new BigInteger(cardId.substring(4,6),16);
//    top += bi.toString() + " ";
//    bi = new BigInteger(cardId.substring(6,8),16);
//    top += bi.toString() + " ";
    TextView tv = (TextView) findViewById(R.id.tv_kaartnummer);
    tv.setText("Kaartnummer(hex): " + cardId);
  }

  public void findExpDate(){
    String sector_string = Util.getSectorStringFromBinArray(0, card_data);
    String exp_hex_string = sector_string.replace(" ", "").substring(23+32,28+32);
    Log.d(log_tag,sector_string + "&exp_hex_string=" + exp_hex_string);
    byte[] expdate_arr = Util.hexStringToByteArray("0"+exp_hex_string);
    BigInteger bi = new BigInteger(expdate_arr);
    int aantal_dagen = bi.intValue();
    GregorianCalendar start_date = new GregorianCalendar();
    start_date.set(1997, 1, 1);
    start_date.add(GregorianCalendar.DAY_OF_YEAR, aantal_dagen);
    TextView tv = (TextView) findViewById(R.id.tv_expdate);
    tv.setText(getString(R.string.expdate) + ": " + start_date.get(GregorianCalendar.DAY_OF_MONTH) + "-" + start_date.get(GregorianCalendar.MONTH) + "-" + start_date.get(GregorianCalendar.YEAR));
  }

  public void findCardType(){
    
  }
}
