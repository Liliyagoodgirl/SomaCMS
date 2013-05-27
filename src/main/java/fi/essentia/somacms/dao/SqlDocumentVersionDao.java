package fi.essentia.somacms.dao;

import fi.essentia.somacms.models.DatabaseDocument;
import fi.essentia.somacms.models.Document;
import fi.essentia.somacms.models.DocumentVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * SQL based implementation for storing the metadata for the version
 */
@Component
public class SqlDocumentVersionDao implements VersionDao {
    private JdbcTemplate jdbcTemplate;
    private SimpleJdbcInsert insertVersion;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public DocumentVersion findSortedByDocumentId(long id) {
        String query = "SELECT * FROM document_version WHERE document_id=? ORDER BY creation_time ASC";
        DocumentVersion version = (DocumentVersion) jdbcTemplate.queryForObject(query, new Object[]{id}, new BeanPropertyRowMapper(DocumentVersion.class));
        return version;
    }

    @Override
    public void insertVersion(long documentId, int documentVersion, Date creation_time) {
        jdbcTemplate.update("INSERT INTO document_version (document_id, document_version, creation_time) VALUES(?, ?, ?)", documentId, documentVersion, creation_time);
    }

    @Override
    public void update(long documentId, int version, Date creation_time) {
    jdbcTemplate.update("UPDATE document_version SET creation_time=? WHERE document_id=? and document_version=?",
            creation_time, documentId, version);
    }

    @Override
    public int maxVersion(long documentId) {
        String query = "SELECT document_version FROM document_version WHERE document_id=? ORDER BY document_version DESC limit 1";
        List<Integer> rows = jdbcTemplate.queryForList(query, Integer.class, documentId);

        return rows.get(0);
    }

    /*
    @Override
    public Long idOfOldestBackup(Long parentId, String documentName) throws ParseException {
        String nameTemplate = documentName + "_%";
        String query = "SELECT * FROM document WHERE parent_id=? and name LIKE ?";
        List<DatabaseDocument> rows = jdbcTemplate.query(query, BeanPropertyRowMapper.newInstance(DatabaseDocument.class), parentId, nameTemplate);
        Date oldestDate = new Date();
        Long id = null;
        for (DatabaseDocument document : rows) {
            Date created = document.getModified();
            if (created.compareTo(oldestDate) < 0 ) {
                oldestDate = created;
                id = document.getId();
            }
        }
        return id;
    }

    /*@Override
    public List<DatabaseDocument> findAllWithoutBackups() {
        return jdbcTemplate.query("SELECT * FROM document WHERE backup<>1", BeanPropertyRowMapper.newInstance(DatabaseDocument.class));
    }
    */
}
