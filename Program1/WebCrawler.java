/*
 * CSS436 
 * 2140483
 * David Chanje Woo
 * 
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.*;
import java.util.regex.*;

public class WebCrawler {
    private Deque<String> urlCollect;
    private List<String> visitedList;

    public WebCrawler() {
        urlCollect = new ArrayDeque<>();
        visitedList = new ArrayList<>();
    }

    private int getHop(int numOfHop, Matcher matcher) {
        int prevN = numOfHop;
        while (matcher.find()) {
            String readedUrl = matcher.group(1);

            if (!visitedList.contains(readedUrl)) {
                visitedList.add(readedUrl);
                System.out.println("founded URL : " + readedUrl + ", remianing numOfHop : " + numOfHop);
                urlCollect.addLast(readedUrl);

                numOfHop--;

                break;
            }

        }
        if (prevN == numOfHop) {
            // numOfHop--;
            if (!urlCollect.isEmpty())
                urlCollect.removeLast();
        }
        return numOfHop;
    }

    public void crawl(String startingPoint, int numOfHop) {
        urlCollect.addLast(startingPoint);
        visitedList.add(startingPoint);

        while (!urlCollect.isEmpty()) {

            String s = urlCollect.peekLast();
            String raw = "";

            try {
                URL url = new URL(s);

                // // make the connection to fetch resource
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(5000);
                connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                connection.addRequestProperty("User-Agent", "Mozilla");
                connection.addRequestProperty("Referer", "google.com");

                boolean redirect = false;
                // DH ServerKeyExchange does not comply to algorithm contstraint below
                int statusCode = connection.getResponseCode();

                // System.out.println(statusCode);

                if (statusCode != HttpURLConnection.HTTP_OK) {
                    if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP
                            || statusCode == HttpURLConnection.HTTP_MOVED_PERM
                            || statusCode == HttpURLConnection.HTTP_SEE_OTHER
                            || statusCode / 100 == 3) {
                        redirect = true;
                    }
                }

                // System.out.println("Response Code : " + statusCode);

                if (redirect) {
                    // get redirect url from "location" header field
                    String newUrl = connection.getHeaderField("Location");

                    // get the cookie if need, for login
                    String cookies = connection.getHeaderField("Set-Cookie");
                    // open the new connection again
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestProperty("Cookie", cookies);
                    connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                    connection.addRequestProperty("User-Agent", "Mozilla");
                    connection.addRequestProperty("Referer", "google.com");

                    System.out.println("Redirect to URL : " + newUrl);
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine = in.readLine();

                while (inputLine != null) {
                    raw += inputLine;
                    inputLine = in.readLine();
                }
                in.close();

            } catch (Exception e) {

                System.out.println("No more accessible links found further in current page: " + s);

                if (!urlCollect.isEmpty())
                    urlCollect.removeLast();

            }

            // String regex = "<a\\s+href\\s*=\\s*(\"[^\"]*\"|[^\\s>]*)\\s*>";
            // String regex = "<a\\s+href\\s*=\\s*\"([^\\s>]*|[\"])\\s*>";

            String regex = "<a\\s+href\\s*=\\s*\"(http[^\"]+)\"";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(raw);

            numOfHop = getHop(numOfHop, matcher);

            if (numOfHop == 0)
                break;
        }

    }

    public static void main(String[] args) throws IOException {

        // parse cli inputs into url string and number of hops(int)
        String stringUrl = args[0];

        int numOfHop = Integer.parseUnsignedInt(args[1]);

        WebCrawler crawler = new WebCrawler();

        crawler.crawl(stringUrl, numOfHop);

        System.out.println("Web Crawling is done");
    }
}
