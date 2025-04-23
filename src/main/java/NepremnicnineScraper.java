
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

//Every property has its own class with info abt it
class Property {
    String placeName;
    String description;
    String price;
    String link;
}

public class NepremnicnineScraper {

    public Property[] scrapeProperties() {
        System.setProperty("webdriver.chrome.driver", "C:/Users/pejki/Downloads/chromedriver-win64/chromedriver-win64/chromedriver.exe");


    // Launch the browser in the background
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("user-agent=Mozilla/5.0 ...");

        WebDriver driver = new ChromeDriver(options);
        Property[] properties = new Property[25];


        // get the needed elements
        try {

            driver.get("https://www.nepremicnine.net/oglasi-oddaja/juzna-primorska/stanovanje/?s=16");


            Thread.sleep(3000);


            List<WebElement> propertyBox = driver.findElements(By.className("property-details"));

            for (int i = 0; i < propertyBox.size(); i++) {
                Property p = new Property();
                p.placeName = propertyBox.get(i).findElement(By.tagName("h2")).getText();

                p.description = propertyBox.get(i).findElement(By.tagName("p")).getText();

                p.price = propertyBox.get(i).findElement(By.tagName("h6")).getText();

                p.link = propertyBox.get(i).findElement(By.tagName("a")).getAttribute("href");

                properties[i] = p;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit(); // Close browser
        }

        return properties;

    }
}
