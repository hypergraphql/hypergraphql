package org.hypergraphql.util;

import org.apache.commons.io.FilenameUtils;

public class PathUtils {

    private static final String S3_REGEX = "(?i)^https?://s3.*\\.amazonaws\\.com/.*";
    private static final String NORMAL_URL_REGEX = "(?i)^https?://.*";
    private static final String FILE_URL_REGEX = "^file://.*";

    public static String makeAbsolute(final String absolutePath, final String possiblyRelativePath) {

        if(isAbsolute(possiblyRelativePath)) { // FQ URL or absolute path
            return possiblyRelativePath;
        } else if(isAbsolute(absolutePath)) { // relative
            final String parentPath = FilenameUtils.getFullPath(absolutePath);
            return parentPath + (parentPath.endsWith("/") ? "" : "/") + possiblyRelativePath;
        }
        return possiblyRelativePath.startsWith("./") ? "" : "./" + possiblyRelativePath;
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
