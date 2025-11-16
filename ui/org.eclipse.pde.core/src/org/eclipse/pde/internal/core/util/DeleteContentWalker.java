package org.eclipse.pde.internal.core.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Deletes content of a directory recursively.
 */
public class DeleteContentWalker implements FileVisitor<Path> {

	private final Path root;
	private final IProgressMonitor monitor;

	public DeleteContentWalker(Path root, IProgressMonitor monitor) {
		this.root = root;
		if (monitor == null) {
			this.monitor = new NullProgressMonitor();
		} else {
			int count;
			try {
				// report progress depending on files in root directory.
				// limit to 1000 to prevent blocking.
				count = (int) Files.list(root).limit(1_000).count();
			} catch (IOException e) {
				// exception not relevant
				count = 1;
			}
			this.monitor = SubMonitor.convert(monitor, count);
		}
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (Files.isSymbolicLink(dir)) {
			Files.deleteIfExists(dir);
			return FileVisitResult.SKIP_SUBTREE;

		}
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		System.out.println("Delete file " + file);
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			System.out.println("failed to delete file.");
			file.toFile().delete();
		}

		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		if (exc == null) {
			System.out.println("Delete Dir: " + dir + " - " + Files.deleteIfExists(dir));
			// Files.deleteIfExists(dir);

			if (dir.getParent().equals(root)) {
				monitor.worked(1);
			}
		} else {
			exc.printStackTrace();
		}
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	/**
	 * Returns the given result if not canceled. If canceled
	 * {@link FileVisitResult#TERMINATE} is returned.
	 */
	private FileVisitResult resultIfNotCanceled(FileVisitResult result) {
		if (monitor.isCanceled()) {
			System.out.println("CANCEL");
			return FileVisitResult.TERMINATE;
		}

		System.out.println("Return " + result);
		return result;
	}
}