import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.runelite.cache.StoreLocation;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;
import org.junit.Test;

public class RegionDumper
{

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Test
	public void extractWithXteas() throws IOException
	{
		File base = StoreLocation.LOCATION;

		File regionDumperBase = new File(
			System.getProperty("user.home")
				+ File.separator + "RegionDumper"
		);

		File xteas = new File(regionDumperBase, "xteas" + File.separator);
		if (!xteas.exists() && !xteas.mkdirs())
		{
			return;
		}

		File keys = new File(regionDumperBase, "xtea.json");

		try (Store store = new Store(base))
		{
			store.load();
			RegionLoader loader = new RegionLoader(store, keys);
			loader.loadRegions();
			ExecutorService service = Executors.newFixedThreadPool(10);

			for (Region region : loader.getRegions())
			{
				service.submit(() -> {
					File json = new File(xteas, region.getRegionID() + ".json");
					try
					{
						gson.toJson(region, Region.class, new FileWriter(json));
					}
					catch (IOException ignored)
					{
					}
				});
			}
		}
	}
}
