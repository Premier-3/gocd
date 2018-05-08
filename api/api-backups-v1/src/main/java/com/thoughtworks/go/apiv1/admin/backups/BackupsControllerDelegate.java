/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.apiv1.admin.backups;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.admin.backups.representers.BackupRepresenter;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.security.HeaderConstraint;
import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.util.SystemEnvironment;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseDeprecatedConfirmHeaderMissing;
import static spark.Spark.*;

public class BackupsControllerDelegate extends ApiController {
    private static final HeaderConstraint HEADER_CONSTRAINT = new HeaderConstraint(new SystemEnvironment());

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final BackupService backupService;

    public BackupsControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper, BackupService backupService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.backupService = backupService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Backups.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this::verifyConfirmHeader);
            before("/*", this::verifyConfirmHeader);

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            post("", mimeType, this::create);
        });
    }

    public String create(Request request, Response response) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerBackup backup = backupService.startBackup(currentUsername(), result);
        if (result.isSuccessful()) {
            return writerForTopLevelObject(request, response, outputWriter -> BackupRepresenter.toJSON(outputWriter, backup));
        }
        return renderHTTPOperationResult(result, request, response);
    }

    private void verifyConfirmHeader(Request request, Response response) {
        if (!HEADER_CONSTRAINT.isSatisfied(request.raw())) {
            throw haltBecauseDeprecatedConfirmHeaderMissing();
        }
    }
}
