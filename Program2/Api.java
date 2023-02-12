import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.net.*;

import org.json.JSONArray;

import org.json.JSONObject;

class Api {
    final static String raidAPI = "insert Your API KEY";
    private static Random random = new Random();
    private static int maxBackoff = 64000;
    private static int rand = random.nextInt(1000);

    final private static int requestTimeout = 408;
    // Server would like to shut down this unused connection
    final private static int internalServerError = 500;
    // Server has faced an erroneous situation and it does not know how to handle
    // that. 
    final private static int badGateway = 502;
    // This error response indicates that the server,
    // while working as a gateway to get a response needed to handle the request,
    // got an invalid response during the flow.
    final private static int serviceUnavailable = 503;
    // The server is not ready to handle the request yet. It can be because the
    // server is down for maintenance or it is overloaded.
    final private static int gatewayTimeout = 504;
    // The server is acting like a gateway and it cannot get a response in a given
    // time.
    final private static int connectionTimeout = 522;
    // Cloudflare timed out contacting the origin server.
    final private static int timeoutOccurred = 524;
    // Cloudflare was able to complete a TCP connection to the origin server, but
    // did not receive a timely HTTP response.
    private static Set<Integer> retriesCode = new HashSet<>(Arrays.asList(requestTimeout, internalServerError,
            badGateway, serviceUnavailable, gatewayTimeout, connectionTimeout, timeoutOccurred));

