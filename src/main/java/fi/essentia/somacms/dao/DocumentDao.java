package fi.essentia.somacms.dao;

import fi.essentia.somacms.models.DatabaseDocument;
import fi.essentia.somacms.models.Document;
import fi.essentia.somacms.tree.TreeDocument;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Stores and loads the metadata for the documents
 */
public interface DocumentDao {
    public DatabaseDocument findById(long id);
    public DatabaseDocument findByParentIdAndName(long parentId, String documentName);
    public long save(DatabaseDocument document);
    void update(Document document);
    List<DatabaseDocument> findByParentId(Long parentId);
    List<DatabaseDocument> findAll();
    List<DatabaseDocument> findAllWithoutBackups();

    Integer numberOfBackups(Long parentId, String documentName);
    Long idOfOldestBackup(Long parentId, String documentName) throws ParseException;

    void deleteById(Long documentId);
}
