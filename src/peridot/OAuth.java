package peridot;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import net.arnx.jsonic.JSON;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import sun.misc.BASE64Encoder;

/**
 *
 * @author lindow
 */
public class OAuth {

    /**
     * 状態
     */
    enum oauthStatus {

        NOT_AUTHORIZED,
        AUTHORIZATION_FAILED,
        AUTHORIZED,
        FAILED,
        SUCCESS
    }

    /**
     * Tweetの内容
     */
    class Tweet {

        public String statusID;
        public String screenName;
        public String name;
        public String text;

        Tweet(String statusid, String screenname, String name, String text) {
            this.statusID = statusid;
            this.screenName = screenname;
            this.name = name;
            this.text = text;
        }
    }
    /**
     * 
     */
    static ArrayList<Tweet> tweetList = new ArrayList<Tweet>();
    static String message = "What's up ? :";
    static Charset charset = Charset.forName("UTF-8");
    static CharsetEncoder charsetEncoder = charset.newEncoder();
    userStream gt = new userStream();
    /**
     * 
     */
    private oauthStatus status;
    private String requestToken = "";
    private String requestTokenSecret = "";
    private String pin = "";
    private String consumerKey = "";
    private String consumerSecret = "";
    private String accessToken = "";
    private String accessTokenSecret = "";
    private String requestTokenURL = "https://api.twitter.com/oauth/request_token";
    private String authorizeURL = "https://twitter.com/oauth/authorize";
    private String accesstokenURL = "https://api.twitter.com/oauth/access_token";
    private String postURL = "https://api.twitter.com/statuses/update.xml";
    private String userStreamURL = "https://userstream.twitter.com/2/user.json";
    private String friendsListURL = "https://api.twitter.com/1/friends/ids.xml";
    private String favURL = "https://api.twitter.com/1/favorites/create/";
    private String unfavURL = "https://api.twitter.com/1/favorites/destroy/";
    private String retweetURL = "https://api.twitter.com/1/statuses/retweet/";
    private String removeURL = "https://api.twitter.com/1/statuses/destroy/";
    private String searchURL = "https://search.twitter.com/search.json";
    private String mentionsURL = "https://api.twitter.com/1/statuses/mentions.json";
    private String favoritesURL = "https://api.twitter.com/1/favorites.json";
    private String timeLineURL = "https://api.twitter.com/1/statuses/home_timeline.json";
    
    /*
    private String summaryURL1 = "http://api.twitter.com/i/statuses/";
    private String summaryURL2 = "/activity/summary.json";
    private String summaryFlag = "";
    * 
    */

    /**
     * @return oauthStatus
     */
    public oauthStatus getStatus() {
        return this.status;
    }

    /**
     * 
     * @param value PIN
     */
    public void setPin(String value) {
        this.pin = value;
    }

    /**
     * @return accessTokenSecret
     */
    public String getAccessTokenSecret() {
        return this.accessTokenSecret;
    }

    /**
     * @param value accessTokenSecret
     */
    public void setAccessTokenSecret(String value) {
        this.accessTokenSecret = value;
    }

    /**
     * @return accessToken
     */
    public String getAccessToken() {
        return this.accessToken;
    }

    /**
     * @param value accessToken
     */
    public void setAccessToken(String value) {
        this.accessToken = value;
    }

    /**
     * 
     * @param consumerKey consumerKey
     * @param consumerSecret consumerSecret
     */
    OAuth(String consumerKey, String consumerSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;

        this.status = oauthStatus.NOT_AUTHORIZED;
        if (this.accessTokenSecret != null && this.accessToken != null) {
            this.status = oauthStatus.AUTHORIZED;
            return;
        }
    }

