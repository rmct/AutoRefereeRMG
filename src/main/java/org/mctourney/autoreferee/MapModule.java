package org.mctourney.autoreferee;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.common.collect.Sets;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;

public class MapModule
{
	public static enum ModuleType
	{
		START,
		MIDDLE,
		END;
	}

	// filenames for module elements in the zip file
	private static final String INTERNAL_SCHM_FILENAME = "module.schematic";
	private static final String INTERNAL_META_FILENAME = "module.xml";

	// directory for packaging authored and unzipped modules temporarily
	private static final File PACKAGING_DIRECTORY = FileUtils.getTempDirectory();

	// facing yaw for "forward" down the RFW lane
	public static final float FORWARD_YAW = -90.0f;

	// range of map modules, bedrock to skybox
	public static final int MIN_Y = 0;
	public static final int MAX_Y = 256;

	private String slug;
	private File module = null;

	private ModuleType type = ModuleType.MIDDLE;
	private Set<String> authors = Sets.newHashSet();
	private int difficulty = 3;

	public MapModule(String slug)
	{
		this.slug = slug;
	}

	private MapModule(File module)
	{
		try
		{
			ZipFile zip = new ZipFile(module);

			// get the metadata and schematic from the zip
			ZipEntry meta = zip.getEntry(INTERNAL_META_FILENAME);
			ZipEntry schm = zip.getEntry(INTERNAL_SCHM_FILENAME);

			// if the requisite files exist
			if (meta != null && schm != null)
			{
				// load the metadata from the input stream
				this.loadMetadata(zip.getInputStream(meta));
				this.module = module;
			}
		}
		catch (ZipException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}

	public void addAuthor(String name)
	{
		authors.add(name);
	}

	public Set<String> getAuthors()
	{
		return authors;
	}

	public void setDifficulty(int difficulty)
	{
		this.difficulty = difficulty;
	}

	public int getDifficulty()
	{
		return this.difficulty;
	}

	public ModuleType getModuleType()
	{
		return this.type;
	}

	public void setModuleType(ModuleType type)
	{
		this.type = type;
	}

	@Override
	public String toString()
	{
		return String.format("%s[%s, t=%s]", this.getClass().getSimpleName(),
			this.slug, this.type.name());
	}

	@Override
	public int hashCode()
	{ return this.slug.hashCode(); }

	public static MapModule fromModule(File file)
	{
		if (file.exists() && file.canRead())
		{
			MapModule module = new MapModule(file);
			if (module.module != null) return module;
		}
		return null;
	}

	private void saveMetadata(File metadata)
	{
		Element root = new Element("module");
		root.setAttribute("slug", this.slug);

		// add difficulty as an integer (1-5)
		root.addContent(new Element("difficulty").setText(Integer.toString(difficulty)));

		// add the type of module (from the enum)
		root.addContent(new Element("type").setText(type.name()));

		// add the list of authors
		Element authroot = new Element("authors");
		for (String auth : authors)
			authroot.addContent(new Element("author").setText(auth));
		root.addContent(authroot);

		try
		{
			XMLOutputter xmlout = new XMLOutputter(Format.getPrettyFormat());
			xmlout.output(root, new FileOutputStream(metadata));
		}
		catch (IOException e) { e.printStackTrace(); };
	}

	public void loadMetadata(File metadata) throws FileNotFoundException
	{ loadMetadata(new FileInputStream(metadata)); }

	private void loadMetadata(InputStream metadata)
	{
		Element root = null;
		try
		{
			root = new SAXBuilder().build(metadata).getRootElement();
			assert root != null && "module".equalsIgnoreCase(root.getName()) : "Invalid metadata";
		}
		catch (JDOMException e) { e.printStackTrace(); return; }
		catch (IOException e) { e.printStackTrace(); return; }

		if (root != null)
		{
			this.slug = root.getAttributeValue("slug");
			this.type = ModuleType.valueOf(root.getChildTextNormalize("type"));

			try { this.difficulty = Integer.parseInt(root.getChildTextNormalize("difficulty")); }
			catch (NumberFormatException e) {  }


			for (Element auth : root.getChild("authors").getChildren("author"))
				this.addAuthor(auth.getTextNormalize());
		}
	}

	private static void addToZip(ZipOutputStream zip, File f, String path, File base) throws IOException
	{
		zip.putNextEntry(new ZipEntry(path));
		if (!f.isDirectory()) IOUtils.copy(new FileInputStream(f), zip);
		else for (File c : f.listFiles()) addToZip(zip, c, base);
	}

	private static void addToZip(ZipOutputStream zip, File f, File base) throws IOException
	{ addToZip(zip, f, base.toURI().relativize(f.toURI()).getPath(), base); }

	public void save(CuboidClipboard clipboard) throws IOException, DataException
	{
		File file = new File(getModuleDirectory(), this.slug + ".arm");
		File tmp_schm = File.createTempFile("schm", null, PACKAGING_DIRECTORY);
		File tmp_meta = File.createTempFile("meta", null, PACKAGING_DIRECTORY);

		this.saveMetadata(tmp_meta);
		SchematicFormat.MCEDIT.save(clipboard, tmp_schm);

		ZipOutputStream zip = new ZipOutputStream(new
			BufferedOutputStream(new FileOutputStream(file)));
		zip.setMethod(ZipOutputStream.DEFLATED);

		addToZip(zip, tmp_schm, INTERNAL_SCHM_FILENAME, PACKAGING_DIRECTORY);
		addToZip(zip, tmp_meta, INTERNAL_META_FILENAME, PACKAGING_DIRECTORY);

		zip.close();
		FileUtils.deleteQuietly(tmp_schm);
		FileUtils.deleteQuietly(tmp_meta);
	}

	public CuboidClipboard getClipboard()
	{
		if (this.module == null) return null;
		try
		{
			// get the schematic from the zip
			ZipFile zip = new ZipFile(this.module);
			ZipEntry schm = zip.getEntry(INTERNAL_SCHM_FILENAME);

			// if the schematic file exists
			if (schm != null)
			{
				// copy the schematic file out
				File tmp_schm = File.createTempFile("schm", null, PACKAGING_DIRECTORY);
				FileUtils.copyInputStreamToFile(zip.getInputStream(schm), tmp_schm);

				// load to a clipboard and delete the temporary file
				CuboidClipboard clipboard = SchematicFormat.MCEDIT.load(tmp_schm);
				FileUtils.deleteQuietly(tmp_schm);
				return clipboard;
			}
		}
		catch (ZipException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		catch (DataException e) { e.printStackTrace(); }

		return null;
	}

	public static File getModuleDirectory()
	{
		// make the module directory if it does not exist already
		File mdir = new File(AutoRefMap.getMapLibrary(), "modules");
		if (!mdir.exists()) mdir.mkdirs();

		return mdir;
	}

	public static Set<MapModule> getInstalledModules()
	{
		File mdir = getModuleDirectory();
		return getInstalledModules(mdir);
	}

	public static Set<MapModule> getInstalledModules(File mdir)
	{
		Set<MapModule> mods = Sets.newHashSet();

		// if this is a directory, search it for modules
		if (mdir.isDirectory()) for (File f : mdir.listFiles())
		{
			// if this is a file, check to see if its a module
			if (!f.isDirectory())
			{
				MapModule module = MapModule.fromModule(f);
				if (module != null) mods.add(module);
			}

			// recursively add all modules from files in subdirectories
			else mods.addAll(getInstalledModules(f));
		}

		return mods;
	}
}
