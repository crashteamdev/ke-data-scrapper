package dev.crashteam.ke_data_scrapper.util;


import java.util.HashMap;
import java.util.Map;

public class RandomUserAgent {

    private static final Map<String, String[]> uaMap = new HashMap<>();
    private static final Map<String, Double> freqMap = new HashMap<>();

    static {
        freqMap.put("System", 30.3);

        uaMap.put("System", new String[]{
                "curl/7.64.1",
                "PostmanRuntime/7.37.3",
                "Jetty/1.20",
                "ReactorNetty/dev",
                "Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0",
                "Opera/9.64 (Windows NT 6.0; U; pl) Presto/2.1.1",
                "Praw/2.23",
                "Netty/2.4.1",
                "RandUse/4.44",
                "SomeOther/5.35",
                "Chrome (AppleWebKit/537.1; Chrome50.0; Windows NT 6.3) AppleWebKit/537.36 (KHTML like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393"
        });
    }

    public static String getRandomUserAgent() {

        double rand = Math.random() * 100;
        String browser = null;
        double count = 0.0;
        for (Map.Entry<String, Double> freq : freqMap.entrySet()) {
            count += freq.getValue();
            if (rand <= count) {
                browser = freq.getKey();
                break;
            }
        }

        if (browser == null) {
            browser = "System";
        }

        String[] userAgents = uaMap.get(browser);
        return userAgents[(int) Math.floor(Math.random() * userAgents.length)];
    }
}
