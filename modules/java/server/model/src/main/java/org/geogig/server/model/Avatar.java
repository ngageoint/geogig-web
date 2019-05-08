package org.geogig.server.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public @Entity @Data @NoArgsConstructor @AllArgsConstructor class Avatar {

    private @Id String emailAddress;

    private @Lob byte[] rawImage;

    public boolean isEmpty() {
        return rawImage == null || rawImage.length == 0;
    }
}
