package fi.essentia.somacms.dao;

/**
 * Stores and loads the byte-based data (i.e. the contents) of the documents
 */
public interface DataDao extends ReadOnlyDataDao {
    void insertData(long documentId, int documentVersion, byte[] data);
    void updateData(long documentId, int documentVersion, byte[] data);
}
