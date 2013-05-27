package fi.essentia.somacms.tree;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import fi.essentia.somacms.controllers.UnauthorizedException;
import fi.essentia.somacms.dao.DataDao;
import fi.essentia.somacms.dao.DocumentDao;
import fi.essentia.somacms.dao.VersionDao;
import fi.essentia.somacms.models.DatabaseDocument;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of the DocumentManager. Keeps the document metadata in memory for faster access.
 */
@Component
@Transactional
public class DocumentManagerImpl implements DocumentManager {
    private static final Logger logger = LoggerFactory.getLogger(DocumentManagerImpl.class);
    private Tika tika = new Tika();
    private final Map<Long, TreeDocument> idToDocument = new ConcurrentHashMap<Long, TreeDocument>();
    private TreeDocument root;

    @Autowired DocumentDao documentDao;
    @Autowired DataDao dataDao;
    @Autowired VersionDao versionDao;

    @PostConstruct
    public void initialize() {
        loadDocuments();
        initializeRoot();
        linkDocuments();
    }

    private void initializeRoot() {
        root = idToDocument.get(TreeDocument.ROOT_ID);
    }

    private void linkDocuments() {
        for (TreeDocument document : idToDocument.values()) {
            if (document == root) {
                continue;
            }

            TreeDocument parent = parentFromId(document.getParentId());
            document.setParent(parent);
            parent.addChild(document);
        }
    }

    private void loadDocuments() {
        List<DatabaseDocument> databaseDocuments = documentDao.findAll();
        for (DatabaseDocument databaseDocument : databaseDocuments) {
            TreeDocument treeDocument = new TreeDocument(databaseDocument);
            /*Integer numberOfBackups = documentDao.numberOfBackups(databaseDocument.getParentId(), databaseDocument.getName());
            for (int i = 1; i <= numberOfBackups; i++) {
                String backupName = databaseDocument.getName() + "_" + i;
                DatabaseDocument backupDocument = documentDao.findByParentIdAndName(databaseDocument.getParentId(), backupName);
                byte[] currentBytes = dataDao.loadData(backupDocument.getId());
                String modifiedDate = backupDocument.getModified().toString();
                modifiedDate = modifiedDate.replace(".0", "");
                treeDocument.addBackupDateToData(modifiedDate, new String(currentBytes));
            }*/
            idToDocument.put(treeDocument.getId(), treeDocument);
        }
    }

    @Override
    public TreeDocument documentFromPath(String path) {
        // Shortcut for root
        if (path.equals("/")) {
            return root;
        }

        // Strip the first slash because otherwise split generates an empty string as the first element
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String[] split = path.split("/");
        TreeDocument document = root;
        for (String name : split) {
            document = document.childByName(name);
            if (document == null) {
                return null;
            }
        }
        return document;
    }

    @Override
    public TreeDocument documentById(Long id) {
        return idToDocument.get(id);
    }

    @Override
    public TreeDocument createFolder(Long parentId, String name) {
        return createDocument(parentId, name, true);
    }

    private TreeDocument createDocument(Long parentId, String name, boolean folder) throws UnsupportedMimeTypeException {
        TreeDocument parent = folder(parentId);
        DatabaseDocument databaseDocument = new DatabaseDocument();
        databaseDocument.setName(name);
        databaseDocument.setFolder(folder);
        if (!folder) {
            String mimeType = tika.detect(name);
            databaseDocument.setMimeType(mimeType);
            if (!databaseDocument.isText()) {
                throw new UnsupportedMimeTypeException();
            }
        }
        databaseDocument.setParentId(parent.getId());

        documentDao.save(databaseDocument);
        return addToTree(databaseDocument, parentId);
    }

    @Override
    public TreeDocument createTextFile(Long parentId, String name) {
        TreeDocument document = createDocument(parentId, name, false);
        versionDao.insertVersion(document.getId(), 0, new Date());
        dataDao.insertData(document.getId(), 0, new byte[0]);
        return document;
    }

    private TreeDocument folder(Long folderId) {
        TreeDocument folder = documentById(folderId);
        if (!folder.isFolder()) {
            throw new UnauthorizedException();
        }
        return folder;
    }

