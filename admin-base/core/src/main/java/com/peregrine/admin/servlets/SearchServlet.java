package com.peregrine.admin.servlets;

/*-
 * #%L
 * admin base - Core
 * %%
 * Copyright (C) 2017 headwire inc.
 * %%
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.Servlet;

import com.peregrine.commons.servlets.AbstractBaseServlet;
import org.osgi.service.component.annotations.Component;

import static com.peregrine.commons.util.PerUtil.EQUALS;
import static com.peregrine.commons.util.PerUtil.GET;
import static com.peregrine.commons.util.PerUtil.PER_PREFIX;
import static com.peregrine.commons.util.PerUtil.PER_VENDOR;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

/**
 * Raw Query Based Search. This is not part of the '/api/admin' but rather '/bin'.
 */
@Component(
    service = Servlet.class,
    property = {
        SERVICE_DESCRIPTION + EQUALS + PER_PREFIX + "Search Servlet",
        SERVICE_VENDOR + EQUALS + PER_VENDOR,
        SLING_SERVLET_METHODS + EQUALS + GET,
        SLING_SERVLET_PATHS + EQUALS + "/bin/search"
    }
)
@SuppressWarnings("serial")
public class SearchServlet extends AbstractBaseServlet {

    private static final long ROWS_PER_PAGE = 100;

    @Override
    protected Response handleRequest(Request request) throws IOException {
        String query = request.getParameter("q", "");
        if(query.trim().length() == 0) {
            return new ErrorResponse().setHttpErrorCode(SC_BAD_REQUEST).setErrorMessage("No Query Provided");
        } else {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            JsonResponse answer = new JsonResponse();
            try {
                QueryManager qm = session.getWorkspace().getQueryManager();
                Query q = qm.createQuery(query, Query.SQL);
                q.setLimit(ROWS_PER_PAGE+1);
                String pageParam = request.getParameter("page", "0");
                int page = 0;
                try {
                    page = Integer.parseInt(pageParam);
                } catch(NumberFormatException e) {
                    logger.warn("Given Page: '" + pageParam + "' could not be converted to an integer -> ignored", e);
                }
                q.setOffset(page*ROWS_PER_PAGE);

                QueryResult res = q.execute();
                NodeIterator nodes = res.getNodes();
                answer.writeAttribute("current", 1);
                answer.writeAttribute("more", nodes.getSize() > ROWS_PER_PAGE);
                answer.writeArray("data");
                while(nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    answer.writeObject();
                    answer.writeAttribute("name", node.getName());
                    answer.writeAttribute("path", node.getPath());
                    answer.writeClose();
                }
                answer.writeClose();
                return answer;
            } catch(Exception e) {
                return new ErrorResponse().setHttpErrorCode(SC_BAD_REQUEST).setErrorMessage("Failed to execute the query").setException(e);
            }
        }
    }
}

