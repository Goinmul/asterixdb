/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.api.http.server;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import org.apache.asterix.api.http.server.QueryServiceServlet.Parameter;
import org.apache.asterix.common.api.IClientRequest;
import org.apache.asterix.common.dataflow.ICcApplicationContext;
import org.apache.asterix.translator.IStatementExecutorContext;
import org.apache.hyracks.http.api.IServletRequest;
import org.apache.hyracks.http.api.IServletResponse;
import org.apache.hyracks.http.server.AbstractServlet;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * The servlet provides a REST API for cancelling an on-going query.
 */
public class CcQueryCancellationServlet extends AbstractServlet {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ICcApplicationContext appCtx;

    public CcQueryCancellationServlet(ConcurrentMap<String, Object> ctx, ICcApplicationContext appCtx,
            String... paths) {
        super(ctx, paths);
        this.appCtx = appCtx;
    }

    @Override
    protected void delete(IServletRequest request, IServletResponse response) throws IOException {
        String clientContextId = request.getParameter(Parameter.CLIENT_ID.str());
        if (clientContextId == null) {
            response.setStatus(HttpResponseStatus.BAD_REQUEST);
            return;
        }
        IStatementExecutorContext executorCtx =
                (IStatementExecutorContext) ctx.get(ServletConstants.RUNNING_QUERIES_ATTR);
        IClientRequest req = executorCtx.get(clientContextId);
        if (req == null) {
            // response: NOT FOUND
            response.setStatus(HttpResponseStatus.NOT_FOUND);
            return;
        }
        try {
            // Cancels the on-going job.
            req.cancel(appCtx);
            // response: OK
            response.setStatus(HttpResponseStatus.OK);
        } catch (Exception e) {
            LOGGER.log(Level.WARN, "unexpected exception thrown from cancel", e);
            // response: INTERNAL SERVER ERROR
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}