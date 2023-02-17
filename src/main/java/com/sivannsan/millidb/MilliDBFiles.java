package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.Validate;
import com.sivannsan.millidata.MilliData;
import com.sivannsan.millidata.MilliList;
import com.sivannsan.millidata.MilliMap;
import com.sivannsan.millidata.MilliNull;
import com.sivannsan.millifile.MilliCollection;
import com.sivannsan.millifile.MilliDocument;
import com.sivannsan.millifile.MilliFile;
import com.sivannsan.millifile.MilliFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class MilliDBFiles {
    @Nonnull
    private static final File directory = new File("files");

    private static MilliCollection files;

    public static void load() {
        files = MilliFiles.load(directory).asMilliCollection();
    }

    @Nonnull
    public static List<MilliFile> getFiles(@Nonnull String userName, @Nonnull String path, MilliDBFilter filter) {
        Validate.nonnull(path);
        List<MilliFile> fs = new ArrayList<>();
        MilliCollection collection = files;
        if (!path.equals("")) {
            for (String s : path.split("/")) {
                collection = collection.getCollection(s);
            }
        }
        for (MilliFile f : collection.getFiles()) {
            String newPath = path.equals("") ? f.getFile().getName() : path + "/" + f.getFile().getName();
            if (!MilliDBUsers.hasPermission(userName, newPath)) {
                continue;
            }
            if (filter == null) {
                fs.add(f);
                continue;
            }
            if (filter instanceof MilliDBFilter.SuperOf) {
                MilliDBFilter.SuperOf superOf = (MilliDBFilter.SuperOf) filter;
                if (!f.isMilliDocument()) continue;
                if (!f.asMilliDocument().getContent().superOf(superOf.getSubMilliData(), superOf.getLevel())) continue;
                fs.add(f);
                continue;
            }
            //And more filters...
        }
        return fs;
    }

    //Used for nested only
    private static MilliDocument getDocument(@Nonnull String path) {
        if (path.equals("")) return null;
        MilliDocument document = null;
        MilliCollection collection = files;
        for (String s : path.split("/")) {
            if (s.endsWith(".mll")) {
                document = collection.getDocument(s);
                break;
            }
            collection = collection.getCollection(s);
        }
        return document;
    }

    //Used for nested only
    private static MilliCollection getCollection(@Nonnull String path) {
        if (path.equals("")) return null;
        MilliCollection collection = files;
        for (String s : path.split("/")) collection = collection.getCollection(s);
        return collection;
    }

    //Already has permission

    public static MilliData get(@Nonnull String documentPath, @Nonnull String path) {
        MilliDocument document = getDocument(documentPath);
        if (document == null) return null;
        return get(document, path);
    }

    public static boolean set(@Nonnull String documentPath, @Nonnull String path, @Nonnull MilliData value) {
        MilliDocument document = getDocument(documentPath);
        if (document == null) return false;
        set(document, path, value);
        return true;
    }

    @Nonnull
    public static MilliData get(@Nonnull MilliDocument document, @Nonnull String path) {
        Validate.nonnull(document);
        Validate.nonnull(path);
        if (path.equals("")) return document.getContent();
        MilliData data = document.getContent();
        for (String key : path.split("/")) {
            if (data.isMilliList()) {
                if (!key.startsWith("0") && key.matches("[0-9]+")) data = data.asMilliList().get(Integer.parseInt(key));
                else return MilliNull.INSTANCE;
            } else if (data.isMilliMap()) {
                if (!key.startsWith("0") && key.matches("[0-9]+")) return MilliNull.INSTANCE;
                else data = data.asMilliMap().get(key);
            } else {
                return MilliNull.INSTANCE;
            }
        }
        return data;
    }

    public static void set(@Nonnull MilliDocument document, @Nonnull String path, @Nonnull MilliData value) {
        Validate.nonnull(document);
        Validate.nonnull(path);
        Validate.nonnull(value);
        if (path.equals("")) {
            document.setContent(value);
            return;
        }
        MilliData data = document.getContent();
        if (!path.contains("/")) {
            if (!path.startsWith("0") && path.matches("[0-9]+")) {
                if (!data.isMilliList()) return;
                data.asMilliList().update(Integer.parseInt(path), value);
            } else {
                if (!data.isMilliMap()) {
                    document.setContent(new MilliMap());
                    data = document.getContent();
                }
                data.asMilliMap().put(path, value);
            }
            //Save?
            return;
        }
        MilliData current = document.getContent();
        String keyOrIndex = path.split("/")[0];
        String nextPath = path.replaceFirst(keyOrIndex, "");
        if (nextPath.startsWith("/")) nextPath = nextPath.replaceFirst("/", "");
        if (keyOrIndex.startsWith("0") && path.matches("[0-9]+")) {
            if (!current.isMilliList()) return;
            if (!nextPath.equals("")) set(current.asMilliList(), Integer.parseInt(keyOrIndex), nextPath, value);
            else current.asMilliList().update(Integer.parseInt(keyOrIndex), value);
        } else {
            if (!current.isMilliMap()) {
                document.setContent(new MilliMap());
                current = document.getContent();
            }
            if (!nextPath.equals("")) set(current.asMilliMap(), keyOrIndex, nextPath, value);
            else current.asMilliMap().put(keyOrIndex, value);
        }
    }

    //Used only for nested path
    private static void set(@Nonnull MilliMap parent, @Nonnull String currentKey, @Nonnull String path, @Nonnull MilliData value) {
        //No checking parameters validation
        MilliData current = parent.get(currentKey);
        String keyOrIndex = path.split("/")[0];
        String nextPath = path.replaceFirst(keyOrIndex, "");
        if (nextPath.startsWith("/")) nextPath = nextPath.replaceFirst("/", "");
        if (keyOrIndex.startsWith("0") && path.matches("[0-9]+")) {
            if (!current.isMilliList()) return;
            if (!nextPath.equals("")) set(current.asMilliList(), Integer.parseInt(keyOrIndex), nextPath, value);
            else current.asMilliList().update(Integer.parseInt(keyOrIndex), value);
        } else {
            if (!current.isMilliMap()) {
                parent.put(currentKey, new MilliMap());
                current = parent.get(currentKey);
            }
            if (!nextPath.equals("")) set(current.asMilliMap(), keyOrIndex, nextPath, value);
            else current.asMilliMap().put(keyOrIndex, value);
        }
    }

    //Used only for nested path
    private static void set(@Nonnull MilliList parent, int currentIndex, @Nonnull String path, @Nonnull MilliData value) {
        //No checking parameters validation
        MilliData current = parent.get(currentIndex);
        String keyOrIndex = path.split("/")[0];
        String nextPath = path.replaceFirst(keyOrIndex, "");
        if (nextPath.startsWith("/")) nextPath = nextPath.replaceFirst("/", "");
        if (keyOrIndex.startsWith("0") && path.matches("[0-9]+")) {
            if (!current.isMilliList()) return;
            if (!nextPath.equals("")) set(current.asMilliList(), Integer.parseInt(keyOrIndex), nextPath, value);
            else current.asMilliList().update(Integer.parseInt(keyOrIndex), value);
        } else {
            if (!current.isMilliMap()) {
                parent.update(currentIndex, new MilliMap());
                current = parent.get(currentIndex);
            }
            if (!nextPath.equals("")) set(current.asMilliMap(), keyOrIndex, nextPath, value);
            else current.asMilliMap().put(keyOrIndex, value);
        }
    }

    /**
     * Delete if exists
     */
    public static boolean delete(@Nonnull String path) {
        if (path.endsWith(".mll")) {
            MilliDocument document = getDocument(path);
            if (document == null) return false;
            document.delete();
        } else {
            MilliCollection collection = getCollection(path);
            if (collection == null) return false;
            collection.delete();
        }
        return true;
    }
}
