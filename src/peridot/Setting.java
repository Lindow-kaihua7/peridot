package peridot;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author lindow
 */
public class Setting {

    public String Token;
    public String Secret;

    public Setting() {
    }
    
    
    
    /**
     * @return AccessToken
     */
    public String getAccessToken() {
        return this.Token;
    }

    /**
     * @param value AccessToken
     */
    public void setAccessToken(String value) {
        this.Token = value;
    }

    /**
     * @return AccessTokenSecret
     */
    public String getAccessSecret() {
        return this.Secret;
    }

    /**
     * @param value AccessTokenSecret
     */
    public void setAccessSecret(String value) {
        this.Secret = value;
    }
    
    /**
     * 
     * @return summaryFlag
     */
    /*
    public String getSummaryFlag() {
        return this.summaryFlag;
    }
    * 
    */
    
    /**
     * @param value summaryFlag
     */
    /*
    public void setSummaryFlag(String value) {
        this.summaryFlag = value;
    }
    * 
    */

    /**
     * save to XML File
     * @param path PATH to save
     * @param object object
     */
    public synchronized void writeXML(String path, Object object) {
        XMLEncoder enc = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileOutputStream fileStream = null;
        try {
            enc = new XMLEncoder(out);
            enc.writeObject(object);
            enc.close();
            byte[] xmlbuff = out.toByteArray();

            fileStream = new FileOutputStream(path);
            fileStream.write(xmlbuff);
            fileStream.flush();
        } catch (IOException ex) {
        } finally {
            if (enc != null) {
                enc.close();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * read from XML File
     * @param path PATH
     */
    public Object readXML(String path) {
        XMLDecoder d = null;
        try {
            try {
                d = new XMLDecoder(new BufferedInputStream(new FileInputStream(path)));
            } catch (FileNotFoundException ex) {
            }
            return d.readObject();
        } finally {
            if (d != null) {
                d.close();
            }
        }
    }

    /**
     * save object
     */
    public void saveSettings() {
        try {
            writeXML(System.getProperty("user.home") + System.getProperty("file.separator") + "config.settings", this);
            System.out.println("saved.");
        } catch (Exception e) {
            System.out.println("Unable to save file.");
        }

    }

    /**
     * read object
     */
    public void loadSettings() {
        try {
            this.Token = ((Setting) readXML(System.getProperty("user.home") + System.getProperty("file.separator") + "config.settings")).getAccessToken();
            this.Secret = ((Setting) readXML(System.getProperty("user.home") + System.getProperty("file.separator") + "config.settings")).getAccessSecret();
        } catch (Exception e) {
            System.out.println("Unable to load file.");
        }
    }
}
