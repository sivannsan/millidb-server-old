package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.Validate;
import com.sivannsan.millidata.*;
import com.sivannsan.millifile.MilliFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MilliDBService {
    @Nonnull
    private final Socket socket;
    private String userName;

    public MilliDBService(@Nonnull Socket socket) {
        this.socket = Validate.nonnull(socket);
    }

    @Nonnull
    public String getIP() {
        return socket.getInetAddress().getHostAddress();
    }

    public void start() {
        new Thread(() -> {
            MilliDBLogs.info("<-> '" + getIP() + "': " + Holder.count() + " connection" + (Holder.count() > 1 ? "s" : ""));
            try {
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String query;
                do {
                    query = reader.readLine();
                    if (query == null) writer.println("null"); //?
                    else {
                        MilliDBLogs.info("<- '" + getIP() + "': " + query);
                        MilliDBResult result = serve(query);
                        writer.println(result.toMilliMap());
                        MilliDBLogs.info("-> '" + getIP() + "': " + result.toMilliMap());
                    }
                } while (query != null);
                reader.close();
            } catch (IOException ignored) {
            } finally {
                Holder.remove(this);
                MilliDBLogs.info("--- '" + socket.getInetAddress().getHostAddress() + "': " + Holder.count() + " connection" + (Holder.count() > 1 ? "s" : ""));
            }
        }).start();
    }

    private MilliDBResult serve(@Nonnull String queryString) {
        MilliDBQuery query;
        try {
            query = MilliDBQuery.Parser.parse(queryString);
        } catch (Exception e) {
            e.printStackTrace();
            return MilliDBResult.invalidResult();
        }
        long id = query.getID();
        MilliDBQuery.Function function = query.getFunction();
        MilliData metadata = query.getMetadata();

        if (function == MilliDBQuery.Function.NONE) {
            return new MilliDBResult(id, true, MilliNull.INSTANCE);
        }
        if (function == MilliDBQuery.Function.CLOSE) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, 1000L * 5);
            return new MilliDBResult(id, true, MilliNull.INSTANCE);
        }
        if (function == MilliDBQuery.Function.HAS_USER) {
            userName = metadata.asMilliMap(new MilliMap()).get("user_name").asMilliValue(new MilliValue()).asString();
            String userPassword = metadata.asMilliMap(new MilliMap()).get("user_password").asMilliValue(new MilliValue()).asString();
            return new MilliDBResult(id, true, new MilliValue(MilliDBUsers.hasUser(userName, userPassword)));
        }
        if (function == MilliDBQuery.Function.GET_FILES) {
            MilliList list = new MilliList();
            for (MilliFile db : MilliDBFiles.getFiles(userName, query.getPath(), MilliDBFilter.Parser.parse(metadata.asMilliMap(new MilliMap())))) list.add(new MilliValue(db.getFile().getName()));
            return new MilliDBResult(id, true, list);
        }
        if (function == MilliDBQuery.Function.GET_COLLECTION) {
            String path = query.getPath().equals("") ? query.getMetadata().asMilliValue().asString() : query.getPath() + "/" + query.getMetadata().asMilliValue().asString();
            return new MilliDBResult(id, true, new MilliValue(MilliDBUsers.hasPermission(userName, path)));
        }
        if (function == MilliDBQuery.Function.GET_DOCUMENT) {
            String path = query.getPath().equals("") ? query.getMetadata().asMilliValue().asString() + ".mll" : query.getPath() + "/" + query.getMetadata().asMilliValue().asString() + ".mll";
            return new MilliDBResult(id, true, new MilliValue(MilliDBUsers.hasPermission(userName, path)));
        }
        if (function == MilliDBQuery.Function.GET) {
            MilliData data = MilliDBFiles.get(query.getPath(), query.getMetadata().asMilliValue().asString());
            return data == null ? MilliDBResult.failedResult(id) : new MilliDBResult(id, true, data);
        }
        if (function == MilliDBQuery.Function.SET) {
            boolean succeed = MilliDBFiles.set(query.getPath(), query.getMetadata().asMilliMap(new MilliMap()).get("p").asMilliValue().asString()/*Not use default path as ""*/, query.getMetadata().asMilliMap(new MilliMap()).get("v"));
            return new MilliDBResult(id, succeed, MilliNull.INSTANCE);
        }
        if (function == MilliDBQuery.Function.DELETE) {
            return new MilliDBResult(id, MilliDBFiles.delete(query.getPath()), MilliNull.INSTANCE);
        }
        return MilliDBResult.invalidResult();
    }

    public static final class Holder {
        @Nonnull
        private static final List<MilliDBService> services = new ArrayList<>();

        public static int count() {
            return services.size();
        }

        @Nonnull
        public static List<MilliDBService> getAll() {
            return new ArrayList<>(services);
        }

        public static void add(@Nonnull MilliDBService service) {
            services.add(Validate.nonnull(service));
        }

        public static void remove(@Nonnull MilliDBService service) {
            services.remove(Validate.nonnull(service));
        }
    }
}