    /**
     * get AccessToken
     * 
     * @return success or not
     */
    public boolean accessTokenAuthorization() {
        if (this.accessTokenSecret != null && this.accessToken != null) {
            this.status = oauthStatus.AUTHORIZED;
            return true;
        }

        if (this.pin == "") {
            this.status = oauthStatus.NOT_AUTHORIZED;
            return false;
        }

        SortedMap<String, String> params = getParamString(this.consumerKey, this.requestToken, this.pin, null);

        String paramStr = "";

        for (Entry<String, String> param : params.entrySet()) {
            paramStr += "&" + param.getKey() + "=" + param.getValue();
        }
        paramStr = paramStr.substring(1);

        String text = "POST" + "&" + urlEncode(this.requestTokenURL) + "&" + urlEncode(paramStr);
        String key = urlEncode(consumerSecret) + "&" + urlEncode(this.requestTokenSecret);

        String sig = getSig(key, text);

        params.put("oauth_signature", sig);


        String authorizationHeader = getHeader(params);


        try {
            URL connectUrl = new URL(this.accesstokenURL);
            HttpURLConnection con = (HttpURLConnection) connectUrl.openConnection();
            con.setRequestMethod("POST");
            con.addRequestProperty("Authorization", authorizationHeader);
            con.connect();

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                String line = null;
                String res = null;
                while ((line = br.readLine()) != null) {
                    res += line;
                }

                String[] response = res.split("&");

                for (String s : response) {
                    if (s.indexOf("oauth_token=") != -1) {
                        this.accessToken = s.substring(s.indexOf("=") + 1);
                    }
                    if (s.indexOf("oauth_token_secret=") != -1) {
                        this.accessTokenSecret = s.substring(s.indexOf("=") + 1);
                    }
                }

                System.out.println("AccessToken : " + this.accessToken);
                System.out.println("AccessTokenSecret : " + this.accessTokenSecret);
            }
        } catch (Exception e) {
            this.status = oauthStatus.AUTHORIZATION_FAILED;
            return false;
        }


