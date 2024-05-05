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
                "Chrome (AppleWebKit/537.1; Chrome50.0; Windows NT 6.3) AppleWebKit/537.36 (KHTML like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; ar; rv:1.9.2) Gecko/20100115 Firefox/3.6",
                "Mozilla/5.0 (Windows; U; Windows NT 6.0; ru; rv:1.9.2) Gecko/20100115 Firefox/3.6",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.1b5pre) Gecko/20090517 Firefox/3.5b4pre (.NET CLR 3.5.30729)",
                "Mozilla/5.0 (X11; U; Gentoo Linux x86_64; pl-PL) Gecko Firefox",
                "Mozilla/5.0 (X11; ; Linux x86_64; rv:1.8.1.6) Gecko/20070802 Firefox",
                "Opera/9.80 (S60; SymbOS; Opera Tablet/9174; U; en) Presto/2.7.81 Version/10.5",
                "Opera/9.80 (X11; U; Linux i686; en-US; rv:1.9.2.3) Presto/2.2.15 Version/10.10",
                "Opera/9.80 (X11; Linux x86_64; U; it) Presto/2.2.15 Version/10.10",
                "Opera/9.80 (Windows NT 6.1; U; de) Presto/2.2.15 Version/10.10",
                "Opera/9.80 (Windows NT 6.0; U; Gecko/20100115; pl) Presto/2.2.15 Version/10.10",
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
