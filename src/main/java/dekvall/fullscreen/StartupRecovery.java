package dekvall.fullscreen;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import net.runelite.client.util.Filepath;

/**
 * Guards every fullscreen change, including automatic entry at startup: the marker is
 * written before GPU renderers stop or the window changes, and cleared once the change
 * has settled. A flushed marker survives native crashes (for example a graphics-driver
 * crash while the window is rebuilt); a lock distinguishes another live client.
 * All file access goes through RuneLite's {@link Filepath}, rooted in the plugin's data directory.
 */
final class StartupRecovery
{
	enum Result
	{
		READY, RECOVERED, BUSY
	}

	private static final String LOCK = "startup.lock";
	private static final String MARKER = "startup.pending";

	private final Filepath directory;
	private FileChannel channel;
	private FileLock lock;
	private boolean ownsMarker;

	StartupRecovery(Filepath directory)
	{
		this.directory = directory;
	}

	Result begin() throws IOException
	{
		if (channel != null)
		{
			throw new IllegalStateException("Startup recovery is already armed");
		}
		directory.createDirectories();
		channel = directory.joinSegment(LOCK).openFileChannel(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		try
		{
			try
			{
				lock = channel.tryLock();
			}
			catch (OverlappingFileLockException ignored)
			{
				lock = null;
			}
			if (lock == null)
			{
				finish(false);
				return Result.BUSY;
			}
			Filepath marker = directory.joinSegment(MARKER);
			if (marker.exists())
			{
				marker.delete();
				finish(false);
				return Result.RECOVERED;
			}
			try (FileChannel output = marker.openFileChannel(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))
			{
				ByteBuffer contents = StandardCharsets.UTF_8.encode("Fullscreen startup pending\n");
				while (contents.hasRemaining())
				{
					output.write(contents);
				}
				output.force(true);
			}
			ownsMarker = true;
			return Result.READY;
		}
		catch (IOException | RuntimeException ex)
		{
			try
			{
				finish(false);
			}
			catch (IOException cleanup)
			{
				ex.addSuppressed(cleanup);
			}
			throw ex;
		}
	}

	/**
	 * At launch: whether a previous fullscreen change never finished. A leftover marker is
	 * removed. BUSY while another client using this profile is in the middle of a change.
	 */
	Result check() throws IOException
	{
		if (channel != null)
		{
			throw new IllegalStateException("Startup recovery is already armed");
		}
		directory.createDirectories();
		try (FileChannel probe = directory.joinSegment(LOCK).openFileChannel(StandardOpenOption.CREATE, StandardOpenOption.WRITE))
		{
			FileLock held;
			try
			{
				held = probe.tryLock();
			}
			catch (OverlappingFileLockException ignored)
			{
				held = null;
			}
			if (held == null)
			{
				return Result.BUSY;
			}
			try
			{
				Filepath marker = directory.joinSegment(MARKER);
				if (marker.exists())
				{
					marker.delete();
					return Result.RECOVERED;
				}
				return Result.READY;
			}
			finally
			{
				held.release();
			}
		}
	}

	void finish(boolean clearMarker) throws IOException
	{
		try
		{
			if (clearMarker && ownsMarker && lock != null && lock.isValid())
			{
				directory.joinSegment(MARKER).deleteIfExists();
			}
		}
		finally
		{
			ownsMarker = false;
			lock = null;
			if (channel != null)
			{
				FileChannel closing = channel;
				channel = null;
				closing.close();
			}
		}
	}
}
