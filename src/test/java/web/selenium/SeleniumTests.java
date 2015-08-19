package web.selenium;

import org.mineotaur.application.Mineotaur;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.Select;
import org.springframework.boot.SpringApplication;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * Created by balintantal on 07/08/2015.
 */
public class SeleniumTests {

    protected WebDriver driver;
    protected String mineotaurName = "sysgrorunnames";

    //@BeforeSuite
    public void setUp() {
        Mineotaur.name = mineotaurName;
        SpringApplication.run(Mineotaur.class);
        driver = new FirefoxDriver();
        driver.get("http://localhost:8080");
    }

    //@AfterSuite
    public void tearDown() {
        driver.quit();
    }

    @DataProvider(name = "testBasicElementsDataProvider")
    public Object[][] testBasicElementsDataProvider() {
        return new Object[][] {
                {"groupwiseScatterPlotForm"},
                {"groupwiseDistributionForm"}
        };
    }

    //@Test(dataProvider = "testBasicElementsDataProvider")
    public void testBasicElements(String element) {
        assertNotNull(driver.findElement(By.id(element)));
    }

    @DataProvider(name = "testGroupwiseScatterPlotDataProvider")
    public Object[][] testGroupwiseScatterPlotDataProvider() {
        WebElement element = driver.findElement(By.id("prop1"));
        List<WebElement> options = element.findElements(By.tagName("option"));
        System.out.println(options.size());
        //int size = options.size();
        int size = 4;
        Object[][] values = new Object[(size*(size-1))/2][2];
        int idx = 0;
        for (int i = 0; i < size-1; ++i) {
            String option1 = options.get(i).getText();
            for (int j = i+1; j < size; ++j) {
                values[idx++] = new Object[] {option1, options.get(j).getText()};
            }
        }
        return values;
    }

    //@Test(dataProvider = "testGroupwiseScatterPlotDataProvider")
    public void testGroupwiseScatterPlot(String option1, String option2) {
        Select select = new Select(driver.findElement(By.id("prop1")));
        select.selectByVisibleText(option1);
        select = new Select(driver.findElement(By.id("prop2")));
        select.selectByVisibleText(option2);
        WebElement element = driver.findElement(By.id("groupwiseScatterPlotFormSubmit"));
        element.click();
        /*WebDriver driver = new FirefoxDriver();
        driver.get("localhost:8080");
        WebElement element = driver.findElement(By.name("valami"));*/
    }


}
