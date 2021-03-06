package fi.essentia.somacms.util;

import fi.essentia.somacms.dao.ReadOnlyDataDao;
import fi.essentia.somacms.tree.DocumentManager;
import fi.essentia.somacms.tree.TreeDocument;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Stores multiple documents to the given folder by reading their contents from a ZIP archive
 */
@Component
public class ArchiveHelper {
    @Autowired private DocumentManager documentManager;
    @Autowired private ReadOnlyDataDao readOnlyDataDao;

    public void storeDocuments(TreeDocument targetFolder, byte[] bytes) throws IOException, ParseException {
        List<DocumentEntry> entries = new ArrayList<DocumentEntry>();
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry zipEntry = in.getNextEntry();
        while(zipEntry!=null){
            File file = new File(zipEntry.getName());
            if (zipEntry.isDirectory()) {
                entries.add(new DocumentEntry(true, file.getParent(), file.getName(), null));
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                IOUtils.copy(in, out);
                out.close();
                entries.add(new DocumentEntry(false, file.getParent(), file.getName(), out.toByteArray()));
            }
            zipEntry = in.getNextEntry();
        }
        in.closeEntry();
        in.close();

        Collections.sort(entries);
        for (DocumentEntry entry : entries) {
            TreeDocument parent;
            if (entry.path == null) {
                parent = documentManager.documentById(targetFolder.getId());
                if (parent == null) {
                    throw new RuntimeException("Failed to find the target folder");
                }
            } else {
                parent = findParent(targetFolder, entry.path);
            }
            if (parent == null) {
                System.out.println("parent = " + null);
            } else {
                System.out.println("parent.getPath() = " + parent.getPath());
            }
            System.out.println("entry.path = " + entry.path);
            if (parent == null) {
                throw new NullPointerException("Failed to find the parent folder for " + entry.name);
            }

            if (entry.folder) {
                if (parent.childByName(entry.name) == null) {
                    documentManager.createFolder(parent.getId(), entry.name);
                }
            } else {
                documentManager.storeDocument(parent.getId(), entry.name, entry.data);
            }
        }
    }

    private TreeDocument findParent(TreeDocument targetFolder, String filePath) {
        if (targetFolder.isRoot()) {
            return documentManager.documentFromPath(filePath);
        } else {
            String rootPath = targetFolder.getPath().substring(1);
            return documentManager.documentFromPath(rootPath + filePath);
        }
    }

    public byte[] documentAsArchive(TreeDocument document) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(byteArrayOutputStream);
        String rootPath;
        if (document.isRoot()) {
            rootPath = "/";
        } else {
            rootPath = document.getParent().getPath();
        }
        archiveDocument(out, document, rootPath);
        out.close();
        return byteArrayOutputStream.toByteArray();
    }

    private void archiveDocument(ZipOutputStream out, TreeDocument document, String rootPath) throws IOException {
        String fullPath = document.getPath();
        String relativePath = fullPath.substring(rootPath.length()-1);

        if (document.isFolder()) {
            // Create an empty zip entry for directories except root
            if (!document.isRoot()) {
                out.putNextEntry(new ZipEntry(relativePath));
                out.closeEntry();
            }
            Collection<TreeDocument> children = document.getChildren();
            for (TreeDocument child : children) {
                archiveDocument(out, child, rootPath);
            }
        } else {
            out.putNextEntry(new ZipEntry(relativePath));
            byte[] bytes = readOnlyDataDao.loadData(document.getId(), 0);
            out.write(bytes);
            out.closeEntry();
        }
    }

    /**
     * Sometimes an archive may first contain the file definition and the folder only afterwards so we'll need to
     * sort them.
     */
    private static class DocumentEntry implements Comparable<DocumentEntry> {
        private boolean folder;
        private String path;
        private String name;
        private byte[] data;

        private DocumentEntry(boolean folder, String path, String name, byte[] data) {
            this.folder = folder;
            this.path = path;
            this.name = name;
            this.data = data;
        }

        @Override
        public int compareTo(DocumentEntry other) {
            if (folder) {
                if (!other.folder) {
                    return -1;
                }
            } else if (other.folder) {
                return 1;
            }

            int pathDiff = pathLength() - other.pathLength();
            if (pathDiff != 0) {
                return pathDiff;
            }

            return name.compareTo(other.name);
        }

        private int pathLength() {
            if (path == null) {
                return 0;
            }
            return path.split("/").length;
        }
    }
}
