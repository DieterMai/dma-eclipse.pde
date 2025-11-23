package org.eclipse.pde.internal.core.util;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Deletes content of a directory recursively.
 */
public class DeleteContentWalker implements FileVisitor<Path> {
	public static void main(String[] args) {
		new DeleteContentWalker(Path.of("C:\\Users\\maidi\\devel\\temp\\example-ws\\root"), null).walk();
	}

	public static boolean isWindows = System.getProperty("os.name").startsWith("Win"); //$NON-NLS-1$ //$NON-NLS-2$

	private final Path root;
	private final IProgressMonitor monitor;

	public DeleteContentWalker(Path root, IProgressMonitor monitor) {
		this.root = root;
		this.monitor = SubMonitor.convert(monitor);
	}

	public void walk() {
		if (root == null || !Files.exists(root)) {
			monitor.done();
			return;
		}

		try {
			log("Start walking");
			Files.walkFileTree(root, this);
			log("Done walking");
		} catch (IOException e) {
			log("Failed walking");
			e.printStackTrace();
		}
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		log("preVisitDirectory: " + dir);
		// if (Files.isSymbolicLink(dir)) {
		// Files.deleteIfExists(dir);
		// return FileVisitResult.SKIP_SUBTREE;
		//
		// }
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
		log("visitFile: " + path);
		try {
			boolean result = Files.deleteIfExists(path);
			log("\tDeleted file: " + result);
		} catch (AccessDeniedException ade) {
			if (isWindows) {
				log("\tWindow read only file. Try again");
				removeReadOnlyWindowsFile(path);
				log("\tWindow read only file deleted");
			}
		}

		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	private void removeReadOnlyWindowsFile(Path path) throws IOException {
		DosFileAttributes attrs = Files.readAttributes(path, DosFileAttributes.class);
		boolean isReadOnly = attrs.isReadOnly();
		if (isReadOnly) { // TODO fail if not
			Files.setAttribute(path, "dos:readonly", false); //$NON-NLS-1$
			Files.delete(path);
		}
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		log("visitFileFailed: " + file);
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path path, IOException exc) throws IOException {
		log("postVisitDirectory: " + path);
		try {
			boolean result = Files.deleteIfExists(path);
			log("\tDeleted dir: " + result);
		} catch (AccessDeniedException ade) {
			if (isWindows) {
				log("\tWindow read only dir. Try again");
				removeReadOnlyWindowsFile(path);
				log("\tWindow read only dir deleted");
			}
		}
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	/**
	 * Returns the given result if not canceled. If canceled
	 * {@link FileVisitResult#TERMINATE} is returned.
	 */
	private FileVisitResult resultIfNotCanceled(FileVisitResult result) {
		if (monitor.isCanceled()) {
			log("CANCEL");
			return FileVisitResult.TERMINATE;
		}

		// log("Return " + result);
		return result;
	}

	private void log(String s) {
		System.out.println(s);
	}
}