    public static void searchMoviename(String name) throws IOException {
        try {
            String siteUrl = "https://imdb8.p.rapidapi.com/title/find?q=" + name;
            URL obj = new URL(siteUrl);

            int retries = 0, maxRetries = 5;
            while (retries < maxRetries) {
                try {
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("X-RapidAPI-Host", "imdb8.p.rapidapi.com");
                    con.setRequestProperty("X-RapidAPI-Key", raidAPI);

                    int responseCode = con.getResponseCode();

                    if ((responseCode / 100) == 2) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        JSONObject json = new JSONObject(response.toString());

                        JSONArray moviesArray = json.getJSONArray("results");

                        if (moviesArray.length() > 0) {
                            for (int i = 0; i < moviesArray.length(); i++) {
                                JSONObject movie = moviesArray.getJSONObject(i);
                                if (!movie.has("title") || !movie.has("titleType") || !movie.has("year"))
                                    continue;
                                String title = movie.getString("title");
                                String titleType = movie.getString("titleType");
                                int year = movie.getInt("year");
                                System.out.println("movie title: " + title);
                                System.out.println("movie type: " + titleType);
                                System.out.println("movie year: " + year);
                            }
                        } else {
                            System.out.println("No Movie found");
                        }
                        break;
                    } else if (retriesCode.contains(responseCode)) {
                        System.out.println("Unable to get data from API, response Code: " + responseCode);
                        retries++;
                        if (retries == maxRetries)
                            break;
                        int backoffTime = Math.min((int) Math.pow(2, retries) * 1000 + rand, maxBackoff);
                        System.out.println("back off time: " + backoffTime + "milliseconds");
                        TimeUnit.MILLISECONDS.sleep(backoffTime);
                    } else {
                        System.out.println("retry back off logic unavaialbe response code: " + responseCode);
                        break;
                    }
                } catch (Exception e) {
                    retries++;
                    if (retries == maxRetries)
                        break;
                    int backoffTime = Math.min((int) Math.pow(2, retries) * 1000 + rand, maxBackoff);
                    System.out.println("back off time :" + backoffTime + " milliseconds");
                    TimeUnit.MILLISECONDS.sleep(backoffTime);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void nameGuess(String name) throws IOException {
        try {
            String siteUrl = "https://the-cocktail-db.p.rapidapi.com/filter.php?i=" + name;

            URL obj = new URL(siteUrl);
            int retries = 0, maxRetries = 5;
            while (retries < maxRetries) {
                try {
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("X-RapidAPI-Host", "the-cocktail-db.p.rapidapi.com");
                    con.setRequestProperty("X-RapidAPI-Key", raidAPI);

                    int responseCode = con.getResponseCode();

                    if ((responseCode / 100) == 2) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        JSONObject json = new JSONObject(response.toString());
                        JSONArray drinksArray = json.getJSONArray("drinks");

                        if (drinksArray.length() > 0) {
                            for (int i = 0; i < drinksArray.length(); i++) {
                                JSONObject drink = drinksArray.getJSONObject(i);
                                String strDrink = drink.getString("strDrink");
                                System.out.println("drink name: " + strDrink);
                            }
                        } else {
                            System.out.println("No drinks found");
                        }
                        break;
                    } else if (retriesCode.contains(responseCode)) {
                        System.out.println("Unable to get data from API, response Code: " + responseCode);
                        retries++;
                        if (retries == maxRetries)
                            break;
                        int backoffTime = Math.min((int) Math.pow(2, retries) * 1000 + rand, maxBackoff);
                        System.out.println("back off time: " + backoffTime + "milliseconds");
                        TimeUnit.MILLISECONDS.sleep(backoffTime);
                    } else {
                        System.out.println("retry back off logic unavaialbe response code: " + responseCode);
                        break;
                    }
                } catch (Exception e) {
                    retries++;
                    if (retries == maxRetries)
                        break;
                    int backoffTime = Math.min((int) Math.pow(2, retries) * 1000 + rand, maxBackoff);
                    System.out.println("back off time :" + backoffTime + " milliseconds");
                    TimeUnit.MILLISECONDS.sleep(backoffTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void getWeather(String city) throws Exception {
        try {
            String apiKey = "insert your API KEY";

            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey;

            URL obj = new URL(url);
            int retries = 0, maxRetries = 5;

            while (retries < maxRetries) {
                try {
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    con.setRequestMethod("GET");

                    int responseCode = con.getResponseCode();

                    if ((responseCode / 100) == 2) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                        String inputLine;

                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        JSONObject myResponse = new JSONObject(response.toString());

                        System.out.println("Weather information: ");
                        String cityname = myResponse.getString("name").toString();
                        double celcious = myResponse.getJSONObject("main").getDouble("temp") - 273.15;
                        double fahrenheit = (myResponse.getJSONObject("main").getDouble("temp") - 273.15) * 9 / 5 + 32;
                        double humidity = myResponse.getJSONObject("main").getDouble("humidity");

                        System.out.println("City: " + cityname);
                        System.out.printf("Temperature: %.2f in Celcius\n", celcious);
                        System.out.printf("Temperature: %.2f in Fahrenheit\n", fahrenheit);
                        System.out.println("Humidity: " + humidity + "%");
                        break;
                    } else if (retriesCode.contains(responseCode)) {
                        System.out.println("Unable to get data from API, response Code: " + responseCode);
                        retries++;
                        if (retries == maxRetries)
                            break;
                        int backoffTime = Math.min((int) Math.pow(2, retries) * 1000 + rand, maxBackoff);
                        System.out.println("back off time : " + backoffTime + " milliseconds");
                        TimeUnit.MILLISECONDS.sleep(backoffTime);
                    } else {
                        System.out.println("retry back off logic unavailable response code: " + responseCode);
                        break;
                    }

                } catch (Exception e) {
                    retries++;
                    if (retries == maxRetries)
                        break;
                    int backoffTime = Math.min((int) Math.pow(2, retries) * 1000 + rand, maxBackoff);
                    System.out.println("back off time : " + backoffTime + " milliseconds");
                    TimeUnit.MILLISECONDS.sleep(backoffTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        // System.out.println("Eneter the name of a city: ");
        System.out.println("Welcome to David's API service~");
        System.out.println("We have three service here");
        System.out.println("Please, Choose one of these options");
        boolean flag = true;
        do {

            System.out.println("1. weather information with city name, if you want this please type: weather");
            System.out.println(
                    "2. cocktail recommandation service with any raw alcohol, if you want this please type: cocktail ");
            System.out.println("3. Search movie name, if you want this please type: movie ");
            System.out.println("if you want to exit, please tpye: quit");
            // String city = sc.nextLine();
            String name = sc.nextLine();

            switch (name) {

                case "weather":
                    try {
                        System.out.println("please type city name you want to know their weather");
                        name = sc.nextLine();
                        Api.getWeather(name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case "cocktail":
                    try {
                        System.out.println("please type alcohol name, we will recommand cocktail with that");
                        name = sc.nextLine();
                        Api.nameGuess(name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case "movie":

                    try {
                        System.out.println("please type movie name or series name, we will search it for you");
                        name = sc.nextLine();
                        Api.searchMoviename(name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case "quit":

                    flag = false;
                    break;

                default:
                    System.out.println("Invalid input, please try again");
            }
        } while (flag);
        sc.close();
    }
}