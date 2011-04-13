package org.nextuxs.ovc;

import android.content.Context;
import android.nfc.tech.MifareClassic;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author NexTuxS
 */
public class Util {

  public static void copyDbToSd() throws IOException {
    File sd = Environment.getExternalStorageDirectory();
    File data = Environment.getDataDirectory();

    if (sd.canWrite()) {

      String currentDBPath = "/data/org.nextuxs.ovc/databases/ovc.db";
      String backupDBPath = "/ovc/ovc.db";

      File currentDB = new File(data, currentDBPath);
      File backupDB = new File(sd, backupDBPath);

      File sdFolder = new File(Environment.getExternalStorageDirectory(), "/ovc/");
      sdFolder.mkdirs();

      if (backupDB.exists()) {
        backupDB.delete();
      }

      if (currentDB.exists()) {
        copyFile(currentDB, backupDB);
      }
    }
  }

  public static byte[] readBinFileIntoByteArray(File file) throws IOException {
    //File file = new File("/somepath/myfile.ext");
    FileInputStream is = new FileInputStream(file);

// Get the size of the file
    long length = file.length();

    if (length > Integer.MAX_VALUE) {
      throw new IOException("The file is too big");
    }

// Create the byte array to hold the data
    byte[] bytes = new byte[(int) length];

// Read in the bytes
    int offset = 0;
    int numRead = 0;
    while (offset < bytes.length
            && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
      offset += numRead;
    }

// Ensure all the bytes have been read in
    if (offset < bytes.length) {
      throw new IOException("The file was not completely read: " + file.getName());
    }

// Close the input stream, all file contents are in the bytes variable
    is.close();
    return bytes;
  }

  public static void copyFile(File oldFile, File newFile) {
    try {
      byte[] old_data = readBinFileIntoByteArray(oldFile);
      writeByteArrayToFile(newFile, old_data);
    } catch (Exception e) {
      Log.e("OVC", "Catch at: Util.copyFile");
      e.printStackTrace();
    }
  }

  public static void writeByteArrayToFile(File file, byte[] data) throws FileNotFoundException, IOException {
    Log.i("NFC", "Util.writeByteArrayToFile: " + file.getAbsolutePath());
    FileOutputStream fos = null;
    file.createNewFile();
    fos = new FileOutputStream(file);
    fos.write(data);
    fos.close();
  }

  public static String byteArrayToString(byte[] b) {
    String result = "";
    for (int i = 0; i < b.length; i++) {
      result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
    }
    //ADD SPACES
    StringBuilder sb = new StringBuilder();
    for (int n = 0; n < result.length(); n++) {
      sb.append(result.toCharArray(), n, 1);
      if ((n + 1) % 4 == 0) {
        sb.append(" ");
      }
    }
    //return result;
    return sb.toString();
  }

  public static byte[] hexStringToByteArray(String s) {
    s = s.replace(" ", "").trim();
    //Log.d("NFC", "hexStringToByteArray.s=" + s);
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
              + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  /**
   * Get the CardNumber
   * @param mfc the card
   * @return the CardNumber
   * @throws IOException
   */
  public static String getCardNumber(MifareClassic mfc) throws IOException {
    mfc.authenticateSectorWithKeyA(0, Util.hexStringToByteArray("000000000000"));
    byte[] readBlock = mfc.readBlock(0);
    return Util.byteArrayToString(readBlock).replace(" ", "").substring(0, 8);
  }

  /**
   * Get Key
   * @param c Context, needed for Database
   * @param cardnumber
   * @param sector
   * @param key_ab must be 'a' of 'b'
   * @return the key as byteArray
   */
  public static byte[] getKey(Context c, String cardnumber, int sector, String key_ab) {
    //Default Keys
    if (sector < 22) {
      if (sector == 0 && key_ab.equals("a")) {
        return Util.hexStringToByteArray("000000000000");
      }
      return Util.hexStringToByteArray("b5ff67cba951");
    }

    DatabaseHelper dh = new DatabaseHelper(c);
    String key = dh.getKey(cardnumber, sector, key_ab);
    return Util.hexStringToByteArray(key);
  }

  /**
   * this is the same as mfc.sectorToBlock() !
   * only used, when a card is not available.
   * @param sector
   * @return number of firstblock from sector
   */
  public static int getFirstBlockOfSector(int sector) {
    int first_block;
    if (sector <= 31) {
      first_block = sector * 4;
    } else {
      // first_block = (sector - 32) * 16 + 128
      first_block = (sector - 24) * 16;
    }
    return first_block;
  }

  public static String getSectorStringFromBinArray(int selectedSector, byte[] carddata) {
    Log.v("OVC", "getSectorFromBinArray");
    int maxBlock = 4;
    if (!(selectedSector <= 31)) {
      maxBlock = 16;
    }
    StringBuilder strs = new StringBuilder();
    for (int tfz = 0; tfz < maxBlock; tfz++) {
      byte[] temp = new byte[16];
      System.arraycopy(carddata, (Util.getFirstBlockOfSector(selectedSector) + tfz) * 16, temp, 0, 16);
      strs.append(Util.byteArrayToString(temp));
      strs.append("\n");
    }
    return strs.toString();
  }

  public static String getCardIdFromBinArray(byte[] arr) {
    String eersteSector = Util.getSectorStringFromBinArray(0, arr);
    //Log.w("NFC","getCardID : " + eersteSector);
    //Log.e("NFC", "fu:" + eersteSector.replace(" ", "").substring(0, 8));
    return eersteSector.replace(" ", "").substring(0, 8);
  }

  public static String getSectorTrailerBlockFromSectorString(String s) {
    Log.v("OVC", "getSectorTrailerBlockFromSectorString: " + s + " (length=" + s.length() + ")");
    s = s.replace(" ", "");
    if (s.length() == ((32 * 4) + 4)) {
      return s.substring((32 * 3) + 3);
    } else {
      return s.substring((32 * 15) + 15);
    }
  }

  public static String[] getKeysFromSectorTrailerBlockString(String block) {
    String[] ans = new String[3];
    ans[0] = block.substring(0, 12);
    ans[1] = block.substring(12, 20);
    ans[2] = block.substring(20);
    return ans;
  }
}
