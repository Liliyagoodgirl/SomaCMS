package fi.essentia.simplecms.models;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Getter @Setter
public class Content {
    private long id;
    private long documentId;
    private byte[] data;
}