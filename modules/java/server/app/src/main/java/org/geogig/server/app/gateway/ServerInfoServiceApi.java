package org.geogig.server.app.gateway;

import org.geogig.web.model.Version;
import org.geogig.web.model.VersionInfo;
import org.geogig.web.server.api.ServiceInfoApi;
import org.geogig.web.server.api.ServiceInfoApiDelegate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * API delegate implementation for the swagger-codegen auto-generated {@link ServiceInfoApi}.
 */
public @Service class ServerInfoServiceApi extends AbstractService
        implements ServiceInfoApiDelegate {

    public @Override ResponseEntity<VersionInfo> getVersion() {
        return ok(Version.get());
    }
}
