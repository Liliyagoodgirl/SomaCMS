package fi.essentia.somacms.dao;

import fi.essentia.somacms.models.DatabaseDocument;
import fi.essentia.somacms.models.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQL based implementation for storing the metadata
 */
@Repository
public class SqlDocumentDao implements DocumentDao {
    private JdbcTemplate jdbcTemplate;
    private SimpleJdbcInsert insertDocument;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        insertDocument = new SimpleJdbcInsert(dataSource).withTableName("document").usingGeneratedKeyColumns("id");
    }

    @Override
    public DatabaseDocument findById(long id) {
        String query = "SELECT * FROM document WHERE id=?";
        DatabaseDocument document = (DatabaseDocument) jdbcTemplate.queryForObject(query, new Object[]{id}, new BeanPropertyRowMapper(DatabaseDocument.class));
        return document;
    }

    @Override
    public long save(DatabaseDocument databaseDocument) {
        Number key = insertDocument.executeAndReturnKey(new BeanPropertySqlParameterSource(databaseDocument));
        databaseDocument.setId(key.longValue());
        return databaseDocument.getId();
    }

    @Override
    public void update(Document document) {
        jdbcTemplate.update("UPDATE document SET name=?, size=?, parent_id=?, mime_type=?, folder=?, created=?, modified=?, isVersion=? WHERE id=?",
                document.getName(), document.getSize(), document.getParentId(), document.getMimeType(), document.isFolder(), document.getCreated(), document.getModified(),
                document.isVersion(), document.getId());
    }

    @Override
    public List<DatabaseDocument> findByParentId(Long parentId) {
        if (parentId == null) {
            return jdbcTemplate.query("SELECT * FROM document WHERE parent_id IS NULL", BeanPropertyRowMapper.newInstance(DatabaseDocument.class));
        } else {
            return jdbcTemplate.query("SELECT * FROM document WHERE parent_id=?", BeanPropertyRowMapper.newInstance(DatabaseDocument.class), parentId);
        }
    }

    @Override
    public Integer numberOfVersions(Long parentId, String documentName) {
        String nameTemplate = documentName + "_%";
        String query = "SELECT name FROM document WHERE parent_id=? and name LIKE ?";
        List<String> rows = jdbcTemplate.queryForList(query, String.class, parentId, nameTemplate);
        Integer maxVersion = 0;
        for (String name : rows) {
            Integer version = Integer.parseInt(name.substring(nameTemplate.length()-1));
            if (version > maxVersion) maxVersion = version;
        }
        return maxVersion;
    }

    @Override
    public Long idOfOldestVersion(Long parentId, String documentName) throws ParseException {
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

    @Override
    public List<DatabaseDocument> findAll() {
        return jdbcTemplate.query("SELECT * FROM document", BeanPropertyRowMapper.newInstance(DatabaseDocument.class));
    }

    @Override
    public List<DatabaseDocument> findAllWithoutVersions() {
        return jdbcTemplate.query("SELECT * FROM document WHERE isVersion IS NULL OR isVersion<>1", BeanPropertyRowMapper.newInstance(DatabaseDocument.class));
    }

    @Override public void deleteById(Long documentId) {
        jdbcTemplate.update("DELETE FROM document WHERE id=?", documentId);
    }
}
