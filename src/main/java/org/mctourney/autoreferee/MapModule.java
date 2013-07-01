package org.mctourney.autoreferee;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;

public class MapModule
{
	// filenames for module elements in the zip file
	private static final String INTERNAL_SCHM_FILENAME = "module.schematic";
	private static final String INTERNAL_META_FILENAME = "module.xml";

	// directory for packaging authored and unzipped modules temporarily
	private static final File PACKAGING_DIRECTORY = FileUtils.getTempDirectory();

	// facing yaw for "forward" down the RFW lane
	public static final float FORWARD_YAW = 90.0f;

	// range of map modules, bedrock to skybox
	public static final int MIN_Y = 0;
	public static final int MAX_Y = 256;

	private final String slug;
	private CuboidClipboard clipboard = null;

	private MapModule(String slug)
	{
		this.slug = slug;
	}

	private MapModule(String slug, File module, File metadata)
		throws IOException, DataException
	{
		this(slug);
		clipboard = SchematicFormat.MCEDIT.load(module);
		this.loadMetadata(metadata);
	}

	public MapModule(String slug, CuboidClipboard clipboard)
	{
		this(slug);
		this.clipboard = clipboard;
	}

	@Override
	public String toString()
	{
		return String.format("%s[%s, w=%d, len=%d", this.getClass().getSimpleName(),
			this.slug, this.clipboard.getWidth(), this.clipboard.getLength());
	}

	@Override
	public int hashCode()
	{ return this.slug.hashCode(); }

	public static MapModule fromModule(File module)
	{
		return null;
	}

	public void saveMetadata(File metadata)
	{

	}

	public void loadMetadata(File metadata)
	{

	}

	private static void addToZip(ZipOutputStream zip, File f, String path, File base) throws IOException
	{
		zip.putNextEntry(new ZipEntry(path));
		if (!f.isDirectory()) IOUtils.copy(new FileInputStream(f), zip);
		else for (File c : f.listFiles()) addToZip(zip, c, base);
	}

	private static void addToZip(ZipOutputStream zip, File f, File base) throws IOException
	{ addToZip(zip, f, base.toURI().relativize(f.toURI()).getPath(), base); }

	public void save(File file) throws IOException, DataException
	{
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

	public void save() throws IOException, DataException
	{ this.save(new File(this.slug + ".arm")); }
}
