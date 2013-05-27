package fi.essentia.somacms.dao;

import fi.essentia.somacms.models.DatabaseDocument;
import fi.essentia.somacms.models.DocumentVersion;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Stores and loads the metadata for the documents
 */
public interface VersionDao {
    DocumentVersion findSortedByDocumentId(long id);
    void insertVersion(long documentId, int version, Date creation_time);
    void update(long documentId, int version, Date creation_time);
    int maxVersion(long documentId);
}
