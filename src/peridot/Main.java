package peridot;

import peridot.OAuth.Tweet;
import peridot.OAuth.oauthStatus;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *
 * @author lindow
 */
public class Main {
    public static void main() {

        OAuth twitter = new OAuth("iFGMzcmHYZk3d5ncPwXjA", "wBfv7Z9YT4CEAv8crDqilil0R6AZi5pY0vub5FP0U");


        Setting set = new Setting();
        set.loadSettings();


        twitter.setAccessToken(set.getAccessToken());
        twitter.setAccessTokenSecret(set.getAccessSecret());
        twitter.setSummaryFlag(set.getSummaryFlag());

        twitter.accessTokenAuthorization();

        System.out.println();
        System.out.println("------------------------------------------");

        if (twitter.getStatus() == oauthStatus.NOT_AUTHORIZED) {
            String url = "";
            System.out.println();
            System.out.println("Please wait for get a authorization key....");

            do {
                url = twitter.requestTokenAuthorization();
                if (url == "" || twitter.getStatus() == oauthStatus.AUTHORIZATION_FAILED) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            } while (url == "");

            System.out.println("Please access following URL to get the pin code!");
            System.out.println(url);
            System.out.println("PIN : ");

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                twitter.setPin(br.readLine());
            } catch (Exception e) {
                System.err.println("Pin URL get Failed...");
            }

            do {
                twitter.accessTokenAuthorization();
                if (twitter.getStatus() != oauthStatus.AUTHORIZED) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            } while (twitter.getStatus() != oauthStatus.AUTHORIZED);

            if (twitter.getSummaryFlag() == null) {
                twitter.setSummaryFlag("false");
            }
            System.out.println(twitter.getSummaryFlag());

            set.setAccessToken(twitter.getAccessToken());
            set.setAccessSecret(twitter.getAccessTokenSecret());
            set.setSummaryFlag(twitter.getSummaryFlag());
            set.saveSettings();
        }

        System.out.println();
        System.out.println("UserStream Connecting...");

        twitter.beginUserStream();
        twitter.getTimeLine();


        while (true) {
            String tweet = "";
            twitter.showTweets();

            tweet = System.console().readLine();

            if (tweet == null || tweet.length() == 0) {
                twitter.endUserStream();
                System.out.print("\u001b[H\u001b[2J");
                System.out.flush();
                System.out.println("Bye.");
                System.exit(0);
                break;
            }
            if (tweet.charAt(0) == '/') {
                boolean isDisposal = false;

                String[] split = tweet.split(" ", 3);

                if (split.length >= 2) {
                    if (Character.isDigit(split[1].charAt(0))) {

                        int id = Integer.parseInt(split[1]);
                        Tweet info = null;
                        info = OAuth.tweetList.get(id - 1);
                        isDisposal = true;
                        if (split[0].equals("/rep")) {
                            twitter.post("@" + info.screenName + " " + split[2], info.statusID);
                        } else if (split[0].equals("/fav")) {
                            twitter.fav(info.statusID);
                        } else if (split[0].equals("/unfav")) {
                            twitter.unFav(info.statusID);
                        } else if (split[0].equals("/rm")) {
                            twitter.remove(info.statusID);
                        } else if (split[0].equals("/rt")) {
                            twitter.retweet(info.statusID);
                        } else {
                            isDisposal = false;
                        }

                    } else {
                        isDisposal = true;
                        if (split[0].equals("/find")) {
                            twitter.search(split[1]);
                        } else {
                            isDisposal = false;
                        }
                    }
                } else {
                    isDisposal = true;
                    if (split[0].equals("/rep")) {
                        twitter.getMentions();
                    } else if (split[0].equals("/fav")) {
                        twitter.getFavorites();
                    } else if (split[0].equals("/get")) {
                        twitter.getTimeLine();
                    } else if (split[0].equals("/sum")) {
                        if (set.getSummaryFlag() != null) {
                            if (set.getSummaryFlag().equals("false")) {
                                set.setSummaryFlag("true");
                            } else {
                                set.setSummaryFlag("false");
                            }
                        } else {
                            set.setSummaryFlag("false");
                        }
                        set.saveSettings();
                        twitter.setSummaryFlag(set.getSummaryFlag());
                        System.out.print(set.getSummaryFlag() + "に設定しました。");
                    } else if (split[0].equals("/exit")) {
                        System.out.print("\u001b[H\u001b[2J");
                        System.out.flush();
                        System.out.println("Bye.");
                        System.exit(0);
                        break;
                    } else {
                        isDisposal = false;
                    }
                }
                if (!isDisposal) {
                    if (tweet.length() > 1 && tweet.charAt(1) == '/') {
                        System.out.print("Updating...");
                        twitter.post(tweet.substring(1), null);
                        System.out.println("...done!");
                    } else {
                        System.out.println("[UNKNOWN COMMAND] - USAGE: /rep, /fav, /unfav, /rt, /rm, /find, /sum, /exit");
                    }
                }
            } else {
                System.out.print("Updating...");
                twitter.post(tweet, null);
                System.out.println("...done!");
            }
            System.out.println("What's up ? : ");
        }
    }
}
