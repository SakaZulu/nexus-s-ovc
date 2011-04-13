package org.nextuxs.ovc;

import android.nfc.tech.MifareClassic;
import android.util.Log;
import java.io.IOException;
import java.util.Arrays;

public class BruteForce {

  static int numberOfTrys = 0;

  public static void main(MifareClassic card, int sector) throws IOException {
    //String password = "pass";
    char[] charset = "0123456789ABCDEF".toCharArray();
    //0000 0000 0000 <-- length of a key
    BruteForce bf = new BruteForce(charset, 12);

    String attempt = bf.toString();
    while (true) {
      //if (attempt.equals(password)) {
      if (!card.isConnected()){
        card.connect();
      }
      if (card.authenticateSectorWithKeyA(sector, Util.hexStringToByteArray(attempt))){
        Log.v("OVC","Password Found: " + attempt);
        break;
      }
      attempt = bf.toString();
      //Log.v("ovc","Tried: " + attempt);
      numberOfTrys++;
      if (numberOfTrys % 200 == 0){
        Log.v("OVC","aantal trys: " + numberOfTrys + " ( " + attempt + " )");
      }
      bf.increment();
    }
  }
  private char[] cs; // Character Set
  private char[] cg; // Current Guess

  public BruteForce(char[] characterSet, int guessLength) {
    cs = characterSet;
    cg = new char[guessLength];
    //Arrays.fill(cg, cs[0]);
    //NOT GOOD CODE AHEAD
    //vul hier in waar hij was gebleven, zodat ie daar verder gaat
    //you can fill in where he was left, it will continue there
    cg[0] = cs[0];
    cg[1] = cs[0];
    cg[2] = cs[0];
    cg[3] = cs[0];
    cg[4] = cs[0];
    cg[5] = cs[0];
    cg[6] = cs[0];
    cg[7] = cs[0]; //A =11
    cg[8] = cs[0]; 
    cg[9] = cs[0]; 
    cg[10] = cs[0]; 
    cg[11] = cs[0]; 
  }

  public void increment() {
    int index = cg.length - 1;
    while (index >= 0) {
      if (cg[index] == cs[cs.length - 1]) {
        if (index == 0) {
          cg = new char[cg.length + 1];
          Arrays.fill(cg, cs[0]);
          break;
        } else {
          cg[index] = cs[0];
          index--;
        }
      } else {
        cg[index] = cs[Arrays.binarySearch(cs, cg[index]) + 1];
        break;
      }
    }
  }

  @Override
  public String toString() {
    return String.valueOf(cg);
  }
}
