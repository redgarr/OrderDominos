package servlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Servlet to handle the ordering of a randomly built half-half pizza from dominos.
 * It's a bit hacked together right now due to lack of time so there are many improvements
 * and cleanup required but it works. For now. Until Dominos makes changes to their website.
 *
 * @author Jonathan Bryhagen
 */

@Controller
public class OrderDominosServlet
{
	private WebDriver driver;

	//boolean to decide if an order should be actually placed or not
	//if set to false the final "order" button wont be pressed.
	private boolean makeRealOrder = false;

	@RequestMapping(value = "/OrderRandom", method = RequestMethod.POST)
	public void orderRandom()
	{
		try
		{
			//close possible previous driver
			if(driver != null)
			{
				try
				{
					driver.quit();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			System.setProperty("webdriver.gecko.driver", "/home/pi/geckodriver");

			FirefoxProfile profile = new FirefoxProfile();
			profile.addExtension(new File("/home/pi/mkiosk.xpi"));

			DesiredCapabilities capabilities = DesiredCapabilities.firefox();
			capabilities.setCapability("marionette", false);
			capabilities.setCapability(FirefoxDriver.PROFILE, profile);

			driver = new FirefoxDriver(capabilities);
			((JavascriptExecutor)driver).executeScript("window.focus()");
			driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
			//driver.manage().window().setSize(new Dimension(750, 480)); //used for testing
			driver.get("http://www.dominos.se");

			new WebDriverWait(driver, 10)
				.until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading")));

			driver.findElement(By.className("navbar-toggle"))
				.click();

			WebElement orderButton = new WebDriverWait(driver, 10)
				.until(ExpectedConditions.elementToBeClickable(By.className("cart-btn")));

			orderButton.click();

			WebElement emailField = new WebDriverWait(driver, 10)
				.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")));

			emailField.sendKeys("xx@xx.se");

			driver.findElement(By.id("login-pass"))
				.sendKeys("xx");

			driver.findElement(By.className("login"))
				.click();

			new WebDriverWait(driver, 10)
				.until(ExpectedConditions.visibilityOfElementLocated(By.className("step1-choice")));

			//get correct element, annoying because the two different options have the same class name
			WebElement homeDelivery = driver.findElements(By.className("step1-choice"))
				.stream()
				.filter(e -> e.getAttribute("data-rel").equals("D")) //D is for Delivery
				.findFirst().get();

			homeDelivery.click();

			new WebDriverWait(driver, 10)
				.until(ExpectedConditions.visibilityOfElementLocated(By.className("make_order")));

			driver.findElement(By.className("make_order"))
				.click();

			//get all pizza elements, filter out the half & half and click it, time expensive
			WebElement halfAndHalfElement = driver.findElements(By.className("product_item"))
				.stream().filter(e -> e.getAttribute("prod-rel").equals("0"))
				.findFirst().get();

			int pizzasToOrder = 1;
			for(int i = 0; i<pizzasToOrder; i++)
			{
				putRandomPizzaInCart(driver, halfAndHalfElement);
			}

			new WebDriverWait(driver, 10)
				.until(ExpectedConditions.visibilityOf(halfAndHalfElement));

			driver.get("http://dominos.se/checkout");

			new WebDriverWait(driver, 10)
				.until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading")));

			//get credit card radio button and click it, no ID or class so we have to go by xpath
			new WebDriverWait(driver, 10)
					.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"order-complete-box\"]/div[2]/ul/li[2]")));

			//remove navbar element as we have very little screen space
			((JavascriptExecutor)driver).executeScript("document.getElementsByClassName(\"navbar navbar-default\")[0].remove();");

			WebElement ccRadioButton = driver.findElement(By.xpath("//*[@id=\"order-complete-box\"]/div[2]/ul/li[2]"));
			ccRadioButton.click();

			WebElement ccNumber = new WebDriverWait(driver, 10)
				.until(ExpectedConditions.elementToBeClickable(By.id("cc-number")));

			//Set opacity of payment fields to 0 to keep them from showing on the screen
			((JavascriptExecutor)driver).executeScript("document.getElementById(\"cc-cvc\").style.opacity = \"0\";\n");
			((JavascriptExecutor)driver).executeScript("document.getElementById(\"cc-number\").style.opacity = \"0\";\n");
			((JavascriptExecutor)driver).executeScript("document.getElementById(\"cc-exp\").style.opacity = \"0\";\n");
			((JavascriptExecutor)driver).executeScript("document.getElementById(\"holder_name\").style.opacity = \"0\";\n");

			//We have to add a delay between each tuple here because there is some
			//operation on the website that will block inputs for a short duration
			ccNumber.sendKeys("XXXX");
			Thread.currentThread().sleep(50);
			ccNumber.sendKeys("XXXX");
			Thread.currentThread().sleep(50);
			ccNumber.sendKeys("XXXX");
			Thread.currentThread().sleep(50);
			ccNumber.sendKeys("XXXX");

			//adding delays because the website will format the numbers and
			//the input is too fast otherwise
			WebElement ccExp = driver.findElement(By.id("cc-exp"));
			ccExp.sendKeys("XX");
			Thread.currentThread().sleep(50);
			ccExp.sendKeys("XX");

			driver.findElement(By.id("cc-cvc"))
				.sendKeys("XXX");

			driver.findElement(By.id("holder_name"))
				.sendKeys("XXX XXX");

			if(makeRealOrder)
			{
				driver.findElement(By.className("new-credit-card-submit"))
					.click();

			}

			new WebDriverWait(driver, 60)
				.until(ExpectedConditions.visibilityOfElementLocated(By.className("tracker-step-title")));

			//remove navbar element as we have very little screen space and its a big element
			((JavascriptExecutor)driver).executeScript("document.getElementsByClassName(\"navbar navbar-default\")[0].remove();");
			//scroll the element into view
			((JavascriptExecutor)driver).executeScript("document.getElementsByClassName(\"start-txt text-center menu-color col-xs-12\")[0].scrollIntoView();\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	private void putRandomPizzaInCart(WebDriver driver, WebElement halfAndHalfElement)
		throws InterruptedException
	{
		new WebDriverWait(driver, 10)
			.until(ExpectedConditions.visibilityOf(halfAndHalfElement));

		halfAndHalfElement.click();

		new WebDriverWait(driver, 10)
			.until(ExpectedConditions.presenceOfElementLocated(By.className("select_half_prod")));

		List<WebElement> elems = driver.findElements(By.className("select_half_prod"));
		List<Select> selects = new ArrayList();

		for(WebElement we : elems)
		{
			selects.add(new Select(we));
		}

		String[] pizzas = new String[selects.size()];
		int[] randomIndexes = ThreadLocalRandom.current()
			.ints(1, selects.get(0).getOptions().size())
			.distinct()
			.limit(2)
			.toArray();

		for(int i=0; i<selects.size(); i++)
		{
			Select select = selects.get(i);
			select.selectByIndex(randomIndexes[i]);
			elems.get(i).click(); //hack for arm
			pizzas[i] = select.getFirstSelectedOption().getText();
			Thread.currentThread().sleep(500); //hack to give page time to update selections
		}

		System.out.println("Pizzas:");
		for(String s : pizzas)
		{
			System.out.println("\t" + s);
		}

		driver.findElement(By.className("Add_btn"))
			.click();

	}
}
