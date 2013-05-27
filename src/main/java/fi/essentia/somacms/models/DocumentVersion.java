package fi.essentia.somacms.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Represents the version of the document
 */
public class DocumentVersion implements Version {
    @Getter @Setter private long document_id;
    @Getter @Setter private int document_version;
    @Getter @Setter private Date creation_time;
}