        this.status = oauthStatus.AUTHORIZED;
        return true;
    }

    /**
     * get PinCode URL
     * get requestToken
     * 
     * @return URL
     */
    public String requestTokenAuthorization() {
        SortedMap<String, String> params = getParamString(this.consumerKey, null, null, null);

        String paramStr = "";

        for (Entry<String, String> param : params.entrySet()) {
            paramStr += "&" + param.getKey() + "=" + param.getValue();
        }
        paramStr = paramStr.substring(1);

        String text = "POST" + "&" + urlEncode(this.requestTokenURL) + "&" + urlEncode(paramStr);
        String key = urlEncode(consumerSecret) + "&" + urlEncode(this.requestTokenSecret);

        String sig = getSig(key, text);

        params.put("oauth_signature", sig);


        String authorizationHeader = getHeader(params);

        try {
            URL connectUrl = new URL(this.requestTokenURL);
            HttpURLConnection con = (HttpURLConnection) connectUrl.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", authorizationHeader);
            con.connect();


            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                //System.out.println("HTTP_OK");
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                String line = null;
                String res = "";
                while ((line = br.readLine()) != null) {
                    res += line;
                }

                String[] response = res.split("&");

                for (String s : response) {
                    if (s.indexOf("oauth_token=") != -1) {
                        this.requestToken = s.substring(s.indexOf("=") + 1);
                    }
                    if (s.indexOf("oauth_token_secret=") != -1) {
                        this.requestTokenSecret = s.substring(s.indexOf("=") + 1);
                    }
                }

                System.out.println("RequestToken : " + this.requestToken);
                System.out.println("RequestTokenSecret : " + this.requestTokenSecret);


            } else {
                System.out.println("Request_Token Failed...");
                this.status = oauthStatus.AUTHORIZATION_FAILED;
                return "";
            }
        } catch (Exception e) {
            this.status = oauthStatus.AUTHORIZATION_FAILED;
            return "";
        }

        this.status = oauthStatus.NOT_AUTHORIZED;
        return this.authorizeURL + "?oauth_token=" + this.requestToken;
    }

    /**
     * Post to Twitter
     * 
     * @param str content
     * @param replyStatusID StatusID
     */
    public boolean post(String str, String replyStatusID) {
        String tweet = urlEncode(str);

        SortedMap<String, String> params = getParamString(this.consumerKey, this.accessToken, null, tweet);

        String paramStr = "";

        for (Entry<String, String> param : params.entrySet()) {
            paramStr += "&" + param.getKey() + "=" + param.getValue();
        }
        paramStr = paramStr.substring(1);

        String text = "POST" + "&" + urlEncode(this.postURL) + "&"
                + ((replyStatusID != null) ? ("in_reply_to_status_id%3D" + replyStatusID + "%26") : "")
                + "include_entities%3Dtrue" + "%26" + urlEncode(paramStr);

        String key = urlEncode(this.consumerSecret) + "&" + urlEncode(this.accessTokenSecret);

        String sig = getSig(key, text);

        params.put("oauth_signature", sig);

        params.remove("status");


        String authorizationHeader = getHeader(params);

        String body = "?status=" + tweet + "&include_entities=true" + (replyStatusID != null ? ("&in_reply_to_status_id=" + replyStatusID) : "");

        try {
            URL connectUrl = new URL(this.postURL + body);
            HttpsURLConnection con = (HttpsURLConnection) connectUrl.openConnection();
            con.setRequestMethod("POST");
            con.addRequestProperty("Authorization", authorizationHeader);
            con.connect();

            if (con.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

            } else {
                System.out.println(con.getResponseCode());
            }
        } catch (Exception e) {
            this.status = oauthStatus.FAILED;
            return false;
        }

        this.status = oauthStatus.SUCCESS;

        return true;
    }

    /**
     * generate parameter string
     * 
     * @param consumerkey consumerKey
     * @param oauthtoken Token(RequestToken or AccessToken)
     * @param oauthverifier PinCode
     * @param status Tweet
     * @return 
     */
    private SortedMap getParamString(String consumerKey, String oauthtoken, String oauthverifier, String status) {
        SortedMap<String, String> params = new TreeMap<String, String>();
        params.put("oauth_consumer_key", consumerKey);
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", String.valueOf(getTime()));
        params.put("oauth_nonce", String.valueOf(Math.random()));
        params.put("oauth_version", "1.0");
        if (oauthtoken != null) {
            params.put("oauth_token", oauthtoken);
        }
        if (oauthverifier != null) {
            params.put("oauth_verifier", oauthverifier);
        }
        if (status != null) {
            params.put("status", status);
        }

        return params;
    }

    /**
     * get TimeLine
     */
    public void getTimeLine() {
        String res = basicTwitterAccess("GET", this.timeLineURL);
        if (res == null) {
            return;
        }

        if (res != null) {
            try {
                ArrayList list = JSON.decode(res, ArrayList.class);

                for (Object o : list) {
                    HashMap<String, Object> map = new HashMap<String, Object>((HashMap) o);

                    if (map.containsKey("user") && map.containsKey("text")) {
                        HashMap user = (HashMap) map.get("user");

                        String id = map.get("id_str").toString();
                        String sn = user.get("screen_name").toString();
                        String name = user.get("name").toString();
                        String text = map.get("text").toString();

                        setTweets(id, sn, name, text);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("GetTimeLine Failed...");
            }
        }
    }

    /**
     * get Favorites
     */
    public void getFavorites() {
        String res = basicTwitterAccess("GET", this.favoritesURL);
        if (res == null) {
            return;
        }

        if (res != null) {
            try {
                ArrayList list = JSON.decode(res, ArrayList.class);

                for (Object o : list) {
                    HashMap<String, Object> map = new HashMap<String, Object>((HashMap) o);


                    if (map.containsKey("user") && map.containsKey("text")) {
                        HashMap user = (HashMap) map.get("user");
                        String id = map.get("id_str").toString();
                        String sn = user.get("screen_name").toString();
                        String name = user.get("name").toString();
                        String text = map.get("text").toString();

                        setTweets(id, sn, name, text);
                    }

                }
            } catch (Exception e) {
                System.out.println("GetFavorites Failed...");
            }
        }
    }

    /**
     * get Mentions
     */
    public void getMentions() {
        String res = basicTwitterAccess("GET", this.mentionsURL);
        if (res == null) {
            return;
        }

        if (res != null) {
            try {
                ArrayList list = JSON.decode(res, ArrayList.class);

                for (Object o : list) {
                    HashMap<String, Object> map = new HashMap<String, Object>((HashMap) o);

                    if (map.containsKey("user") && map.containsKey("text")) {
                        HashMap user = (HashMap) map.get("user");
                        String id = map.get("id_str").toString();
                        String sn = user.get("screen_name").toString();
                        String name = user.get("name").toString();
                        String text = map.get("text").toString();
                        
                        setTweets(id, sn, name, text);
                    }

                }
            } catch (Exception e) {
                System.out.println("GetFavorites Failed...");
            }
        }
    }

    /**
     * add to favorites
     * 
     * @param statusID statusid
     */
    public boolean fav(String statusID) {
        String res = basicTwitterAccess("POST", this.favURL + statusID + ".xml");
        return (res != null);
    }

    /**
     * remove from favorites
     * 
     * @param statusID statusid
     */
    public boolean unFav(String statusID) {
        String res = basicTwitterAccess("POST", this.unfavURL + statusID + ".xml");
        return (res != null);
    }

    /**
     * Retweet
     * 
     * @param statusID statusid
     */
    public boolean retweet(String statusID) {
        String res = basicTwitterAccess("POST", this.retweetURL + statusID + ".xml");
        return (res != null);
    }

    /**
     * remove post
     * 
     * @param statusID statusid
     */
    public boolean remove(String statusID) {
        String res = basicTwitterAccess("POST", this.removeURL + statusID + ".xml");
        return (res != null);
    }

    /**
     * search query word
     * 
     * @param query queryword
     */
    public void search(String query) {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(this.searchURL + "?q=" + urlEncode(query));

        BufferedReader reader = null;
        try {
            HttpResponse response = client.execute(httpget);
            StatusLine statusline = response.getStatusLine();

            String line = null;
            String res = "";
            if (statusline.getStatusCode() == HttpStatus.SC_OK) {

                HttpEntity httpentity = response.getEntity();
                reader = new BufferedReader(new InputStreamReader(httpentity.getContent()));

                while ((line = reader.readLine()) != null) {
                    res += line;
                }

                ArrayList list = JSON.decode(res, ArrayList.class);

                for (Object o : list) {
                    String str = o + "";

                    try {
                        if (str.length() > 100) {
                            for (Object ob : (ArrayList) o) {
                                HashMap<String, Object> map = new HashMap<String, Object>((HashMap) ob);

                                String id = map.get("id_str").toString();
                                String sn = map.get("from_user").toString();
                                String name = map.get("from_user_name").toString();
                                String text = map.get("text").toString();


                                setTweets(id, sn, name, text);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
            System.out.println("finished.");
        } catch (IOException e) {
            this.status = oauthStatus.FAILED;
            return;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                httpget.abort();
            }
        }
    }

    /**
     * connect to UserStream
     */
    public void beginUserStream() {
        gt.start();
    }

    /**
     * disconnect to UserStream
     */
    public void endUserStream() {
        gt.halt();
    }

    /**
     * UserStream
     */
    class userStream extends Thread {

        private boolean halt_ = false;

        public userStream() {
            halt_ = false;
        }

        @Override
        public void run() {

            BufferedReader reader = null;
            HttpResponse response = null;
            do {

                String method = "GET", url = userStreamURL;
                SortedMap<String, String> params = getParamString(consumerKey, accessToken, null, null);

                String paramStr = "";

                for (Entry<String, String> param : params.entrySet()) {
                    paramStr += "&" + param.getKey() + "=" + param.getValue();
                }
                paramStr = paramStr.substring(1);

                String text = method + "&" + urlEncode(url) + "&" + urlEncode(paramStr);
                String key = urlEncode(consumerSecret) + "&" + urlEncode(accessTokenSecret);

                String sig = getSig(key, text);

                params.put("oauth_signature", sig);

                String authorizationHeader = getHeader(params);

                HttpClient client = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(url);
                httpget.addHeader("Authorization", authorizationHeader);

                try {
                    response = client.execute(httpget);
                    StatusLine statusline = response.getStatusLine();

                    if (statusline.getStatusCode() == HttpStatus.SC_OK) {
                        HttpEntity httpentity = response.getEntity();
                        reader = new BufferedReader(new InputStreamReader(httpentity.getContent()));

                        String line = null;
                        while (!halt_) {
                            try {
                                line = reader.readLine();

                                if (line != null) {
                                    if (line.length() > 0) {
                                        HashMap map = JSON.decode(line, HashMap.class);

                                        if (map.containsKey("user") && map.containsKey("text")) {
                                            HashMap user = (HashMap) map.get("user");
                                            String id = map.get("id_str").toString();
                                            String sn = user.get("screen_name").toString();
                                            String name = user.get("name").toString();
                                            String txt = map.get("text").toString();

                                            setTweets(id, sn, name, txt);
                                        }
                                    }
                                } else {
                                    Thread.sleep(100);
                                }

                            } catch (Exception e) {
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Failed.");
                    status = oauthStatus.FAILED;
                    e.getStackTrace();
                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        httpget.abort();
                    }
                }


                if (response == null) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ex) {
                    }
                }
            } while (response == null);



        }

        public void halt() {
            halt_ = true;
            interrupt();
        }
    }

    /**
     * TweetList
     * 
     * @param statusID statusid
     * @param screenName screenname
     * @param name name
     * @param text content
     */
    public void setTweets(String statusID, String screenName, String name, String text) {
        synchronized (tweetList) {
            if (!tweetList.isEmpty() && statusID.compareTo(tweetList.get(0).statusID) < 0) {
                tweetList.add(new Tweet(statusID, screenName, name, text));
            } else {
                tweetList.add(0, new Tweet(statusID, screenName, name, text));
            }
            if (tweetList.size() > 20) {
                tweetList.remove(tweetList.size() - 1);
            }
            showTweets();
        }
    }

    /**
     * Tweetを表示する
     */
    public void showTweets() {
        synchronized (tweetList) {
            int start = 0;
            int count = 0;

            Console console = System.console();
            if (console == null) {
                System.out.println("Couldn't get Console object !");
            }

            System.out.print("\u001b[H\u001b[2J");
            System.out.flush();

            int line = 0;
            for (int i = 0; i < tweetList.size(); i++) {
                try {
                    count += (tweetList.get(i).text.getBytes("UTF-8").length) / 90 + 2 + 1;
                } catch (UnsupportedEncodingException ex) {
                }
                if (tweetList.get(i).text.equals(System.getProperty("line.separator"))) {
                    count += 1;
                }
                if (count < 32) {
                    line = i;
                }
                start++;
            }

            if (tweetList.size() != start) {
                tweetList.subList(0, tweetList.size() - start);
            }

            count = 0;

            for (Tweet tw : tweetList) {
                count++;
                if (count < line) {
                    System.out.println("[" + count + "] -- " + tw.name + " [ @" + tw.screenName + " ]" + System.getProperty("line.separator") + tw.text);
                    System.out.println("----------");
                }
            }
            System.out.print(message);
        }

    }

    /**
     * API access
     */
    private String basicTwitterAccess(String method, String url) {
        SortedMap<String, String> params = getParamString(this.consumerKey, this.accessToken, null, null);

        String paramStr = "";

        for (Entry<String, String> param : params.entrySet()) {
            paramStr += "&" + param.getKey() + "=" + param.getValue();
        }
        paramStr = paramStr.substring(1);

        String text = method + "&" + urlEncode(url) + "&" + urlEncode(paramStr);
        String key = urlEncode(consumerSecret) + "&" + urlEncode(this.accessTokenSecret);

        String sig = getSig(key, text);

        params.put("oauth_signature", sig);

        String authorizationHeader = getHeader(params);

        String line = null;
        String res = "";

        if (method == "GET") {

            HttpClient client = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader("Authorization", authorizationHeader);

            BufferedReader reader = null;
            try {
                HttpResponse response = client.execute(httpget);
                StatusLine statusline = response.getStatusLine();

                if (statusline.getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity httpentity = response.getEntity();
                    reader = new BufferedReader(new InputStreamReader(httpentity.getContent()));

                    while ((line = reader.readLine()) != null) {
                        res += line;
                    }

                    return res;
                } else {
                    System.out.println(statusline.getStatusCode());
                    System.out.println(response);
                }
            } catch (IOException e) {
                System.out.println("Failed.");
                this.status = oauthStatus.FAILED;
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    httpget.abort();
                    e.printStackTrace();
                }
            }
        } else if (method == "POST") {
            HttpClient client = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(url);
            httppost.addHeader("Authorization", authorizationHeader);

            BufferedReader reader = null;
            try {
                HttpResponse response = client.execute(httppost);
                StatusLine statusline = response.getStatusLine();

                if (statusline.getStatusCode() == HttpStatus.SC_OK) {

                    HttpEntity httpentity = response.getEntity();
                    reader = new BufferedReader(new InputStreamReader(httpentity.getContent()));

                    while ((line = reader.readLine()) != null) {
                        res += line;
                    }

                    return res;
                }
            } catch (IOException e) {
                this.status = oauthStatus.FAILED;
            } finally {
                //HTTP接続の「優雅な」解放も兼ねる
                try {
                    reader.close();
                } catch (IOException e) {
                    httppost.abort();
                }
            }
        }


        this.status = oauthStatus.SUCCESS;

        return res;
    }

    /**
     * generate header
     */
    private String getHeader(SortedMap<String, String> params) {
        String paramStr = "";

        for (Entry<String, String> param : params.entrySet()) {
            paramStr += ", " + param.getKey() + "=\""
                    + urlEncode(param.getValue()) + "\"";
        }
        paramStr = paramStr.substring(2);
        String authorizationHeader = "OAuth " + paramStr;

        return authorizationHeader;
    }

    /**
     * create signature
     */
    private String getSig(String key, String text) {

        //String signatureBaseString = consumerSecret + "&" + accessSecret;

        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
        Mac mac;
        try {
            mac = Mac.getInstance(signingKey.getAlgorithm());
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(text.getBytes());
            String sig = new BASE64Encoder().encode(rawHmac);
            // 署名をパラメータに追加
            //params.put("oauth_signature", sig);
            return sig;
        } catch (InvalidKeyException ex) {
        } catch (NoSuchAlgorithmException ex) {
        }

        return "";
    }

    /**
     * @return time
     */
    private long getTime() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * URL Encode
     */
    private static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
