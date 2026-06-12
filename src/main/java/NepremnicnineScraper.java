import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

class Property {
    String placeName;
    String description;
    String price;
    String link;
}

public class NepremnicnineScraper {

    private static final String SEARCH_URL =
            "https://www.nepremicnine.net/oglasi-oddaja/juzna-primorska/stanovanje/%s?s=16";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final int MAX_PAGES = 50;

    private static final boolean SCRAPE_ALL_PAGES = false;

    private static final Pattern PAGE_NUMBER = Pattern.compile("stanovanje/(\\d+)/");

    static {
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
    }

    @FunctionalInterface
    public interface PageListener {
        void onPage(List<Property> pageProperties, int page, int totalPages);
    }

    public List<Property> scrapeProperties() {
        List<Property> all = new ArrayList<>();
        scrapeProperties((pageProperties, page, totalPages) -> all.addAll(pageProperties));
        return all;
    }

    public void scrapeProperties(PageListener listener) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("user-agent=" + USER_AGENT);
        options.addArguments("--blink-settings=imagesEnabled=false");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        WebDriver driver = new ChromeDriver(options);

        try {
            int totalPages = 1;

            for (int page = 1; page <= totalPages && page <= MAX_PAGES; page++) {
                List<Property> pageProperties;
                try {
                    String pagePath = (page == 1) ? "" : page + "/";
                    driver.get(String.format(SEARCH_URL, pagePath));

                    if (!waitForListings(driver, Duration.ofSeconds(12))) {
                        if (isCloudflareChallenge(driver)) {
                            System.out.println("Cloudflare challenge on page " + page
                                    + "; stopping pagination.");
                        } else {
                            System.out.println("No listings on page " + page + "; stopping.");
                        }
                        break;
                    }

                    if (page == 1 && SCRAPE_ALL_PAGES) {
                        totalPages = readTotalPages(driver);
                    }

                    pageProperties = extractProperties(driver);
                } catch (Exception e) {
                    if (isInterruption(e)) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    System.out.println("Stopped scraping at page " + page + ": "
                            + e.getClass().getSimpleName());
                    break;
                }

                listener.onPage(pageProperties, page, totalPages);

                if (page < totalPages) {
                    Thread.sleep(800);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            boolean wasInterrupted = Thread.interrupted();
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean waitForListings(WebDriver driver, Duration timeout)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(By.className("property-details")).isEmpty()) {
                return true;
            }
            if (isCloudflareChallenge(driver)) {
                return false;
            }
            Thread.sleep(400);
        }
        return false;
    }

    private boolean isCloudflareChallenge(WebDriver driver) {
        try {
            String title = driver.getTitle();
            return title != null && title.contains("Just a moment");
        } catch (Exception e) {
            return false;
        }
    }

    private List<Property> extractProperties(WebDriver driver) {
        List<Property> list = new ArrayList<>();
        for (WebElement box : driver.findElements(By.className("property-details"))) {
            Property p = new Property();
            p.placeName = textOrEmpty(box, By.tagName("h2"));
            p.description = textOrEmpty(box, By.tagName("p"));
            p.price = textOrEmpty(box, By.tagName("h6"));

            p.link = box.getAttribute("data-href");
            if (p.link == null || p.link.isEmpty()) {
                p.link = attrOrEmpty(box, By.tagName("a"), "href");
            }

            list.add(p);
        }
        return list;
    }

    private boolean isInterruption(Throwable t) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof InterruptedException) {
                return true;
            }
        }
        return false;
    }

    private int readTotalPages(WebDriver driver) {
        try {
            List<WebElement> last = driver.findElements(By.cssSelector("li.paging_last a"));
            if (!last.isEmpty()) {
                String href = last.get(0).getAttribute("href");
                if (href != null) {
                    Matcher m = PAGE_NUMBER.matcher(href);
                    if (m.find()) {
                        return Integer.parseInt(m.group(1));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private String textOrEmpty(WebElement parent, By by) {
        try {
            return parent.findElement(by).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private String attrOrEmpty(WebElement parent, By by, String attr) {
        try {
            String v = parent.findElement(by).getAttribute(attr);
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }
}
