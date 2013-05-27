package fi.essentia.somacms.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * SQL based implementation for storing the document contents
 */
@Component
public class SqlDataDao implements DataDao {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void insertData(long documentId, int documentVersion, byte[] data) {
        jdbcTemplate.update("INSERT INTO document_data (document_id, document_version, data) VALUES(?, ?)", documentId, documentVersion, data);
    }

    @Override
    public void updateData(long documentId, int documentVersion, byte[] data) {
        jdbcTemplate.update("UPDATE document_data SET data=? WHERE document_id=? AND document_version=?", data, documentId, documentVersion);
    }

    @Override
    public byte[] loadData(long documentId, int documentVersion) {
        Blob blob = jdbcTemplate.queryForObject("SELECT data FROM document_data WHERE document_id=? and document_version=?", Blob.class, documentId, documentVersion);
        try {
            return blob.getBytes(1, (int)blob.length());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
