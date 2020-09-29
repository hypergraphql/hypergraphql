package org.hypergraphql.util;

import org.apache.commons.io.FilenameUtils;

public abstract class PathUtils {

    private static final String S3_REGEX = "(?i)^https?://s3.*\\.amazonaws\\.com/.*";
    private static final String NORMAL_URL_REGEX = "(?i)^https?://.*";
    private static final String FILE_URL_REGEX = "^file://.*";

    public static String makeAbsolute(final String generalPath, final String path) {

        if (isAbsolute(path)) { // FQ URL or absolute path
            return path;
        } else {
            final String parentPath = FilenameUtils.getFullPath(generalPath);
            return parentPath + (parentPath.endsWith("/") ? "" : "/") + path;
        }
    }

    public static boolean hasProtocol(final String path) {
        return isS3(path) || isNormalURL(path) || isFileUrl(path);
    }

    public static boolean isAbsolute(final String path) {

        return isS3(path) || isNormalURL(path) || isFileUrl(path) || path.startsWith("/");
    }

    public static boolean isS3(final String path) {
        return path.matches(S3_REGEX);
    }

    public static boolean isNormalURL(final String path) {
        return path.matches(NORMAL_URL_REGEX);
    }

    public static boolean isFileUrl(final String path) {
        return path.matches(FILE_URL_REGEX);
    }
}