    private TreeDocument parentFromId(Long parentId) {
        return idToDocument.get(parentId);
    }

    @Override
    public TreeDocument storeDocument(Long parentId, String fileName, byte[] bytes) throws ParseException {
        String mimeType = tika.detect(bytes, fileName);
        TreeDocument parent = folder(parentId);
        TreeDocument document = parent.childByName(fileName);
        Date currentDate = new Date();

        if (document == null) {

            logger.debug("New document, parentId = " + parentId + ", fileName = " + fileName);
            DatabaseDocument databaseDocument = new DatabaseDocument();
            databaseDocument.setName(fileName);
            databaseDocument.setParentId(parent.getId());
            databaseDocument.setModified(currentDate);
            databaseDocument.setSize(bytes.length);
            databaseDocument.setMimeType(mimeType);
            long documentId = documentDao.save(databaseDocument);
            versionDao.insertVersion(documentId, 0, currentDate);
            dataDao.insertData(documentId, 0, bytes);
            document = addToTree(databaseDocument, parentId);
        } else {

            // Store the current document as the next version
            Long documentId = document.getId();
            Integer latestVersion = versionDao.maxVersion(documentId);
            logger.debug("Latest version " + latestVersion);
            byte[] currentBytes = dataDao.loadData(documentId, 0);
            versionDao.insertVersion(documentId, latestVersion + 1, currentDate);
            dataDao.insertData(documentId, latestVersion + 1, currentBytes);
            logger.debug("New version " + latestVersion + 1  + " for the document, parentId = " + parentId + ", fileName = " + fileName);

            // Overwrite the document itself
            document.setModified(currentDate);
            document.setSize(bytes.length);
            document.setMimeType(mimeType);
            documentDao.update(document);
            versionDao.update(document.getId(), 0, currentDate);
            dataDao.updateData(documentId, 0, bytes);


            /*
            Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = formatter.format(date);

            // Overwrite the oldest version
            else {
                Long replaceDocumentId = documentDao.idOfOldestBackup(parentId, fileName);
                logger.debug("replace id = " + replaceDocumentId);
                DatabaseDocument oldestDocument = documentDao.findById(replaceDocumentId);
                String previousDate = oldestDocument.getModified().toString().replace(".0","");
                Date newDate = new Date();
                oldestDocument.setModified(newDate);
                Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String newDateString = formatter.format(newDate);
                byte[] currentBytes = dataDao.loadData(document.getId(), );
                oldestDocument.setSize(currentBytes.length);
                oldestDocument.setMimeType(document.getMimeType());
                oldestDocument.setBackup(true);
                documentDao.update(oldestDocument);
                dataDao.updateData(replaceDocumentId, currentBytes);
                document.removeBackupData(previousDate);
                document.addBackupDateToData(newDateString, new String(currentBytes));
                logger.debug("Replaced oldest version for the document, documentId = " + replaceDocumentId);
            }
            */
        }
        return document;
    }

    @Override
    public TreeDocument deleteDocument(Long documentId) {
        TreeDocument document = documentById(documentId);
        if (document.isRoot()) {
            throw new UnauthorizedException();
        }

        List<TreeDocument> children = new ArrayList<TreeDocument>(document.getChildren());
        for (TreeDocument child : children) {
            deleteDocument(child.getId());
        }

        documentDao.deleteById(documentId);
        document.getParent().removeChild(document);
        idToDocument.remove(documentId);
        return document;
    }

    @Override
    public Collection<TreeDocument> documentsByPath(final String path) {
        return Collections2.filter(idToDocument.values(), new Predicate<TreeDocument>() {
            @Override
            public boolean apply(TreeDocument treeDocument) {
                return StringUtils.containsIgnoreCase(treeDocument.getPath(), path) && treeDocument.isViewable() && !treeDocument.isRoot();
            }
        });
    }

    private TreeDocument addToTree(DatabaseDocument databaseDocument, Long parentId) {
        TreeDocument treeDocument = new TreeDocument(databaseDocument);
        TreeDocument parent = parentFromId(parentId);
        parent.addChild(treeDocument);
        treeDocument.setParent(parent);
        idToDocument.put(treeDocument.getId(), treeDocument);
        return treeDocument;
    }
}
