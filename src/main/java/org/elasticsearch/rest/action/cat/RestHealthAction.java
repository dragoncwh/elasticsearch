/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
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

package org.elasticsearch.rest.action.cat;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Table;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestTable;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.Locale;

import static org.elasticsearch.rest.RestRequest.Method.GET;

public class RestHealthAction extends AbstractCatAction {

    @Inject
    public RestHealthAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/_cat/health", this);
    }

    @Override
    void documentation(StringBuilder sb) {
        sb.append("/_cat/health\n");
    }

    @Override
    public void doRequest(final RestRequest request, final RestChannel channel) {
        ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest();

        client.admin().cluster().health(clusterHealthRequest, new ActionListener<ClusterHealthResponse>() {
            @Override
            public void onResponse(final ClusterHealthResponse health) {
                try {
                    channel.sendResponse(RestTable.buildResponse(buildTable(health, request), request, channel));
                } catch (Throwable t) {
                    onFailure(t);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }

    @Override
    Table getTableWithHeader(final RestRequest request) {
        Table t = new Table();
        t.startHeaders();
        t.addCell("time(ms)", "desc:time, in milliseconds since epoch UTC, that the count was executed");
        t.addCell("timestamp", "desc:time that the count was executed");
        t.addCell("cluster", "desc:cluster name");
        t.addCell("status", "desc:health status");
        t.addCell("nodeTotal", "text-align:right;desc:total number of nodes");
        t.addCell("nodeData", "text-align:right;desc:number of nodes that can store data");
        t.addCell("shards", "text-align:right;desc:total number of shards");
        t.addCell("pri", "text-align:right;desc:number of primary shards");
        t.addCell("relo", "text-align:right;desc:number of relocating nodes");
        t.addCell("init", "text-align:right;desc:number of initializing nodes");
        t.addCell("unassign", "text-align:right;desc:number of unassigned shards");
        t.endHeaders();

        return t;
    }

    private DateTimeFormatter dateFormat = DateTimeFormat.forPattern("HH:mm:ss");

    private Table buildTable(final ClusterHealthResponse health, final RestRequest request) {
        long time = System.currentTimeMillis();
        Table t = getTableWithHeader(request);
        t.startRow();
        t.addCell(time);
        t.addCell(dateFormat.print(time));
        t.addCell(health.getClusterName());
        t.addCell(health.getStatus().name().toLowerCase(Locale.ROOT));
        t.addCell(health.getNumberOfNodes());
        t.addCell(health.getNumberOfDataNodes());
        t.addCell(health.getActiveShards());
        t.addCell(health.getActivePrimaryShards());
        t.addCell(health.getRelocatingShards());
        t.addCell(health.getInitializingShards());
        t.addCell(health.getUnassignedShards());
        t.endRow();
        return t;
    }
}
